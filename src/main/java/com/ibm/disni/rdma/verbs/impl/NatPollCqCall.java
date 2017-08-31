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
import java.nio.ByteBuffer;

import com.ibm.disni.rdma.verbs.IbvCQ;
import com.ibm.disni.rdma.verbs.IbvWC;
import com.ibm.disni.rdma.verbs.SVCPollCq;
import com.ibm.disni.util.MemBuf;
import com.ibm.disni.util.MemoryAllocation;


//struct ibv_wc {
//    uint64_t                wr_id;
//    enum ibv_wc_status      status;
//    enum ibv_wc_opcode      opcode;
//    uint32_t                vendor_err;
//    uint32_t                byte_len;
//    uint32_t                imm_data;       /* in network byte order */
//    uint32_t                qp_num;
//    uint32_t                src_qp;
//    int                     wc_flags;
//    uint16_t                pkey_index;
//    uint16_t                slid;
//    uint8_t                 sl;
//    uint8_t                 dlid_path_bits;
//};

public class NatPollCqCall extends SVCPollCq {
	private NativeDispatcher nativeDispatcher;
	private RdmaVerbsNat verbs;
	private MemoryAllocation memAlloc;

	private NatIbvCQ cq;
	private IbvWC[] wcList;
	private int ne;
	
	private MemBuf cmd;
	private int csize;
	private int result;
	private boolean valid;
	
	public NatPollCqCall(RdmaVerbsNat verbs, NativeDispatcher nativeDispatcher, MemoryAllocation memAlloc) {
		this.verbs = verbs;
		this.nativeDispatcher = nativeDispatcher;
		this.memAlloc = memAlloc;
		this.valid = false;
	}

	public void set(IbvCQ cq, IbvWC[] wcList, int ne) {
		this.cq = (NatIbvCQ) cq;
		this.wcList = wcList;
		this.ne = ne;
		
		this.csize = wcList.length*IbvWC.CSIZE;
		if (cmd != null){
			cmd.free();
			cmd = null;
		}		
		this.cmd = memAlloc.allocate(csize, MemoryAllocation.MemType.DIRECT, this.getClass().getCanonicalName());
		this.valid = true;
	}
	
	private void update(IbvWC wc, ByteBuffer buffer) {
		int initialPos = buffer.position();
		wc.setWr_id(buffer.getLong());
		wc.setStatus(buffer.getInt());
		wc.setOpcode(buffer.getInt());
		if (wc.getStatus() == IbvWC.IbvWcStatus.IBV_WC_SUCCESS.ordinal()){
			wc.setVendor_err(buffer.getInt());
			wc.setByte_len(buffer.getInt());
			wc.setImm_data(buffer.getInt());
			wc.setQp_num(buffer.getInt());
			wc.setSrc_qp(buffer.getInt());
			wc.setWc_flags(buffer.getInt());
			wc.setPkey_index(buffer.getShort());
			wc.setSlid(buffer.getShort());
			wc.setSl(buffer.get());
			wc.setDlid_path_bits(buffer.get());
		}
		int newPosition = initialPos + IbvWC.CSIZE;
		buffer.position(newPosition);
	}

	@Override
	public SVCPollCq execute() throws IOException {
		this.result = 0;
		if (!cq.isOpen()) {
			throw new IOException("Trying to poll closed CQ.");
		}
		this.result = nativeDispatcher._pollCQ(cq.getObjId(),  ne, cmd.address());
		if (result < 0){
			throw new IOException("Polling CQ failed");
		} else if (result > 0){
			cmd.getBuffer().clear();
			for (int i = 0; i < result; i++){
				update(wcList[i], cmd.getBuffer());
			}
		}
		return this;
	}


	@Override
	public int getPolls() {
		return result;
	}	

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public SVCPollCq free() {
		if (cmd != null){
			cmd.free();
			cmd = null;
		}		
		this.valid = false;
		verbs.free(this);
		return this;
	}
}
