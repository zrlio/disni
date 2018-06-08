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
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryAllocation {
	public enum MemType {
		INDIRECT, DIRECT,
	}

	private static MemoryAllocation instance = null;
	private ConcurrentHashMap<String, MemBuf> table;

	public synchronized static MemoryAllocation getInstance() {
		if (instance == null) {
			instance = new MemoryAllocation();
		}
		return instance;
	}
	
	private MemoryAllocation() {
		table = new ConcurrentHashMap<String, MemBuf>();
	}
	
	public MemBuf allocate(int size, MemType type, String classname) {
		if (type == MemType.DIRECT) {
			String key = generateKey(size, type, classname);
			MemBuf buf = table.remove(key);
			if (buf != null) {
				buf.getBuffer().clear();
				return buf;
			}
		}
		return _allocate(size, type, classname);
	}
	

	public void free(MemBuf memBuf) {
		if (memBuf.getType() == MemType.DIRECT) {
			String key = generateKey(memBuf.getBuffer().capacity(),
					memBuf.getType(), memBuf.getClassname());
			table.put(key, memBuf);
		} 
	}

	private String generateKey(int size, MemType type, String classname) {
		String key = classname + ":" + size + ":" + type.ordinal();
		return key;
	}

	private MemBuf _allocate(int size, MemType type, String classname) {
		if (type == MemType.INDIRECT) {
			ByteBuffer buffer = ByteBuffer.allocate(size);
			buffer.order(ByteOrder.nativeOrder());
			long address = 0;
			return new MemBuf(MemType.INDIRECT, buffer, address, this,
					classname);
		} else {
			ByteBuffer buffer = ByteBuffer.allocateDirect(size);
			buffer.order(ByteOrder.nativeOrder());
			long address = MemoryUtils.getAddress(buffer);
			return new MemBuf(MemType.DIRECT, buffer, address, this, classname);
		}
	}
}
