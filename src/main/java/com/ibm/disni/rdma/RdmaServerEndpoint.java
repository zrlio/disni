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
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;

import com.ibm.disni.DiSNIServerEndpoint;
import com.ibm.disni.rdma.verbs.IbvMr;
import com.ibm.disni.rdma.verbs.IbvPd;
import com.ibm.disni.rdma.verbs.RdmaCmEvent;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.rdma.verbs.SVCRegMr;
import com.ibm.disni.util.DiSNILogger;

/**
 * This class represent a server endpoint. Conceptually it is similar to a server socket providing operations like bind() and accept().  
 */
public class RdmaServerEndpoint<C extends RdmaEndpoint> implements DiSNIServerEndpoint<C>{
	private static final Logger logger = DiSNILogger.getLogger();
	
	private static int CONN_STATE_INITIALIZED = 0;
	private static int CONN_STATE_READY_FOR_ACCEPT = 1;
	private static int CONN_STATE_CLOSED = 2;

	protected int endpointId;
	protected IbvPd pd;
	protected RdmaCmId idPriv;
	private LinkedBlockingDeque<C> requested;
	private RdmaEndpointGroup<C> group;
	
	protected int access;	
	private boolean isClosed;
	private int connState;
	
	public RdmaServerEndpoint(RdmaEndpointGroup<C> endpointGroup, RdmaCmId idPriv){
		this.endpointId = endpointGroup.getNextId();
		this.group = endpointGroup;
		this.idPriv = idPriv;
		this.connState = CONN_STATE_INITIALIZED;
		this.requested = new LinkedBlockingDeque<C>();
		this.isClosed = false;
		this.access = IbvMr.IBV_ACCESS_LOCAL_WRITE | IbvMr.IBV_ACCESS_REMOTE_WRITE | IbvMr.IBV_ACCESS_REMOTE_READ;
		logger.info("new server endpoint, id " + endpointId);
	}
	
	/**
	 * Bind this server endpoint to a specific IP address / port. 
	 *
	 * @param src (rdma://host:port)
	 * @return the rdma server endpoint
	 * @throws Exception the exception
	 */	
	@Override
	public synchronized RdmaServerEndpoint<C> bind(SocketAddress src, int backlog) throws Exception {
		if (connState != CONN_STATE_INITIALIZED) {
			throw new IOException("endpoint has to be disconnected for bind");
		}
		connState = CONN_STATE_READY_FOR_ACCEPT;
		
		if (idPriv.bindAddr(src) != 0){
			throw new IOException("binding server address " + src.toString() + ", failed");
		}
		if (idPriv.listen(backlog) != 0){
			throw new IOException("listen to server address " + src.toString() + ", failed");
		}
		this.pd = group.createProtectionDomainRaw(this);
		logger.info("PD value " + pd.getHandle());
		return this;
	}	
	
//	/**
//	 * Bind this server endpoint to a specific IP address / port. 
//	 *
//	 * @param src the src
//	 * @param backlog the backlog
//	 * @return the rdma server endpoint
//	 * @throws Exception the exception
//	 */
//	public synchronized RdmaServerEndpoint<C> bind(SocketAddress src, int backlog) throws IOException {
//		if (src == null){
//			throw new IOException("address not defined");
//		}
//		if (connState != CONN_STATE_INITIALIZED) {
//			throw new IOException("endpoint has to be disconnected for bind");
//		}
//		connState = CONN_STATE_READY_FOR_ACCEPT;
//		
//		if (idPriv.bindAddr(src) != 0){
//			throw new IOException("binding server address " + src.toString() + ", failed");
//		}
//		if (idPriv.listen(backlog) != 0){
//			throw new IOException("listen to server address " + src.toString() + ", failed");
//		}
//		this.pd = group.createProtectionDomainRaw(this);
//		logger.info("PD value " + pd.getHandle());
//		return this;
//	}

	/**
	 * Extract the first connection request on the queue of pending connections. 
	 *
	 * @param dispatcher used to signal the completion of the accept call.
	 * @throws Exception on failure.
	 */
	public C accept() throws IOException {
		try {
			synchronized(this){
				if (connState != CONN_STATE_READY_FOR_ACCEPT) {
					throw new IOException("bind needs to be called before accept (1), current state =" + connState);
				}
				logger.info("starting accept");
				if (requested.peek() == null){
					wait();
				}
			}
			C endpoint = requested.poll();
			logger.info("connect request received");
			endpoint.accept();
			return endpoint;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	public synchronized void dispatchCmEvent(RdmaCmEvent cmEvent) throws IOException {
		try {
			int eventType = cmEvent.getEvent();
			if (eventType == RdmaCmEvent.EventType.RDMA_CM_EVENT_CONNECT_REQUEST.ordinal()) {
//				logger.info("got event type + RDMA_CM_EVENT_CONNECT_REQUEST, serverAddress " + this.getSrcAddr());
				RdmaCmId connId = cmEvent.getConnIdPriv();
				C ep = group.createEndpoint(connId);
				ep.dispatchCmEvent(cmEvent);
				requested.add(ep);
				notifyAll();
			} else if (eventType == RdmaCmEvent.EventType.RDMA_CM_EVENT_DISCONNECTED
					.ordinal()) {
//				logger.info("got event type + RDMA_CM_EVENT_CONNECT_REQUEST, serverAddress " + this.getSrcAddr());
				connState = CONN_STATE_CLOSED;
				notifyAll();
			} else {
				logger.info("got event type + UNKNOWN, serverAddress " + this.getSrcAddr());
			}
		} catch(Exception e){
			throw new IOException(e);
		}
	}
	
	/**
	 * Close this endpoint. Destroy the Queue Pair (QP).
	 * @throws InterruptedException 
	 *
	 * @throws Exception the exception
	 */
	public synchronized void close() throws IOException, InterruptedException {
		if (isClosed){
			return;
		}

		logger.info("closing server endpoint");
		idPriv.destroyId();
		group.unregisterServerEp(this);
		isClosed = true;
	}
	
	/**
	 * Checks if is this endpoint is bound to a address;
	 *
	 * @return true, if is bound
	 */
	public synchronized boolean isBound() {
		return (connState == CONN_STATE_READY_FOR_ACCEPT);
	}
	
	/**
	 * Checks if is this endpoint is closed;
	 *
	 * @return true, if is closed
	 */
	public synchronized boolean isClosed() {
		return (connState == CONN_STATE_CLOSED);
	}	
	
	/**
	 * Gets the local address information of this endpoint.
	 *
	 * @return the local socket address.
	 * @throws Exception the exception
	 */
	public SocketAddress getSrcAddr() throws Exception{
		return idPriv.getSource();
	}	

	/**
	 * Gets the RdmaCmId for this endpoint.
	 *
	 * @return the RDMA identifier.
	 */
	public RdmaCmId getIdPriv() {
		return idPriv;
	}

	public int getEndpointId() {
		return endpointId;
	}

	public IbvPd getPd() {
		return pd;
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
	 * Deregister memory.
	 *
	 * @param mr the memory region to be de-registered.
	 * @throws Exception on failure.
	 */
	public void deregisterMemory(IbvMr mr) throws IOException {
		mr.deregMr().execute().free();
	}
}
