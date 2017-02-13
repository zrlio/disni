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

import java.util.List;

public class NvmfSubsystem extends NatObject {
	private final NativeDispatcher nativeDispatcher;
	private final String nqn;
	private final NvmfSubtype type;
	private final NvmfSubsystemMode mode;

	NvmfSubsystem(long objId, NativeDispatcher nativeDispatcher, String nqn, NvmfSubtype type, NvmfSubsystemMode mode) {
		super(objId);
		this.nativeDispatcher = nativeDispatcher;
		this.nqn = nqn;
		this.type = type;
		this.mode = mode;
	}

	public void addListener(NvmfTransportName transportName, String address, String serviceId) throws Exception {
		int ret = nativeDispatcher._nvmf_subsystem_add_listener(getObjId(), transportName.name(), address, serviceId);
		if (ret != 0) {
			throw new Exception("spdk_nvmf_subsystem_add_listener failed with " + ret);
		}
	}

	//TODO: pci address type
	public void addController(NvmeController controller, String pciAddress) throws Exception {
		int ret = nativeDispatcher._nvmf_subsystem_add_ctrlr(getObjId(), controller.getObjId(), pciAddress);
		if (ret != 0) {
			throw new Exception("spdk_nvmf_subsystem_add_ctrlr failed with " + ret);
		}
	}

	public void poll(List<NvmfConnection> connects) {
		int ret = nativeDispatcher._nvmf_subsystem_poll(getObjId(), null);
		for (int i = 0; i < ret; i++) {
			connects.add(new NvmfConnection());
		}
	}

	public void delete() {
		nativeDispatcher._nvmf_delete_subsystem(getObjId());
	}

	public String getNqn() {
		return nqn;
	}

	public NvmfSubtype getType() {
		return type;
	}

	public NvmfSubsystemMode getMode() {
		return mode;
	}
}
