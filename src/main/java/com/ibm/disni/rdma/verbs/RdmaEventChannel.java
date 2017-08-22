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

//struct rdma_event_channel {
//    int                     fd;
//};

/**
 * Represents a communication channel. 
 * 
 * RDMA identifiers require an associated communication channel. All connection events (e.g., RDMA_CM_EVENT_CONNECT_REQUEST, RDMA_CM_EVENT_ESTABLISHED) will be delivered through this channel.
 */
public class RdmaEventChannel {
	private RdmaCm cm;
	private int fd;
	protected volatile boolean isOpen;
	
	protected RdmaEventChannel(int fd) throws IOException{
		this.cm = RdmaCm.open();
		this.fd = fd;
		this.isOpen = true;
	}

	/**
	 * The file descriptor associated with this communication channel. 
	 *
	 * @return the file descriptor. 
	 */
	public int getFd() {
		return fd;
	}
	
	public boolean isOpen() {
		return isOpen;
	}

	public void close() {
		isOpen = false;
	}

	//---------- oo-verbs
	
	public static RdmaEventChannel createEventChannel() throws IOException{
		RdmaCm cm = RdmaCm.open();
		return cm.createEventChannel();
	}
	
	public RdmaCmId createId(short rdma_ps) throws IOException{
		return cm.createId(this, rdma_ps);
	}
	
	public RdmaCmEvent getCmEvent(int timeout) throws IOException{
		return cm.getCmEvent(this, timeout);
	}
	
	public int destroyEventChannel() throws IOException{
		return cm.destroyEventChannel(this);
	}
} 
