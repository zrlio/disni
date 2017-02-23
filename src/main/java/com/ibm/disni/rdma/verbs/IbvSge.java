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

// TODO: Auto-generated Javadoc
//struct ibv_sge {
//    uint64_t                addr;
//    uint32_t                length;
//    uint32_t                lkey;
//};

/**
 * A scatter/gather element. Describes a local buffer. 
 */
public class IbvSge {
	protected long addr;
	protected int length;
	protected int lkey;

	public IbvSge() { 
	}

	/**
	 * The address of the local buffer;
	 *
	 * @return the addr
	 */
	public long getAddr() {
		return addr;
	}

	/**
	 * Sets the address of the local buffer;
	 *
	 * @param addr the new addr
	 */
	public void setAddr(long addr) {
		this.addr = addr;
	}

	/**
	 * The length of the local buffer;
	 *
	 * @return the length
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Sets the length of the local buffer;
	 *
	 * @param length the new length
	 */
	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * The RDMA key of the local buffer;
	 *
	 * @return the lkey
	 */
	public int getLkey() {
		return lkey;
	}

	/**
	 * Sets the RDMA key of the local buffer;
	 *
	 * @param lkey the new lkey
	 */
	public void setLkey(int lkey) {
		this.lkey = lkey;
	}

	public String getClassName() {
		return IbvSge.class.getCanonicalName();
	}
}
