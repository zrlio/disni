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

import java.util.LinkedList;


// TODO: Auto-generated Javadoc
//struct ibv_recv_wr {
//    uint64_t                wr_id;
//    struct ibv_recv_wr     *next;
//    struct ibv_sge         *sg_list;
//    int                     num_sge;
//};

/**
 * Represent a receive work request. Applications create work request and post them onto queue pairs for execution.
 * 
 * Each work request is composed of several scatter/gather elements, each of which referring to one single buffer.
 */
public class IbvRecvWR {
	protected long wr_id;
	protected LinkedList<IbvSge> sg_list;
	protected int num_sge;

	public IbvRecvWR() {
	}

	/**
	 * A unique identifier for this work request. A subsequent completion event will have a matching id. 
	 *
	 * @return the wr_id
	 */
	public long getWr_id() {
		return wr_id;
	}

	/**
	 * Allows setting the work request identifier. A subsequent completion event will have a matching id.
	 *
	 * @param wr_id the new wr_id
	 */
	public void setWr_id(long wr_id) {
		this.wr_id = wr_id;
	}

	/**
	 * The number of scatter/gather elements in this work request.
	 *
	 * @return the num_sge
	 */
	public int getNum_sge() {
		return num_sge;
	}

	/**
	 * Unsupported.
	 *
	 * @param num_sge the new num_sge
	 */
	public void setNum_sge(int num_sge) {
		this.num_sge = num_sge;
	}

	/**
	 * Gets the scatter/gather elements of this work request. Each scatter/gather element refers to one single buffer.
	 *
	 * @return the sg_list
	 */
	public LinkedList<IbvSge> getSg_list() {
		return sg_list;
	}

	/**
	 * Sets the scatter/gather elements of this work request. Each scatter/gather element refers to one single buffer.
	 *
	 * @param sg_list the new sg_list
	 */
	public void setSg_list(LinkedList<IbvSge> sg_list) {
		this.sg_list = sg_list;
		this.num_sge = sg_list.size();
	}

}
