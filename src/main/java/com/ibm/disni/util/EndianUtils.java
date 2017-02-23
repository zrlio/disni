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

public class EndianUtils {
	public static short swap(short x) {
		return (short) ((x << 8) | ((char) x >>> 8));
	}

	public static char swap(char x) {
		return (char) ((x << 8) | (x >>> 8));
	}

	public static int swap(int x) {
		return ((x << 24) | ((x & 0x0000ff00) << 8) | ((x & 0x00ff0000) >>> 8) | (x >>> 24));
	}

	public static long swap(long x) {
		return (((long) swap((int) x) << 32) | ((long) swap((int) (x >>> 32)) & 0xffffffffL));
	}
}
