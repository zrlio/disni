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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IOCompletion {
	public static int CSIZE = 8;
	private int statusCodeType;
	private int statusCode;

	private static final int INVALID_STATUS_CODE_TYPE = -1;

	private final MemBuf memBuf;
//	private final int position;
	private ByteBuffer buffer;
	
	private NativeDispatcher nativeDispatcher;
	private long objId;
	private long queueObjId;
	private long address;
	private long lba;
	private int count;
	private long completionAddress;

	IOCompletion(MemoryAllocation memoryAllocation) {
		this.memBuf = memoryAllocation.allocate(CSIZE,
				MemoryAllocation.MemType.DIRECT, this.getClass().getCanonicalName());
		this.buffer = memBuf.getBuffer();
		buffer.order(ByteOrder.nativeOrder());
//		position =  buffer.position();
		buffer.putInt(0, INVALID_STATUS_CODE_TYPE);
		update();
	}

	public IOCompletion(MemoryAllocation memoryAllocation, NativeDispatcher nativeDispatcher, long objId, long queueObjId, long address, long lba, int count) {
		this.memBuf = memoryAllocation.allocate(CSIZE,
				MemoryAllocation.MemType.DIRECT, this.getClass().getCanonicalName());
		this.buffer = memBuf.getBuffer();
		buffer.order(ByteOrder.nativeOrder());
//		this.position =  buffer.position();
		buffer.putInt(0, INVALID_STATUS_CODE_TYPE);
		
		this.nativeDispatcher = nativeDispatcher;
		this.objId = objId;
		this.queueObjId = queueObjId;
		this.address = address;
		this.lba = lba;
		this.count = count;
		this.completionAddress = this.address();
	}
	
	public void execute(long lbAddress) throws Exception {
		buffer.putInt(0, INVALID_STATUS_CODE_TYPE);
		statusCodeType = INVALID_STATUS_CODE_TYPE;
		System.out.println("calling native disp with: objId " + objId + ", queueObjId " + queueObjId + ", address " + address + ", linearBlockAddress " + lbAddress + ", count " + count + ", complAddress " + completionAddress + ", false " + false);
		int ret = nativeDispatcher._nvme_ns_io_cmd(objId, this.queueObjId, address, lbAddress, count, completionAddress, false);
		if (ret < 0) {
			throw new IOException("nvme_ns_cmd_read failed with " + ret);
		}		
	}

	private void update() {
//		ByteBuffer buffer = memBuf.getBuffer();
//		buffer.position(position);
		statusCodeType = buffer.getInt(0);
		statusCode = buffer.getInt(4);
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
