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

import com.ibm.disni.rdma.verbs.IbvRecvWR;
import com.ibm.disni.rdma.verbs.SVCPostRecv.RecvWRMod;

//struct ibv_recv_wr {
//uint64_t                wr_id;
//struct ibv_recv_wr     *next;
//struct ibv_sge         *sg_list;
//int                     num_sge;
//};


public class NatIbvRecvWR extends IbvRecvWR implements RecvWRMod {
	public static int CSIZE = 32;
	private long next;
	private long ptr_sge_list;
	
	public NatIbvRecvWR(IbvRecvWR recvWR){
		super();
		this.next = 0;
		this.ptr_sge_list = 0;		
		
		this.wr_id = recvWR.getWr_id();
		this.num_sge = recvWR.getNum_sge();
	}
	
	public void writeBack(ByteBuffer buffer) {
		int initialPos = buffer.position();
		buffer.putLong(wr_id);
		buffer.putLong(next);
		buffer.putLong(ptr_sge_list);		
		buffer.putInt(num_sge);
		int newPos = initialPos + CSIZE;
		buffer.position(newPos);
	}	

	public long getNext() {
		return next;
	}

	public void setNext(long next) {
		this.next = next;
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
}
