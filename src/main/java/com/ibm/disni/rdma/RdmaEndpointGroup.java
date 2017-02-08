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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import com.ibm.disni.rdma.verbs.IbvPd;
import com.ibm.disni.rdma.verbs.IbvQP;
import com.ibm.disni.rdma.verbs.RdmaCm;
import com.ibm.disni.rdma.verbs.RdmaCmEvent;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.rdma.verbs.RdmaConnParam;
import com.ibm.disni.util.DiSNILogger;

public abstract class RdmaEndpointGroup <C extends RdmaEndpoint> {
	private static final Logger logger = DiSNILogger.getLogger();
	private static int idCounter = 0;
	
	protected RdmaEndpointProvider endpointProvider;
	protected RdmaCmProcessor cmProcessor;
	protected HashMap<RdmaCmId, RdmaServerEndpoint<C>> serverEndpointMap;
	protected HashMap<RdmaCmId, C> clientEndpointMap;
	protected AtomicBoolean closed;
	protected RdmaEndpointFactory<C> factory;
	protected RdmaConnParam connParam;
	
	public abstract RdmaCqProvider createCqProvider(C endpoint) throws IOException;
	
	public abstract IbvQP createQpProvider(C endpoint) throws IOException;
	
	public abstract void allocateResources(C endpoint) throws Exception;	
	
	public RdmaEndpointGroup(int timeout) throws IOException{
		this.endpointProvider = RdmaEndpointProvider.getEndpointProvider();
		this.serverEndpointMap = new HashMap<RdmaCmId, RdmaServerEndpoint<C>>();
		this.clientEndpointMap = new HashMap<RdmaCmId, C>();
		this.cmProcessor = new RdmaCmProcessor(this, timeout);
		this.closed = new AtomicBoolean(true);
		this.connParam = new RdmaConnParam();
	}
	
	public void init(RdmaEndpointFactory<C> factory){
		this.closed.set(false);
		this.factory = factory;
		cmProcessor.start();
	}
	
	public synchronized IbvPd createProtectionDomain(C endpoint) throws IOException {
		return endpointProvider.createProtectionDomain(endpoint);
	}	
	
	public synchronized IbvPd createProtectionDomain(RdmaServerEndpoint<C> endpoint) throws IOException {
		return endpointProvider.createProtectionDomain(endpoint);
	}		
	
	public synchronized final RdmaServerEndpoint<C> createServerEndpoint() throws IOException{
		RdmaCmId idPriv = cmProcessor.createId(RdmaCm.RDMA_PS_TCP);
		RdmaServerEndpoint<C> ep = new RdmaServerEndpoint<C>(this, idPriv);
		serverEndpointMap.put(idPriv, ep);
		
		return ep;
	}
	
	public synchronized final C createEndpoint() throws IOException {
		RdmaCmId idPriv = cmProcessor.createId(RdmaCm.RDMA_PS_TCP);
		C ep = factory.createEndpoint(idPriv, false);
		clientEndpointMap.put(idPriv, ep);		
		return ep;
	}
	
	protected synchronized final C createEndpoint(RdmaCmId idPriv)	throws IOException {
		C ep = factory.createEndpoint(idPriv, true);
		clientEndpointMap.put(idPriv, ep);
		return ep;
	}	
	
	public final void dispatchCmEvent(RdmaCmEvent cmEvent) {
		try {
			if (closed.get()){
				return;
			}
			
			RdmaCmId idPriv = cmEvent.getListenIdPriv();
			RdmaCmId clientID = cmEvent.getConnIdPriv();
			int event = cmEvent.getEvent();
			
			if (event == RdmaCmEvent.EventType.RDMA_CM_EVENT_CONNECT_REQUEST.ordinal()) {
				if (idPriv != null && serverEndpointMap.containsKey(idPriv)) {
					serverEndpointMap.get(idPriv).dispatchCmEvent(cmEvent);
				}
			} else if (event == RdmaCmEvent.EventType.RDMA_CM_EVENT_ESTABLISHED.ordinal()) {
				if (clientID != null && clientEndpointMap.containsKey(clientID)) {
					clientEndpointMap.get(clientID).dispatchCmEvent(cmEvent);
				} else {
					logger.info("have no client endpoint to this event");
				}
			} else if (event == RdmaCmEvent.EventType.RDMA_CM_EVENT_ADDR_RESOLVED
					.ordinal()) {
				if (clientID != null && clientEndpointMap.containsKey(clientID)) {
					clientEndpointMap.get(clientID).dispatchCmEvent(cmEvent);
				} else {
					logger.info("have no client endpoint to this event");
				}
			} else if (event == RdmaCmEvent.EventType.RDMA_CM_EVENT_ROUTE_RESOLVED
					.ordinal()) {
				if (clientID != null && clientEndpointMap.containsKey(clientID)) {
					C ep = clientEndpointMap.get(clientID);
					ep.dispatchCmEvent(cmEvent);
				} else {
					logger.info("have no client endpoint to this event");
				}
			} else if (event == RdmaCmEvent.EventType.RDMA_CM_EVENT_DISCONNECTED.ordinal()) {
				if (clientID != null && clientEndpointMap.containsKey(clientID)) {
					clientEndpointMap.get(clientID).dispatchCmEvent(cmEvent);
				}
				if (idPriv != null && serverEndpointMap.containsKey(idPriv)) {
					serverEndpointMap.get(idPriv).dispatchCmEvent(cmEvent);
				}			
			} else {
				if (clientID != null && clientEndpointMap.containsKey(clientID)) {
					clientEndpointMap.get(clientID).dispatchCmEvent(cmEvent);
				}
				if (idPriv != null && serverEndpointMap.containsKey(idPriv)) {
					serverEndpointMap.get(idPriv).dispatchCmEvent(cmEvent);
				}
			}
		} catch(Exception e){
			logger.info(e.getMessage());
		}
	}	
	
	public RdmaConnParam getConnParam() {
		return connParam;
	}

	public synchronized void close() throws IOException, InterruptedException {
		logger.info("shutting down group");
		if (closed.get()){
			return;
		}

		LinkedList<RdmaEndpoint> clientEps = new LinkedList<RdmaEndpoint>();
		for (RdmaEndpoint ep : clientEndpointMap.values()) {
			clientEps.add(ep);
		}
		for (RdmaEndpoint ep : clientEps){
			ep.close();
		}
		
		LinkedList<RdmaServerEndpoint<C>> serverEps = new LinkedList<RdmaServerEndpoint<C>>();
		for (RdmaServerEndpoint<C> ep : serverEndpointMap.values()) {
			serverEps.add(ep);
		}
		for (RdmaServerEndpoint<C> ep: serverEps){
			ep.close();
		}
		
		cmProcessor.close();
		closed.set(true);
		logger.info("shutting down group done");
	}
	
	public boolean isClosed() {
		return closed.get();
	}
	
	public synchronized int getNextId(){
		int id = idCounter;
		idCounter++;
		return id;
	}	
	
	synchronized IbvPd createProtectionDomainRaw(RdmaEndpoint endpoint) throws IOException{
		return createProtectionDomain(clientEndpointMap.get(endpoint.getIdPriv()));
	}
	
	synchronized IbvPd createProtectionDomainRaw(RdmaServerEndpoint<C> endpoint) throws IOException{
		return createProtectionDomain(serverEndpointMap.get(endpoint.getIdPriv()));
	}	
	
	synchronized RdmaCqProvider createCqProviderRaw(RdmaEndpoint endpoint) throws IOException {
		return createCqProvider(clientEndpointMap.get(endpoint.getIdPriv()));
	}
	
	synchronized IbvQP createQpProviderRaw(RdmaEndpoint endpoint) throws IOException{
		return createQpProvider(clientEndpointMap.get(endpoint.getIdPriv()));
	}
	
	synchronized void allocateResourcesRaw(RdmaEndpoint endpoint) throws Exception {
		allocateResources(clientEndpointMap.get(endpoint.getIdPriv()));
	}	

	synchronized void unregisterClientEp(RdmaEndpoint endpoint) throws IOException {
		if (clientEndpointMap.containsKey(endpoint.getIdPriv())) {
			clientEndpointMap.remove(endpoint.getIdPriv());
		}
	}	
	
	synchronized void unregisterServerEp(RdmaServerEndpoint<C> endpoint) {
		if (serverEndpointMap.containsKey(endpoint.getIdPriv())) {
			serverEndpointMap.remove(endpoint.getIdPriv());
		}
	}		
}
