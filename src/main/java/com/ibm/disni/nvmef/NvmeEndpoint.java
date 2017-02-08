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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import sun.nio.ch.DirectBuffer;

import com.ibm.disni.nvmef.spdk.IOCompletion;
import com.ibm.disni.nvmef.spdk.NvmeController;
import com.ibm.disni.nvmef.spdk.NvmeNamespace;
import com.ibm.disni.nvmef.spdk.NvmeQueuePair;

public class NvmeEndpoint {
	private NvmeEndpointGroup group;
    private NvmeQueuePair queuePair;
    private NvmeNamespace namespace;
	
	public NvmeEndpoint(NvmeEndpointGroup group){
		this.group = group;
		this.queuePair = null;
		this.namespace = null;
	}
	
	public void connect(String address, String port, int controller, int namespace) throws IOException {
		NvmeController nvmecontroller = group.probe(address, port, controller);
		this.namespace = nvmecontroller.getNamespace(namespace);
		this.queuePair = nvmecontroller.allocQueuePair();		
	}
	
	public IOCompletion write(ByteBuffer buffer, long linearBlockAddress, int count) throws IOException{
		IOCompletion completion = namespace.write(queuePair, ((DirectBuffer) buffer).address(), linearBlockAddress, count);
		return completion;
	}
	
	public IOCompletion read(ByteBuffer buffer, long linearBlockAddress, int count) throws IOException{
		IOCompletion completion = namespace.read(queuePair, ((DirectBuffer) buffer).address(), linearBlockAddress, count);
		return completion;
	}	
	
	public int processCompletions(int length) throws IOException {
		return queuePair.processCompletions(length);
	}
	
	public int getSectorSize() { 
		return namespace.getSectorSize();
	}

	public long getSize() {
		return namespace.getSize();
	}
	
	public int getMaxIOTransferSize() {
		return namespace.getMaxIOTransferSize();
	}	
}
