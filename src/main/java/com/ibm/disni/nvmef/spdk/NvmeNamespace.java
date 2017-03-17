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

import java.io.IOException;

public class NvmeNamespace extends NatObject {
    private final NativeDispatcher nativeDispatcher;
	private final MemoryAllocation memoryAllocation;

	private final long size;
	private final int sectorSize;

    NvmeNamespace(long objId, NativeDispatcher nativeDispatcher, MemoryAllocation memoryAllocation) {
        super(objId);
        this.nativeDispatcher = nativeDispatcher;
		this.memoryAllocation = memoryAllocation;
		size = nativeDispatcher._nvme_ns_get_size(getObjId());
		sectorSize = nativeDispatcher._nvme_ns_get_sector_size(getObjId());
    }

    public boolean isActive() {
        return nativeDispatcher._nvme_ns_is_active(getObjId());
    }

    public long getSize() {
        return size;
    }

	public int getSectorSize() {
		return sectorSize;
	}

	public int getMaxIOTransferSize() { return nativeDispatcher._nvme_ns_get_max_io_xfer_size(getObjId()); }

	public void Op(NvmeQueuePair queuePair, long address, long linearBlockAddress, int count, IOCompletion completion, boolean write) throws IOException {
		try {
			completion.reset();
		} catch (PendingOperationException e) {
			throw new IllegalArgumentException("Completion not done", e);
		}
		completion.setQueuePair(queuePair);
		int ret = nativeDispatcher._nvme_ns_io_cmd(getObjId(), queuePair.getObjId(), address,
				linearBlockAddress, count, completion.address(), write);
		if (ret < 0) {
			throw new IOException("nvme_ns_cmd_read failed with " + ret);
		}
	}

	public void read(NvmeQueuePair queuePair, long address, long linearBlockAddress, int count, IOCompletion completion) throws IOException {
		Op(queuePair, address, linearBlockAddress, count, completion, false);
	}

	public void write(NvmeQueuePair queuePair, long address, long linearBlockAddress, int count, IOCompletion completion) throws IOException {
		Op(queuePair, address, linearBlockAddress, count, completion, true);
	}
}
