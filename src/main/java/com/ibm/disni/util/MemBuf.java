/*
 * DiSNI: Direct Storage and Networking Interface
 *
 * Author: Patrick Stuedi <stu@zurich.ibm.com>
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

package com.ibm.disni.util;

import java.nio.ByteBuffer;

import org.slf4j.Logger;

public class MemBuf {
	private static final Logger logger = DiSNILogger.getLogger();

	private MemoryAllocation.MemType type;
	private long address;
	private ByteBuffer buffer;
	private MemoryAllocation memAlloc;
	private String classname;

	public MemBuf(MemoryAllocation.MemType type, ByteBuffer buffer,
			long address, MemoryAllocation memAlloc, String classname) {
		this.type = type;
		this.buffer = buffer;
		this.address = address;
		this.memAlloc = memAlloc;
		this.classname = classname;
	}
	
	public final long address() {
		return address;
	}
	
	//----------------------------
	
	public final ByteBuffer getBuffer() {
		return buffer;
	}

	public MemoryAllocation.MemType getType() {
		return type;
	}
	
	public String getClassname() {
		return this.classname;
	}	
	
	public void dump() {
		dump(0, buffer.capacity());
	}
	
	public void dump(int offset, int length) {
		MemBuf.dump(buffer, offset, length);
	}
	
	public static void dump(ByteBuffer buffer, int offset, int length) {
		if (offset + length > buffer.capacity()) {
			return;
		}

		String outputbuf = "";
		for (int i = offset; i < offset + length; i++) {
			byte tmp1 = buffer.get(i);
			int tmp2 = tmp1 & 0xff;
			outputbuf += tmp2 + " ";
		}
		logger.info(outputbuf);
	}

	public void free() {
		if (memAlloc != null){
			memAlloc.free(this);
		}
	}
}
