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
import java.util.LinkedList;

import com.ibm.disni.rdma.verbs.IbvSendWR;
import com.ibm.disni.rdma.verbs.IbvSge;
import com.ibm.disni.rdma.verbs.SVCPostSend.RdmaMod;
import com.ibm.disni.rdma.verbs.SVCPostSend.SendWRMod;
import com.ibm.disni.rdma.verbs.SVCPostSend.SgeMod;


//struct ibv_send_wr {
//uint64_t                wr_id; 
//struct ibv_send_wr     *next;
//struct ibv_sge         *sg_list;
//int                     num_sge;
//enum ibv_wr_opcode      opcode;
//int                     send_flags;
//uint32_t                imm_data;       /* in network byte order */
//union {
//      struct {
//              uint64_t        remote_addr;
//              uint32_t        rkey;
//      } rdma;
//      struct {
//              uint64_t        remote_addr;
//              uint64_t        compare_add;
//              uint64_t        swap;
//              uint32_t        rkey;
//      } atomic;
//      struct {
//              struct ibv_ah  *ah;
//              uint32_t        remote_qpn;
//              uint32_t        remote_qkey;
//      } ud;
//} wr;
//};

public class NatIbvSendWR extends IbvSendWR implements SendWRMod {
	public static int CSIZE = 72;
	public static int WRID_OFFSET = 0;
//	public static int NEXT_OFFSET = 8;
//	public static int SGLIST_OFFSET = 16;
//	public static int NUMSGE_OFFSET = 24;
//	public static int OPCODE_OFFSET = 28;
	public static int SENDFLAGS_OFFSET = 32;
//	public static int IMMDATA_OFFSET = 36;
	public static int REMOTEADDR_OFFSET = 40;
	public static int RKEY_OFFSET = 48;
	
	private NatPostSendCall postSendCall;
	private int bufPosition;
	private long next;
	private long ptr_sge_list;
	private NatRdma natRdma;

	public NatIbvSendWR(NatPostSendCall postSendCall, NatRdma natRdma, IbvSendWR sendWR, LinkedList<IbvSge> sg_list) {
		super(natRdma, null, null, sg_list);
		this.natRdma = natRdma;
		this.next = 0;
		this.ptr_sge_list = 0;
		
		this.wr_id = sendWR.getWr_id();
		this.num_sge = sendWR.getNum_sge();
		this.opcode = sendWR.getOpcode();
		this.send_flags = sendWR.getSend_flags();
		this.imm_data = sendWR.getImm_data();
		
		this.postSendCall = postSendCall;
		this.bufPosition = 0;
	}

	public void writeBack(ByteBuffer buffer) {
		this.bufPosition = buffer.position();
		int initialPos = this.bufPosition;
		buffer.putLong(wr_id);
		buffer.putLong(next);
		buffer.putLong(ptr_sge_list);
		buffer.putInt(num_sge);
		buffer.putInt(opcode);
		buffer.putInt(send_flags);
		buffer.putInt(imm_data);
		
		buffer.position(initialPos + NatIbvSendWR.REMOTEADDR_OFFSET);
		natRdma.writeBack(buffer);
		int newPos = initialPos + CSIZE;
		buffer.position(newPos);
	}

	public long getNext() {
		return next;
	}

	public void setNext(long next) {
		this.next = next;
	}

	public int getCsize() {
		return CSIZE;
	}

	public long getPtr_sge_list() {
		return ptr_sge_list;
	}

	public void setPtr_sge_list(long ptr_sge_list) {
		this.ptr_sge_list = ptr_sge_list;
	}

	public void shiftAddress(long address) {
		next += address;
		ptr_sge_list += address;
	}

	public int getBufPosition() {
		return bufPosition;
	}

	//--------------------- modifiable sendWR
	
	@Override
	public void setWr_id(long wr_id) {
		super.setWr_id(wr_id);
		postSendCall.setWr_id(this, WRID_OFFSET);
	}
	
	@Override
	public void setSend_flags(int send_flags) {
		super.setSend_flags(send_flags);
		postSendCall.setSend_flags(this, SENDFLAGS_OFFSET);
	}	
	
	@Override
	public RdmaMod getRdmaMod() {
		return (RdmaMod) this.rdma;
	}

	@Override
	public SgeMod getSgeMod(int index) {
		return (SgeMod) sg_list.get(index);
	}	


	public static class NatRdma extends IbvSendWR.Rdma implements RdmaMod {
		private NatPostSendCall postSendCall;
		private int bufPosition;
		
		public NatRdma(Rdma rdma, NatPostSendCall postSendCall){
			this.remote_addr = rdma.getRemote_addr();
			this.reserved = rdma.getReserved();
			this.rkey = rdma.getRkey();
			
			this.postSendCall = postSendCall;
//			this.bufPosition = 0;
		}
		
		@Override
		public void setRemote_addr(long remote_addr) {
			super.setRemote_addr(remote_addr);
			postSendCall.setRemote_addr(this, 0);
		}

		@Override
		public void setRkey(int rkey) {
			super.setRkey(rkey);
			postSendCall.setRkey(this, 8);
		}
		
		@Override
		public void setReserved(int reserved) {
			super.setReserved(reserved);
			postSendCall.setReserved(this, 12);
		}
		
		public void writeBack(ByteBuffer buffer) {
			this.bufPosition = buffer.position();
			buffer.putLong(getRemote_addr());
			buffer.putInt(getRkey());
			buffer.putInt(getReserved());			
		}
		
		public int getBufPosition() {
			return bufPosition;
		}
	}
}
