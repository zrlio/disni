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

public class NvmfClient {
	private final ArrayList<NvmeController> controllers;
	private final NvmeNamespace namespace;
	private final NvmeQueuePair queuePair;
	private final Nvme nvme;

	private final ThreadLocalRandom random;

	NvmfClient(NvmeTransportId tid) throws IOException {
		nvme = new Nvme(new NvmeTransportType[]{tid.getType()}, "/dev/hugepages", new long[]{256,256});
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
		IOCompletion completions[] = new IOCompletion[queueDepth];
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
				if (completions[i] == null) {
					post = true;
				} else if (completions[i].done()) {
					completed++;
					completions[i] = null;
					post = true;
				}

				if (post && posted < iterations) {
					if (write) {
						completions[i] = namespace.write(queuePair, bufferAddress, lba, sectorCount);
					} else {
						completions[i] = namespace.read(queuePair, bufferAddress, lba, sectorCount);
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
					posted++;
				}
			}
			while(completed < iterations && queuePair.processCompletions(completions.length) == 0);
		}
		long end = System.nanoTime();
		nvme.freeBuffer(buffer);
		return end - start;
	}

	public static void main(String[] args) throws IOException {

		Options options = new Options();
		Option address = Option.builder("a").required().desc("ip address or PCIe address").hasArg().build();
		Option port = Option.builder("p").desc("port").hasArg().build();
		Option subsystemNQN = Option.builder("nqn").desc("subsystem NVMe qualified name").hasArg().build();
		Option iterations = Option.builder("i").required().desc("iterations").hasArg().type(Number.class).build();
		Option queueDepth = Option.builder("qd").required().desc("queue depth").hasArg().type(Number.class).build();
		Option size = Option.builder("s").required().desc("size (bytes)").hasArg().type(Number.class).build();
		Option accessPattern = Option.builder("m").required().desc("access pattern: rand/seq/same").hasArg().build();
		Option readWrite = Option.builder("rw").required().desc("read/write").hasArg().build();

		options.addOption(address);
		options.addOption(port);
		options.addOption(subsystemNQN);
		options.addOption(iterations);
		options.addOption(queueDepth);
		options.addOption(size);
		options.addOption(accessPattern);
		options.addOption(readWrite);

		CommandLineParser parser = new DefaultParser();
		CommandLine line = null;
		HelpFormatter formatter = new HelpFormatter();
		int iterationsValue = 0;
		int queueDepthValue = 0;
		int sizeValue = 0;
		try {
			line = parser.parse(options, args);
			iterationsValue = ((Number)line.getParsedOptionValue("i")).intValue();
			queueDepthValue = ((Number)line.getParsedOptionValue("qd")).intValue();
			sizeValue = ((Number)line.getParsedOptionValue("s")).intValue();
		} catch (ParseException e) {
			formatter.printHelp("nvmef", options);
			System.exit(-1);
		}

		boolean isRDMA = line.hasOption("p") && line.hasOption("nqn");
		NvmeTransportId transportId;
		if (isRDMA) {
			transportId = NvmeTransportId.rdma(NvmfAddressFamily.IPV4, line.getOptionValue("a"),
					line.getOptionValue("p"), line.getOptionValue("nqn"));
		} else {
			transportId = NvmeTransportId.pcie(line.getOptionValue("a"));
		}

		AccessPattern accessPatternValue = AccessPattern.valueOf(line.getOptionValue("m"));
		String str = line.getOptionValue("rw");
		boolean write = false;
		if (str.compareTo("write") == 0) {
			write = true;
		}

		NvmfClient nvmef = new NvmfClient(transportId);

		long time = nvmef.run(iterationsValue, queueDepthValue, sizeValue, accessPatternValue, write);

		System.out.println(write ? "wrote" : "read" + " " + sizeValue + "bytes with QD = " + queueDepthValue +
				", iterations = " + iterationsValue + ", pattern = " + accessPatternValue.name());
		double timeUs = time / 1000.0;
		System.out.println("Latency = " + timeUs / iterationsValue + "us");
		double iops = (double)iterationsValue * 1000 * 1000 * 1000 / time;
		System.out.println("IOPS = " + iops);
		System.out.println("MB/s = " + iops * sizeValue / 1024.0 / 1024.0);

		nvmef.close();
	}
}
