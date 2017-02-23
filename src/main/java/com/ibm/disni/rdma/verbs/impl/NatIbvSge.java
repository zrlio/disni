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

import java.nio.ByteBuffer;

import com.ibm.disni.rdma.verbs.IbvSge;
import com.ibm.disni.rdma.verbs.SVCPostSend.SgeMod;

public class NatIbvSge extends IbvSge implements SgeMod {
	public static int CSIZE = 16;
	
	private NatPostSendCall postSendCall;
	private int bufPosition;
	
	public NatIbvSge(NatPostSendCall postSendCall, IbvSge sge){
		this.addr = sge.getAddr();
		this.length = sge.getLength();
		this.lkey = sge.getLkey();
		
		this.bufPosition = 0;
		this.postSendCall = postSendCall;
	}

	public int getBufPosition() {
		return bufPosition;
	}

	public void setBufPosition(int bufPosition) {
		this.bufPosition = bufPosition;
	}

	@Override
	public void setAddr(long addr) {
		super.setAddr(addr);
		postSendCall.setAddr(this, 0);
	}

	@Override
	public void setLength(int length) {
		super.setLength(length);
		postSendCall.setLength(this, 8);
	}

	@Override
	public void setLkey(int lkey) {
		super.setLkey(lkey);
		postSendCall.setLkey(this, 12);
	}

	public void writeBack(ByteBuffer buffer) {
		this.bufPosition = buffer.position();
		buffer.putLong(getAddr());
		buffer.putInt(getLength());
		buffer.putInt(getLkey());		
	}
	
	
}
