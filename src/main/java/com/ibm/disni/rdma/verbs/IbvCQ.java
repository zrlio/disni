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


//struct ibv_cq {
//    struct ibv_context     *context;
//    struct ibv_comp_channel *channel;
//    void                   *cq_context;
//    uint32_t                handle;
//    int                     cqe;
//
//    pthread_mutex_t         mutex;
//    pthread_cond_t          cond;
//    uint32_t                comp_events_completed;
//    uint32_t                async_events_completed;
//};

/**
 * Represents a completion queue. 
 * 
 * IOCompletion queues hold events indicated the completion of data transfer operations.
 * 
 * Several queue pairs (QPs) can share the same completion queue.
 */
public class IbvCQ {
	private RdmaVerbs verbs;
	
	protected IbvContext context;
	protected IbvCompChannel channel;
	protected int cqe;
	protected int handle;
	protected volatile boolean isOpen;

	public IbvCQ(IbvContext context, IbvCompChannel compChannel, int handle) throws IOException  {
		this.verbs = RdmaVerbs.open();
		this.context = context;
		this.channel = compChannel;
		this.handle = handle;
		this.isOpen = true;
	}

	/**
	 * A unique handle for this completion queue.
	 *
	 * @return a unique handle.
	 */
	public int getHandle() {
		return handle;
	}

	/**
	 * The device context for this completion queue.
	 *
	 * @return the device context.
	 */
	public IbvContext getContext() {
		return context;
	}

	/**
	 * Gets the completion channel associated with this CQ.
	 *
	 * @return the completion channel.
	 */
	public IbvCompChannel getChannel() {
		return channel;
	}

	/**
	 * Gets the number of event entries this CQ can possibly store.
	 *
	 * @return the maximum number of event entries.
	 */
	public int getCqe() {
		return cqe;
	}
	
	public boolean isOpen() {
		return isOpen;
	}

	public void close() {
		isOpen = false;
	}

	//---------- oo-verbs
	
	public SVCPollCq poll(IbvWC[] wcList, int ne) throws IOException {
		return verbs.pollCQ(this, wcList, ne);
	}
	
	public SVCReqNotify reqNotification(boolean solicited_only) throws IOException {
		return verbs.reqNotifyCQ(this, solicited_only);
	}
	
	public int ackEvents(int nevents) throws IOException {
		return verbs.ackCqEvents(this, nevents);
	}
	
	public int destroyCQ() throws IOException {
		return verbs.destroyCQ(this);
	}
}
