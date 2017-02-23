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

import org.slf4j.Logger;

import com.ibm.disni.rdma.verbs.IbvCQ;
import com.ibm.disni.rdma.verbs.IbvQP;
import com.ibm.disni.rdma.verbs.IbvQPInitAttr;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.util.DiSNILogger;


/**
 * The default implementation of an RdmaEndpoint Group.
 * 
 * An RdmaEndpoint group manages endpoints and performs event processing (both communication events and completion event) for these endpoints.
 * 
 * A RdmaEndpoint group control the association between completion queue and endpoints, and also between communication channels and endpoints.
 * 
 * This default implementation assigns a new CQ to each endpoint, without any active CQ processing. Further, all endpoints share one commmunication channel.
 * 
 * Other extensions of the RdmaEndointGroup provide different associations.
 */
public class RdmaPassiveEndpointGroup<C extends RdmaEndpoint> extends RdmaEndpointGroup<C> {
	private static final Logger logger = DiSNILogger.getLogger();
	
	private int maxWR;		
	private int maxSge;
	private int cqSize;
	
	public RdmaPassiveEndpointGroup<RdmaEndpoint> createDefaultGroup(int timeout, int maxWR, int maxSge, int cqSize) throws IOException{
		RdmaPassiveEndpointGroup<RdmaEndpoint> group = new RdmaPassiveEndpointGroup<RdmaEndpoint>(timeout, maxWR, maxSge, cqSize);
		group.init(new RawEndpointFactory(group));
		return group;
	}	
	
	public RdmaPassiveEndpointGroup(int timeout, int maxWR, int maxSge, int cqSize) throws IOException{
		super(timeout);
		this.maxWR = maxWR;		
		this.maxSge = maxSge;
		this.cqSize = cqSize;
		logger.info("passive endpoint group, maxWR " + this.maxWR + ", maxSge " + this.maxSge + ", cqSize " + this.cqSize);
	}
	
	public RdmaCqProvider createCqProvider(C endpoint) throws IOException {
		logger.info("setting up cq processor");
		return new RdmaCqProvider(endpoint.getIdPriv().getVerbs(), cqSize);
	}
	
	public IbvQP createQpProvider(C endpoint) throws IOException{
		RdmaCqProvider cqProvider = endpoint.getCqProvider();
		IbvCQ cq = cqProvider.getCQ();
		IbvQPInitAttr attr = new IbvQPInitAttr();
		attr.cap().setMax_recv_sge(this.maxSge);
		attr.cap().setMax_recv_wr(this.maxWR);
		attr.cap().setMax_send_sge(this.maxSge);
		attr.cap().setMax_send_wr(this.maxWR);
		attr.setQp_type(IbvQP.IBV_QPT_RC);
		attr.setRecv_cq(cq);
		attr.setSend_cq(cq);	
		IbvQP qp = endpoint.getIdPriv().createQP(endpoint.getPd(), attr);
		return qp;
	}
	
	public void allocateResources(C endpoint) throws Exception {
		endpoint.allocateResources();
	}
	
	public int getMaxWR() {
		return maxWR;
	}

	public int getMaxSge() {
		return maxSge;
	}

	public int getCqSize() {
		return cqSize;
	}

	protected class RawEndpointFactory implements RdmaEndpointFactory<RdmaEndpoint> {
		RdmaPassiveEndpointGroup<RdmaEndpoint> group;
		
		public RawEndpointFactory(RdmaPassiveEndpointGroup<RdmaEndpoint> group){
			this.group = group;
		}
		
		public RdmaEndpoint createEndpoint(RdmaCmId idPriv, boolean serverSide) throws IOException {
			return new RdmaEndpoint(group, idPriv, serverSide);
		}
	}	
}
