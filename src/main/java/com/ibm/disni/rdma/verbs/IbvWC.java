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

package com.ibm.disni.rdma.verbs;

// TODO: Auto-generated Javadoc
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

/**
 * Represents a work completion event. Application will use the pollCQ verbs call to query for completion events.
 */
public class IbvWC {
	public static int CSIZE = 48;

	public enum IbvWcStatus {
		IBV_WC_SUCCESS, IBV_WC_LOC_LEN_ERR, IBV_WC_LOC_QP_OP_ERR, IBV_WC_LOC_EEC_OP_ERR, IBV_WC_LOC_PROT_ERR, IBV_WC_WR_FLUSH_ERR, IBV_WC_MW_BIND_ERR, IBV_WC_BAD_RESP_ERR, IBV_WC_LOC_ACCESS_ERR, IBV_WC_REM_INV_REQ_ERR, IBV_WC_REM_ACCESS_ERR, IBV_WC_REM_OP_ERR, IBV_WC_RETRY_EXC_ERR, IBV_WC_RNR_RETRY_EXC_ERR, IBV_WC_LOC_RDD_VIOL_ERR, IBV_WC_REM_INV_RD_REQ_ERR, IBV_WC_REM_ABORT_ERR, IBV_WC_INV_EECN_ERR, IBV_WC_INV_EEC_STATE_ERR, IBV_WC_FATAL_ERR, IBV_WC_RESP_TIMEOUT_ERR, IBV_WC_GENERAL_ERR;

		public static IbvWcStatus valueOf(int value) {
			/* this is slow but ok for debugging purposes */
			for (IbvWcStatus status : values()) {
				if (status.ordinal() == value) {
					return status;
				}
			}
			throw new IllegalArgumentException();
		}
	};

	public enum IbvWcOpcode {
		IBV_WC_SEND(0),
		IBV_WC_RDMA_WRITE(1),
		IBV_WC_RDMA_READ(2),
		IBV_WC_COMP_SWAP(3),
		IBV_WC_FETCH_ADD(4),
		IBV_WC_BIND_MW(5),
		IBV_WC_RECV(128),
		IBV_WC_RECV_RDMA_WITH_IMM(129);

		private int opcode;
		IbvWcOpcode(int opcode) { this.opcode = opcode; }

		public int getOpcode() { return opcode; }

		public static IbvWcOpcode valueOf(int value) {
			for (IbvWcOpcode opcode : values()) {
				if (opcode.getOpcode() == value) {
					return opcode;
				}
			}
			throw new IllegalArgumentException();
		}
	}

    public static int CQ_OK =  0;
    public static int CQ_EMPTY = -1;
    public static int CQ_POLL_ERR = -2;

	protected long wr_id;
	protected int status;
	protected int opcode;
	protected int vendor_err;
	protected int byte_len;
	protected int imm_data; /* in network byte order */
	protected int qp_num;
	protected int src_qp;
	protected int wc_flags;
	protected short pkey_index;
	protected short slid;
	protected short sl;
	protected short dlid_path_bits;

	protected int err;
	protected boolean isSend;
	protected short wqIndex;
	protected short tail1;
	protected short tail2;
	protected short tail3;
	protected short diff;
	protected int wrIndex;
	protected int sqcqn;

	public IbvWC() {
		err = CQ_OK;
		isSend = false;
		wqIndex = -1;
	}

	public IbvWC clone(){
		IbvWC wc = new IbvWC();
		wc.byte_len = this.byte_len;
		wc.diff = this.diff;
		wc.dlid_path_bits = this.dlid_path_bits;
		wc.err = this.err;
		wc.imm_data = this.imm_data;
		wc.isSend = this.isSend;
		wc.opcode = this.opcode;
		wc.pkey_index = this.pkey_index;
		wc.qp_num = this.qp_num;
		wc.sl = this.sl;
		wc.slid = this.slid;
		wc.wc_flags = this.wc_flags;
		wc.status = this.status;
		wc.wqIndex = this.wqIndex;
		wc.wr_id = this.wr_id;

		return wc;
	}

	/**
	 * Gets the work request id. This matches with the id of IbvSendWR or IbvRecvWR and allows application to refer back to the original work request.
	 *
	 * @return the wr_id
	 */
	public long getWr_id() {
		return wr_id;
	}

	/**
	 * Unsupported.
	 *
	 * @param wr_id the new wr_id
	 */
	public void setWr_id(long wr_id) {
		this.wr_id = wr_id;
	}

	/**
	 * The status of this work completion. Will be of type IbvWcStatus.
	 *
	 * @return the status
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Unsupported.
	 *
	 * @param status the new status
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * Represents the opcode of the original operation for this completion event.
	 *
	 * @return the opcode
	 */
	public int getOpcode() {
		return opcode;
	}

	/**
	 * Unsupported.
	 *
	 * @param opcode the new opcode
	 */
	public void setOpcode(int opcode) {
		this.opcode = opcode;
	}

	/**
	 * Vendor specific error.
	 *
	 * @return the vendor_err
	 */
	public int getVendor_err() {
		return vendor_err;
	}

	/**
	 * Unsupported.
	 *
	 * @param vendor_err the new vendor_err
	 */
	public void setVendor_err(int vendor_err) {
		this.vendor_err = vendor_err;
	}

	/**
	 * The number of bytes sent or received.
	 *
	 * @return the byte_len
	 */
	public int getByte_len() {
		return byte_len;
	}

	/**
	 * Unsupported.
	 *
	 * @param byte_len the new byte_len
	 */
	public void setByte_len(int byte_len) {
		this.byte_len = byte_len;
	}

	/**
	 * Unsupported.
	 *
	 * @return the imm_data
	 */
	public int getImm_data() {
		return imm_data;
	}

	/**
	 * Unsupported.
	 *
	 * @param imm_data the new imm_data
	 */
	public void setImm_data(int imm_data) {
		this.imm_data = imm_data;
	}

	/**
	 * The identifier of the QP where the work request was originally posted.
	 *
	 * @return the qp_num
	 */
	public int getQp_num() {
		return qp_num;
	}

	/**
	 * Unsupported.
	 *
	 * @param qp_num the new qp_num
	 */
	public void setQp_num(int qp_num) {
		this.qp_num = qp_num;
	}

	/**
	 * The identifier of the shared receive queue where the work request was originally posted. Unsupported.
	 *
	 * @return the src_qp
	 */
	public int getSrc_qp() {
		return src_qp;
	}

	/**
	 * Unsupported.
	 *
	 * @param src_qp the new src_qp
	 */
	public void setSrc_qp(int src_qp) {
		this.src_qp = src_qp;
	}

	/**
	 * Unsupported.
	 *
	 * @return the wc_flags
	 */
	public int getWc_flags() {
		return wc_flags;
	}

	/**
	 * Unsupported.
	 *
	 * @param wc_flags the new wc_flags
	 */
	public void setWc_flags(int wc_flags) {
		this.wc_flags = wc_flags;
	}

	/**
	 * Unsupported.
	 *
	 * @return the pkey_index
	 */
	public short getPkey_index() {
		return pkey_index;
	}

	/**
	 * Unsupported.
	 *
	 * @param pkey_index the new pkey_index
	 */
	public void setPkey_index(short pkey_index) {
		this.pkey_index = pkey_index;
	}

	/**
	 * Unsupported.
	 *
	 * @return the slid
	 */
	public short getSlid() {
		return slid;
	}

	/**
	 * Unsupported.
	 *
	 * @param slid the new slid
	 */
	public void setSlid(short slid) {
		this.slid = slid;
	}

	/**
	 * Unsupported.
	 *
	 * @return the sl
	 */
	public short getSl() {
		return sl;
	}

	/**
	 * Unsupported.
	 *
	 * @param sl the new sl
	 */
	public void setSl(short sl) {
		this.sl = sl;
	}

	/**
	 * Unsupported.
	 *
	 * @return the dlid_path_bits
	 */
	public short getDlid_path_bits() {
		return dlid_path_bits;
	}

	/**
	 * Unsupported.
	 *
	 * @param dlid_path_bits the new dlid_path_bits
	 */
	public void setDlid_path_bits(short dlid_path_bits) {
		this.dlid_path_bits = dlid_path_bits;
	}

	public String getClassName() {
		return IbvWC.class.getCanonicalName();
	}

	/**
	 * Unsupported.
	 *
	 * @return the err
	 */
	public int getErr() {
		return err;
	}

	/**
	 * Unsupported.
	 *
	 * @param err the new err
	 */
	public void setErr(int err) {
		this.err = err;
	}

	/**
	 * Checks if this completion event is due to a send operation.
	 *
	 * @return true, if is send
	 */
	public boolean isSend() {
		return isSend;
	}

	/**
	 * Unsupported.
	 *
	 * @param isSend the new send
	 */
	public void setSend(boolean isSend) {
		this.isSend = isSend;
	}

	/**
	 * The queue index of this work completion event.
	 *
	 * @return the wq index
	 */
	public short getWqIndex() {
		return wqIndex;
	}

	/**
	 * Unsupported.
	 *
	 * @param wqIndex the new wq index
	 */
	public void setWqIndex(short wqIndex) {
		this.wqIndex = wqIndex;
	}

	/**
	 * Unsupported.
	 *
	 * @return the tail1
	 */
	public short getTail1() {
		return tail1;
	}

	/**
	 * Unsupported.
	 *
	 * @param tail1 the new tail1
	 */
	public void setTail1(short tail1) {
		this.tail1 = tail1;
	}

	/**
	 * Unsupported.
	 *
	 * @return the tail2
	 */
	public short getTail2() {
		return tail2;
	}

	/**
	 * Unsupported.
	 *
	 * @param tail2 the new tail2
	 */
	public void setTail2(short tail2) {
		this.tail2 = tail2;
	}

	/**
	 * Unsupported.
	 *
	 * @return the tail3
	 */
	public short getTail3() {
		return tail3;
	}

	/**
	 * Unsupported.
	 *
	 * @param tail3 the new tail3
	 */
	public void setTail3(short tail3) {
		this.tail3 = tail3;
	}

	/**
	 * Unsupported.
	 *
	 * @param diff the new diff
	 */
	public void setDiff(short diff) {
		this.diff = diff;
	}

	/**
	 * Unsupported.
	 *
	 * @return the diff
	 */
	public short getDiff(){
		return diff;
	}

	/**
	 * Unsupported.
	 *
	 * @return the sqcqn
	 */
	public int getSqcqn() {
		return sqcqn;
	}

	/**
	 * Unsupported.
	 *
	 * @param sqcqn the new sqcqn
	 */
	public void setSqcqn(int sqcqn) {
		this.sqcqn = sqcqn;
	}
}
