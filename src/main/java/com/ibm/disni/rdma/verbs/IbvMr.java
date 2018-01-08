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


// TODO: Auto-generated Javadoc
//struct ibv_mr {
//    struct ibv_context     *context;
//    struct ibv_pd          *pd;
//    void                   *addr;
//    size_t                  length;
//    uint32_t                handle;
//    uint32_t                lkey;
//    uint32_t                rkey;
//};

/**
 * Represents a memory region registered with the RDMA device.
 * 
 * Users should use the regMr verbs call to register memory and obtain a memory region. 
 */
public class IbvMr {
	public static int IBV_ACCESS_LOCAL_WRITE = 1;
	public static int IBV_ACCESS_REMOTE_WRITE = (1 << 1);
	public static int IBV_ACCESS_REMOTE_READ = (1 << 2);
	public static int IBV_ACCESS_REMOTE_ATOMIC = (1 << 3);
	public static int IBV_ACCESS_MW_BIND = (1 << 4);
	public static int IBV_ACCESS_ON_DEMAND = (1 << 6);
	
	private RdmaVerbs verbs;
	protected IbvContext context;
	protected long addr;
	protected int length;
	protected int access;
	protected int lkey;
	protected int rkey;
	protected int handle;
	protected volatile boolean isOpen;

//	public IbvMr() throws IOException {
//		this.context = null;
//	}

	public IbvMr(IbvContext context, long addr, int length, int access, int lkey, int rkey, int handle) throws IOException  {
		this.verbs = RdmaVerbs.open();
		this.context = context;
		this.addr = addr;
		this.length = length;
		this.access = access;
		this.lkey = lkey;
		this.rkey = rkey;
		this.handle = handle;
		this.isOpen = true;
	}

	/**
	 * Gets the address of the memory associated with this MR.
	 *
	 * @return the address of the memory registered.
	 */
	public long getAddr() {
		return addr;
	}
	
	/**
	 * Unsupported.
	 *
	 * @param addr the new addr
	 */
	public void setAddr(long addr) {
		this.addr = addr;
	}
	
	/**
	 * Gets the length of the memory associated with this MR.
	 *
	 * @return the length
	 */
	public int getLength() {
		return length;
	}
	
	/**
	 * Unsupported.
	 *
	 * @param length the new length
	 */
	public void setLength(int length) {
		this.length = length;
	}	

	/**
	 * Gets the local key of the memory associated with this MR.
	 * 
	 * Local keys can be passed around to enable accessing this memory from remote nodes.
	 *
	 * @return the local key.
	 */
	public int getLkey() {
		return lkey;
	}

	/**
	 * Unsupported.
	 *
	 * @param lkey the new lkey
	 */
	public void setLkey(int lkey) {
		this.lkey = lkey;
	}

	/**
	 * Gets the remote key of the memory associated with this MR.
	 *
	 * @return the remote key.
	 */
	public int getRkey() {
		return rkey;
	}

	/**
	 * Unsupported.
	 *
	 * @param rkey the new rkey
	 */
	public void setRkey(int rkey) {
		this.rkey = rkey;
	}
	
	/**
	 * A unique handle for this MR.
	 *
	 * @return the handle
	 */
	public int getHandle() {
		return handle;
	}	 

	public String toString() {
		return "handle=" + handle + ", addr= " + addr + ", length=" + length + ", access= " + access + ", lkey=" + lkey + ", rkey=" + rkey;
	}
	
	/**
	 * The RDMA device context this MR is registered with.
	 *
	 * @return the context
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
	
	public SVCDeregMr deregMr() throws IOException {
		return verbs.deregMr(this);
	}

	public int expPrefetchMr(long address, int length) throws IOException {
		return verbs.expPrefetchMr(this, address, length);
	}
}
