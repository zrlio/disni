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

public class NativeAffinity {
	
	static {
	    System.loadLibrary("disni");
	}	
	
	public static void setAffinity(long affinity){
		if (affinity > 0){
			_setAffinity(affinity);
		}
	}
	
	public static long getAffinity(){
		return _getAffinity();
	}

	private native static void _setAffinity(long affinity);
	
	private native static long _getAffinity();
}
