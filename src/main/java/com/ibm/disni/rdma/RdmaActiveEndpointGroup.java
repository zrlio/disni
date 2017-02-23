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

import org.slf4j.Logger;

import com.ibm.disni.rdma.verbs.IbvCQ;
import com.ibm.disni.rdma.verbs.IbvContext;
import com.ibm.disni.rdma.verbs.IbvQP;
import com.ibm.disni.rdma.verbs.IbvQPInitAttr;
import com.ibm.disni.util.DiSNILogger;

/**
 * Extends the RdmaEndpointGroup by assigning active CQ processing units to endpoints. 
 * 
 * All endpoints within the same group with share one completion queue. The group takes care of processing the completion queue and delivering CQ events to the appropriate endpoints.
 */
public class RdmaActiveEndpointGroup<C extends RdmaActiveEndpoint> extends RdmaEndpointGroup<C> {
	private static final Logger logger = DiSNILogger.getLogger();
	private HashMap<Integer, RdmaActiveCqProcessor<C>> cqMap;
	private int timeout;
	private boolean polling;
	protected int cqSize;
	protected int maxSge;
	protected int maxWR;		

	public RdmaActiveEndpointGroup(int timeout, boolean polling, int maxWR, int maxSge, int cqSize) throws IOException {
		super(timeout);
		this.timeout = timeout;
		this.polling = polling;
		cqMap = new HashMap<Integer, RdmaActiveCqProcessor<C>>();
		this.cqSize = cqSize;
		this.maxSge = maxSge;
		this.maxWR = maxWR;
		logger.info("active endpoint group, maxWR " + maxWR + ", maxSge " + maxSge + ", cqSize " + cqSize);
	}
	
	public RdmaCqProvider createCqProvider(C endpoint) throws IOException {
		logger.info("setting up cq processor");
		IbvContext context = endpoint.getIdPriv().getVerbs();
		if (context != null) {
			logger.info("setting up cq processor, context found");
			RdmaActiveCqProcessor<C> cqProcessor = null;
			int key = context.getCmd_fd();
			if (!cqMap.containsKey(key)) {
				cqProcessor = new RdmaActiveCqProcessor<C>(context, cqSize, maxWR, 0, 1, timeout, polling);
				cqMap.put(context.getCmd_fd(), cqProcessor);
				cqProcessor.start();
			}
			cqProcessor = cqMap.get(context.getCmd_fd());
	
			return cqProcessor;
		} else {
			throw new IOException("setting up cq processor, no context found");
		}		
	}
	
	public IbvQP createQpProvider(C endpoint) throws IOException{
		IbvContext context = endpoint.getIdPriv().getVerbs();
		RdmaActiveCqProcessor<C> cqProcessor = cqMap.get(context.getCmd_fd());
		IbvCQ cq = cqProcessor.getCQ();
		
		IbvQPInitAttr attr = new IbvQPInitAttr();
		attr.cap().setMax_recv_sge(maxSge);
		attr.cap().setMax_recv_wr(maxWR);
		attr.cap().setMax_send_sge(maxSge);
		attr.cap().setMax_send_wr(maxWR);
		attr.setQp_type(IbvQP.IBV_QPT_RC);
		attr.setRecv_cq(cq);
		attr.setSend_cq(cq);		
		IbvQP qp = endpoint.getIdPriv().createQP(endpoint.getPd(), attr);		
		
		logger.info("registering endpoint with cq");
		
		cqProcessor.registerQP(qp.getQp_num(), endpoint);
		return qp;
	}
	
	public void allocateResources(C endpoint) throws Exception {
		endpoint.allocateResources();
	}	
	
	public void close() throws IOException, InterruptedException {
		super.close();
		for (RdmaActiveCqProcessor<C> cqProcessor : cqMap.values()){
			cqProcessor.close();
		}
	}

	void close(RdmaEndpoint endpoint) throws IOException {
		IbvContext context = endpoint.getIdPriv().getVerbs();
		RdmaActiveCqProcessor<C> cqProcessor = cqMap.get(context.getCmd_fd());
		cqProcessor.unregister(endpoint);
	}
	
	public int getMaxWR() {
		return maxWR;
	}

	public int getCqSize() {
		return cqSize;
	}

	public int getMaxSge() {
		return maxSge;
	}
}
