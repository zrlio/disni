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
import org.apache.commons.cli.*;
import sun.nio.ch.DirectBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class NvmfClient extends NvmfClientBenchmark {
	private ArrayList<NvmeController> controllers;
	private NvmeNamespace namespace;
	private NvmeQueuePair queuePair;
	private Nvme nvme;

	private final ThreadLocalRandom random;

	NvmfClient() {
		random = ThreadLocalRandom.current();
	}

	void close() throws IOException {
		// Freeing the queue pair is not really necessary as it is freed when detaching the controller
		queuePair.free();
		for (NvmeController controller : controllers) {
			controller.detach();
		}
	}

	long run(long iterations, int queueDepth, int transferSize, AccessPattern accessPattern, boolean write) throws IOException {
		IOCompletion completions[] = new IOCompletion[queueDepth];
		for (int i = 0; i < completions.length; i++) {
			completions[i] = new IOCompletion();
		}
		ByteBuffer buffer = nvme.allocateBuffer(transferSize, 4096);
		byte bytes[] = new byte[buffer.capacity()];
		random.nextBytes(bytes);
		buffer.put(bytes);
		final long bufferAddress = ((DirectBuffer)buffer).address();

		final int sectorSize = namespace.getSectorSize();
		final int sectorCount = transferSize / sectorSize;
		final long totalSizeSector = namespace.getSize() / sectorSize;

		long start = System.nanoTime();
		long posted = 0;
		// start at random offset
		long lba = random.nextLong(totalSizeSector - sectorCount);
		// align to transfer size sector count
		lba -= lba % sectorCount;
		for (long completed = 0; completed < iterations; ) {
			for (int i = 0; i < completions.length; i++) {
				boolean post = false;
				if (!completions[i].isPending()) {
					post = true;
				} else if (completions[i].done()) {
					completed++;
					post = true;
				}

				if (post && posted < iterations) {
					if (write) {
						namespace.write(queuePair, bufferAddress, lba, sectorCount, completions[i]);
					} else {
						namespace.read(queuePair, bufferAddress, lba, sectorCount, completions[i]);
					}
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
			}
			while(completed < iterations && queuePair.processCompletions(completions.length) == 0);
		}
		long end = System.nanoTime();
		nvme.freeBuffer(buffer);
		return end - start;
	}

	void connect(NvmeTransportId tid) throws IOException {
		nvme = new Nvme(new NvmeTransportType[]{tid.getType()}, "/dev/hugepages", new long[]{256,256});
		controllers = new ArrayList<NvmeController>();
		nvme.probe(tid, controllers);
		NvmeController controller = controllers.get(0);
		namespace = controller.getNamespace(1);
		queuePair = controller.allocQueuePair();
	}

	public static void main(String[] args) throws IOException {
		new NvmfClient().start(args);
	}
}
