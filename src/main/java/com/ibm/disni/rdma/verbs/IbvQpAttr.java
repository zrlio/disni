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


//struct ibv_qp_attr {
//    enum ibv_qp_state       qp_state;
//    enum ibv_qp_state       cur_qp_state;
//    enum ibv_mtu            path_mtu;
//    enum ibv_mig_state      path_mig_state;
//    uint32_t                qkey;
//    uint32_t                rq_psn;
//    uint32_t                sq_psn;
//    uint32_t                dest_qp_num;
//    int                     qp_access_flags;
//    struct ibv_qp_cap       cap;
//    struct ibv_ah_attr      ah_attr;
//    struct ibv_ah_attr      alt_ah_attr;
//    uint16_t                pkey_index;
//    uint16_t                alt_pkey_index;
//    uint8_t                 en_sqd_async_notify;
//    uint8_t                 sq_draining;
//    uint8_t                 max_rd_atomic;
//    uint8_t                 max_dest_rd_atomic;
//    uint8_t                 min_rnr_timer;
//    uint8_t                 port_num;
//    uint8_t                 timeout;
//    uint8_t                 retry_cnt;
//    uint8_t                 rnr_retry;
//    uint8_t                 alt_port_num;
//    uint8_t                 alt_timeout;
//};

//enum ibv_qp_attr_mask {
//    IBV_QP_STATE                    = 1 <<  0,
//    IBV_QP_CUR_STATE                = 1 <<  1,
//    IBV_QP_EN_SQD_ASYNC_NOTIFY      = 1 <<  2,
//    IBV_QP_ACCESS_FLAGS             = 1 <<  3,
//    IBV_QP_PKEY_INDEX               = 1 <<  4,
//    IBV_QP_PORT                     = 1 <<  5,
//    IBV_QP_QKEY                     = 1 <<  6,
//    IBV_QP_AV                       = 1 <<  7,
//    IBV_QP_PATH_MTU                 = 1 <<  8,
//    IBV_QP_TIMEOUT                  = 1 <<  9,
//    IBV_QP_RETRY_CNT                = 1 << 10,
//    IBV_QP_RNR_RETRY                = 1 << 11,
//    IBV_QP_RQ_PSN                   = 1 << 12,
//    IBV_QP_MAX_QP_RD_ATOMIC         = 1 << 13,
//    IBV_QP_ALT_PATH                 = 1 << 14,
//    IBV_QP_MIN_RNR_TIMER            = 1 << 15,
//    IBV_QP_SQ_PSN                   = 1 << 16,
//    IBV_QP_MAX_DEST_RD_ATOMIC       = 1 << 17,
//    IBV_QP_PATH_MIG_STATE           = 1 << 18,
//    IBV_QP_CAP                      = 1 << 19,
//    IBV_QP_DEST_QPN                 = 1 << 20
//};

/**
 * Various attributes to be used when creating a queue pair (QP).
 */
public class IbvQpAttr {        
	protected int qp_state;
	protected int cur_qp_state;
	protected int path_mtu;
	protected int path_mig_state;
	protected int qkey;
	protected int rq_psn;
	protected int sq_psn;
	protected int dest_qp_num;
	protected int qp_access_flags;	
	protected IbvQPCap cap;
	protected IbvAhAttr ah_attr;
	protected IbvAhAttr alt_ah_attr;
	protected short pkey_index;
	protected short alt_pkey_index;
	protected byte en_sqd_async_notify;
	protected byte sq_draining;
	protected byte max_rd_atomic;
	protected byte max_dest_rd_atomic;
	protected byte min_rnr_timer;
	protected byte port_num;
	protected byte timeout;
	protected byte retry_cnt;
	protected byte rnr_retry;
	protected byte alt_port_num;
	protected byte alt_timeout;
	
	protected int qpAttrMask;

	protected IbvQpAttr(IbvAhAttr ah_attr, IbvAhAttr alt_ah_attr, IbvQPCap cap) {
		this.ah_attr = ah_attr;
		this.alt_ah_attr = alt_ah_attr;
		this.cap = cap;
	}

	public int getQp_state() {
		return qp_state;
	}

	public void setQp_state(int qp_state) {
		this.qp_state = qp_state;
	}

	public int getQpAttrMask() {
		return qpAttrMask;
	}

	public void setQpAttrMask(int qpAttrMask) {
		this.qpAttrMask = qpAttrMask;
	}

	public int getQp_access_flags() {
		return qp_access_flags;
	}

	public void setQp_access_flags(int qp_access_flags) {
		this.qp_access_flags = qp_access_flags;
	}

	public byte getMax_dest_rd_atomic() {
		return max_dest_rd_atomic;
	}

	public void setMax_dest_rd_atomic(byte max_dest_rd_atomic) {
		this.max_dest_rd_atomic = max_dest_rd_atomic;
	}

	public byte getPort_num() {
		return port_num;
	}

	public void setPort_num(byte port_num) {
		this.port_num = port_num;
	}

	public int getPath_mtu() {
		return path_mtu;
	}

	public void setPath_mtu(int path_mtu) {
		this.path_mtu = path_mtu;
	}

	public IbvAhAttr getAh_attr() {
		return ah_attr;
	}

	public IbvAhAttr getAlt_ah_attr() {
		return alt_ah_attr;
	}

	public int getCur_qp_state() {
		return cur_qp_state;
	}

	public void setCur_qp_state(int cur_qp_state) {
		this.cur_qp_state = cur_qp_state;
	}

	public int getPath_mig_state() {
		return path_mig_state;
	}

	public void setPath_mig_state(int path_mig_state) {
		this.path_mig_state = path_mig_state;
	}

	public int getQkey() {
		return qkey;
	}

	public void setQkey(int qkey) {
		this.qkey = qkey;
	}

	public int getRq_psn() {
		return rq_psn;
	}

	public void setRq_psn(int rq_psn) {
		this.rq_psn = rq_psn;
	}

	public int getSq_psn() {
		return sq_psn;
	}

	public void setSq_psn(int sq_psn) {
		this.sq_psn = sq_psn;
	}

	public int getDest_qp_num() {
		return dest_qp_num;
	}

	public void setDest_qp_num(int dest_qp_num) {
		this.dest_qp_num = dest_qp_num;
	}

	public IbvQPCap getCap() {
		return cap;
	}

	public short getPkey_index() {
		return pkey_index;
	}

	public void setPkey_index(short pkey_index) {
		this.pkey_index = pkey_index;
	}

	public short getAlt_pkey_index() {
		return alt_pkey_index;
	}

	public void setAlt_pkey_index(short alt_pkey_index) {
		this.alt_pkey_index = alt_pkey_index;
	}

	public byte getEn_sqd_async_notify() {
		return en_sqd_async_notify;
	}

	public void setEn_sqd_async_notify(byte en_sqd_async_notify) {
		this.en_sqd_async_notify = en_sqd_async_notify;
	}

	public byte getSq_draining() {
		return sq_draining;
	}

	public void setSq_draining(byte sq_draining) {
		this.sq_draining = sq_draining;
	}

	public byte getMax_rd_atomic() {
		return max_rd_atomic;
	}

	public void setMax_rd_atomic(byte max_rd_atomic) {
		this.max_rd_atomic = max_rd_atomic;
	}

	public byte getMin_rnr_timer() {
		return min_rnr_timer;
	}

	public void setMin_rnr_timer(byte min_rnr_timer) {
		this.min_rnr_timer = min_rnr_timer;
	}

	public byte getTimeout() {
		return timeout;
	}

	public void setTimeout(byte timeout) {
		this.timeout = timeout;
	}

	public byte getRetry_cnt() {
		return retry_cnt;
	}

	public void setRetry_cnt(byte retry_cnt) {
		this.retry_cnt = retry_cnt;
	}

	public byte getRnr_retry() {
		return rnr_retry;
	}

	public void setRnr_retry(byte rnr_retry) {
		this.rnr_retry = rnr_retry;
	}

	public byte getAlt_port_num() {
		return alt_port_num;
	}

	public void setAlt_port_num(byte alt_port_num) {
		this.alt_port_num = alt_port_num;
	}

	public byte getAlt_timeout() {
		return alt_timeout;
	}

	public void setAlt_timeout(byte alt_timeout) {
		this.alt_timeout = alt_timeout;
	}

}
