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

import java.io.IOException;
import java.util.List;


// TODO: Auto-generated Javadoc
//struct ibv_qp {
//    struct ibv_context     *context;
//    void                   *qp_context;
//    struct ibv_pd          *pd;
//    struct ibv_cq          *send_cq;
//    struct ibv_cq          *recv_cq;
//    struct ibv_srq         *srq;
//    uint32_t                handle;
//    uint32_t                qp_num;
//    enum ibv_qp_state       state;
//    enum ibv_qp_type        qp_type;
//
//    pthread_mutex_t         mutex;
//    pthread_cond_t          cond;
//    uint32_t                events_completed;
//};

//enum ibv_qp_type {
//    IBV_QPT_RC = 2,
//    IBV_QPT_UC,
//    IBV_QPT_UD
//};

/**
 * Representing a Queue Pair (QP).
 */
public class IbvQP {
	public static final byte IBV_QPT_RC = 2;
	public static final byte IBV_QPT_UC = 3;
	public static final byte IBV_QPT_UD = 4;

	public static final int IBV_QPS_RESET = 0;
	public static final int IBV_QPS_INIT = 1;
	public static final int IBV_QPS_RTR = 2;
	public static final int IBV_QPS_RTS = 3;
	public static final int IBV_QPS_SQD = 4;
	public static final int IBV_QPS_SQE = 5;
	public static final int IBV_QPS_ERR = 6;

	private RdmaVerbs verbs;
	protected IbvContext context;
	protected IbvPd pd;
	protected IbvCQ send_cq;
	protected IbvCQ recv_cq;
	protected int handle;
	protected int qp_num;
	protected int state;
	protected int qp_type;
	protected volatile boolean isOpen;

	public IbvQP(int qpnum) throws IOException {
		this.verbs = RdmaVerbs.open();
		this.qp_num = qpnum;
		this.isOpen = true;
	}

	/**
	 * The device context this QP is associated with.
	 *
	 * @return the context
	 */
	public IbvContext getContext() {
		return context;
	}

	/**
	 * The protection domain this QP is associated with.
	 *
	 * @return the protectio domain.
	 */
	public IbvPd getPd() {
		return pd;
	}

	/**
	 * Gets the completion queue holding events related to send operations.
	 *
	 * @return the completion queue.
	 */
	public IbvCQ getSend_cq() {
		return send_cq;
	}

	/**
	 * GGets the completion queue holding events related to receive operations.
	 *
	 * @return the recv_cq
	 */
	public IbvCQ getRecv_cq() {
		return recv_cq;
	}

	/**
	 * A unique handle for this QP.
	 *
	 * @return the handle
	 */
	public int getHandle() {
		return handle;
	}

	/**
	 * An identifier for this QP.
	 *
	 * @return the qp_num
	 */
	public int getQp_num() throws IOException {
		return qp_num;
	}

	/**
	 * Gets the state this QP is currently in. Not supported.
	 *
	 * @return the state
	 */
	public int getState() {
		return this.state;
	}

	/**
	 * Gets type of the QP. Only IBV_QPT_RC is supported. 
	 *
	 * @return the QP type.
	 */
	public int getQp_type() {
		return qp_type;
	}
	
	public String toString() {
		return "handle=" + handle + ",qp_num=" + qp_num + ",state=" + state;
	}	
	
	public boolean isOpen() {
		return isOpen;
	}

	public void close() {
		isOpen = false;
	}

	//---------- oo-verbs
	
	public SVCPostSend postSend(List<IbvSendWR> wrList, List<IbvSendWR> badwrList) throws IOException {
		return verbs.postSend(this, wrList, badwrList);
	}
	
	public SVCPostRecv postRecv(List<IbvRecvWR> wrList, List<IbvRecvWR> badwrList) throws IOException {
		return verbs.postRecv(this, wrList, badwrList);
	}
}
