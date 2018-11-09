/*
 * DiSNI: Direct Storage and Networking Interface
 *
 * Author: Patrick Stuedi <stu@zurich.ibm.com>
 *
 * Copyright (C) 2016-2018, IBM Corporation
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
import java.util.concurrent.ConcurrentSkipListMap;

public class MemoryAllocation {
	private static MemoryAllocation instance = null;
	private ConcurrentSkipListMap<Integer, MemBuf> table;
	private static final int MIN_BLOCK_SIZE = 64; // 64B

	public synchronized static MemoryAllocation getInstance() {
		if (instance == null) {
			instance = new MemoryAllocation();
		}
		return instance;
	}
	
	private MemoryAllocation() {
		table = new ConcurrentSkipListMap<Integer, MemBuf>();
	}

	private int roundUpSize(int size){
		// Round up length to the nearest power of two, or the minimum block size
		if (size < MIN_BLOCK_SIZE) {
			size = MIN_BLOCK_SIZE;
		} else {
			size--;
			size |= size >> 1;
			size |= size >> 2;
			size |= size >> 4;
			size |= size >> 8;
			size |= size >> 16;
			size++;
		}
		return size;
	}


	public MemBuf allocate(int size) {
		size = roundUpSize(size);
		MemBuf buf = table.remove(size);
		if (buf == null) {
			return _allocate(size);
		} else {
			return buf;
		}
	}

	void free(MemBuf memBuf) {
		memBuf.getBuffer().clear();
		table.putIfAbsent(memBuf.size(), memBuf);
	}

	private MemBuf _allocate(int size) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(size);
		buffer.order(ByteOrder.nativeOrder());
		return new MemBuf(buffer, this);
	}

}
