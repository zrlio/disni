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

import com.ibm.disni.nvmef.NvmeEndpoint;
import com.ibm.disni.nvmef.NvmeEndpointGroup;
import com.ibm.disni.nvmef.NvmeServerEndpoint;
import com.ibm.disni.nvmef.spdk.NvmeTransportType;

public class NvmfEndpointServer {
	private NvmeEndpointGroup group;
	private NvmeServerEndpoint serverEndpoint;
	
	public NvmfEndpointServer(String address, String port, String subsystem, String pci) throws Exception {
		this.group = new NvmeEndpointGroup(new NvmeTransportType[]{NvmeTransportType.PCIE, NvmeTransportType.RDMA},
				"/dev/hugepages", new long[]{256,256});
		this.serverEndpoint = group.createServerEndpoint();
		URI url = new URI("nvmef://" + address + ":" + port + "/0/1?subsystem=" + subsystem + "&pci=" + pci);
		serverEndpoint.bind(url);
	}

	public void run() throws IOException{
		while(true){
			NvmeEndpoint endpoint = serverEndpoint.accept();
		}
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
		
		NvmfEndpointServer server = new NvmfEndpointServer(address, port, subsystem, pci);
		server.run();
	}
}
