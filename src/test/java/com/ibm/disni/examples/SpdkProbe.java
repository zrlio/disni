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

package com.ibm.disni.examples;

import com.ibm.disni.nvmef.spdk.*;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class SpdkProbe {
	public static void main(String[] args) throws Exception {
		if (args.length != 3 && args.length != 1) {
			System.out.println("<address> [<port> <subsystemNQN>]");
			System.exit(-1);
		}
		boolean isRDMA = args.length == 3;

		NvmeTransportId transportId;
		if (isRDMA) {
			transportId = NvmeTransportId.rdma(NvmfAddressFamily.IPV4, args[0], args[1], args[2]);
		} else {
			transportId = NvmeTransportId.pcie(args[0]);
		}
		Nvme nvme = new Nvme(new NvmeTransportType[]{transportId.getType()}, "/dev/hugepages", new long[]{256, 256});
		ArrayList<NvmeController> controllers = new ArrayList<NvmeController>();
		nvme.probe(transportId, controllers);
		for (NvmeController controller : controllers) {
			NvmeControllerData data = controller.getData();
			System.out.println("-------------------------------------------");
			System.out.println("Controller: " + controller.getObjId());
			System.out.println("PCI Vendor ID = " + Integer.toHexString(0xffff & data.getPCIVendorID()));
			System.out.println("PCI Subsystem Vendor ID = " + Integer.toHexString(0xffff & data.getPCISubsystemVendorID()));
			System.out.println("Serial Number = " + data.getSerialNumber());
			System.out.println("Model Number = " + data.getModelNumber());
			System.out.println("Firmware Revision = " + data.getFirmwareRevision());
			System.out.println("Recommended arbitration burst = " + data.getRecommendedArbitrationBurst());
			System.out.println("IEEE oui identifier = ");

			System.out.println("Controller multi-path I/O and namespace sharing capabilities:");
			NvmeControllerData.MultipathIOCapabilities multipathIOCapabilities = data.getMultipathIOCapabilities();
			System.out.println(" Multi port = " + multipathIOCapabilities.hasMultiPort());
			System.out.println(" Multi host = " + multipathIOCapabilities.hasMultiHost());
			System.out.println(" SR-IOV = " + multipathIOCapabilities.hasSingleRootIOVirtualization());

			System.out.print("Maximum data transfer size = ");
			if (data.getMaximumDataTransferSize() == 0) {
				System.out.println("unlimited");
			} else {
				//FIXME cap.bits.mpsmin
				System.out.println(1l << (12 + data.getMaximumDataTransferSize()));
			}
			System.out.println("Controller ID = " + data.getControllerId());

			NvmeControllerData.NvmeVersion version = data.getVersion();
			System.out.println("NVMe Version = " + version.getMajor() + "." + version.getMinor() + "." + version.getTertiary());

			System.out.println("RTD3 resume latency = " + data.getRTD3ResumeLatency());
			System.out.println("RTD3 entry latency = " + data.getRTD3EntryLatency());

			System.out.println("Optional asynchronous events supported:");
			NvmeControllerData.OptionalAsynchronousEventSupport optionalAsynchronousEventSupport = data.getOptionalAsynchronousEventSupport();
			System.out.println(" Supports sending Namespace Attribute Notices = " + optionalAsynchronousEventSupport.hasNamespaceAttributeNotices());
			System.out.println(" Supports sending Firmware Activation Notices = " + optionalAsynchronousEventSupport.hasFirmwareActivationNotices());

			System.out.println("Controller Attributes: ");
			NvmeControllerData.ControllerAttributes controllerAttributes = data.getControllerAttributes();
			System.out.println(" Supports Host ID exhid = " + controllerAttributes.isHostIDexhidSupported());

			System.out.println("Optional admin command support: ");
			NvmeControllerData.OptionalAdminCommandSupport adminCommandSupport = data.getOptionalAdminCommandSupport();
			System.out.println(" Security = " + adminCommandSupport.hasSecurity());
			System.out.println(" Format = " + adminCommandSupport.hasFormat());
			System.out.println(" Firmware = " + adminCommandSupport.hasFirmware());
			System.out.println(" Namespace Manage = " + adminCommandSupport.hasNamespaceManage());

			System.out.println("Abort command limit = " + data.getAbortCommandLimit());
			System.out.println("Asynchronous event request limit = " + data.getAsynchronousEventRequestLimit());

			System.out.println("Firmware updates: ");
			NvmeControllerData.FirmwareUpdates firmwareUpdates = data.getFirmwareUpdates();
			System.out.println(" Slot 1 read only = " + firmwareUpdates.isSlot1ReadOnly());
			System.out.println(" Number of slots = " + firmwareUpdates.getNumberSlots());
			System.out.println(" Activation without reset = " + firmwareUpdates.hasActivationWithoutReset());

			System.out.println("Log page attributes: ");
			NvmeControllerData.LogPageAttributes logPageAttributes = data.getLogPageAttributes();
			System.out.println(" Namespace SMART = " + logPageAttributes.hasNamespaceSMART());
			System.out.println(" celp = " + logPageAttributes.hasCelp());
			System.out.println(" edlp = " + logPageAttributes.hasEdlp());

			System.out.println("Error log page entries = " + data.getErrorLogPageEntries());
			System.out.println("Number of power states supported = " + data.getNumberPowerStatesSupported());

			System.out.println("Admin vendor specific command configuration: ");
			NvmeControllerData.AdminVendorSpecificCommandConfiguration adminVendorSpecificCommandConfiguration = data.getAdminVendorSpecificCommandConfiguration();
			System.out.println(" specific format = " + adminVendorSpecificCommandConfiguration.isSpecificFormat());

			System.out.println("Autonomous power state transition attributes: ");
			NvmeControllerData.AutonomousPowerStateTransitionAttributes autonomousPowerStateTransitionAttributes = data.getAutonomousPowerStateTransitionAttributes();
			System.out.println("Supported = " + autonomousPowerStateTransitionAttributes.isSupported());

			System.out.println("Warning composite temperature threshold = " + data.getWarningCompositeTemperatureThreshold());
			System.out.println("Critical composite temperature threshold = " + data.getCriticalCompositeTemperatureThreshold());
			System.out.println("Maximum time for firmware activation = " + data.getMaximumTimeForFirmwareActivation());
			System.out.println("Host memory buffer preferred size = " + data.getHostMemoryBufferPreferredSize());
			System.out.println("Host memory buffer minimum size = " + data.getHostMemoryBufferMinimumSize());
			System.out.println("total NVM capacity = " + Long.toHexString(data.getTotalNVMcapacity()[0]) + Long.toHexString(data.getTotalNVMcapacity()[1]));
			System.out.println("unallocated NVM capacity = " + Long.toHexString(data.getUnallocatedNVMcapacity()[0]) + Long.toHexString(data.getUnallocatedNVMcapacity()[1]));
			
			System.out.println("Options:");
			NvmeControllerOptions options = controller.getOptions();
			System.out.println("Number of I/O queues = " + options.getNumberIOQueues());
			System.out.println("Enable submission queue in controller memory buffer = " + options.usesControllerMemoryBufferSubmissionQueues());
			System.out.println("Arbitration mechanism = " + options.getControllerArbitrationMechanism().name());
			System.out.println("Transport retry count = " + options.getTransportRetryCount());
			System.out.println("I/O queue size = " + options.getIOQueueSize());
			System.out.println("Host NVMe qualified name = " + options.getHostNvmeQualifiedName());

			int numberOfNamespaces = controller.getNumberOfNamespaces();
			for (int i = 0; i < numberOfNamespaces; i++) {
				NvmeNamespace namespace = controller.getNamespace(i + 1);
				System.out.println("Namespace: " + namespace.getObjId());
				boolean active = namespace.isActive();
				System.out.println("\tIs active = " + active);
				if (active) {
					System.out.println("\tSize (bytes) = " + namespace.getSize());
					System.out.println("\tSector Size (bytes) = " + namespace.getSectorSize());
				}
			}
			System.out.println();
		}
		// alloc qpair
		NvmeController controller = controllers.get(0);
		NvmeQueuePair queuePair = controller.allocQueuePair();
		NvmeNamespace namespace = controller.getNamespace(1);

		ByteBuffer writeBuf;
		final int bufferSize = 4096;
		if (!isRDMA) {
			writeBuf = nvme.allocateBuffer(bufferSize, bufferSize);
		} else {
			writeBuf = ByteBuffer.allocateDirect(bufferSize);
		}
		writeBuf.put(new byte[]{'H', 'e', 'l', 'l', 'o'});
		IOCompletion completion = new IOCompletion();
		namespace.write(queuePair, ((DirectBuffer) writeBuf).address(), 0, 1, completion);

		do {
			queuePair.processCompletions(10);
		} while (!completion.done());
		System.out.println("write completed with status type = " + completion.getStatusCodeType().name());
		if (completion.getStatusCodeType() == NvmeStatusCodeType.GENERIC) {
			System.out.println("Status code = " +
					NvmeGenericCommandStatusCode.valueOf(completion.getStatusCode()).name());
		}
		if (!isRDMA) {
			nvme.freeBuffer(writeBuf);
		}

		ByteBuffer buffer;
		if (!isRDMA) {
			buffer = nvme.allocateBuffer(bufferSize, bufferSize);
		} else {
			buffer = ByteBuffer.allocateDirect(bufferSize);
		}
		namespace.read(queuePair, ((DirectBuffer) buffer).address(), 0, 1, completion);
		do {
			queuePair.processCompletions(10);
		} while (!completion.done());
		System.out.println("read completed with status type = " + completion.getStatusCodeType().name());
		if (completion.getStatusCodeType() == NvmeStatusCodeType.GENERIC) {
			System.out.println("Status code = " +
					NvmeGenericCommandStatusCode.valueOf(completion.getStatusCode()).name());
		}

		byte cString[] = new byte[5];
		buffer.get(cString);
		System.out.println("Read " + new String(cString));
		if (!isRDMA) {
			nvme.freeBuffer(buffer);
		}
	}
}
