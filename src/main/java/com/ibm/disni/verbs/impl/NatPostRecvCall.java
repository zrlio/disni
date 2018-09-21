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
import java.util.ArrayList;
import java.util.List;

import com.ibm.disni.verbs.IbvQP;
import com.ibm.disni.verbs.IbvRecvWR;
import com.ibm.disni.verbs.IbvSge;
import com.ibm.disni.verbs.SVCPostRecv;
import com.ibm.disni.util.MemBuf;
import com.ibm.disni.util.MemoryAllocation;


public class NatPostRecvCall extends SVCPostRecv {
	private NativeDispatcher nativeDispatcher;
	private RdmaVerbsNat verbs;
	private MemoryAllocation memAlloc;
	
	private NatIbvQP qp;
	private ArrayList<NatIbvRecvWR> wrNatList;
	private ArrayList<IbvSge> sgeNatList;
	
	private MemBuf cmd;
	private boolean valid;
	
	public NatPostRecvCall(RdmaVerbsNat verbs, NativeDispatcher nativeDispatcher,
	                       MemoryAllocation memAlloc, IbvQP qp, List<IbvRecvWR> wrList) {
		this.verbs = verbs;
		this.nativeDispatcher = nativeDispatcher;
		this.memAlloc = memAlloc;
		this.qp = (NatIbvQP) qp;
		
		this.wrNatList = new ArrayList<NatIbvRecvWR>(wrList.size());
		this.sgeNatList = new ArrayList<IbvSge>();
		this.valid = false;

		int size = 0;
		for (IbvRecvWR recvWR : wrList) {
			size += NatIbvRecvWR.CSIZE;
			if (recvWR.getNum_sge() > 0) {
				size += recvWR.getSg_list().size()*NatIbvSge.CSIZE;
			}
		}
		this.cmd = memAlloc.allocate(size);
		setWrList(wrList);
	}
	
	private void setWrList(List<IbvRecvWR> wrList) {
		wrNatList.clear();
		sgeNatList.clear();
		cmd.getBuffer().clear();

		long sgeOffset = wrList.size()*NatIbvRecvWR.CSIZE;
		long wrOffset = NatIbvRecvWR.CSIZE;
		for (IbvRecvWR recvWR : wrList){
			NatIbvRecvWR natRecvWR = new NatIbvRecvWR(recvWR);
			natRecvWR.setNext(wrOffset);
			wrNatList.add(natRecvWR);

			wrOffset += NatIbvRecvWR.CSIZE;

			if (recvWR.getNum_sge() > 0) {
				natRecvWR.setPtr_sge_list(sgeOffset);
				sgeNatList.addAll(recvWR.getSg_list());
				sgeOffset += recvWR.getSg_list().size()*NatIbvSge.CSIZE;
			}
		}
		
		for (NatIbvRecvWR natWR : wrNatList){
			natWR.shiftAddress(cmd.address());
		}
		wrNatList.get(wrNatList.size() - 1).setNext(0);
		
		for (NatIbvRecvWR natWR : wrNatList){
			natWR.writeBack(cmd.getBuffer());
		}
		for (IbvSge sge : sgeNatList){
			cmd.getBuffer().putLong(sge.getAddr());
			cmd.getBuffer().putInt(sge.getLength());
			cmd.getBuffer().putInt(sge.getLkey());
		}
		
		this.valid = true;
	}	

	@Override
	public SVCPostRecv execute() throws IOException {
		if (!qp.isOpen()) {
			throw new IOException("Trying to post receive on closed QP");
		}
		int ret = nativeDispatcher._postRecv(qp.getObjId(), cmd.address());
		if (ret != 0){
			throw new IOException("Post recv failed");
		}
		return this;
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public SVCPostRecv free() {
		if (cmd != null){
			cmd.free();
			cmd = null;
		}		
		this.valid = false;
		return this;
	}

	@Override
	public RecvWRMod getWrMod(int index) throws IOException {
		return wrNatList.get(index);
	}

}
