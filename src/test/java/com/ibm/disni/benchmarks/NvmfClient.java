/*
 * DiSNI: Direct Storage and Networking Interface
 *
 * Author: Jonas Pfefferle <jpf@zurich.ibm.com>
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

import com.ibm.disni.nvmef.spdk.*;
import sun.nio.ch.DirectBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class NvmfClient {
	private final ArrayList<NvmeController> controllers;
	private final NvmeNamespace namespace;
	private final NvmeQueuePair queuePair;

	private final ThreadLocalRandom random;

	NvmfClient(NvmeTransportId tid) throws IOException {
		Nvme nvme = new Nvme(new NvmeTransportType[]{tid.getType()}, "/dev/hugepages", new long[]{256,256});
		controllers = new ArrayList<NvmeController>();
		nvme.probe(tid, controllers);
		NvmeController controller = controllers.get(0);
		namespace = controller.getNamespace(1);
		queuePair = controller.allocQueuePair();

		random = ThreadLocalRandom.current();
	}

	void close() throws IOException {
		// Freeing the queue pair is not really necessary as it is freed when detaching the controller
		queuePair.free();
		for (NvmeController controller : controllers) {
			controller.detach();
		}
	}

	enum AccessPattern {
		SEQUENTIAL,
		RANDOM,
		SAME
	}

	long run(long iterations, int queueDepth, int transferSize, AccessPattern accessPattern, boolean write) throws IOException {
		NvmfOperation completions[] = new NvmfOperation[queueDepth];
		ByteBuffer buffer = ByteBuffer.allocateDirect(transferSize);
		byte bytes[] = new byte[buffer.capacity()];
		random.nextBytes(bytes);
		buffer.put(bytes);

		final int sectorCount = transferSize / namespace.getSectorSize();
		final long totalSizeSector = namespace.getSize() / namespace.getSectorSize();

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
							lba = random.nextLong(totalSizeSector - sectorCount);
							break;
						case SAME:
							lba = 1024;
							break;
					}
					if (write) {
						completions[i] = namespace.write(queuePair, ((DirectBuffer)buffer).address(), lba, sectorCount);
					} else {
						completions[i] = namespace.read(queuePair, ((DirectBuffer)buffer).address(), lba, sectorCount);
					}
					posted++;
				}
			}
			while(completed < iterations && queuePair.processCompletions(completions.length) == 0);
		}
		long end = System.nanoTime();
		return (end - start)/iterations;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 3 && args.length != 1) {
			System.out.println("<address> [<port> <subsystemNQN>]");
			System.exit(-1);
		}

		NvmeTransportId transportId;
		if (args.length == 3) {
			transportId = NvmeTransportId.rdma(NvmfAddressFamily.IPV4, args[0], args[1], args[2]);
		} else {
			transportId = NvmeTransportId.pcie(args[0]);
		}

		NvmfClient nvmef = new NvmfClient(transportId);

		final int maxTransferSize = nvmef.namespace.getMaxIOTransferSize();
		//Write whole device once
		//nvmef.run(nvmef.namespace.getSize()/maxTransferSize, 64, maxTransferSize, AccessPattern.SEQUENTIAL, true);

		//Warmup
		nvmef.run(1000, 1, 512, AccessPattern.RANDOM, false);
		nvmef.run(1000, 1, 512, AccessPattern.RANDOM, true);

		System.out.println("Latency - QD = 1, Size = 512byte");
		int iterations = 10000;
		System.out.println("Read latency (random) = " +
				nvmef.run(iterations, 1, 512, AccessPattern.RANDOM, false) + "ns");
		System.out.println("Write latency (random) = " +
				nvmef.run(iterations, 1, 512, AccessPattern.RANDOM, true) + "ns");

		final int queueDepth = 64;
		iterations = 100000;

		System.out.println("Throughput - QD = " + queueDepth + ", Size = 128KiB");
		System.out.println("Read throughput (sequential) = " +
				maxTransferSize * 1000 / nvmef.run(iterations, queueDepth, maxTransferSize, AccessPattern.SEQUENTIAL, false) +
				"MB/s");

		nvmef.close();
	}
}
