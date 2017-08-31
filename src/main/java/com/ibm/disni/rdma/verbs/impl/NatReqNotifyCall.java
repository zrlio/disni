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

import com.ibm.disni.rdma.verbs.IbvCQ;
import com.ibm.disni.rdma.verbs.SVCReqNotify;


public class NatReqNotifyCall extends SVCReqNotify {
	private NativeDispatcher nativeDispatcher;
	private RdmaVerbsNat verbs;
	
	private NatIbvCQ cq;
	private int solicited;
	private boolean valid;
	
	
	public NatReqNotifyCall(RdmaVerbsNat verbs, NativeDispatcher nativeDispatcher) {
		this.verbs = verbs;
		this.nativeDispatcher = nativeDispatcher;
		this.valid = false;
	}

	public void set(IbvCQ cq, boolean solicited_only) {
		this.cq = (NatIbvCQ) cq;
		this.solicited = solicited_only ? 1 : 0;
		this.valid = true;
	}

	public SVCReqNotify execute() throws IOException {
		if (!cq.isOpen()) {
			throw new IOException("Trying to execute reqNotifyCQ() on closed CQ.");
		}
		int ret = nativeDispatcher._reqNotifyCQ(cq.getObjId(), solicited);
		if (ret != 0){
			throw new IOException("Request for Notification failed");
		}
		return this;
	}

	public boolean isValid() {
		return valid;
	}

	public SVCReqNotify free() {
		this.valid = false;
		verbs.free(this);
		return this;
	}

}
