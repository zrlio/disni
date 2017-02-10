package com.ibm.disni.benchmarks;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ThreadLocalRandom;
import com.ibm.disni.benchmarks.NvmefClient.AccessPattern;
import com.ibm.disni.nvmef.NvmeEndpointGroup;
import com.ibm.disni.nvmef.NvmeServerEndpoint;

public class NvmefEndpointServer {
	private NvmeEndpointGroup group;
	private NvmeServerEndpoint endpoint;
	
	public NvmefEndpointServer(String address, String port, String subsystem, String pci) throws Exception {
		this.group = new NvmeEndpointGroup();
		this.endpoint = group.createServerEndpoint();
		URI url = new URI("nvmef://" + address + ":" + port + "/0/1?subsystem=" + subsystem + "&pci=" + pci);
		endpoint.bind(url);
	}

	public void run() throws IOException{
	}

	public static void main(String[] args) throws Exception{
		if (args.length < 4) {
			System.out.println("<pci-address> <subsystemNQN> <address> <port>");
			System.exit(-1);
		}

		String pci = args[0];
		String subsystem = args[1];
		String address = args[2];
		String port = args[3];
		
		NvmefEndpointServer server = new NvmefEndpointServer(pci, subsystem, address, port);
		server.run();
	}
}
