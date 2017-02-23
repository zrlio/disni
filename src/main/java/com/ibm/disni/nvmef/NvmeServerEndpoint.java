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

package com.ibm.disni.nvmef;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingQueue;

import com.ibm.disni.nvmef.spdk.*;

public class NvmeServerEndpoint {
	private NvmeEndpointGroup group;
	private NvmfTarget target;
	private NvmfSubsystem nvmesubsystem;
	private ArrayList<NvmfConnection> currentConnects;
	private LinkedBlockingQueue<NvmfConnection> establishedConnects;
	private NvmeController nvmecontroller;
	
	
	public NvmeServerEndpoint(NvmeEndpointGroup group){
		this.group = group;
		this.target = null;
		this.nvmesubsystem = null;
		this.currentConnects = new ArrayList<NvmfConnection>();
		this.establishedConnects = new LinkedBlockingQueue<NvmfConnection>();
	}
	
	public synchronized NvmeServerEndpoint bind(URI uri) throws Exception {
		NvmeResourceIdentifier nvmeResource = NvmeResourceIdentifier.parse(uri);
		NvmeTransportId transportId = NvmeTransportId.pcie(nvmeResource.getPci());
		nvmecontroller = group.probe(transportId, nvmeResource.getController());
		this.target = group.createNvmfTarget();
		this.nvmesubsystem = target.createSubsystem(nvmeResource.getSubsystem(), NvmfSubtype.NVME, NvmfSubsystemMode.DIRECT);
		nvmesubsystem.addController(nvmecontroller, nvmeResource.getPci());
		nvmesubsystem.addListener(NvmfTransportName.RDMA, nvmeResource.getAddress(), nvmeResource.getPort());		
		
		return this;
	}
	
	public NvmeEndpoint accept() throws IOException {
		currentConnects.clear();
		while (establishedConnects.isEmpty()) {
			target.poll();
			nvmesubsystem.poll(currentConnects);
			if (!currentConnects.isEmpty()){
				for (int i = 0; i < currentConnects.size(); i++){
					this.establishedConnects.add(currentConnects.get(i));
				}
			}
		}		
		NvmfConnection newConnection = establishedConnects.poll();
		return group.createEndpoint(newConnection);
	}
	
	public void pollSubsystem(){
		currentConnects.clear();
		this.nvmesubsystem.poll(currentConnects);
		if (!currentConnects.isEmpty()){
			for (int i = 0; i < currentConnects.size(); i++){
				this.establishedConnects.add(currentConnects.get(i));
			}
		}		
	}
	
	public synchronized void close() throws Exception {
		target.fini();
		nvmesubsystem.delete();
	}

	public NvmeController getNvmecontroller() {
		return nvmecontroller;
	}
}
