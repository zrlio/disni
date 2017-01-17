/*
 * jVerbs: RDMA verbs support for the Java Virtual Machine
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
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Nvmef {

	private final NvmeNamespace namespace;
	private final NvmeQueuePair queuePair;

	private final ThreadLocalRandom random;

	Nvmef(String address, String port, String subsystemNQN) throws IOException {
		Nvme nvme = new Nvme();
		NvmeTransportId tid = new NvmeTransportId(NvmeTransportType.RDMA, NvmfAddressFamily.IPV4, "10.40.0.17", "4420",
				"nqn.2014-08.org.nvmexpress.discovery");
		ArrayList<NvmeController> controllers = new ArrayList<NvmeController>();
		nvme.probe(tid, controllers);
		NvmeController controller = controllers.get(0);
		namespace = controller.getNamespace(1);
		queuePair = controller.allocQueuePair();

		random = ThreadLocalRandom.current();
	}

	enum AccessPattern {
		SEQUENTIAL,
		RANDOM,
		SAME
	}

	long run(long iterations, int queueDepth, int transferSize, AccessPattern accessPattern, boolean write) throws IOException {
		IOCompletion completions[] = new IOCompletion[queueDepth];
		ByteBuffer buffer = ByteBuffer.allocateDirect(transferSize);
		byte bytes[] = new byte[buffer.capacity()];
		random.nextBytes(bytes);
		buffer.put(bytes);

		int sectorCount = transferSize / namespace.getSectorSize();

		long start = System.nanoTime();
		for (long j = 0; j < iterations; ) {
			for (int i = 0; i < completions.length; i++) {
				boolean post = false;
				if (completions[i] == null) {
					post = true;
				} else {
					if (completions[i].done()) {
						post = true;
					}
				}

				if (post) {
					long lba = 0;
					switch (accessPattern) {
						case SEQUENTIAL:
							lba = j * sectorCount;
							break;
						case RANDOM:
							lba = random.nextLong(namespace.getSize() / namespace.getSectorSize());
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
					j++;
				}
			}
			while (queuePair.processCompletions(completions.length) == 0);
		}
		long end = System.nanoTime();
		return (end - start)/iterations;
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.out.println("<address> <port> <subsystemNQN>");
			System.exit(-1);
		}
		Nvmef nvmef = new Nvmef(args[0], args[1], args[2]);

		final int maxTransferSize = 128*1024;
		//Write whole device once
		nvmef.run(nvmef.namespace.getSize()/maxTransferSize, 64, maxTransferSize, AccessPattern.SEQUENTIAL, true);

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
	}
}
