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

import java.io.IOException;

public class NvmeQueuePair extends NatObject {

	private NativeDispatcher nativeDispatcher;

	NvmeQueuePair(long objId, NativeDispatcher nativeDispatcher) {
		super(objId);
		this.nativeDispatcher = nativeDispatcher;
	}

	public int processCompletions(int maxCompletions) throws IOException {
		int ret = nativeDispatcher._nvme_qpair_process_completions(getObjId(), maxCompletions);
		if (ret < 0) {
			throw new IOException("nvme_qpair_process_completions failed with " + ret);
		}
		return ret;
	}

	public void free() throws IOException {
		int ret = nativeDispatcher._nvme_ctrlr_free_io_qpair(getObjId());
		if (ret != 0) {
			throw new IOException("spdk_nvme_ctrlr_free_io_qpair failed with " + ret);
		}
	}
}
