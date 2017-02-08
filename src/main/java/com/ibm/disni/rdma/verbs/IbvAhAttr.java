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

//struct ibv_ah_attr {
//struct ibv_global_route grh;
//uint16_t                dlid;
//uint8_t                 sl;
//uint8_t                 src_path_bits;
//uint8_t                 static_rate;
//uint8_t                 is_global;
//uint8_t                 port_num;
//};

/**
 * The Class IbvAhAttr.
 */
public class IbvAhAttr {  
	protected IbvGlobalRoute grh;  
	protected short dlid;
	protected byte sl;
	protected byte src_path_bits;
	protected byte static_rate;
	protected byte is_global;
	protected byte port_num; 
	
	protected IbvAhAttr(IbvGlobalRoute grh){
		this.grh = grh;
	}

	public IbvGlobalRoute getGrh() {
		return grh;
	}

	public void setGrh(IbvGlobalRoute grh) {
		this.grh = grh;
	}

	public short getDlid() {
		return dlid;
	}

	public void setDlid(short dlid) {
		this.dlid = dlid;
	}

	public byte getSl() {
		return sl;
	}

	public void setSl(byte sl) {
		this.sl = sl;
	}

	public byte getSrc_path_bits() {
		return src_path_bits;
	}

	public void setSrc_path_bits(byte src_path_bits) {
		this.src_path_bits = src_path_bits;
	}

	public byte getStatic_rate() {
		return static_rate;
	}

	public void setStatic_rate(byte static_rate) {
		this.static_rate = static_rate;
	}

	public byte getIs_global() {
		return is_global;
	}

	public void setIs_global(byte is_global) {
		this.is_global = is_global;
	}

	public byte getPort_num() {
		return port_num;
	}

	public void setPort_num(byte port_num) {
		this.port_num = port_num;
	}  
}
