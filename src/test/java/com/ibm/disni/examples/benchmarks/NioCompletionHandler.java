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

import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;

public class NioCompletionHandler<V, A> implements CompletionHandler<V, A> {
	private boolean taskDone;
	private boolean successful;
	private V result;

	public NioCompletionHandler() {
		reset();
	}

	public void reset() {
		this.taskDone = false;
		this.successful = false;
		this.result = null;
	}

	public V get() throws InterruptedException, ExecutionException {
		V _result = null;
		synchronized (this) {
			if (!taskDone) {
				this.wait();
			}
			_result = result;
		}
		return _result;
	}

	@Override
	public void completed(V result, A attachment) {
		synchronized (this) {
			this.taskDone = true;
			this.successful = true;
			this.result = result;
			this.notify();
		}
	}

	@Override
	public void failed(Throwable exc, A attachment) {
		synchronized (this) {
			this.taskDone = true;
			this.successful = false;
			this.notify();
		}
	}

	public boolean isSuccessful() {
		return successful;
	}
}
