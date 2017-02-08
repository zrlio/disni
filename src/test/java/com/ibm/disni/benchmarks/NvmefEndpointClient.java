package com.ibm.disni.benchmarks;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import com.ibm.disni.benchmarks.NvmefClient.AccessPattern;
import com.ibm.disni.nvmef.NvmeEndpoint;
import com.ibm.disni.nvmef.NvmeEndpointGroup;
import com.ibm.disni.nvmef.spdk.IOCompletion;

public class NvmefEndpointClient {
	private final ThreadLocalRandom random;
	private NvmeEndpointGroup group;
	private NvmeEndpoint endpoint;
	
	public NvmefEndpointClient(String address, String port, String subsystem) throws Exception {
		this.random = ThreadLocalRandom.current();
		this.group = new NvmeEndpointGroup();
		this.endpoint = group.createEndpoint();
		URI url = new URI("nvmef://" + address + ":" + port + "/0/1");
//		URI url = new URI("nvmef", address, port, "/0/1");
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

	public static void main(String[] args) throws Exception{
		if (args.length < 3) {
			System.out.println("<address> <port> <subsystemNQN>");
			System.exit(-1);
		}
		System.out.println("Starting NvmefEndpointClient, address " + args[0] + ", port " + args[1]);
		NvmefEndpointClient client = new NvmefEndpointClient(args[0], args[1], args[2]);
		
		final int maxTransferSize = client.getMaxIOTransferSize();
		
		//Warmup
		client.run(1000, 1, 512, AccessPattern.RANDOM, false);
		client.run(1000, 1, 512, AccessPattern.RANDOM, true);

		System.out.println("Latency - QD = 1, Size = 512byte");
		int iterations = 10000;
		System.out.println("Read latency (random) = " +
				client.run(iterations, 1, 512, AccessPattern.RANDOM, false) + "ns");
		System.out.println("Write latency (random) = " +
				client.run(iterations, 1, 512, AccessPattern.RANDOM, true) + "ns");

		final int queueDepth = 64;
		iterations = 100000;

		System.out.println("Throughput - QD = " + queueDepth + ", Size = 128KiB");
		System.out.println("Read throughput (sequential) = " + maxTransferSize * 1000 / client.run(iterations, queueDepth, maxTransferSize, AccessPattern.SEQUENTIAL, false) +
				"MB/s");		
	}
}
