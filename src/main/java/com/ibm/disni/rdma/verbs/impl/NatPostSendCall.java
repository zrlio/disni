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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.ibm.disni.rdma.verbs.IbvQP;
import com.ibm.disni.rdma.verbs.IbvSendWR;
import com.ibm.disni.rdma.verbs.IbvSge;
import com.ibm.disni.rdma.verbs.SVCPostSend;
import com.ibm.disni.rdma.verbs.impl.NatIbvSendWR.NatRdma;
import com.ibm.disni.util.MemBuf;
import com.ibm.disni.util.MemoryAllocation;


public class NatPostSendCall extends SVCPostSend {
	private NativeDispatcher nativeDispatcher;
	private RdmaVerbsNat verbs;
	private MemoryAllocation memAlloc;
	
	private ArrayList<NatIbvSendWR> wrNatList;
	private ArrayList<NatIbvSge> sgeNatList;
	private NatIbvQP qp;
	
	private MemBuf cmd;
	private boolean valid;
	
	public NatPostSendCall(RdmaVerbsNat verbs, NativeDispatcher nativeDispatcher, MemoryAllocation memAlloc) {
		this.verbs = verbs;
		this.nativeDispatcher = nativeDispatcher;
		this.memAlloc = memAlloc;
		
		this.wrNatList = new ArrayList<NatIbvSendWR>();
		this.sgeNatList = new ArrayList<NatIbvSge>();
		this.valid = false;
	}

	public void set(IbvQP qp, List<IbvSendWR> wrList) {
		this.qp = (NatIbvQP) qp;
		wrNatList.clear();
		sgeNatList.clear();
		int size = 0;
		
		long sgeOffset = wrList.size()*NatIbvSendWR.CSIZE;
		long wrOffset = NatIbvSendWR.CSIZE;
		for (IbvSendWR sendWR : wrList){
			LinkedList<IbvSge> sg_list = new LinkedList<IbvSge>();
			for (IbvSge sge : sendWR.getSg_list()){
				NatIbvSge natSge = new NatIbvSge(this, sge);
				sg_list.add(natSge);
				sgeNatList.add(natSge);
			}
			
			NatRdma natRdma = new NatRdma(sendWR.getRdma(), this);
			NatIbvSendWR natSendWR = new NatIbvSendWR(this, natRdma, sendWR, sg_list);
			natSendWR.setPtr_sge_list(sgeOffset);
			natSendWR.setNext(wrOffset);
			wrNatList.add(natSendWR);
			
			size += NatIbvSendWR.CSIZE;
			size += sendWR.getSg_list().size()*NatIbvSge.CSIZE;
			wrOffset += NatIbvSendWR.CSIZE;
			sgeOffset += sendWR.getSg_list().size()*NatIbvSge.CSIZE;
		}
		
		if (cmd != null){
			cmd.free();
			cmd = null;
		}		
		this.cmd = memAlloc.allocate(size, MemoryAllocation.MemType.DIRECT, this.getClass().getCanonicalName());
		
		for (NatIbvSendWR natWR : wrNatList){
			natWR.shiftAddress(cmd.address());
		}
		wrNatList.get(wrNatList.size() - 1).setNext(0);
		
		for (NatIbvSendWR natWR : wrNatList){
			natWR.writeBack(cmd.getBuffer());
		}
		for (NatIbvSge sge : sgeNatList){
			sge.writeBack(cmd.getBuffer());
		}
		
		this.valid = true;
	}

	@Override
	public SVCPostSend execute() throws IOException {
		if (!qp.isOpen()) {
			throw new IOException("Trying to post send on closed QP");
		}
		int ret = nativeDispatcher._postSend(qp.getObjId(), cmd.address());
		if (ret != 0){
			throw new IOException("Post send failed");
		}
		return this;
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public SVCPostSend free() {
		if (cmd != null){
			cmd.free();
			cmd = null;
		}		
		this.valid = false;
		verbs.free(this);
		return this;
	}
	
	public SendWRMod getWrMod(int index) throws IOException{
		return wrNatList.get(index);
	}
	
	//---------------------
	
	void setWr_id(NatIbvSendWR sendWR, int offset) {
		int position = sendWR.getBufPosition() + offset;
		cmd.getBuffer().putLong(position, sendWR.getWr_id());		
	}
	
	public void setRemote_addr(NatRdma rdma, int offset) {
		int position = rdma.getBufPosition() + offset;
		cmd.getBuffer().putLong(position, rdma.getRemote_addr());
	}

	public void setRkey(NatRdma rdma, int offset) {
		int position = rdma.getBufPosition() + offset;
		cmd.getBuffer().putInt(position, rdma.getRkey());		
	}

	public void setReserved(NatRdma rdma, int offset) {
		int position = rdma.getBufPosition() + offset;
		cmd.getBuffer().putInt(position, rdma.getReserved());
	}

	public void setAddr(NatIbvSge sge, int offset) {
		int position = sge.getBufPosition() + offset;
		cmd.getBuffer().putLong(position, sge.getAddr());
	}

	public void setLength(NatIbvSge sge, int offset) {
		int position = sge.getBufPosition() + offset;
		cmd.getBuffer().putInt(position, sge.getLength());		
	}

	public void setLkey(NatIbvSge sge, int offset) {
		int position = sge.getBufPosition() + offset;
		cmd.getBuffer().putInt(position, sge.getLkey());	
	}
	
	void setSend_flags(NatIbvSendWR sendWR, int offset) {
		int position = sendWR.getBufPosition() + offset;
		cmd.getBuffer().putInt(position, sendWR.getSend_flags());		
	}	
}
