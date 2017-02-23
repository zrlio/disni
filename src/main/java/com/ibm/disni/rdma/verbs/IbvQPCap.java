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
//struct ibv_qp_cap {
//uint32_t                max_send_wr;
//uint32_t                max_recv_wr;
//uint32_t                max_send_sge;
//uint32_t                max_recv_sge;
//uint32_t                max_inline_data;
//};

/**
 * These attributes define the maximal queue size of a QP.
 */
public class IbvQPCap {
	private int max_send_wr;
	private int max_recv_wr;
	private int max_send_sge;
	private int max_recv_sge;
	private int max_inline_data;

	protected IbvQPCap() {
	}

	/**
	 * Gets the maximum number of send work request allowed.
	 *
	 * @return the max_send_wr
	 */
	public int getMax_send_wr() {
		return max_send_wr;
	}

	/**
	 * Sets the maximum number of send work request allowed.
	 *
	 * @param max_send_wr the new max_send_wr
	 */
	public void setMax_send_wr(int max_send_wr) {
		this.max_send_wr = max_send_wr;
	}

	/**
	 * Gets the maximum number of receive work request allowed.
	 *
	 * @return the max_recv_wr
	 */
	public int getMax_recv_wr() {
		return max_recv_wr;
	}

	/**
	 * Sets the maximum number of receive work request allowed.
	 *
	 * @param max_recv_wr the new max_recv_wr
	 */
	public void setMax_recv_wr(int max_recv_wr) {
		this.max_recv_wr = max_recv_wr;
	}

	/**
	 * Gets the maximum number of send scatter/gather elements allowed.
	 *
	 * @return the max_send_sge
	 */
	public int getMax_send_sge() {
		return max_send_sge;
	}

	/**
	 * Sets the maximum number of send scatter/gather elements allowed.
	 *
	 * @param max_send_sge the new max_send_sge
	 */
	public void setMax_send_sge(int max_send_sge) {
		this.max_send_sge = max_send_sge;
	}

	/**
	 * Gets the maximum number of receive scatter/gather elements allowed.
	 *
	 * @return the max_recv_sge
	 */
	public int getMax_recv_sge() {
		return max_recv_sge;
	}

	/**
	 * Sets the maximum number of receive scatter/gather elements allowed.
	 *
	 * @param max_recv_sge the new max_recv_sge
	 */
	public void setMax_recv_sge(int max_recv_sge) {
		this.max_recv_sge = max_recv_sge;
	}

	/**
	 * Gets the inline threshold. Data transfers below this value will be inlined. 
	 *
	 * @return the max_inline_data
	 */
	public int getMax_inline_data() {
		return max_inline_data;
	}

	/**
	 * Sets the inline threshold. Data transfers below this value will be inlined. 
	 *
	 * @param max_inline_data the new max_inline_data
	 */
	public void setMax_inline_data(int max_inline_data) {
		this.max_inline_data = max_inline_data;
	}
}
