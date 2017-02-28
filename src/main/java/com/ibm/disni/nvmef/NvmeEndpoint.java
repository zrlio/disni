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
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

import sun.nio.ch.DirectBuffer;

import com.ibm.disni.nvmef.spdk.IOCompletion;
import com.ibm.disni.nvmef.spdk.NvmeController;
import com.ibm.disni.nvmef.spdk.NvmeNamespace;
import com.ibm.disni.nvmef.spdk.NvmeQueuePair;
import com.ibm.disni.nvmef.spdk.NvmeTransportId;
import com.ibm.disni.nvmef.spdk.NvmfAddressFamily;
import com.ibm.disni.nvmef.spdk.NvmfConnection;

public class NvmeEndpoint {
	private NvmeEndpointGroup group;
    private NvmeQueuePair queuePair;
    private NvmeNamespace namespace;
    private AtomicBoolean isOpen;
	
	public NvmeEndpoint(NvmeEndpointGroup group, NvmfConnection newConnection) {
		this.group = group;
		this.queuePair = null;
		this.namespace = null;
		this.isOpen = new AtomicBoolean(newConnection != null);
	}

	//rdma://<host>:<port>
	//nvmef:://<host>:<port>/controller/namespace"
	public synchronized void connect(URI uri) throws IOException {
		if (isOpen.get()){
			return;
		}
		NvmeResourceIdentifier nvmeResource = NvmeResourceIdentifier.parse(uri);
		NvmeTransportId transportId = NvmeTransportId.rdma(NvmfAddressFamily.IPV4, nvmeResource.getAddress(), nvmeResource.getPort(), nvmeResource.getSubsystem());
		NvmeController nvmecontroller = group.probe(transportId, nvmeResource.getController());
		this.namespace = nvmecontroller.getNamespace(nvmeResource.getNamespace());
		this.queuePair = nvmecontroller.allocQueuePair();	
		this.isOpen.set(true);
	}

	private enum Operation {
		READ,
		WRITE
	}

	private IOCompletion doIO(Operation op, ByteBuffer buffer, long linearBlockAddress) throws IOException {
		if (!isOpen.get()){
			throw new IOException("endpoint is closed");
		}
		if (buffer.remaining() % namespace.getSectorSize() != 0){
			throw new IOException("Remaining buffer a multiple of sector size");
		}
		int sectorCount = buffer.remaining() / namespace.getSectorSize();
		long bufferAddress = ((DirectBuffer) buffer).address() + buffer.position();

		IOCompletion completion = null;
		switch(op) {
			case READ:
				completion = namespace.read(queuePair, bufferAddress, linearBlockAddress, sectorCount);
				break;
			case WRITE:
				completion = namespace.write(queuePair, bufferAddress, linearBlockAddress, sectorCount);
				break;
		}
		buffer.position(buffer.limit());

		return completion;
	}
	
	public synchronized IOCompletion write(ByteBuffer buffer, long linearBlockAddress) throws IOException{
		return doIO(Operation.WRITE, buffer, linearBlockAddress);
	}
	
	public synchronized IOCompletion read(ByteBuffer buffer, long linearBlockAddress) throws IOException {
		return doIO(Operation.READ, buffer, linearBlockAddress);
	}
	
	public synchronized void close() throws IOException, InterruptedException {
		queuePair.free();
		isOpen.set(false);
	}
	
	public int processCompletions(int length) throws IOException {
		return queuePair.processCompletions(length);
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
}
