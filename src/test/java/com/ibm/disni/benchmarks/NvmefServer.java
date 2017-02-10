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

package com.ibm.disni.benchmarks;

import com.ibm.disni.nvmef.spdk.*;

import java.util.ArrayList;
import java.util.List;

public class NvmefServer {
	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.out.println("<pci-address> <subsystemNQN> <address> <port>");
			System.exit(-1);
		}

		Nvme nvme = new Nvme();
		NvmeTransportId transportId = NvmeTransportId.pcie(args[0]);
		List<NvmeController> controllers = new ArrayList<NvmeController>();
		nvme.probe(transportId, controllers);
		for (NvmeController controller : controllers) {
			NvmeControllerData data = controller.getData();
			System.out.println("Controller: " + controller.getObjId());
			System.out.println("PCI Vendor ID = " + Integer.toHexString(0xffff & data.getPCIVendorID()));
			System.out.println("PCI Subsystem Vendor ID = " +
					Integer.toHexString(0xffff & data.getPCISubsystemVendorID()));
			System.out.println("Serial Number = " + data.getSerialNumber());
			System.out.println("Model Number = " + data.getModelNumber());
		}
	}
}
