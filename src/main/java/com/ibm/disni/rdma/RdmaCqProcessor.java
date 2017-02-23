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
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.ibm.disni.rdma.verbs.IbvContext;
import com.ibm.disni.rdma.verbs.IbvWC;
import com.ibm.disni.rdma.verbs.SVCPollCq;
import com.ibm.disni.rdma.verbs.SVCReqNotify;
import com.ibm.disni.util.DiSNILogger;
import com.ibm.disni.util.NativeAffinity;
/**
 * Extends the raw RdmaCqClient with active processing of CQ events.
 * 
 * This class has an event loop querying the CQ and delivering event to a ICqConsumer.
 */
public abstract class RdmaCqProcessor<C extends RdmaEndpoint> extends RdmaCqProvider implements Runnable {
	private static final Logger logger = DiSNILogger.getLogger();
	
	private static int MAX_ACK_COUNT = 1;
	private IbvWC[] wcList;
	private SVCReqNotify reqNotify;
	private SVCPollCq poll;
	private int timeout;
	private int ackCounter;
	private boolean blocking;
	private boolean running;
	private long affinity;
	private int clusterId;
	private Thread thread;
	private int wrSize;
	private ConcurrentHashMap<Integer, C> qpMap;
	
	public RdmaCqProcessor(IbvContext context, int cqSize, int wrSize, long affinity, int clusterId, int timeout, boolean polling) throws IOException {
		super(context, cqSize);
		this.clusterId = clusterId;
		this.affinity = affinity;
		this.running = false;
		this.wrSize = Math.min(cqSize, wrSize);
		this.wcList = new IbvWC[this.wrSize];
		for (int i = 0; i < wcList.length; i++){
			wcList[i] = new IbvWC();
		}
		this.blocking = !polling;
		this.ackCounter = 0;	 			
		
		this.reqNotify = cq.reqNotification(false);
		this.poll = cq.poll(wcList, wcList.length);
		this.timeout = timeout;
		
		if (blocking){
			reqNotify.execute();
		} 
		
		this.qpMap = new ConcurrentHashMap<Integer, C>();
		this.thread = new Thread(this);
	}
	
	public synchronized void registerQP(int qpnum, C endpoint) throws IOException {
		qpMap.put(qpnum, endpoint);
	}
	
	public synchronized void unregister(RdmaEndpoint endpoint) throws IOException {
		logger.info("unregister ep with cq processor");
		if (qpMap.containsKey(endpoint.getQp().getQp_num())){
			qpMap.remove(endpoint.getQp().getQp_num());
		}
	}
	
	public synchronized boolean isRunning() {
		return running;
	}
	
	public synchronized void start(){
		running = true;
		thread.start();
	}
	
	public final void dispatchCqEvent(IbvWC wc) throws IOException {
		Integer qpNum = wc.getQp_num();
		C clientEndpoint = qpMap.get(qpNum);
		if (clientEndpoint != null) {
			dispatchCqEvent(clientEndpoint, wc);
		}
	}	
	
	public abstract void dispatchCqEvent(C endpoint, IbvWC wc) throws IOException;
	
	public void run() {
		NativeAffinity.setAffinity(affinity);
		logger.info("running cq processing, index " + clusterId + ", affinity " + affinity + ", blocking " + blocking);
		running = true;
		while (running) {
			try {
				boolean success = true;
				if (blocking){
					success = compChannel.getCqEvent(cq, timeout);
					if (success){
						this.ackCounter++;
						if (ackCounter == MAX_ACK_COUNT){
							cq.ackEvents(ackCounter);
							ackCounter = 0;
						}
						reqNotify.execute();
					}
				}
				int res = -1;
				if (success){
					res = poll.execute().getPolls();
				}
				while (res > 0) {
					for (int i = 0; i < res; i++) {
						this.dispatchCqEvent(wcList[i]);
					}
					res = poll.execute().getPolls();
				}
			} catch (Exception e) {
				if (isClosed()) {
					logger.info("error " + e.getMessage());
					break;
				} else {
					logger.info("cq processing, caught exception but keep going " + e.getMessage());
					e.printStackTrace();
				}
			} 
		}
		logger.info("terminating cq polling " + isClosed());
	}

	private boolean isClosed() {
		return !running;
	}

	public int getClusterId() {
		return clusterId;
	}
	
	public void close() throws IOException, InterruptedException {
		running = false;
		thread.join();
		super.close();
	}
}	
