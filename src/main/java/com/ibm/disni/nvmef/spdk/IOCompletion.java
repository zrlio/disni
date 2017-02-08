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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IOCompletion {
	public static int CSIZE = 8;
	private int statusCodeType;
	private int statusCode;

	private static final int INVALID_STATUS_CODE_TYPE = -1;

	private final MemBuf memBuf;
	private final int position;

	IOCompletion(MemoryAllocation memoryAllocation) {
		this.memBuf = memoryAllocation.allocate(CSIZE,
				MemoryAllocation.MemType.DIRECT, this.getClass().getCanonicalName());
		ByteBuffer buffer = memBuf.getBuffer();
		buffer.order(ByteOrder.nativeOrder());
		position =  buffer.position();
		buffer.putInt(INVALID_STATUS_CODE_TYPE);
		update();
	}

	private void update() {
		ByteBuffer buffer = memBuf.getBuffer();
		buffer.position(position);
		statusCodeType = buffer.getInt();
		statusCode = buffer.getInt();
	}

	private void free() {
		memBuf.free();
	}

	long address() {
		return memBuf.address();
	}

	public NvmeStatusCodeType getStatusCodeType() {
		return NvmeStatusCodeType.valueOf(statusCodeType);
	}

	public int getStatusCode() {
		return statusCode;
	}

	public boolean done() {
		if (statusCodeType == INVALID_STATUS_CODE_TYPE) {
			update();
			if (statusCodeType != INVALID_STATUS_CODE_TYPE) {
				free();
			}
		}
		return statusCodeType != INVALID_STATUS_CODE_TYPE;
	}
}
