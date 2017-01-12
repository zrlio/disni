/*
 * jVerbs: RDMA verbs support for the Java Virtual Machine
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class NvmeControllerData {
	public static int CSIZE = 512;
	private short pciVendorID;
	private short pciSubsystemVendorID;
	private byte serialNumber[];
	private byte modelNumber[];
	private byte firmwareRevision[];

	NvmeControllerData() {
		serialNumber = new byte[20];
		modelNumber = new byte[40];
		firmwareRevision = new byte[8];
	}

	void update(ByteBuffer buffer) {
		buffer.order(ByteOrder.nativeOrder());
		pciVendorID = buffer.getShort();
		pciSubsystemVendorID = buffer.getShort();
		buffer.get(serialNumber);
		buffer.get(modelNumber);
		buffer.get(firmwareRevision);
		// TODO: missing ctrlr_data fields
	}

	public short getPciVendorID() {
		return pciVendorID;
	}

	public short getPciSubsystemVendorID() {
		return pciSubsystemVendorID;
	}

	public byte[] getSerialNumber() {
		return serialNumber;
	}

	public byte[] getModelNumber() {
		return modelNumber;
	}

	public byte[] getFirmwareRevision() {
		return firmwareRevision;
	}
}
