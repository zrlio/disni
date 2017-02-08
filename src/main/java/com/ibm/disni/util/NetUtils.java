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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NetUtils {
	private final static boolean nativeIsNetwork = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
	
	private NetUtils(){
	}
	
	public static int getIntIPFromName(String name) throws UnknownHostException {
		InetAddress localHost = InetAddress.getByName(name);
		byte[] addr = localHost.getAddress();
		ByteBuffer buffer = ByteBuffer.wrap(addr);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.clear();
		return buffer.getInt();
	}

	public static int getIntIPFromInetAddress(InetAddress localHost) throws UnknownHostException {
		if (localHost == null){
			throw new UnknownHostException("Address not defined");
		}
		byte[] addr = localHost.getAddress();
		ByteBuffer buffer = ByteBuffer.wrap(addr);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.clear();
		return buffer.getInt();
	}
	
	public static InetAddress getInetAddressFromIntIP(int intIP)
			throws UnknownHostException {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(intIP);
		buffer.clear();
		byte[] addr = new byte[4];
		buffer.get(addr);
		InetAddress address = InetAddress.getByAddress(addr);
		return address;
	}
	
	public static short hostToNetworkByteOrder(short x) {
		if (nativeIsNetwork){
			return x;
		}
		return EndianUtils.swap(x);
	}

	public static char hostToNetworkByteOrder(char x) {
		if (nativeIsNetwork){
			return x;
		}		
		return EndianUtils.swap(x);
	}

	public static int hostToNetworkByteOrder(int x) {
		if (nativeIsNetwork){
			return x;
		}		
		return EndianUtils.swap(x);
	}

	public static long hostToNetworkByteOrder(long x) {
		if (nativeIsNetwork){
			return x;
		}		
		return EndianUtils.swap(x);
	}	
	
	public static short networkToHostByteOrder(short x) {
		if (nativeIsNetwork){
			return x;
		}		
		return EndianUtils.swap(x);
	}
	
	public static char networkToHostByteOrder(char x) {
		if (nativeIsNetwork){
			return x;
		}		
		return EndianUtils.swap(x);
	}
	
	public static int networkToHostByteOrder(int x) {
		if (nativeIsNetwork){
			return x;
		}		
		return EndianUtils.swap(x);
	}
	
	public static long networkToHostByteOrder(long x) {
		if (nativeIsNetwork){
			return x;
		}		
		return EndianUtils.swap(x);
	}

	public static int getIntIPFromInetAddress(String string) {
		return 0;
	}	
}
