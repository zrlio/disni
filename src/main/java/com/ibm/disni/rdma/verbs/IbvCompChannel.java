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

package com.ibm.disni.rdma.verbs;

import java.io.IOException;

//struct ibv_comp_channel {
//    struct ibv_context     *context;
//    int                     fd;
//    int                     refcnt;
//};

/**
 * Represents a completion channel. IOCompletion channels are used to query completion queues (CQs) for new events.
 */
public class IbvCompChannel {
	private RdmaVerbs verbs;
	private int fd;
	protected IbvContext context;
	protected volatile boolean isOpen;

	protected IbvCompChannel(int fd, IbvContext context) throws IOException {
		this.verbs = RdmaVerbs.open();
		this.fd = fd;
		this.context = context;
		this.isOpen = true;
	}

	/**
	 * This method returns the file descriptor used by the completion channel.
	 *
	 * @return the file descriptor.
	 */
	public int getFd() {
		return fd;
	}
	
	/**
	 * Gets the device context associated with this completion channel.
	 *
	 * @return the device context.
	 */
	public IbvContext getContext(){
		return this.context;
	}
	
	public boolean isOpen() {
		return isOpen;
	}

	public void close() {
		isOpen = false;
	}

	//---------- oo-verbs
	
	public boolean getCqEvent(IbvCQ cq, int timeout) throws IOException {
		return verbs.getCqEvent(this, cq, timeout);
	}
	
	public int destroyCompChannel() throws IOException {
		return verbs.destroyCompChannel(this);
	}
}
