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

import com.ibm.disni.util.MemBuf;
import com.ibm.disni.util.MemoryAllocation;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IOCompletion {
	public final static int CSIZE = 8;

	private final static int STATUS_CODE_TYPE_INDEX = 0;
	private final static int STATUS_CODE_INDEX = 4;

	private static final int INVALID_STATUS_CODE_TYPE = -1;


	private int statusCodeType;
	private final MemBuf memBuf;
	private final ByteBuffer buffer;
	private final long address;
	private boolean pending;

	public IOCompletion() {
		MemoryAllocation memoryAllocation = MemoryAllocation.getInstance();
		memBuf = memoryAllocation.allocate(CSIZE, MemoryAllocation.MemType.DIRECT,
				IOCompletion.class.getCanonicalName());
		buffer = memBuf.getBuffer();
		buffer.order(ByteOrder.nativeOrder());
		address = ((DirectBuffer)buffer).address();
		pending = false;
	}

	void reset() throws PendingOperationException {
		if (statusCodeType == INVALID_STATUS_CODE_TYPE) {
			throw new PendingOperationException();
		}
		this.statusCodeType = INVALID_STATUS_CODE_TYPE;
		buffer.putInt(STATUS_CODE_INDEX, INVALID_STATUS_CODE_TYPE);
		pending = true;
	}

	void free() {
		memBuf.free();
	}

	long address() {
		return address;
	}

	public boolean isPending() {
		return pending;
	}

	public NvmeStatusCodeType getStatusCodeType() {
		return NvmeStatusCodeType.valueOf(statusCodeType);
	}

	public int getStatusCode() {
		return buffer.getInt(STATUS_CODE_INDEX);
	}

	public boolean done() {
		if (statusCodeType == INVALID_STATUS_CODE_TYPE) {
			/* Update */
			statusCodeType = buffer.getInt(STATUS_CODE_TYPE_INDEX);
		}
		pending = statusCodeType == INVALID_STATUS_CODE_TYPE;
		return !pending;
	}
}
