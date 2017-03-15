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
import java.nio.ByteBuffer;

import com.ibm.disni.nvmef.spdk.*;

public class NvmeEndpoint {
	private final NvmeEndpointGroup group;
    private NvmeQueuePair queuePair;
	private NvmeNamespace namespace;
	private volatile boolean open;
	private NvmeControllerOptions controllerOptions;
	
	public NvmeEndpoint(NvmeEndpointGroup group, NvmfConnection newConnection) {
		this.group = group;
		this.queuePair = null;
		this.namespace = null;
		this.open = newConnection != null;
	}

	//rdma://<host>:<port>
	//nvmef:://<host>:<port>/controller/namespace"
	public synchronized void connect(URI uri) throws IOException {
		if (open){
			return;
		}
		NvmeResourceIdentifier nvmeResource = NvmeResourceIdentifier.parse(uri);
		NvmeTransportId transportId = nvmeResource.toTransportId();
		NvmeController nvmecontroller = group.probe(transportId, nvmeResource.getController());
		this.namespace = nvmecontroller.getNamespace(nvmeResource.getNamespace());
		this.queuePair = nvmecontroller.allocQueuePair();	
		this.open = true;
		this.controllerOptions = nvmecontroller.getOptions();
	}

	private enum Operation {
		READ,
		WRITE
	}

	public NvmeCommand Op(Operation op, ByteBuffer buffer, long linearBlockAddress) throws IOException {
		if (open){
			throw new IOException("endpoint is closed");
		}
		if (buffer.remaining() % namespace.getSectorSize() != 0){
			throw new IOException("Remaining buffer a multiple of sector size");
		}
		IOCompletion completion = new IOCompletion();
		return new NvmeCommand(this, buffer, linearBlockAddress, completion, op == Operation.WRITE);
	}

	public NvmeCommand write(ByteBuffer buffer, long linearBlockAddress) throws IOException{
		return Op(Operation.WRITE, buffer, linearBlockAddress);
	}
	
	public NvmeCommand read(ByteBuffer buffer, long linearBlockAddress) throws IOException {
		return Op(Operation.READ, buffer, linearBlockAddress);
	}

	public NvmeCommand newCommand() {
		return new NvmeCommand(this, new IOCompletion());
	}

	NvmeNamespace getNamespace() {
		return namespace;
	}

	NvmeQueuePair getQueuePair() {
		return queuePair;
	}

	public boolean isOpen() {
		return open;
	}
	
	public synchronized void close() throws IOException, InterruptedException {
		queuePair.free();
		open = false;
	}
	
	public synchronized int processCompletions(long[] completed) throws IOException {
		return queuePair.processCompletions(completed);
	}
	
	public int getSectorSize() { 
		return namespace.getSectorSize();
	}

	public long getNamespaceSize() {
		return namespace.getSize();
	}
	
	public int getMaxTransferSize() {
		return namespace.getMaxIOTransferSize();
	}

	public int getIOQueueSize() {
		return controllerOptions.getIOQueueSize();
	}
}
