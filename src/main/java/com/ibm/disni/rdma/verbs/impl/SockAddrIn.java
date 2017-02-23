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

package com.ibm.disni.rdma.verbs.impl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SockAddrIn {
	public static short AF_INET = 2;
	public static int CSIZE = 28;
	
	protected short sin_family;
	protected int sin_addr;
	protected short sin_port;
	
	protected byte[] sin_zero; 

	SockAddrIn() {
		this.sin_family = AF_INET;
		this.sin_addr = 0;
		this.sin_port = 0;
		
		sin_zero = new byte[8];
	}
	
	SockAddrIn(short sin_family, int sin_addr, short sin_port) {
		this.sin_family = sin_family;
		this.sin_addr = sin_addr;
		this.sin_port = sin_port;
		
		sin_zero = new byte[8];
	}
	
	public void writeBack(ByteBuffer buffer) {
		buffer.putShort(sin_family);
		buffer.putShort(sin_port);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(sin_addr);
		buffer.order(ByteOrder.nativeOrder());
	}

	public void update(ByteBuffer buffer) {
		sin_family = buffer.getShort();
		sin_port = buffer.getShort();
		sin_addr = buffer.getInt();
	}

	public int size() {
		return CSIZE;
	}

	public short getSin_family() {
		return sin_family;
	}

	public void setSin_family(short sin_family) {
		this.sin_family = sin_family;
	}

	public short getSin_port() {
		return sin_port;
	}

	public void setSin_port(short sin_port) {
		this.sin_port = sin_port;
	}

	public int getSin_addr() {
		return sin_addr;
	}

	public void setSin_addr(int sin_addr) {
		this.sin_addr = sin_addr;
	}

	public byte[] getSin_zero() {
		return sin_zero;
	}

	public void setSin_zero(byte[] sin_zero) {
		this.sin_zero = sin_zero;
	}

	public String getClassName() {
		return SockAddrIn.class.getCanonicalName();
	}
}
