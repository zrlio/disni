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

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import com.ibm.disni.rdma.verbs.IbvCompChannel;
import com.ibm.disni.rdma.verbs.SVCGetCqEvent;


public class NatGetCqCall extends SVCGetCqEvent {
	private NativeDispatcher nativeDispatcher;
	private LinkedBlockingQueue<NatGetCqCall> getCqList;

	private NatIbvCompChannel compChannel;
	private boolean valid;
	private int timeout;
	
	public NatGetCqCall(LinkedBlockingQueue<NatGetCqCall> getCqList, NativeDispatcher nativeDispatcher, int timeout) {
		this.getCqList = getCqList;
		this.nativeDispatcher = nativeDispatcher;
		this.valid = false;
		this.timeout = timeout;
	}

	public void set(IbvCompChannel compChannel) {
		this.compChannel = (NatIbvCompChannel) compChannel;
		this.valid = true;
	}

	@Override
	public SVCGetCqEvent execute() throws IOException {
		if (!compChannel.isOpen()) {
			throw new IOException("Trying to get CQ event on closed completion channel.");
		}
		int ret = nativeDispatcher._getCqEvent(compChannel.getObjId(), timeout);
		if (ret != 0){
			throw new IOException("GetCQEvent failed");
		}
		return this;
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public SVCGetCqEvent free() {
		this.valid = false;
		getCqList.add(this);
		return this;
	}



}
