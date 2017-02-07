package com.ibm.disni.benchmarks;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;
import com.ibm.disni.benchmarks.Nvmef.AccessPattern;
import com.ibm.disni.nvmef.NvmeEndpoint;
import com.ibm.disni.nvmef.NvmeEndpointGroup;
import com.ibm.disni.nvmef.spdk.IOCompletion;

public class NvmefEndpointClient {
	private String address;
	private String port;
	private String subsystem;
	private final ThreadLocalRandom random;

	
	public NvmefEndpointClient(String address, String port, String subsystem) {
		this.address = address;
		this.port = port;
		this.subsystem = subsystem;
		this.random = ThreadLocalRandom.current();
	}
	
	public void run(long iterations, int queueDepth, int transferSize, AccessPattern accessPattern, boolean write) throws IOException{
		NvmeEndpointGroup group = new NvmeEndpointGroup();
		NvmeEndpoint endpoint = group.createEndpoint();
		endpoint.connect(address, port, 0, 1);
		
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
							lba = random.nextLong(endpoint.getSize() / endpoint.getSectorSize());
							break;
						case SAME:
							lba = 1024;
							break;
					}
					if (write) {
						completions[i] = endpoint.write(buffer, lba, sectorCount);
					} else {
						completions[i] = endpoint.read(buffer, lba, sectorCount);
					}
					posted++;
				}
			}
			while(completed < iterations && endpoint.processCompletions(completions.length) == 0);
		}
		long end = System.nanoTime();
	}

	public static void main(String[] args){
		if (args.length < 3) {
			System.out.println("<address> <port> <subsystemNQN>");
			System.exit(-1);
		}
		NvmefEndpointClient client = new NvmefEndpointClient(args[0], args[1], args[2]);
		
		try {
			int iterations = 10000;
			client.run(iterations, 1, 512, AccessPattern.RANDOM, false);
			client.run(iterations, 1, 512, AccessPattern.RANDOM, true);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
