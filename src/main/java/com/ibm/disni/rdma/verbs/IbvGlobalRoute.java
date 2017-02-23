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

//struct ibv_global_route {
//    union ibv_gid           dgid;
//    uint32_t                flow_label;
//    uint8_t                 sgid_index;
//    uint8_t                 hop_limit;
//    uint8_t                 traffic_class;
//};

/**
 * Routing properties, used within the context of QP attributes.
 */
public class IbvGlobalRoute {  
	protected byte[] dgid;
	protected int flow_label;
	protected byte sgid_index; 
	protected byte hop_limit;
	protected byte traffic_class;

	protected IbvGlobalRoute(){
		dgid = new byte[16];
	}
	
	public int getFlow_label() {
		return flow_label;
	}

	public void setFlow_label(int flow_label) {
		this.flow_label = flow_label;
	}

	public byte getSgid_index() {
		return sgid_index;
	}

	public void setSgid_index(byte sgid_index) {
		this.sgid_index = sgid_index;
	}

	public byte getHop_limit() {
		return hop_limit;
	}

	public void setHop_limit(byte hop_limit) {
		this.hop_limit = hop_limit;
	}

	public byte getTraffic_class() {
		return traffic_class;
	}

	public void setTraffic_class(byte traffic_class) {
		this.traffic_class = traffic_class;
	}

	public byte[] getDgid() {
		return dgid;
	}

	public void setDgid(byte[] dgid) {
		for (int i = 0; i < dgid.length; i++){
			this.dgid[i] = dgid[i]; 
		}
	}
}
