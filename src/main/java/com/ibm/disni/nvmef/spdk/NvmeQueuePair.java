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

import sun.nio.ch.DirectBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NvmeQueuePair extends NatObject {

	private final NativeDispatcher nativeDispatcher;
	private final ByteBuffer completedArray;
	private final long completedArrayAddress;

	private static final int COMPLETED_ARRAY_INDEX_OFFSET = 0;
	private static final int COMPLETED_ARRAY_START_OFFSET = 8;
	private static final int COMPLETED_ARRAY_SIZE = 1024;


	NvmeQueuePair(long objId, NativeDispatcher nativeDispatcher) {
		super(objId);
		this.nativeDispatcher = nativeDispatcher;
		this.completedArray = ByteBuffer.allocateDirect(COMPLETED_ARRAY_SIZE * 8 + COMPLETED_ARRAY_START_OFFSET);
		this.completedArray.order(ByteOrder.nativeOrder());
		this.completedArrayAddress = ((DirectBuffer)completedArray).address();
	}

	public int processCompletions(int maxCompletions) throws IOException {
		if (maxCompletions > COMPLETED_ARRAY_SIZE) {
			throw new IllegalArgumentException("Maximum number of completions to process is to large (max " + COMPLETED_ARRAY_SIZE + ")");
		}
		completedArray.putInt(COMPLETED_ARRAY_INDEX_OFFSET, 0);
		int ret = nativeDispatcher._nvme_qpair_process_completions(getObjId(), maxCompletions);
		if (ret < 0) {
			throw new IOException("nvme_qpair_process_completions failed with " + ret);
		}
		return ret;
	}

	public int processCompletions(long[] completed) throws IOException {
		processCompletions(completed.length);
		int numCompleted = completedArray.getInt(COMPLETED_ARRAY_INDEX_OFFSET);
		for (int i = 0; i < numCompleted; i++) {
			completed[i] = completedArray.getLong(i*8 + COMPLETED_ARRAY_START_OFFSET);
		}
		return numCompleted;
	}

	public void free() throws IOException {
		int ret = nativeDispatcher._nvme_ctrlr_free_io_qpair(getObjId());
		if (ret != 0) {
			throw new IOException("spdk_nvme_ctrlr_free_io_qpair failed with " + ret);
		}
	}

	long getCompletedArrayAddress() {
		return completedArrayAddress;
	}
}
