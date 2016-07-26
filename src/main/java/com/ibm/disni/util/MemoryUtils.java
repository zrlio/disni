package com.ibm.disni.util;

import java.nio.ByteBuffer;

public class MemoryUtils {
	public static long getAddress(ByteBuffer buffer) {
		return ((sun.nio.ch.DirectBuffer) buffer).address();
	}
}
