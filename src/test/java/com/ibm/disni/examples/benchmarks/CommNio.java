/*
 * jVerbs: RDMA verbs support for the Java Virtual Machine
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

package com.ibm.disni.examples.benchmarks;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

//used for control and data traffic, needs wait, start, read, write
public class CommNio {
	private SocketChannel socketChannel;
	
	public CommNio(SocketChannel socketChannel){
		this.socketChannel = socketChannel;
	}
	
	public void waitForNextRound(ByteBuffer buffer) throws IOException {
		buffer.clear();
		while (buffer.remaining() > 0) {
			socketChannel.read(buffer);
		}
	}

	public void startNextRound(ByteBuffer buffer) throws IOException {
		buffer.clear();
		while (buffer.remaining() > 0) {
			socketChannel.write(buffer);
		}
	}
	

	public void writeSG(ByteBuffer[] fragments, int size, int round) throws IOException {
		for (int i = 0; i < fragments.length; i++) {
			ByteBuffer buffer = fragments[i];
			buffer.clear();
		}

		long sum = 0;
		while (sum < size) {
			long result = socketChannel.write(fragments, 0, fragments.length);
			if (result > 0) {
				sum += result;
			} 
		}		
	}

	public void read(ByteBuffer[] fragments, int size, int round) throws IOException {
		for (int i = 0; i < fragments.length; i++) {
			ByteBuffer buffer = fragments[i];
			buffer.clear();
		}		
		
		long sum = 0;
		while (sum < size) {
			long result = socketChannel.read(fragments, 0, fragments.length);
			if (result > 0) {
				sum += result;
			}
		}
	}
}
