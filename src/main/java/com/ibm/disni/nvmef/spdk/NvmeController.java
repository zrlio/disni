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

package com.ibm.disni.nvmef.spdk;

import com.ibm.disni.util.MemoryAllocation;
import sun.nio.ch.DirectBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class NvmeController extends NatObject {
	private NvmeNamespace namespaces[];
	private int numberOfNamespaces;

	private final NativeDispatcher nativeDispatcher;
	private final MemoryAllocation memoryAllocation;

	private NvmeControllerData data;
	private NvmeControllerOptions options;

	NvmeController(long objId, NativeDispatcher nativeDispatcher, MemoryAllocation memoryAllocation) throws IOException {
		super(objId);
		this.nativeDispatcher = nativeDispatcher;
		this.memoryAllocation = memoryAllocation;
		numberOfNamespaces = nativeDispatcher._nvme_ctrlr_get_num_ns(getObjId());
		if (numberOfNamespaces < 0) {
			throw new IOException("nvme_ctrlr_get_num_ns failed with " + numberOfNamespaces);
		}
		namespaces = new NvmeNamespace[numberOfNamespaces];
	}

	public NvmeQueuePair allocQueuePair() throws IOException {
		//TODO: priorities for weighted round-robin scheduling
		long qPair = nativeDispatcher._nvme_ctrlr_alloc_io_qpair(getObjId(), 0);
		if (qPair == 0) {
			throw new IOException("nvme_ctrlr_alloc_io_qpair failed");
		}
		return new NvmeQueuePair(qPair, nativeDispatcher);
	}

	public NvmeNamespace getNamespace(int id) throws IOException {
		int idx = id - 1;
		if (namespaces[idx] == null) {
			long namespace = nativeDispatcher._nvme_ctrlr_get_ns(getObjId(), id);
			if (namespace == 0) {
				throw new IOException("nvme_ctrlr_get_ns failed");
			}
			namespaces[idx] = new NvmeNamespace(namespace, nativeDispatcher, memoryAllocation);
		}
		return namespaces[idx];
	}

	public int getNumberOfNamespaces() throws IOException {
		return numberOfNamespaces;
	}

	public NvmeControllerData getData() {
		if (data == null) {
			ByteBuffer buffer = ByteBuffer.allocateDirect(NvmeControllerData.CSIZE);
			nativeDispatcher._nvme_ctrlr_get_data(getObjId(), ((DirectBuffer) buffer).address());
			data = new NvmeControllerData();
			data.update(buffer);
		}
		return data;
	}

	public NvmeControllerOptions getOptions() {
		if (options == null) {
			ByteBuffer buffer = ByteBuffer.allocateDirect(NvmeControllerOptions.CSIZE);
			nativeDispatcher._nvme_ctrlr_get_opts(getObjId(), ((DirectBuffer) buffer).address());
			options = new NvmeControllerOptions();
			options.update(buffer);
		}
		return options;
	}

	public void detach() throws IOException {
		int ret = nativeDispatcher._nvme_detach(getObjId());
		if (ret != 0) {
			throw new IOException("spdk_nvme_detach failed with " + ret);
		}
	}
}
