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
import com.ibm.disni.nvmef.spdk.IOCompletion;
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

	public long run(long iterations, int queueDepth, int transferSize, AccessPattern accessPattern, boolean write) throws IOException{
		IOCompletion completions[] = new IOCompletion[queueDepth];
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
	
	public long run2(long iterations, int queueDepth, int transferSize, AccessPattern accessPattern, boolean write) throws Exception{
		ByteBuffer buffer = ByteBuffer.allocateDirect(transferSize);
		buffer.clear();
		int sectorCount = transferSize / endpoint.getSectorSize();
		
		long start = System.nanoTime();
		long lba = random.nextLong(endpoint.getNamespaceSize() / endpoint.getSectorSize());
		IOCompletion completion = endpoint.read(buffer, lba);
		while(!completion.done()){
			int res = endpoint.processCompletions(queueDepth);
			while (res == 0){
				res = endpoint.processCompletions(1);
			}
		}		
		for (long i = 0; i < iterations; i++) {
//			lba = random.nextLong(endpoint.getNamespaceSize() / endpoint.getSectorSize());
			completion.execute(lba);
			while(!completion.done()){
				int res = endpoint.processCompletions(queueDepth);
				while (res == 0){
					res = endpoint.processCompletions(1);
				}
			}
		}
		long end = System.nanoTime();
		return (end - start)/iterations/1000;
	}

	public static void main(String[] args) throws Exception{
		if (args.length < 3) {
			System.out.println("<address> <port> <subsystemNQN>");
			System.exit(-1);
		}
		System.out.println("Starting NvmfEndpointClient, address " + args[0] + ", port " + args[1]);
		NvmfEndpointClient client = new NvmfEndpointClient(args[0], args[1], args[2]);
		
		final int maxTransferSize = client.getMaxIOTransferSize();
		
		//Warmup
		client.run(1000, 1, 512, AccessPattern.RANDOM, false);
		client.run(1000, 1, 512, AccessPattern.RANDOM, true);

		System.out.println("Latency - QD = 1, Size = 512byte");
		int iterations = 10000;
		System.out.println("Read latency (random) = " +
				client.run2(iterations, 1, 512, AccessPattern.RANDOM, false) + "us");
//		System.out.println("Write latency (random) = " +
//				client.run(iterations, 1, 512, AccessPattern.RANDOM, true) + "ns");

//		final int queueDepth = 64;
//		iterations = 100000;
//
//		System.out.println("Throughput - QD = " + queueDepth + ", Size = 128KiB");
//		System.out.println("Read throughput (sequential) = " + maxTransferSize * 1000 / client.run(iterations, queueDepth, maxTransferSize, AccessPattern.SEQUENTIAL, false) +
//				"MB/s");		
	}
}
