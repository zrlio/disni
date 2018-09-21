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

package com.ibm.disni.verbs.impl;

import java.io.IOException;

import com.ibm.disni.verbs.IbvContext;

public class NatIbvContext extends IbvContext implements NatObject {
	private long objId;
	private NativeDispatcher nativeDispatcher;

	public NatIbvContext(long objId, NativeDispatcher nativeDispatcher) throws IOException{
		super(-1, -1);
		this.objId = objId;
		this.nativeDispatcher = nativeDispatcher;
	}

	public long getObjId() {
		return objId;
	}

	@Override
	public int getCmd_fd() throws IOException {
		if (this.cmd_fd < 0){
			if (!isOpen()) {
				throw new IOException("Trying to get context FD while context is already closed.");
			}
			this.cmd_fd = nativeDispatcher._getContextFd(objId);
		}		
		return super.getCmd_fd();
	}

	@Override
	public int getNumCompVectors() {
		if (this.numCompVectors < 0){
			this.numCompVectors = nativeDispatcher._getContextNumCompVectors(objId);
		}
		return super.getNumCompVectors();
	}
}
