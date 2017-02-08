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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NvmeControllerData {
	public static int CSIZE = 1024;
	private short pciVendorID;
	private short pciSubsystemVendorID;
	private byte serialNumber[];
	private byte modelNumber[];
	private byte firmwareRevision[];
	private byte recommendedArbitrationBurst;
	private byte IEEEOuiIdentifier[];
	private MultipathIOCapabilities multipathIOCapabilities;
	private byte maximumDataTransferSize;
	private short controllerId;
	private NvmeVersion version;
	private int RTD3ResumeLatency;
	private int RTD3EntryLatency;
	private OptionalAsynchronousEventSupport optionalAsynchronousEventSupport;
	private ControllerAttributes controllerAttributes;
	private OptionalAdminCommandSupport optionalAdminCommandSupport;
	private byte abortCommandLimit;
	private byte asynchronousEventRequestLimit;
	private FirmwareUpdates firmwareUpdates;
	private LogPageAttributes logPageAttributes;
	private byte errorLogPageEntries;
	private byte numberPowerStatesSupported;
	private AdminVendorSpecificCommandConfiguration adminVendorSpecificCommandConfiguration;
	private AutonomousPowerStateTransitionAttributes autonomousPowerStateTransitionAttributes;
	private short warningCompositeTemperatureThreshold;
	private short criticalCompositeTemperatureThreshold;
	private short maximumTimeForFirmwareActivation;
	private int hostMemoryBufferPreferredSize;
	private int hostMemoryBufferMinimumSize;
	private long totalNVMcapacity[];
	private long unallocatedNVMcapacity[];

	NvmeControllerData() {
		serialNumber = new byte[20];
		modelNumber = new byte[40];
		firmwareRevision = new byte[8];
		IEEEOuiIdentifier = new byte[3];
		totalNVMcapacity = new long[2];
		unallocatedNVMcapacity = new long[2];
	}

	private static boolean isBitSet(long b, int n) {
		long mask = (1l << n);
		return (b & mask) == mask;
	}

	public AdminVendorSpecificCommandConfiguration getAdminVendorSpecificCommandConfiguration() {
		return adminVendorSpecificCommandConfiguration;
	}

	public AutonomousPowerStateTransitionAttributes getAutonomousPowerStateTransitionAttributes() {
		return autonomousPowerStateTransitionAttributes;
	}

	public short getWarningCompositeTemperatureThreshold() {
		return warningCompositeTemperatureThreshold;
	}

	public short getCriticalCompositeTemperatureThreshold() {
		return criticalCompositeTemperatureThreshold;
	}

	public short getMaximumTimeForFirmwareActivation() {
		return maximumTimeForFirmwareActivation;
	}

	public int getHostMemoryBufferPreferredSize() {
		return hostMemoryBufferPreferredSize;
	}

	public int getHostMemoryBufferMinimumSize() {
		return hostMemoryBufferMinimumSize;
	}

	public long[] getTotalNVMcapacity() {
		return totalNVMcapacity;
	}

	public long[] getUnallocatedNVMcapacity() {
		return unallocatedNVMcapacity;
	}

	public OptionalAdminCommandSupport getOptionalAdminCommandSupport() {
		return optionalAdminCommandSupport;
	}

	public byte getAbortCommandLimit() {
		return abortCommandLimit;
	}

	public byte getAsynchronousEventRequestLimit() {
		return asynchronousEventRequestLimit;
	}

	public byte getRecommendedArbitrationBurst() {
		return recommendedArbitrationBurst;
	}

	public byte[] getIEEEOuiIdentifier() {
		return IEEEOuiIdentifier;
	}

	public static class MultipathIOCapabilities {
		private boolean multiPort;
		private boolean multiHost;
		private boolean singleRootIOVirtualization;

		MultipathIOCapabilities(byte b) {
			multiPort = isBitSet(b, 0);
			multiHost = isBitSet(b, 1);
			singleRootIOVirtualization = isBitSet(b, 2);
		}

		public boolean hasMultiPort() {
			return multiPort;
		}

		public boolean hasMultiHost() {
			return multiHost;
		}

		public boolean hasSingleRootIOVirtualization() {
			return singleRootIOVirtualization;
		}
	}

	public static class NvmeVersion {
		private int tertiary;
		private int minor;
		private int major;

		NvmeVersion(int i) {
			tertiary = i & 0xFF;
			minor = (i >> 8) & 0xFF;
			major = (i >> 16) & 0xFFFF;
		}

		public int getTertiary() {
			return tertiary;
		}

		public int getMinor() {
			return minor;
		}

		public int getMajor() {
			return major;
		}
	}

	public static class OptionalAsynchronousEventSupport {
		private boolean namespaceAttribute;
		private boolean firmwareActivation;

		OptionalAsynchronousEventSupport(int i) {
			namespaceAttribute = isBitSet(i, 8);
			firmwareActivation = isBitSet(i, 9);
		}

		public boolean hasNamespaceAttributeNotices() {
			return namespaceAttribute;
		}

		public boolean hasFirmwareActivationNotices() {
			return firmwareActivation;
		}
	}

	public static class ControllerAttributes {
		private boolean hostIDexhidSupported;

		ControllerAttributes(int i) {
			hostIDexhidSupported = isBitSet(i, 0);
		}

		public boolean isHostIDexhidSupported() {
			return hostIDexhidSupported;
		}
	}

	public static class OptionalAdminCommandSupport {
		private boolean security;
		private boolean format;
		private boolean firmware;
		private boolean namespaceManage;

		OptionalAdminCommandSupport(short s) {
			security = isBitSet(s, 0);
			format = isBitSet(s, 1);
			firmware = isBitSet(s, 2);
			namespaceManage = isBitSet(s, 3);
		}

		public boolean hasSecurity() {
			return security;
		}

		public boolean hasFormat() {
			return format;
		}

		public boolean hasFirmware() {
			return firmware;
		}

		public boolean hasNamespaceManage() {
			return namespaceManage;
		}
	}

	public static class FirmwareUpdates {
		private boolean slot1ReadOnly;
		private int numberSlots;
		private boolean activationWithoutReset;

		FirmwareUpdates(byte b) {
			slot1ReadOnly = isBitSet(b, 0);
			numberSlots = (b >> 1) & 0x7;
			activationWithoutReset = isBitSet(b, 5);
		}

		public boolean isSlot1ReadOnly() {
			return slot1ReadOnly;
		}

		public int getNumberSlots() {
			return numberSlots;
		}

		public boolean hasActivationWithoutReset() {
			return activationWithoutReset;
		}
	}

	public static class LogPageAttributes {
		private boolean namespaceSmart;
		private boolean celp;
		private boolean edlp;

		LogPageAttributes(byte b) {
			namespaceSmart = isBitSet(b, 0);
			celp = isBitSet(b, 1);
			edlp = isBitSet(b, 2);
		}

		public boolean hasNamespaceSMART() {
			return namespaceSmart;
		}

		public boolean hasCelp() {
			return celp;
		}

		public boolean hasEdlp() {
			return edlp;
		}
	}

	public static class AdminVendorSpecificCommandConfiguration {
		private boolean specificFormat;

		AdminVendorSpecificCommandConfiguration(byte b) {
			specificFormat = isBitSet(b, 0);
		}

		public boolean isSpecificFormat() {
			return specificFormat;
		}
	}

	public static class AutonomousPowerStateTransitionAttributes {
		private boolean supported;

		AutonomousPowerStateTransitionAttributes(byte b) {
			supported = isBitSet(b, 0);
		}

		public boolean isSupported() {
			return supported;
		}
	}

	void update(ByteBuffer buffer) {
		buffer.order(ByteOrder.nativeOrder());
		pciVendorID = buffer.getShort();
		pciSubsystemVendorID = buffer.getShort();
		buffer.get(serialNumber);
		buffer.get(modelNumber);
		buffer.get(firmwareRevision);
		recommendedArbitrationBurst = buffer.get();
		buffer.get(IEEEOuiIdentifier);
		multipathIOCapabilities = new MultipathIOCapabilities(buffer.get());
		maximumDataTransferSize = buffer.get();
		controllerId = buffer.getShort();
		version = new NvmeVersion(buffer.getInt());
		RTD3ResumeLatency = buffer.getInt();
		RTD3EntryLatency = buffer.getInt();
		optionalAsynchronousEventSupport = new OptionalAsynchronousEventSupport(buffer.getInt());
		controllerAttributes = new ControllerAttributes(buffer.getInt());
		buffer.position(buffer.position() + 156);
		optionalAdminCommandSupport = new OptionalAdminCommandSupport(buffer.getShort());
		abortCommandLimit = buffer.get();
		asynchronousEventRequestLimit = buffer.get();
		firmwareUpdates = new FirmwareUpdates(buffer.get());
		logPageAttributes = new LogPageAttributes(buffer.get());
		errorLogPageEntries = buffer.get();
		numberPowerStatesSupported = buffer.get();
		adminVendorSpecificCommandConfiguration = new AdminVendorSpecificCommandConfiguration(buffer.get());
		autonomousPowerStateTransitionAttributes = new AutonomousPowerStateTransitionAttributes(buffer.get());
		warningCompositeTemperatureThreshold = buffer.getShort();
		criticalCompositeTemperatureThreshold = buffer.getShort();
		maximumTimeForFirmwareActivation = buffer.getShort();
		hostMemoryBufferPreferredSize = buffer.getInt();
		hostMemoryBufferMinimumSize = buffer.getInt();
		totalNVMcapacity[0] = buffer.getLong();
		totalNVMcapacity[1] = buffer.getLong();
		unallocatedNVMcapacity[0] = buffer.getLong();
		unallocatedNVMcapacity[1] = buffer.getLong();
	}

	public short getPCIVendorID() {
		return pciVendorID;
	}

	public short getPCISubsystemVendorID() {
		return pciSubsystemVendorID;
	}

	public String getSerialNumber() {
		return new String(serialNumber);
	}

	public String getModelNumber() {
		return new String(modelNumber);
	}

	public String getFirmwareRevision() {
		return new String(firmwareRevision);
	}

	public MultipathIOCapabilities getMultipathIOCapabilities() {
		return multipathIOCapabilities;
	}

	public byte getMaximumDataTransferSize() {
		return maximumDataTransferSize;
	}

	public short getControllerId() {
		return controllerId;
	}

	public NvmeVersion getVersion() {
		return version;
	}

	public int getRTD3ResumeLatency() {
		return RTD3ResumeLatency;
	}

	public int getRTD3EntryLatency() {
		return RTD3EntryLatency;
	}

	public OptionalAsynchronousEventSupport getOptionalAsynchronousEventSupport() {
		return optionalAsynchronousEventSupport;
	}

	public ControllerAttributes getControllerAttributes() {
		return controllerAttributes;
	}

	public FirmwareUpdates getFirmwareUpdates() {
		return firmwareUpdates;
	}

	public LogPageAttributes getLogPageAttributes() {
		return logPageAttributes;
	}

	public byte getErrorLogPageEntries() {
		return errorLogPageEntries;
	}

	public byte getNumberPowerStatesSupported() {
		return numberPowerStatesSupported;
	}
}
