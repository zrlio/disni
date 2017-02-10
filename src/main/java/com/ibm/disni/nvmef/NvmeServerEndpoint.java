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

package com.ibm.disni.nvmef;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.URI;
import java.util.StringTokenizer;

import com.ibm.disni.nvmef.spdk.NvmeController;
import com.ibm.disni.nvmef.spdk.NvmeTransportId;
import com.ibm.disni.nvmef.spdk.NvmfSubsystem;
import com.ibm.disni.nvmef.spdk.NvmfSubsystemMode;
import com.ibm.disni.nvmef.spdk.NvmfSubtype;
import com.ibm.disni.nvmef.spdk.NvmfTarget;
import com.ibm.disni.nvmef.spdk.NvmfTransportName;

//this class is most likely not used
public class NvmeServerEndpoint {
	private NvmeEndpointGroup group;
	private NvmfTarget target;
	private NvmfSubsystem nvmesubsystem;
	
	public NvmeServerEndpoint(NvmeEndpointGroup group){
		this.group = group;
		this.target = null;
		this.nvmesubsystem = null;
	}
	
	public synchronized NvmeServerEndpoint bind(URI uri) throws Exception {
		if (!uri.getScheme().equalsIgnoreCase("nvmef")){
			throw new IOException("URL has wrong protocol " + uri.getScheme());
		}
		
		System.out.println("uri passed to bind " + uri.toString());
		String address = uri.getHost();
		String port = Integer.toString(uri.getPort());
		int controller = 0;
		String subsystem = "tmp";
		String pci = "tmp2";
		
		String path = uri.getPath();
		if (path != null){
			StringTokenizer pathTokenizer = new StringTokenizer(path, "/");
			if (pathTokenizer.countTokens() > 2){
				throw new IOException("URL format error, too many elements in path");
			}
			for (int i = 0; pathTokenizer.hasMoreTokens(); i++){
				String token = pathTokenizer.nextToken();
				System.out.println("token " + token + ", i " + i);
				switch(i) {
				case 0:
					controller = Integer.parseInt(token);
					break;
				}
			}
		}
		
		String query = uri.getQuery();
		if (query != null){
			StringTokenizer queryTokenizer = new StringTokenizer(query, "&");
			while (queryTokenizer.hasMoreTokens()){
				String param = queryTokenizer.nextToken();
				if (param.startsWith("subsystem")){
					subsystem = param.substring(10);
				}
				if (param.startsWith("pci")){
					pci = param.substring(4);
				}				
			}			
		}
		
		System.out.println("binding to address " + address + ", port " + port + ", subsystem " + subsystem + ", pci " + pci + ", controller " + controller);
		NvmeTransportId transportId = NvmeTransportId.pcie(pci);
		NvmeController nvmecontroller = group.probe(transportId, controller);
		this.target = group.createNvmfTarget();
		this.nvmesubsystem = target.createSubsystem(subsystem, NvmfSubtype.NVME, NvmfSubsystemMode.DIRECT);
		nvmesubsystem.addController(nvmecontroller, pci);
		nvmesubsystem.addListener(NvmfTransportName.RDMA, address, port);		
		
		return this;
	}
	
	public NvmeEndpoint accept() throws IOException {
		while (true) {
			target.poll();
			nvmesubsystem.poll();
		}		
	}
	
	public synchronized void close() throws IOException, InterruptedException {
		
	}
}
