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
import java.nio.charset.StandardCharsets;

public class NvmeControllerOptions {
	public static final int CSIZE = 568;

	private int numberIOQueues;
	private boolean useControllerMemoryBufferSubmissionQueues;
	private NvmeControllerArbitrationMechanism arbitrationMechanism;
	private int keepAliveTimeoutms;
	private int transportRetryCount;
	private int ioQueueSize;
	private String hostNvmeQualifiedName;
	private static int HOSTNQNSTRSIZE = 224;
	private int ioQueueRequests;
	private String sourceAddress;
	private static int SOURCE_ADDRESS_SIZE = 257;
	private String sourceServiceId;
	private static int SOURCE_SERVICE_ID_SIZE = 33;
	private byte[] hostId;
	private static int HOST_ID_SIZE = 8;
	private byte[] extendedHostId;
	private static int EXTENDED_HOST_ID_SIZE = 16;

	public int getNumberIOQueues() {
		return numberIOQueues;
	}

	public boolean usesControllerMemoryBufferSubmissionQueues() {
		return useControllerMemoryBufferSubmissionQueues;
	}

	public NvmeControllerArbitrationMechanism getControllerArbitrationMechanism() {
		return arbitrationMechanism;
	}

	public int getKeepAliveTimeoutms() {
		return keepAliveTimeoutms;
	}

	public int getTransportRetryCount() {
		return transportRetryCount;
	}

	public int getIOQueueSize() {
		return ioQueueSize;
	}

	public String getHostNvmeQualifiedName() {
		return hostNvmeQualifiedName;
	}

	public int getIoQueueRequests() {
		return ioQueueRequests;
	}

	public String getSourceAddress() {
		return sourceAddress;
	}

	public String getSourceServiceId() {
		return sourceServiceId;
	}

	public byte[] getHostId() {
		return hostId.clone();
	}

	public byte[] getExtendedHostId() {
		return extendedHostId.clone();
	}


	public enum NvmeControllerArbitrationMechanism {
		ROUND_ROBIN(0x0), WEIGHTED_ROUND_ROBIN(0x1), VENDOR_SPECIFIC(0x7);

		private int numVal;

		NvmeControllerArbitrationMechanism(int numVal) {
			this.numVal = numVal;
		}

		public int getNumVal() {
			return numVal;
		}

		static NvmeControllerArbitrationMechanism valueOf(int numVal) {
			for (NvmeControllerArbitrationMechanism arbitrationMechanism : NvmeControllerArbitrationMechanism.values()) {
				if (numVal == arbitrationMechanism.getNumVal()) {
					return arbitrationMechanism;
				}
			}
			throw new IllegalArgumentException();
		}
	}

	void update(ByteBuffer buffer) {
		byte[] rawString = new byte[Math.max(HOSTNQNSTRSIZE,
				Math.max(SOURCE_ADDRESS_SIZE, SOURCE_SERVICE_ID_SIZE))];
		buffer.order(ByteOrder.nativeOrder());
		numberIOQueues = buffer.getInt();
		useControllerMemoryBufferSubmissionQueues = buffer.getInt() != 0;
		arbitrationMechanism = NvmeControllerArbitrationMechanism.valueOf(buffer.getInt());
		keepAliveTimeoutms = buffer.getInt();
		transportRetryCount = buffer.getInt();
		ioQueueSize = buffer.getInt();
		buffer.get(rawString, 0, HOSTNQNSTRSIZE);
		hostNvmeQualifiedName = new String(rawString, StandardCharsets.US_ASCII);
		ioQueueRequests = buffer.getInt();
		buffer.get(rawString, 0, SOURCE_ADDRESS_SIZE);
		sourceAddress = new String(rawString, StandardCharsets.US_ASCII);
		buffer.get(rawString, 0, SOURCE_SERVICE_ID_SIZE);
		sourceServiceId = new String(rawString, StandardCharsets.US_ASCII);
		hostId = new byte[HOST_ID_SIZE];
		buffer.get(hostId);
		extendedHostId = new byte[EXTENDED_HOST_ID_SIZE];
		buffer.get(extendedHostId);
	}
}
