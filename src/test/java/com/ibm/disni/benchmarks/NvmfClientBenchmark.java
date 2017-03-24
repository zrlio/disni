package com.ibm.disni.benchmarks;

import com.ibm.disni.nvmef.spdk.NvmeTransportId;
import com.ibm.disni.nvmef.spdk.NvmfAddressFamily;
import org.apache.commons.cli.*;

import java.io.IOException;

public abstract class NvmfClientBenchmark {

	enum AccessPattern {
		SEQUENTIAL,
		RANDOM,
		SAME
	}

	abstract long run(long iterations, int queueDepth, int transferSize, AccessPattern accessPattern, boolean write) throws IOException;

	abstract void connect(NvmeTransportId transportId) throws IOException;

	abstract void close() throws IOException;

	void start(String[] args) throws IOException {
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
		connect(transportId);

		AccessPattern accessPatternValue = AccessPattern.valueOf(line.getOptionValue("m"));
		String str = line.getOptionValue("rw");
		boolean write = false;
		if (str.compareTo("write") == 0) {
			write = true;
		}

		long time = run(iterationsValue, queueDepthValue, sizeValue, accessPatternValue, write);

		System.out.println((write ? "wrote" : "read") + " " + sizeValue + "bytes with QD = " + queueDepthValue +
				", iterations = " + iterationsValue + ", pattern = " + accessPatternValue.name());
		System.out.println("------------------------------------------------");
		double timeUs = time / 1000.0;
		System.out.println("Latency = " + timeUs / iterationsValue + "us");
		double iops = (double)iterationsValue * 1000 * 1000 * 1000 / time;
		System.out.println("IOPS = " + iops);
		System.out.println("MB/s = " + iops * sizeValue / 1024.0 / 1024.0);
		System.out.println("------------------------------------------------");

		close();
	}
}
