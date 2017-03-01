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

public class NvmeControllerOptions {
	public static int CSIZE = 248;

	private int numberIOQueues;
	private boolean useControllerMemoryBufferSubmissionQueues;
	private NvmeControllerArbitrationMechanism arbitrationMechanism;
	private int keepAliveTimeoutms;
	private int transportRetryCount;
	private int ioQueueSize;
	private String hostNvmeQualifiedName;
	private static int HOSTNQNSTRSIZE = 224;

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
		buffer.order(ByteOrder.nativeOrder());
		numberIOQueues = buffer.getInt();
		useControllerMemoryBufferSubmissionQueues = buffer.getInt() != 0;
		arbitrationMechanism = NvmeControllerArbitrationMechanism.valueOf(buffer.getInt());
		keepAliveTimeoutms = buffer.getInt();
		transportRetryCount = buffer.getInt();
		ioQueueSize = buffer.getInt();
		byte[] rawString = new byte[HOSTNQNSTRSIZE];
		buffer.get(rawString);
		hostNvmeQualifiedName = new String(rawString);
	}
}
