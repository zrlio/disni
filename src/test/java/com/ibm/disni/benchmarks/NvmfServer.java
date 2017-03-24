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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class NvmfServer {
	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.out.println("<pci-address> <subsystemNQN> <address> <port>");
			System.exit(-1);
		}

		String pci_addr = args[0];
		String nqn = args[1];
		String address = args[2];
		String port = args[3];

		Nvme nvme = new Nvme(new NvmeTransportType[]{NvmeTransportType.PCIE, NvmeTransportType.RDMA},
				"/dev/hugepages", new long[]{256,256});
		nvme.logEnableTrace();

		NvmeTransportId transportId = NvmeTransportId.pcie(pci_addr);
		List<NvmeController> controllers = new ArrayList<NvmeController>();
		nvme.probe(transportId, controllers);
		if (controllers.size() == 0) {
			System.out.println("No Nvme device with PCIe address " + pci_addr + " found!");
			System.exit(-2);
		}
		System.out.println(controllers.size() + " local controllers found.");
		NvmeController controller = controllers.get(0);
		NvmeControllerData data = controller.getData();
		System.out.println("Controller: " + controller.getObjId());
		System.out.println("PCI Vendor ID = " + Integer.toHexString(0xffff & data.getPCIVendorID()));
		System.out.println("PCI Subsystem Vendor ID = " +
				Integer.toHexString(0xffff & data.getPCISubsystemVendorID()));
		System.out.println("Serial Number = " + data.getSerialNumber());
		System.out.println("Model Number = " + data.getModelNumber());
		System.out.println();

		System.out.println("Create subsystem with NQN " + nqn);
		NvmfTarget target = nvme.createNvmfTarget((short)128, (short)4, 4096, 128*1024 /* 128 KB */);
		NvmfSubsystem subsystem =
				target.createSubsystem(nqn, NvmfSubtype.NVME, NvmfSubsystemMode.DIRECT);
		subsystem.addController(controller, pci_addr);
		subsystem.addListener(NvmfTransportName.RDMA, address, port);

		ArrayList<NvmfConnection> connections = new ArrayList<NvmfConnection>();
		while (true) {
			target.poll();
			subsystem.poll(connections);
			if (connections.size() != 0) {
				System.out.println(connections.size() + " new connections!");
				connections.clear();
			}
		}
	}
}
