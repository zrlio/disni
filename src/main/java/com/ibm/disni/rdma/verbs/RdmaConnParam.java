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

// TODO: Auto-generated Javadoc
//struct rdma_conn_param {
//        const void *private_data;
//        uint8_t private_data_len;
//        uint8_t responder_resources;
//        uint8_t initiator_depth;
//        uint8_t flow_control;
//        uint8_t retry_count;            /* ignored when accepting */
//        uint8_t rnr_retry_count;
//        /* Fields below ignored if a QP is created on the rdma_cm_id. */
//        uint8_t srq;
//        uint32_t qp_num;
//};

/**
 * RDMA connection properties. Used when establishing an RDMA connection.
 */
public class RdmaConnParam {
	protected long private_data_addr;
	protected byte private_data_len;
	protected byte responder_resources;
	protected byte initiator_depth;
	protected byte flow_control;
	protected byte retry_count;
	protected byte rnr_retry_count;
	protected byte srq;
	protected int qp_num;

	public RdmaConnParam() {
		this.private_data_addr = 0;
		this.private_data_len = 0;
		this.responder_resources = 0;
		this.initiator_depth = 0;
		this.flow_control = 0;
		this.retry_count = 0;
		this.rnr_retry_count = 0;
		this.srq = 0;
		this.qp_num = 0;
	}

	/**
	 * Gets the private_data.
	 *
	 * @return the private data
	 */
	public long getPrivate_data() {
		return private_data_addr;
	}

	/**
	 * Sets the private_data.
	 *
	 * The private data passed will be copied into the connection request message.
	 *
	 * @param private_data_addr the new private data.
	 */
	public void setPrivate_data(long private_data_addr) throws IOException {
		this.private_data_addr = private_data_addr;
	}

	/**
	 * Gets the length of the private data.
	 *
	 * @return the lenght of the private data.
	 */
	public byte getPrivate_data_len() {
		return private_data_len;
	}

	public void setPrivate_data_len(byte private_data_len) {
		this.private_data_len = private_data_len;
	}

	/**
	 * Gets the responder resources.
	 *
	 * @return the responder resources.
	 */
	public byte getResponder_resources() {
		return responder_resources;
	}

	/**
	 * Sets the responder resources.
	 *
	 * @param responder_resources the new responder resources.
	 */
	public void setResponder_resources(byte responder_resources) throws IOException {
		throw new IOException("Operation currently not supported");
	}

	/**
	 * Gets the initiator depth.
	 *
	 * @return the initiator depth.
	 */
	public byte getInitiator_depth() {
		return initiator_depth;
	}

	/**
	 * Sets the initiator depth.
	 *
	 * @param initiator_depth the new initiater depth.
	 */
	public void setInitiator_depth(byte initiator_depth) throws IOException {
		throw new IOException("Operation currently not supported");
	}

	/**
	 * Gets the flow control.
	 *
	 * @return the flow control
	 */
	public byte getFlow_control() {
		return flow_control;
	}

	/**
	 * Sets the flow control.
	 *
	 * @param flow_control the new flow control.
	 */
	public void setFlow_control(byte flow_control) throws IOException {
		throw new IOException("Operation currently not supported");
	}

	/**
	 * The number of times a data operation will be re-tried.
	 *
	 * @return the retry count.
	 */
	public byte getRetry_count() {
		return retry_count;
	}

	/**
	 * Sets the retry count.
	 *
	 * @param retry_count the new retry count.
	 */
	public void setRetry_count(byte retry_count) throws IOException {
		this.retry_count = retry_count;
	}

	/**
	 * The maximum number of times that a send operation from the remote peer should be retried.
	 *
	 * @return the rnr retry count.
	 */
	public byte getRnr_retry_count() {
		return rnr_retry_count;
	}

	/**
	 * Sets the rnr retry_count.
	 *
	 * @param rnr_retry_count the new rnr retry_count
	 */
	public void setRnr_retry_count(byte rnr_retry_count) {
		this.rnr_retry_count = rnr_retry_count;
	}

	/**
	 * Gets the shared receive queue. Not supported.
	 *
	 * @return the shared receive queue.
	 */
	public byte getSrq() {
		return srq;
	}

	/**
	 * Sets the shared receive queue.
	 *
	 * @param srq the new shared receive queue.
	 */
	public void setSrq(byte srq) throws IOException {
		throw new IOException("Operation currently not supported");
	}

	/**
	 * Gets the qp_num.
	 *
	 * @return the qp_num
	 */
	public int getQp_num() {
		return qp_num;
	}

	/**
	 * Sets the qp_num.
	 *
	 * @param qp_num the new qp_num
	 */
	public void setQp_num(int qp_num) throws IOException {
		throw new IOException("Operation currently not supported");
	}
}
