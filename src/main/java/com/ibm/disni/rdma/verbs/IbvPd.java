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
import java.nio.ByteBuffer;

// TODO: Auto-generated Javadoc
//struct ibv_pd {
//struct ibv_context     *context;
//uint32_t                handle;
//};

/**
 * The protection domain. Used for memory registration.
 */
public class IbvPd  {
	private RdmaVerbs verbs;
	
	protected int handle;
	protected IbvContext context;
	protected volatile boolean isOpen;

//	protected IbvPd() throws IOException{
//		this.verbs = RdmaVerbs.open();
//	}
	
	public IbvPd(IbvContext context) throws IOException {
		this.verbs = RdmaVerbs.open();
		this.context = context;
		this.isOpen = true;
	}

	/**
	 * A unique handle identifying this protection domain.
	 *
	 * @return the handle
	 * @throws IOException
	 */
	public int getHandle() throws IOException {
		return handle;
	}
	
	/**
	 * The RDMA device context this protection domain is attached to.
	 *
	 * @return the context
	 */
	public IbvContext getContext(){
		return context;
	}
	
	public boolean isOpen() {
		return isOpen;
	}

	public void close() {
		isOpen = false;
	}

	//---------- oo-verbs
	
	public SVCRegMr regMr(ByteBuffer buffer, int access) throws IOException {
		return verbs.regMr(this, buffer, access);
	}
	
	public SVCRegMr regMr(long address, int length, int access) throws IOException {
		return verbs.regMr(this, address, length, access);
	}

	public int deallocPd() throws Exception {
		return verbs.deallocPd(this);
	}
}
