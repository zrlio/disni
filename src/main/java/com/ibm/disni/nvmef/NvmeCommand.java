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
import java.nio.ByteBuffer;

import com.ibm.disni.nvmef.spdk.IOCompletion;
import com.ibm.disni.nvmef.spdk.NvmeNamespace;
import com.ibm.disni.nvmef.spdk.NvmeQueuePair;
import com.ibm.disni.util.MemoryUtils;

public class NvmeCommand {
	private final NvmeEndpoint endpoint;
	private final NvmeNamespace namespace;
	private final NvmeQueuePair queuePair;
	private ByteBuffer buffer;
	private long bufferAddress;
	private long linearBlockAddress;
	private int sectorCount;
	private final IOCompletion completion;
	private boolean isWrite;

	NvmeCommand(NvmeEndpoint endpoint, ByteBuffer buffer, long linearBlockAddress,
				IOCompletion completion, boolean isWrite) {
		this(endpoint, completion);
		this.linearBlockAddress = linearBlockAddress;
		this.isWrite = isWrite;
		setBuffer(buffer);
	}

	NvmeCommand(NvmeEndpoint endpoint, IOCompletion completion) {
		this.endpoint = endpoint;
		this.namespace = endpoint.getNamespace();
		this.queuePair = endpoint.getQueuePair();
		this.completion = completion;
	}

	public void execute() throws IOException {
		if (bufferAddress == 0) {
			throw new IllegalArgumentException("Buffer not set");
		}

		synchronized (endpoint) {
			if (!endpoint.isOpen()) {
				throw new IOException("Endpoint not open!");
			}
			if (isWrite) {
				namespace.write(queuePair, bufferAddress, linearBlockAddress, sectorCount, completion);
			} else {
				namespace.read(queuePair, bufferAddress, linearBlockAddress, sectorCount, completion);
			}
		}
	}

	public boolean isDone(){
		return completion.done();
	}

	public boolean isPending() {  return completion.isPending(); }

	public void free() {
		completion.free();
	}

	public NvmeCommand setLinearBlockAddress(long linearBlockAddress) {
		this.linearBlockAddress = linearBlockAddress;
		return this;
	}

	public NvmeCommand setBuffer(ByteBuffer buffer) {
		this.sectorCount = buffer.remaining() / namespace.getSectorSize();
		this.bufferAddress = MemoryUtils.getAddress(buffer) + buffer.position();
		this.buffer = buffer;
		return this;
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}

	public NvmeCommand read() {
		this.isWrite = false;
		return this;
	}

	public NvmeCommand write() {
		this.isWrite = true;
		return this;
	}

	public NvmeCommand setId(long id) {
		completion.setId(id);
		return this;
	}

	public long getId() {
		return completion.getId();
	}

	public IOCompletion getCompletion() {
		return completion;
	}
}
