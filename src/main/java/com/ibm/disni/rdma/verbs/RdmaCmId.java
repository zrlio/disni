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
import java.net.SocketAddress;

// TODO: Auto-generated Javadoc
//struct rdma_cm_id {
//    struct ibv_context      *verbs;
//    struct rdma_event_channel *channel;
//    void                    *context;
//    struct ibv_qp           *qp;
//    struct rdma_route        route;
//    enum rdma_port_space     ps;				//344
//    uint8_t                  port_num;		//348
//};

/**
 * The RDMA identifier used to identify a RDMA endpoint. The RDMA analog to sockets.
 */
public class RdmaCmId  {
	private RdmaCm cm;
	
	protected int ps;
	protected byte port_num;

	protected RdmaEventChannel cmChannel;
	protected IbvContext verbs;
	protected IbvQP qpObj;
	protected volatile boolean isOpen;

	protected RdmaCmId(RdmaEventChannel cmChannel, IbvContext verbs) throws IOException {
		this.cm = RdmaCm.open();
		this.cmChannel = cmChannel;
		this.verbs = verbs;
		this.qpObj = null;
		this.isOpen = true;
	}

	/**
	 * Gets the port space. 
	 * 
	 * Only RDMA_PS_TCP supported. 
	 *
	 * @return the port space.
	 */
	public int getPs() {
		return ps;
	}

	/**
	 * Gets the port_num of this id.
	 *
	 * @return the port number. With RDMA_PS_TCP this is the TCP port number used.
	 */
	public byte getPort_num() {
		return port_num;
	}

	/**
	 * Gets the communication channel associated with this id.
	 *
	 * @return the communication channel.
	 */
	public RdmaEventChannel getCmChannel() {
		return cmChannel;
	}
	
	/**
	 * Gets the context associated with this id.
	 *
	 * @return the context.
	 * @throws Exception 
	 */
	public IbvContext getVerbs() throws IOException {
		return verbs;
	}

	/**
	 * Gets the Queue Pair (QP) associated with this id. 
	 *
	 * @return the Queue Pair (QP). 
	 */
	public IbvQP getQp() {
		return qpObj;
	}

	protected void setQp(IbvQP qpObj) {
		this.qpObj = qpObj;
	}
	
	protected void setPort_num(byte port_num) {
		this.port_num = port_num;
	}
	
	protected void setVerbs(IbvContext verbs){
		this.verbs = verbs;
	}
	
	public boolean isOpen() {
		return isOpen;
	}

	public void close() {
		isOpen = false;
	}

	//---------- oo-verbs
	
	public IbvQP createQP(IbvPd pd, IbvQPInitAttr attr) throws IOException{
		return cm.createQP(this, pd, attr);
	}
	
	public int bindAddr(SocketAddress addr) throws IOException{
		return cm.bindAddr(this, addr);
	}
	
	public int listen(int backlog) throws IOException{
		return cm.listen(this, backlog);
	}	
	
	public int resolveAddr(SocketAddress src, SocketAddress dst, int timeout) throws IOException{
		return cm.resolveAddr(this, src, dst, timeout);
	}
	
	public int resolveRoute(int timeout) throws IOException{
		return cm.resolveRoute(this, timeout);
	}
	
	public int connect(RdmaConnParam connParam) throws IOException{
		return cm.connect(this, connParam);
	}
	
	public int accept(RdmaConnParam connParam) throws IOException{
		return cm.accept(this, connParam);
	}
	
	public int disconnect() throws IOException{
		return cm.disconnect(this);
	}
	
	public int destroyId() throws IOException {
		return cm.destroyCmId(this);
	}
	
	public int destroyQP() throws IOException {
		return cm.destroyQP(this);
	}	
	
	public SocketAddress getSource() throws IOException {
		return cm.getSrcAddr(this);
	}
	
	public SocketAddress getDestination() throws IOException {
		return cm.getDstAddr(this);
	}
	
	public int destroyEp() throws IOException {
		return cm.destroyEp(this);
	}	
}
