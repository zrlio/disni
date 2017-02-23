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

//struct ibv_qp_init_attr {
//    void                   *qp_context;
//    struct ibv_cq          *send_cq;
//    struct ibv_cq          *recv_cq;
//    struct ibv_srq         *srq;
//    struct ibv_qp_cap       cap;
//    enum ibv_qp_type        qp_type;
//    int                     sq_sig_all;
//};

/**
 * Attributes used when initializing a queue pair.
 */
public class IbvQPInitAttr {
	private long qp_context;
	private long srq;
	private IbvQPCap cap;
	private byte qp_type;
	private byte sq_sig_all;

	private IbvCQ send_cq_obj;
	private IbvCQ recv_cq_obj;

	public IbvQPInitAttr() { 
		cap = new IbvQPCap();
	}

	public long getQp_context() {
		return qp_context;
	}

	public void setQp_context(long qp_context) {
		this.qp_context = qp_context;
	}

	public long getSrq() {
		return srq;
	}

	public void setSrq(long srq) {
		this.srq = srq;
	}

	public byte getQp_type() {
		return qp_type;
	}

	public void setQp_type(byte qp_type) {
		this.qp_type = qp_type;
	}

	public byte getSq_sig_all() {
		return sq_sig_all;
	}

	public void setSq_sig_all(byte sq_sig_all) {
		this.sq_sig_all = sq_sig_all;
	}

	public IbvCQ getSend_cq() {
		return send_cq_obj;
	}

	public void setSend_cq(IbvCQ send_cq_obj) {
		this.send_cq_obj = send_cq_obj;
	}

	public IbvCQ getRecv_cq() {
		return recv_cq_obj;
	}

	public void setRecv_cq(IbvCQ recv_cq_obj) {
		this.recv_cq_obj = recv_cq_obj;
	}

	public IbvQPCap cap() {
		return cap;
	}
}
