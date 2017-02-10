package com.ibm.disni.benchmarks;

import java.io.IOException;
import java.net.URI;
import com.ibm.disni.nvmef.NvmeEndpoint;
import com.ibm.disni.nvmef.NvmeEndpointGroup;
import com.ibm.disni.nvmef.NvmeServerEndpoint;

public class NvmefEndpointServer {
	private NvmeEndpointGroup group;
	private NvmeServerEndpoint serverEndpoint;
	
	public NvmefEndpointServer(String address, String port, String subsystem, String pci) throws Exception {
		this.group = new NvmeEndpointGroup();
		this.serverEndpoint = group.createServerEndpoint();
		URI url = new URI("nvmef://" + address + ":" + port + "/0/1?subsystem=" + subsystem + "&pci=" + pci);
		serverEndpoint.bind(url);
	}

	public void run() throws IOException{
		NvmeEndpoint endpoint = serverEndpoint.accept();
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
		
		NvmefEndpointServer server = new NvmefEndpointServer(address, port, subsystem, pci);
		server.run();
	}
}
