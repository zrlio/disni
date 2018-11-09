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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.ibm.disni.verbs.IbvQP;
import com.ibm.disni.verbs.IbvSendWR;
import com.ibm.disni.verbs.IbvSge;
import com.ibm.disni.verbs.SVCPostSend;
import com.ibm.disni.verbs.impl.NatIbvSendWR.NatRdma;
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
	
	public NatPostSendCall(RdmaVerbsNat verbs, NativeDispatcher nativeDispatcher,
	                       MemoryAllocation memAlloc, IbvQP qp, List<IbvSendWR> wrList) {
		this.verbs = verbs;
		this.nativeDispatcher = nativeDispatcher;
		this.memAlloc = memAlloc;
		
		this.wrNatList = new ArrayList<NatIbvSendWR>(wrList.size());
		this.sgeNatList = new ArrayList<NatIbvSge>();
		this.valid = false;
		this.qp = (NatIbvQP) qp;
		int size = 0;
		for (IbvSendWR sendWR : wrList){
			size += NatIbvSendWR.CSIZE;
			size += sendWR.getSg_list().size()*NatIbvSge.CSIZE;
		}
		this.cmd = memAlloc.allocate(size);
		setWrList(wrList);
	}

	private void setWrList(List<IbvSendWR> wrList) {
		wrNatList.clear();
		sgeNatList.clear();
		cmd.getBuffer().clear();
		
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

			wrOffset += NatIbvSendWR.CSIZE;
			sgeOffset += sendWR.getSg_list().size()*NatIbvSge.CSIZE;
		}
		
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
		nativeDispatcher._postSend(qp.getObjId(), cmd.address());
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
