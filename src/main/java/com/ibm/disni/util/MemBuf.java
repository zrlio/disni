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

	private long address;
	private ByteBuffer buffer;

	public static long getAddress(ByteBuffer buffer) {
		return ((sun.nio.ch.DirectBuffer) buffer).address();
	}

	public MemBuf(ByteBuffer buffer) {
		this.buffer = buffer;
		this.address = getAddress(buffer);
	}
	
	public final long address() {
		return address;
	}
	
	//----------------------------
	
	public final ByteBuffer getBuffer() {
		return buffer;
	}

	public final Integer size() {
		return buffer.capacity();
	}
}
