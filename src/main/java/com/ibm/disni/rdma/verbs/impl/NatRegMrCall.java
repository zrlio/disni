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
import java.nio.ByteBuffer;

import com.ibm.disni.rdma.verbs.IbvMr;
import com.ibm.disni.rdma.verbs.IbvPd;
import com.ibm.disni.rdma.verbs.SVCRegMr;
import com.ibm.disni.util.MemBuf;
import com.ibm.disni.util.MemoryAllocation;

public class NatRegMrCall extends SVCRegMr {
	private NativeDispatcher nativeDispatcher;
	private RdmaVerbsNat verbs;
	private MemoryAllocation memAlloc;

	private NatIbvPd pd;
	private int access;
	private int bufferCapacity;
	private MemBuf cmd;
	private long userAddress;
	
	private IbvMr mr;
	private boolean valid;
	
	public NatRegMrCall(RdmaVerbsNat verbs, NativeDispatcher nativeDispatcher, MemoryAllocation memAlloc) {
		this.verbs = verbs;
		this.nativeDispatcher = nativeDispatcher;
		this.memAlloc = memAlloc;
		this.cmd = null;
		this.valid = false;
	}

	public void set(IbvPd pd, ByteBuffer buffer, int access) {
		set(pd, ((sun.nio.ch.DirectBuffer) buffer).address(), buffer.capacity(), access);
	}

	public void set(IbvPd pd, long address, int length, int access) {
		this.pd = (NatIbvPd) pd;
		this.userAddress = address;
		this.bufferCapacity = length;
		this.access = access;
		
		if (cmd != null){
			cmd.free();
			cmd = null;
		}
		this.cmd = memAlloc.allocate(3*4, MemoryAllocation.MemType.DIRECT, this.getClass().getCanonicalName());
		this.valid = true;
	}

	public SVCRegMr execute() throws IOException {
		cmd.getBuffer().clear();
		if (!pd.isOpen()) {
			throw new IOException("Trying to register memory with closed PD.");
		}
		long objId = nativeDispatcher._regMr(pd.getObjId(), userAddress, bufferCapacity, access, cmd.address(), cmd.address() + 4, cmd.address() + 8);
		if (objId <= 0){
			throw new IOException("Memory registration failed with " + objId);
		} else {
			int lkey = cmd.getBuffer().getInt();
			int rkey = cmd.getBuffer().getInt();
			int handle = cmd.getBuffer().getInt();
			this.mr = new NatIbvMr(objId, null, userAddress, bufferCapacity, access, lkey, rkey, handle);
		}
		
		return this;
	}

	public IbvMr getMr() {
		return mr;
	}

	public boolean isValid() {
		return valid;
	}

	public SVCRegMr free() {
		if (cmd != null){
			cmd.free();
			cmd = null;
		}		
		this.valid = false;
		verbs.free(this);
		return this;
	}

	
}
