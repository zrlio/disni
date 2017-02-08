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

//this class is most likely not used
public class NvmeServerEndpoint {
	private NvmeEndpointGroup group;
	
	public NvmeServerEndpoint(NvmeEndpointGroup group){
		this.group = group;
	}
	
	public synchronized NvmeServerEndpoint bind(SocketAddress src, int backlog) throws IOException {
		return this;
	}
	
	public NvmeEndpoint accept() throws IOException {
		return null;
	}
	
	public synchronized void close() throws IOException, InterruptedException {
		
	}
}
