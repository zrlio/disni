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

package com.ibm.disni.rdma;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;

import com.ibm.disni.DiSNIEndpoint;
import com.ibm.disni.rdma.verbs.IbvMr;
import com.ibm.disni.rdma.verbs.IbvPd;
import com.ibm.disni.rdma.verbs.IbvQP;
import com.ibm.disni.rdma.verbs.IbvRecvWR;
import com.ibm.disni.rdma.verbs.IbvSendWR;
import com.ibm.disni.rdma.verbs.RdmaCmEvent;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.rdma.verbs.RdmaConnParam;
import com.ibm.disni.rdma.verbs.SVCPostRecv;
import com.ibm.disni.rdma.verbs.SVCPostSend;
import com.ibm.disni.rdma.verbs.SVCRegMr;
import com.ibm.disni.util.DiSNILogger;


/**
 * This class represents an RDMA client endpoint. 
 * 
 * Conceptually, endpoints behave like sockets for control operations (e.g., connect(), disconnect()), but behave like RdmaCmId's once connected (offering postSend((), postRecv(), registerMemory()). 
 */
public class RdmaEndpoint implements DiSNIEndpoint {
	private static final Logger logger = DiSNILogger.getLogger();
	
	private static int CONN_STATE_INITIALIZED = 0;
	private static int CONN_STATE_ADDR_RESOLVED = 1;
	private static int CONN_STATE_ROUTE_RESOLVED = 2;
	private static int CONN_STATE_RESOURCES_ALLOCATED = 3;
	private static int CONN_STATE_CONNECTED = 4;
	private static int CONN_STATE_CLOSED = 5;

	protected int endpointId;
	protected RdmaEndpointGroup<? extends RdmaEndpoint> group;
	protected RdmaCmId idPriv;
	protected IbvQP qp;
	protected IbvPd pd;
	protected RdmaCqProvider cqProcessor;
	protected int access;	
	private int connState;
	private boolean isClosed;
	private boolean isInitialized;
	private boolean serverSide;
	
	protected RdmaEndpoint(RdmaEndpointGroup<? extends RdmaEndpoint> group, RdmaCmId idPriv, boolean serverSide) throws IOException{
		this.endpointId = group.getNextId();
		this.group = group;
		this.idPriv = idPriv;
		this.access = IbvMr.IBV_ACCESS_LOCAL_WRITE | IbvMr.IBV_ACCESS_REMOTE_WRITE | IbvMr.IBV_ACCESS_REMOTE_READ; 	
		
		this.qp = null;
		this.pd = null;
		this.cqProcessor = null;
		this.isInitialized = false;
		this.isClosed = false;
		this.connState = CONN_STATE_INITIALIZED;
		this.serverSide = serverSide;
		logger.info("new client endpoint, id " + endpointId + ", idPriv " + idPriv.getPs());
	}
	
	/**
	 * Connect this endpoint to a remote server endpoint.
	 *
	 * @param uri (rdma://host:port)
	 */	
	@Override
	public synchronized void connect(URI uri) throws Exception {
		if (connState != CONN_STATE_INITIALIZED) {
			throw new IOException("endpoint already connected");
		}
		if (uri == null){
			throw new IOException("uri not defined");
		}
		if (uri.getHost() == null){
			throw new IOException("host not defined");
		}
		
		InetSocketAddress dst = new InetSocketAddress(uri.getHost(), uri.getPort());
		int timeout = 1000;
		idPriv.resolveAddr(null, dst, timeout);
		while(connState < CONN_STATE_ADDR_RESOLVED){
			wait();
		}
		if (connState != CONN_STATE_ADDR_RESOLVED){
			throw new IOException("resolve address failed");
		}
		
		idPriv.resolveRoute(timeout);
		while(connState < CONN_STATE_ROUTE_RESOLVED){
			wait();
		}
		if (connState != CONN_STATE_ROUTE_RESOLVED){
			throw new IOException("resolve route failed");
		}			
		
		group.allocateResourcesRaw(this);
		while(connState < CONN_STATE_RESOURCES_ALLOCATED){
			wait();
		}	
		if (connState != CONN_STATE_RESOURCES_ALLOCATED){
			throw new IOException("resolve route failed");
		}	
		
		RdmaConnParam connParam = getConnParam();
		idPriv.connect(connParam);
		
		while(connState < CONN_STATE_CONNECTED){
			wait();
		}			
	}		
	
	/* (non-Javadoc)
	 * @see com.ibm.jverbs.endpoints.ICmConsumer#dispatchCmEvent(com.ibm.jverbs.cm.RdmaCmEvent)
	 */
	public synchronized void dispatchCmEvent(RdmaCmEvent cmEvent)
			throws IOException {
		try {
			int eventType = cmEvent.getEvent();
			if (eventType == RdmaCmEvent.EventType.RDMA_CM_EVENT_ADDR_RESOLVED.ordinal()) {
				connState = RdmaEndpoint.CONN_STATE_ADDR_RESOLVED;
				notifyAll();
			} else if (cmEvent.getEvent() == RdmaCmEvent.EventType.RDMA_CM_EVENT_ROUTE_RESOLVED.ordinal()) {
				connState = RdmaEndpoint.CONN_STATE_ROUTE_RESOLVED;
				notifyAll();
			} else if (eventType == RdmaCmEvent.EventType.RDMA_CM_EVENT_ESTABLISHED.ordinal()) {
				logger.info("got event type + RDMA_CM_EVENT_ESTABLISHED, srcAddress " + this.getSrcAddr() + ", dstAddress " + this.getDstAddr());
				connState = CONN_STATE_CONNECTED;
				notifyAll();
			} else if (eventType == RdmaCmEvent.EventType.RDMA_CM_EVENT_DISCONNECTED.ordinal()) {
				logger.info("got event type + RDMA_CM_EVENT_DISCONNECTED, srcAddress " + this.getSrcAddr() + ", dstAddress " + this.getDstAddr());
				connState = CONN_STATE_CLOSED;
				notifyAll();
			} else if (eventType == RdmaCmEvent.EventType.RDMA_CM_EVENT_CONNECT_REQUEST.ordinal()) {
				logger.info("got event type + RDMA_CM_EVENT_CONNECT_REQUEST, srcAddress " + this.getSrcAddr() + ", dstAddress " + this.getDstAddr());
			} else {
				logger.info("got event type + UNKNOWN, srcAddress " + this.getSrcAddr() + ", dstAddress " + this.getDstAddr());
			}
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	public final synchronized void allocateResources() throws IOException {
		if (!isInitialized) {
			this.pd = group.createProtectionDomainRaw(this);
			this.cqProcessor = group.createCqProviderRaw(this);
			this.qp = group.createQpProviderRaw(this);
			isInitialized = true;
			init();
			connState = CONN_STATE_RESOURCES_ALLOCATED;
			notifyAll();
		}
	}

	synchronized void accept() throws Exception {
		group.allocateResourcesRaw(this);
		while(connState < CONN_STATE_RESOURCES_ALLOCATED){
			wait();
		}	
		if (connState != CONN_STATE_RESOURCES_ALLOCATED){
			throw new IOException("resolve route failed");
		}		
		
		RdmaConnParam connParam = getConnParam();
		idPriv.accept(connParam);			
		while(connState < CONN_STATE_CONNECTED){
			wait();
		}		
	}

	/**
	 * Close this endpoint. 
	 * 
	 * This closes the connection and free's all the resources, e.g., queue pair. 
	 * @throws InterruptedException 
	 *
	 * @throws Exception the exception
	 */
	public synchronized void close() throws IOException, InterruptedException {
		if (isClosed){
			return;
		}
		
		logger.info("closing client endpoint");
		if (connState == CONN_STATE_CONNECTED) {
			idPriv.disconnect();
			this.wait(1000);
		}
		if (connState >= CONN_STATE_RESOURCES_ALLOCATED) {
			idPriv.destroyQP();
		}
		idPriv.destroyId();
		group.unregisterClientEp(this);
		isClosed = true;
		logger.info("closing client done");
	}
	
	/**
	 * Checks if the endpoint is connected.
	 *
	 * @return true, if is connected
	 */
	public synchronized boolean isConnected() {
		return (connState == CONN_STATE_CONNECTED);
	}

	/**
	 * Checks if the endpoint is closed.
	 *
	 * @return true, if is connected
	 */
	public synchronized boolean isClosed() {
		return (connState == CONN_STATE_CLOSED);
	}
	
	/**
	 * Gets the src addr of this endpoint.
	 *
	 * @return the src addr.
	 * @throws Exception the exception
	 */
	public SocketAddress getSrcAddr() throws IOException{
		return idPriv.getSource();
	}
	
	/**
	 * Gets the dst addr of this endpoint.
	 *
	 * @return the dst addr
	 * @throws Exception the exception
	 */
	public SocketAddress getDstAddr() throws IOException {
		return idPriv.getDestination();
	}

	/**
	 * Gets the cq processor of this endpoint.
	 *
	 * @return the cq processor
	 */
	public RdmaCqProvider getCqProvider() {
		return cqProcessor;
	}

	/**
	 * Register memory on this endpoint.
	 *
	 * @param buffer the buffer to be registered. The buffer needs to represent off-heap memory.
	 * @return a stateful verb call (SVC) ready to execute the memory registration. 
	 * @throws Exception on failure
	 */
	public SVCRegMr registerMemory(ByteBuffer buffer) throws IOException {
		return pd.regMr(buffer, access);
	}
	
	/**
	 * Post a receive operation on this endpoint.
	 *
	 * @param recvList list of receive requests.
	 * @return a stateful verb call (SVC) ready to execute the receive operations. 
	 * @throws Exception on failure.
	 */
	public SVCPostRecv postRecv(List<IbvRecvWR> recvList) throws IOException { 
		return qp.postRecv(recvList, null);
	}
	
	/**
	 * Post a send operation on this endpoint.
	 *
	 * @param sendList list of send requests.
	 * @return a stateful verb call (SVC) ready to execute the send operations. 
	 * @throws Exception on failure.
	 */
	public SVCPostSend postSend(List<IbvSendWR> sendList)
			throws IOException {
		return qp.postSend(sendList, null);
	}	

	/**
	 * Deregister memory.
	 *
	 * @param mr the memory region to be de-registered.
	 * @throws Exception on failure.
	 */
	public void deregisterMemory(IbvMr mr) throws IOException {
		mr.deregMr().execute().free();
	}
	
	/**
	 * Gets the RdmaCmId of this endpoint.
	 *
	 * @return the RDMA identifier.
	 */
	public RdmaCmId getIdPriv() {
		return idPriv;
	}

	/**
	 * Gets the Queue Pair (QP) of this connection.
	 *
	 * @return the Queue Pair (QP).
	 */
	public IbvQP getQp() {
		return qp;
	}

	/**
	 * Checks if is endpoint is has been created by a accept call 
	 *
	 * @return true, if endpoint is server side
	 */
	public boolean isServerSide(){
		return serverSide;
	}

	/**
	 * Unique identifier for this endpoint.
	 *
	 * @return the endpoint id.
	 */
	public int getEndpointId() {
		return endpointId;
	}
	
	public IbvPd getPd() {
		return pd;
	}

	protected synchronized void init() throws IOException {
	}

	public int getConnState() {
		return connState;
	}

	public RdmaConnParam getConnParam() {
		return group.getConnParam();
	}
}
