/*
 * DiSNI: Direct Storage and Networking Interface
 *
 * Author: Jonas Pfefferle <jpf@zurich.ibm.com>
 *         Patrick Stuedi  <stu@zurich.ibm.com>
 *
 * Copyright (C) 2016, IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.ibm.disni.benchmarks;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import com.ibm.disni.benchmarks.NvmfClient.AccessPattern;
import com.ibm.disni.nvmef.NvmeEndpoint;
import com.ibm.disni.nvmef.NvmeEndpointGroup;
import com.ibm.disni.nvmef.spdk.NvmfOperation;
import com.ibm.disni.nvmef.spdk.NvmeTransportId;
import com.ibm.disni.nvmef.spdk.NvmeTransportType;
import com.ibm.disni.nvmef.spdk.NvmfAddressFamily;

public class NvmfEndpointClient {
	private final ThreadLocalRandom random;
	private NvmeEndpointGroup group;
	private NvmeEndpoint endpoint;
	
	public NvmfEndpointClient(String address, String port, String subsystem) throws Exception {
		this.random = ThreadLocalRandom.current();
		NvmeTransportId tid = NvmeTransportId.rdma(NvmfAddressFamily.IPV4, address, port, subsystem);
		this.group = new NvmeEndpointGroup(new NvmeTransportType[]{tid.getType()}, "/dev/hugepages",
				new long[]{256,256});
		this.endpoint = group.createEndpoint();
		URI url = new URI("nvmef://" + address + ":" + port + "/0/1?subsystem=" + subsystem);
		endpoint.connect(url);
	}
	
	public int getMaxIOTransferSize() {
		return endpoint.getMaxTransferSize();
	}

	public long benchmark(long iterations, int queueDepth, int transferSize, AccessPattern accessPattern, boolean write) throws IOException{
		NvmfOperation completions[] = new NvmfOperation[queueDepth];
		ByteBuffer buffer = ByteBuffer.allocateDirect(transferSize);
		byte bytes[] = new byte[buffer.capacity()];
		random.nextBytes(bytes);
		buffer.put(bytes);

		int sectorCount = transferSize / endpoint.getSectorSize();

		long start = System.nanoTime();
		long posted = 0;
		for (long completed = 0; completed < iterations; ) {
			for (int i = 0; i < completions.length; i++) {
				boolean post = false;
				if (completions[i] == null) {
					post = true;
				} else if (completions[i].done()) {
					completed++;
					completions[i] = null;
					post = true;
				}

				if (post && posted < iterations) {
					long lba = 0;
					switch (accessPattern) {
						case SEQUENTIAL:
							lba = posted * sectorCount;
							break;
						case RANDOM:
							lba = random.nextLong(endpoint.getNamespaceSize() / endpoint.getSectorSize());
							break;
						case SAME:
							lba = 1024;
							break;
					}
					if (write) {
						completions[i] = endpoint.write(buffer, lba);
					} else {
						completions[i] = endpoint.read(buffer, lba);
					}
					posted++;
				}
			}
			while(completed < iterations && endpoint.processCompletions(completions.length) == 0);
		}
		long end = System.nanoTime();
		return (end - start)/iterations;
	}
	
	public long latency(long iterations, int queueDepth, int transferSize, AccessPattern accessPattern, boolean write) throws Exception{
		ByteBuffer buffer = ByteBuffer.allocateDirect(transferSize);
		buffer.clear();
		
		NvmfOperation completion = endpoint.read(buffer, 0);
		long start = System.nanoTime();
		for (long i = 0; i < iterations; i++) {
			long lba = random.nextLong(endpoint.getNamespaceSize() / endpoint.getSectorSize());
			completion.setLba(lba);
			completion.execute();
			while(!completion.done()){
				int res = endpoint.processCompletions(queueDepth);
				while (res == 0){
					res = endpoint.processCompletions(queueDepth);
				}
			}
		}
		long end = System.nanoTime();
		completion.free();
		return (end - start)/iterations/1000;
	}
	
	public long verify(long iterations, int queueDepth, int transferSize, AccessPattern accessPattern, boolean write) throws Exception{
		ByteBuffer buffer = ByteBuffer.allocateDirect(transferSize);
		buffer.clear();
		int sectorCount = transferSize / endpoint.getSectorSize();
		
		long start = System.nanoTime();
		long lbas[] = new long[(int) iterations];
		int front[] = new int[lbas.length];
		int back[] = new int[lbas.length];
		for (int i = 0; i < lbas.length; i++){
			lbas[i] = random.nextLong(endpoint.getNamespaceSize() / endpoint.getSectorSize());
			front[i] = random.nextInt(Integer.MAX_VALUE);
			back[i] = random.nextInt(Integer.MAX_VALUE);
		}
		
		NvmfOperation writeCompletion = endpoint.write(buffer, 0);
		for (int i = 0; i < iterations; i++) {
			long lba = lbas[i];
			buffer.putInt(0, front[i]);
			buffer.putInt(transferSize - 4, back[i]);
			writeCompletion.setLba(lba);
			writeCompletion.execute();
			while(!writeCompletion.done()){
				int res = endpoint.processCompletions(1);
				while (res == 0){
					res = endpoint.processCompletions(1);
				}
			}
		}
		writeCompletion.free();
		
		NvmfOperation readCompletion = endpoint.read(buffer, 0);
		for (int i = 0; i < iterations; i++) {
			long lba = lbas[i];
			readCompletion.setLba(lba);
			readCompletion.execute();
			while(!readCompletion.done()){
				int res = endpoint.processCompletions(1);
				while (res == 0){
					res = endpoint.processCompletions(1);
				}
			}
			int _front = buffer.getInt(0);
			int _back = buffer.getInt(transferSize -4);
			System.out.println("i " + i + ", front " + front[i] + ", _front " + _front + ", back " + back[i] + ", _back " + _back);
		}
		readCompletion.free();
		long end = System.nanoTime();
		return (end - start)/iterations/1000;
	}	

	public static void main(String[] args) throws Exception{
		if (args.length < 3) {
			System.out.println("<address> <port> <subsystemNQN>");
			System.exit(-1);
		}
		System.out.println("Starting NvmfEndpointClient, v3, address " + args[0] + ", port " + args[1]);
		NvmfEndpointClient client = new NvmfEndpointClient(args[0], args[1], args[2]);
		
		int iterations = 100;
		client.verify(iterations, 1, 512, AccessPattern.RANDOM, false);
		iterations = 100000;
		System.out.println("Read latency 512 " + client.latency(iterations, 1, 512, AccessPattern.RANDOM, false) + "us");
		System.out.println("Read latency 512 " + client.latency(iterations, 1, 512, AccessPattern.RANDOM, false) + "us");
		System.out.println("Read latency 4K  " + client.latency(iterations, 1, 4096, AccessPattern.RANDOM, false) + "us");
		System.out.println("Read latency 4K  " + client.latency(iterations, 1, 4096, AccessPattern.RANDOM, false) + "us");
	}
}
