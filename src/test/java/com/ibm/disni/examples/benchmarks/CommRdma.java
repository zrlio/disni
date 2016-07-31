/*
 * jVerbs: RDMA verbs support for the Java Virtual Machine
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

package com.ibm.disni.examples.benchmarks;

import java.nio.ByteBuffer; 
import java.util.LinkedList;

import org.slf4j.Logger;

import com.ibm.disni.util.DiSNILogger;
import com.ibm.disni.verbs.IbvCQ;
import com.ibm.disni.verbs.IbvCompChannel;
import com.ibm.disni.verbs.IbvContext;
import com.ibm.disni.verbs.IbvQP;
import com.ibm.disni.verbs.IbvRecvWR;
import com.ibm.disni.verbs.IbvSendWR;
import com.ibm.disni.verbs.IbvWC;
import com.ibm.disni.verbs.RdmaVerbs;
import com.ibm.disni.verbs.SVCPollCq;
import com.ibm.disni.verbs.SVCPostRecv;
import com.ibm.disni.verbs.SVCPostSend;
import com.ibm.disni.verbs.SVCReqNotify;


//only used for data traffic, needs read, write
public class CommRdma {
	private static final Logger logger = DiSNILogger.getLogger();
	
	public static boolean CachingON = true;
	
	public static int MAX_SGE = 1;
	public static int MAX_WR = 100;

	private SVCPostSend postSendCall;
	private SVCPostRecv postRecvCall;
	private SVCReqNotify reqNotifyCall;
	private SVCPollCq pollCqCall;
	private IbvWC[] wcList;

	private IbvQP qp;
	private RdmaVerbs verbs;
	private IbvCompChannel compChannel;
	private IbvCQ cq;
	
	public CommRdma(RdmaVerbs verbs, IbvContext context,
			IbvCompChannel compChannel, IbvQP qp, IbvCQ cq) throws Exception {
		this.postSendCall = null;
		this.postRecvCall = null;
		this.reqNotifyCall = null;
		this.pollCqCall = null;

		this.verbs = verbs;
		this.compChannel = compChannel;
		this.qp = qp;
		this.cq = cq;
		
		reqNotifyCall = getReqNotifyCall();
		reqNotifyCall.execute();		
		
		logger.info("new CommRdma (1), caching " + CachingON);
	}
	
	public boolean send(ByteBuffer[] fragments, LinkedList<IbvSendWR> wrList,
			boolean signaled, boolean polling) throws Exception {
		for (int i = 0; i < fragments.length; i++) {
			ByteBuffer buffer = fragments[i];
			buffer.clear();
		}

		postSendCall = getPostSendCall(wrList);
		postSendCall.execute();
		if (postSendCall.success() == true) {
			if (signaled) {
				return checkCq(wrList.size(), polling);
			} else {
				return true;
			}
		} else {
			logger.info("post send returned false");
		}

		return false;
	}
	
	public boolean initSGRecv(ByteBuffer[] fragments, LinkedList<IbvRecvWR> wrList)
			throws Exception {
		for (int i = 0; i < fragments.length; i++) {
			fragments[i].clear();
		}
		
		postRecvCall = getPostRecvCall(wrList);
		postRecvCall.execute();
		
		return postRecvCall.success();
	}
	
	public boolean completeSGRecv(ByteBuffer[] fragments,
			LinkedList<IbvRecvWR> wrList, boolean polling) throws Exception {
		return checkCq(wrList.size(), polling);
	}
	
	private boolean checkCq(int expectedElements, boolean polling) throws Exception{
		boolean success = false;
		int elementsRead = 0;
		
		while (true) {
			if (!polling){
				reqNotifyCall = getReqNotifyCall();
				reqNotifyCall.execute();
			}
			
			pollCqCall = getPollCqCall(1);
			int res = pollCqCall.execute().getPolls();
			if (res < 0){
				break;
			} else if (res > 0){
				if (wcList[0].getStatus() != IbvWC.IbvWcStatus.IBV_WC_SUCCESS.ordinal()){
					break;
				} 
			}
			elementsRead += res;
			if (elementsRead == expectedElements){
				success = true;
				break;
			} else {
				if (elementsRead > expectedElements){
					success = true;
					break;
				} else if (res == 0){
					if (!polling){
						verbs.getCqEvent(compChannel, cq, -1);
						verbs.ackCqEvents(cq, 1);
					}
				}
			}			
		}
		return success;		
	}
	
	private SVCPostSend getPostSendCall(LinkedList<IbvSendWR> wrList) throws Exception{
		if (CachingON == false || this.postSendCall == null || !postSendCall.isValid()) {
			logger.info("creating new postsend SVC");
			this.postSendCall = verbs.postSend(qp, wrList, null);
		} 
		return postSendCall;
	}
	
	private SVCPostRecv getPostRecvCall(LinkedList<IbvRecvWR> wrList) throws Exception{
		if (CachingON == false || postRecvCall == null || !postRecvCall.isValid()) {
			postRecvCall = verbs.postRecv(qp, wrList, null);
		} 
		return postRecvCall;
	}
	
	private SVCReqNotify getReqNotifyCall() throws Exception{
		if (CachingON == false || reqNotifyCall == null || !reqNotifyCall.isValid()) {
			reqNotifyCall = verbs.reqNotifyCQ(cq, false);
		} 
		return reqNotifyCall;
	}
	
	private SVCPollCq getPollCqCall(int size) throws Exception{
		if (CachingON == false || pollCqCall == null || !pollCqCall.isValid()) {
			wcList = new IbvWC[size];
			for (int i = 0; i < size; i++){
				wcList[i] = new IbvWC();
			}
			pollCqCall = verbs.pollCQ(cq, wcList, size);
		} 
		return pollCqCall;
	}
}
