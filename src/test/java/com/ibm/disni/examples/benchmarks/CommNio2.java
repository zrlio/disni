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

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

//only used for data traffic, needs read, write
public class CommNio2 {
	private AsynchronousSocketChannel socketChannel;
	private NioCompletionHandler<Long, Nio2Server> completionHandler;
	
	public CommNio2(AsynchronousSocketChannel socketChannel){
		this.socketChannel = socketChannel;
		this.completionHandler = new NioCompletionHandler<Long, Nio2Server>();
	}

	public NioCompletionHandler<Long, Nio2Server> initSGRead(ByteBuffer[] fragments) {
		for (int i = 0; i < fragments.length; i++) {
			fragments[i].clear();
		}
		completionHandler.reset();
		socketChannel.read(fragments, 0, fragments.length, 10, TimeUnit.SECONDS, null, completionHandler);
		return completionHandler;
	}

	public boolean completeSGRead(ByteBuffer[] fragments,
			int size, int round)
			throws InterruptedException, ExecutionException {
		long sum = 0;
		Long result = completionHandler.get();
		if (result != null) {
			sum += result.longValue();
		}
		while (sum < size) {
			completionHandler.reset();
			socketChannel.read(fragments, 0, fragments.length, 10, TimeUnit.SECONDS, null, completionHandler);
			result = completionHandler.get();
			if (result != null) {
				sum += result.longValue();
			}
		}
		boolean res = true;
		return res;
	}
	
	public void writeSG(ByteBuffer[] fragments, long size, int round)
			throws InterruptedException, ExecutionException {
		for (int i = 0; i < fragments.length; i++) {
			ByteBuffer buffer = fragments[i];
			buffer.clear();
		}

		long sum = 0;

		while (sum < size) {
			completionHandler.reset();
			socketChannel.write(fragments, 0, fragments.length, 10,
					TimeUnit.SECONDS, null, completionHandler);
			Long result = completionHandler.get();
			if (result != null) {
				sum += result.longValue();
			} 
		}
	}
}
