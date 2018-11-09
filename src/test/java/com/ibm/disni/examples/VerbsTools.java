/*
 * DiSNI: Direct Storage and Networking Interface
 *
 * Author: Patrick Stuedi <stu@zurich.ibm.com>
 *
 * Copyright (C) 2016-2018, IBM Corporation
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

package com.ibm.disni.examples;

import com.ibm.disni.verbs.*;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public class VerbsTools {
	public static boolean CachingON = false;

	public static int MAX_SGE = 1;
	public static int MAX_WR = 100;

	private SVCPostSend postSendCall;
	private SVCPostRecv postRecvCall;
	private SVCReqNotify reqNotifyCall;
	private SVCPollCq pollCqCall;
	private IbvWC[] wcList;

	private IbvQP qp;
	private IbvCompChannel compChannel;
	private IbvCQ cq;

	public VerbsTools(IbvContext context, IbvCompChannel compChannel, IbvQP qp, IbvCQ cq) {
		this.postSendCall = null;
		this.postRecvCall = null;
		this.reqNotifyCall = null;
		this.pollCqCall = null;

		this.compChannel = compChannel;
		this.qp = qp;
		this.cq = cq;
	}

	public boolean send(ByteBuffer[] fragments,
			LinkedList<IbvSendWR> wrList, boolean signaled, boolean polling)
			throws Exception {
		for (int i = 0; i < fragments.length; i++) {
			ByteBuffer buffer = fragments[i];
			buffer.clear();
		}

		postSendCall = getPostSendCall(wrList);
		postSendCall.execute();
		if (signaled) {
			return checkCq(wrList.size(), polling);
		}

		return false;
	}

	public void initSGRecv(LinkedList<IbvRecvWR> wrList)
			throws Exception {
		postRecvCall = getPostRecvCall(wrList);
		postRecvCall.execute();
	}

	public boolean completeSGRecv(LinkedList<IbvRecvWR> wrList, boolean polling) throws Exception {
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
				if (wcList[0].getStatus() == IbvWC.IbvWcStatus.IBV_WC_SUCCESS.ordinal()){
				} else {
					break;
				}
			}

			elementsRead += res;
			cq.ackEvents(res);
			if (elementsRead == expectedElements){
				success = true;
				break;
			} else {
				if (elementsRead > expectedElements){
					success = true;
					break;
				} else if (res == 0){
					if (!polling){
						compChannel.getCqEvent(cq, -1);
					}
				}
			}
		}
		return success;
	}

	private SVCPostSend getPostSendCall(LinkedList<IbvSendWR> wrList) throws Exception{
		if (!CachingON || this.postSendCall == null || !postSendCall.isValid()) {
			this.postSendCall = qp.postSend(wrList, null);
		}
		return postSendCall;
	}

	private SVCPostRecv getPostRecvCall(LinkedList<IbvRecvWR> wrList) throws Exception{
		if (!CachingON || postRecvCall == null || !postRecvCall.isValid()) {
			postRecvCall = qp.postRecv(wrList, null);
		}
		return postRecvCall;
	}

	private SVCReqNotify getReqNotifyCall() throws Exception{
		if (!CachingON || reqNotifyCall == null || !reqNotifyCall.isValid()) {
			reqNotifyCall = cq.reqNotification(false);
		}
		return reqNotifyCall;
	}

	private SVCPollCq getPollCqCall(int size) throws Exception{
		if (!CachingON || pollCqCall == null || !pollCqCall.isValid()) {
			wcList = new IbvWC[size];
			for (int i = 0; i < size; i++){
				wcList[i] = new IbvWC();
			}
			pollCqCall = cq.poll(wcList, size);
		}
		return pollCqCall;
	}
}

