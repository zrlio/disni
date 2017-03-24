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
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import com.ibm.disni.nvmef.NvmeCommand;
import com.ibm.disni.nvmef.NvmeEndpoint;
import com.ibm.disni.nvmef.NvmeEndpointGroup;
import com.ibm.disni.nvmef.spdk.NvmeTransportId;
import com.ibm.disni.nvmef.spdk.NvmeTransportType;
import sun.nio.ch.DirectBuffer;

public class NvmfEndpointClient extends NvmfClientBenchmark {
	private final ThreadLocalRandom random;
	private NvmeEndpointGroup group;
	private NvmeEndpoint endpoint;
	
	public NvmfEndpointClient() {
		this.random = ThreadLocalRandom.current();
	}

	public long run(long iterations, int queueDepth, int transferSize, AccessPattern accessPattern, boolean write) throws IOException {
		ByteBuffer buffer = group.allocateBuffer(transferSize, 4096);
		byte bytes[] = new byte[buffer.capacity()];
		random.nextBytes(bytes);
		buffer.put(bytes);
		buffer.clear();

		NvmeCommand commands[] = new NvmeCommand[queueDepth];
		long[] processed = new long[queueDepth];
		for (int i = 0; i < commands.length; i++) {
			commands[i] = endpoint.newCommand();
			commands[i].setBuffer(buffer);
			commands[i].setId(i);
			processed[i] = i;
		}

		final int sectorSize = endpoint.getSectorSize();
		final int sectorCount = transferSize / sectorSize;
		final long totalSizeSector = endpoint.getNamespaceSize() / sectorSize;

		long start = System.nanoTime();
		long posted = 0;
		// start at random offset
		long lba = random.nextLong(totalSizeSector - sectorCount);
		// align to transfer size sector count
		lba -= lba % sectorCount;
		long lastNumProcessed = queueDepth;
		for (long completed = 0; completed < iterations; completed += lastNumProcessed) {
			for (int i = 0; i < lastNumProcessed && posted < iterations; i++) {
				NvmeCommand command = commands[(int)processed[i]];
				if (command.isPending() && !command.isDone()) {
					throw new IOException("Was completed but is not done??");
				}
				command.setLinearBlockAddress(lba);
				if (write) {
					command.write();
				} else {
					command.read();
				}
				command.execute();
				switch (accessPattern) {
					case SEQUENTIAL:
						lba += sectorCount;
						lba = lba % (totalSizeSector - sectorCount);
						break;
					case RANDOM:
						lba = random.nextLong(totalSizeSector - sectorCount);
						break;
				}
				lba -= lba % sectorCount;
				posted++;
			}
			do {
				lastNumProcessed = endpoint.processCompletions(processed);
			} while (lastNumProcessed == 0);
		}
		long end = System.nanoTime();
		group.freeBuffer(buffer);
		return end - start;
	}

	void connect(NvmeTransportId tid) throws IOException {
		this.group = new NvmeEndpointGroup(new NvmeTransportType[]{tid.getType()}, "/dev/hugepages",
				new long[]{256,256});
		this.endpoint = group.createEndpoint();

		URI url;
		try {
			if (tid.getType() == NvmeTransportType.RDMA){
				url = new URI("nvmef://" + tid.getAddress() + ":" + tid.getServiceId() + "/0/1?subsystem=" + tid.getSubsystemNQN());
			} else {
				url = new URI("nvmef://127.0.0.1:7777?pci=" + tid.getAddress());
			}
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
		endpoint.connect(url);
	}

	void close() throws IOException {
		try {
			endpoint.close();
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}

	public static void main(String[] args) throws IOException {
		new NvmfEndpointClient().start(args);
	}
}
