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


public class NvmfTarget {

	private final NativeDispatcher nativeDispatcher;

	private static boolean initialized = false;

	NvmfTarget(NativeDispatcher nativeDispatcher, short maxQueueDepth, short maxConnectionPerSession,
			   int inCapsuleDataSize, int maxIOSize) throws Exception {
		if (initialized) {
			throw new Exception("The target is already initialized!");
		}
		this.nativeDispatcher = nativeDispatcher;
		int ret = nativeDispatcher._nvmf_tgt_init(maxQueueDepth, maxConnectionPerSession, inCapsuleDataSize, maxIOSize);
		if (ret != 0) {
			throw new IllegalArgumentException("spdk_nvmf_tgt_init failed with " + ret);
		}
		initialized = true;
	}

	public NvmfSubsystem createSubsystem(String nqn, NvmfSubtype type, NvmfSubsystemMode mode) throws Exception {
		long objId = nativeDispatcher._nvmf_create_subsystem(nqn, type.getNumVal(), mode.getNumVal());
		if (objId == 0) {
			throw new Exception("spdk_nvmf_create_subsystem failed");
		}
		return new NvmfSubsystem(objId, nativeDispatcher, nqn, type, mode);
	}

	public void poll() {
		//TODO: handle disconnects
		nativeDispatcher._nvmef_acceptor_poll(null);
	}

	public void fini() throws Exception {
		int ret = nativeDispatcher._nvmf_tgt_fini();
		if (ret != 0) {
			throw new Exception("spdk_nvmf_tgt_fini failed with " + ret);
		}
		initialized = false;
	}
}
