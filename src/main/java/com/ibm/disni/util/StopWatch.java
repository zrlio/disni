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

public class StopWatch {
	private long startTime;
	private long executionTime;

	public StopWatch() {
		reset();
	}
	
	public void start() {
		startTime = getTime();
	}
	
	public void stop(){
		if (startTime > 0) {
			long currentTime = getTime();
			long _executionTime = currentTime - startTime;
			executionTime += _executionTime;
			startTime = 0;
		}		
	}
	
	public void reset(){
		startTime = 0;
		executionTime = 0;
	}
	
	public long getExecutionTime() {
		stop();
		return executionTime;
	}
	
	// --- private
	
	long getTime(){
		return System.currentTimeMillis();
	}
}
