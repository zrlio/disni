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

import com.ibm.disni.rdma.verbs.IbvContext;
import com.ibm.disni.rdma.verbs.IbvQP;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.rdma.verbs.RdmaEventChannel;


public class NatCmaIdPrivate extends RdmaCmId implements NatObject {
	private long objId;
	private NativeDispatcher nativeDispatcher;

	public NatCmaIdPrivate(long objId, RdmaEventChannel cmChannel, NativeDispatcher nativeDispatcher) throws IOException {
		super(cmChannel, null);
		this.objId = objId;
		this.nativeDispatcher = nativeDispatcher;
	}

	public long getObjId() {
		return objId;
	}

	@Override
	public String toString() {
		return "NatCmaIdPrivate [natid=" + objId + "]";
	}

	@Override
	public IbvContext getVerbs() throws IOException {
		if (verbs == null){
			if (!isOpen()) {
				throw new IOException("Trying to get context on closed ID");
			}
			long _obj_id = nativeDispatcher._getContext(objId);
			if (_obj_id >= 0){
				NatIbvContext context = new NatIbvContext(_obj_id, nativeDispatcher);
				setVerbs(context);
			}
		}
		return super.getVerbs();
	}
	
	public void setVerbs(IbvContext verbs){
		super.setVerbs(verbs);
	}

	protected void setQp(IbvQP qpObj) {
		super.setQp(qpObj);
	}

}
