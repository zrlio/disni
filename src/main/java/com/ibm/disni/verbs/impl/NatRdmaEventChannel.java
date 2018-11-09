/*
 * DiSNI: Direct Storage and Networking Interface
 *
 * Author: Patrick Stuedi <stu@zurich.ibm.com>
 *
 * Copyright (C) 2016-2018, IBM Corporation
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
import java.util.HashMap;

import com.ibm.disni.verbs.RdmaEventChannel;


public class NatRdmaEventChannel extends RdmaEventChannel implements NatObject {
	private HashMap<Long, NatCmaIdPrivate> idMap;
	private long objId;
	
	public NatRdmaEventChannel(long objId, int fd) throws IOException {
		super(fd);
		this.objId = objId;
		this.idMap = new HashMap<Long, NatCmaIdPrivate>();
	}

	public void addCmId(NatCmaIdPrivate cmId){
		this.idMap.put(cmId.getObjId(), cmId);
	}
	
	public NatCmaIdPrivate getCmId(long natid){
		return this.idMap.get(natid);
	}

	public long getObjId() {
		return objId;
	}
}
