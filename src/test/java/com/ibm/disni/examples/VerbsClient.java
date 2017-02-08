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

package com.ibm.disni.examples;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import com.ibm.disni.rdma.verbs.IbvCQ;
import com.ibm.disni.rdma.verbs.IbvCompChannel;
import com.ibm.disni.rdma.verbs.IbvContext;
import com.ibm.disni.rdma.verbs.IbvMr;
import com.ibm.disni.rdma.verbs.IbvPd;
import com.ibm.disni.rdma.verbs.IbvQP;
import com.ibm.disni.rdma.verbs.IbvQPInitAttr;
import com.ibm.disni.rdma.verbs.IbvRecvWR;
import com.ibm.disni.rdma.verbs.IbvSendWR;
import com.ibm.disni.rdma.verbs.IbvSge;
import com.ibm.disni.rdma.verbs.RdmaCm;
import com.ibm.disni.rdma.verbs.RdmaCmEvent;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.rdma.verbs.RdmaConnParam;
import com.ibm.disni.rdma.verbs.RdmaEventChannel;
import com.ibm.disni.util.GetOpt;

public class VerbsClient { 
	private String ipAddress;
	
	public void run() throws Exception {
		System.out.println("VerbsClient::starting...");
		//open the CM and the verbs interfaces
		
		//create a communication channel for receiving CM events 
		RdmaEventChannel cmChannel = RdmaEventChannel.createEventChannel();
		if (cmChannel == null){
			System.out.println("VerbsClient::cmChannel null");
			return;
		}
		
		//create a RdmaCmId for this client
		RdmaCmId idPriv = cmChannel.createId(RdmaCm.RDMA_PS_TCP);
		if (idPriv == null){
			System.out.println("VerbsClient::id null");
			return;
		}
		
		//before connecting, we have to resolve addresses
		InetAddress _dst = InetAddress.getByName(ipAddress);
		InetSocketAddress dst = new InetSocketAddress(_dst, 1919);			
		int ret = idPriv.resolveAddr(null, dst, 2000);
		if (ret < 0){
			System.out.println("VerbsClient::resolveAddr failed");
			return;
		}
		
		//resolve addr returns an event, we have to catch that event
		RdmaCmEvent cmEvent = cmChannel.getCmEvent(-1);
		if (cmEvent == null){
			System.out.println("VerbsClient::cmEvent null");
			return;
		} else if (cmEvent.getEvent() != RdmaCmEvent.EventType.RDMA_CM_EVENT_ADDR_RESOLVED
				.ordinal()) {
			System.out.println("VerbsClient::wrong event received: " + cmEvent.getEvent());
			return;
		} 
		cmEvent.ackEvent();
		
		//we also have to resolve the route
		ret = idPriv.resolveRoute(2000);
		if (ret < 0){
			System.out.println("VerbsClient::resolveRoute failed");
			return;
		}
		
		//and catch that event too
		cmEvent = cmChannel.getCmEvent(-1);
		if (cmEvent == null){
			System.out.println("VerbsClient::cmEvent null");
			return;
		} else if (cmEvent.getEvent() != RdmaCmEvent.EventType.RDMA_CM_EVENT_ROUTE_RESOLVED
				.ordinal()) {
			System.out.println("VerbsClient::wrong event received: " + cmEvent.getEvent());
			return;
		} 
		cmEvent.ackEvent();
		
		//let's create a device context
		IbvContext context = idPriv.getVerbs();
		
		//and a protection domain, we use that one later for registering memory
		IbvPd pd = context.allocPd();
		if (pd == null){
			System.out.println("VerbsClient::pd null");
			return;
		}
		
		//the comp channel is used for getting CQ events
		IbvCompChannel compChannel = context.createCompChannel();
		if (compChannel == null){
			System.out.println("VerbsClient::compChannel null");
			return;
		}
		
		//let's create a completion queue
		IbvCQ cq = context.createCQ(compChannel, 50, 0);
		if (cq == null){
			System.out.println("VerbsClient::cq null");
			return;
		}
		//and request to be notified for this queue
		cq.reqNotification(false).execute().free();

		//we prepare for the creation of a queue pair (QP)
		IbvQPInitAttr attr = new IbvQPInitAttr();
		attr.cap().setMax_recv_sge(1);
		attr.cap().setMax_recv_wr(10);
		attr.cap().setMax_send_sge(1);
		attr.cap().setMax_send_wr(10);
		attr.setQp_type(IbvQP.IBV_QPT_RC);
		attr.setRecv_cq(cq);
		attr.setSend_cq(cq);
		//let's create a queue pair
		IbvQP qp = idPriv.createQP(pd, attr);
		if (qp == null){
			System.out.println("VerbsClient::qp null");
			return;
		}
		
		int buffercount = 3;
		int buffersize = 100;
		ByteBuffer buffers[] = new ByteBuffer[buffercount];
		IbvMr mrlist[] = new IbvMr[buffercount];
		int access = IbvMr.IBV_ACCESS_LOCAL_WRITE | IbvMr.IBV_ACCESS_REMOTE_WRITE | IbvMr.IBV_ACCESS_REMOTE_READ; 

		//before we connect we also want to register some buffers
		for (int i = 0; i < buffercount; i++){
			buffers[i] = ByteBuffer.allocateDirect(buffersize);
			mrlist[i] = pd.regMr(buffers[i], access).execute().free().getMr();
		}
		
		ByteBuffer dataBuf = buffers[0];
		IbvMr dataMr = mrlist[0];
		IbvMr sendMr = mrlist[1];
		ByteBuffer recvBuf = buffers[2];
		IbvMr recvMr = mrlist[2];
		
		LinkedList<IbvRecvWR> wrList_recv = new LinkedList<IbvRecvWR>();	
		
		IbvSge sgeRecv = new IbvSge();
		sgeRecv.setAddr(recvMr.getAddr());
		sgeRecv.setLength(recvMr.getLength());
		sgeRecv.setLkey(recvMr.getLkey());
		LinkedList<IbvSge> sgeListRecv = new LinkedList<IbvSge>();
		sgeListRecv.add(sgeRecv);	
		IbvRecvWR recvWR = new IbvRecvWR();
		recvWR.setSg_list(sgeListRecv);
		recvWR.setWr_id(1000);
		wrList_recv.add(recvWR);
		
		//it's important to post those receive operations before connecting
		//otherwise the server may issue a send operation and which cannot be received
		//this class wraps soem of the RDMA data operations
		VerbsTools commRdma = new VerbsTools(context, compChannel, qp, cq);
		commRdma.initSGRecv(wrList_recv);
		
		//now let's connect to the server
		RdmaConnParam connParam = new RdmaConnParam();
		connParam.setInitiator_depth((byte) 5);
		connParam.setResponder_resources((byte) 5);
		connParam.setRetry_count((byte) 2);
		ret = idPriv.connect(connParam);
		if (ret < 0){
			System.out.println("VerbsClient::connect failed");
			return;
		}
		
		//wait until we are really connected
		cmEvent = cmChannel.getCmEvent(-1);
		if (cmEvent == null){
			System.out.println("VerbsClient::cmEvent null");
			return;
		} else if (cmEvent.getEvent() != RdmaCmEvent.EventType.RDMA_CM_EVENT_ESTABLISHED
				.ordinal()) {
			System.out.println("VerbsClient::wrong event received: " + cmEvent.getEvent());
			return;
		}
		cmEvent.ackEvent();
		
		//let's wait for the first message to be received from the server
		commRdma.completeSGRecv(wrList_recv, false);

		//here we go, it contains the RDMA information of a remote buffer
		recvBuf.clear();
		long addr = recvBuf.getLong();
		int length = recvBuf.getInt();
		int lkey = recvBuf.getInt();
		recvBuf.clear();		
		System.out.println("VerbsClient::receiving rdma information, addr " + addr + ", length " + length + ", key " + lkey);
		System.out.println("VerbsClient::preparing read operation...");

		//let's prepare a one-sided RDMA read operation to fetch the content of that remote buffer
		LinkedList<IbvSendWR> wrList_send = new LinkedList<IbvSendWR>();	
		IbvSge sgeSend = new IbvSge();
		sgeSend.setAddr(dataMr.getAddr());
		sgeSend.setLength(dataMr.getLength());
		sgeSend.setLkey(dataMr.getLkey());
		LinkedList<IbvSge> sgeList = new LinkedList<IbvSge>();
		sgeList.add(sgeSend);
		IbvSendWR sendWR = new IbvSendWR();
		sendWR.setWr_id(1001);
		sendWR.setSg_list(sgeList);
		sendWR.setOpcode(IbvSendWR.IBV_WR_RDMA_READ);
		sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
		sendWR.getRdma().setRemote_addr(addr);
		sendWR.getRdma().setRkey(lkey);
		wrList_send.add(sendWR);
		
		//now we post the operation, the RDMA/read operation will take off
		//the wrapper class will also wait of the CQ event
		//once the CQ event is received we know the RDMA/read operation has completed
		//we should have the content of the remote buffer stored in our own local buffer
		//let's print it 
		commRdma.send(buffers, wrList_send, true, false);
		dataBuf.clear();
		System.out.println("VerbsClient::read memory from server: " + dataBuf.asCharBuffer().toString());
		
		//now we send a final message to signal everything went fine
		sgeSend = new IbvSge();
		sgeSend.setAddr(sendMr.getAddr());
		sgeSend.setLength(sendMr.getLength());
		sgeSend.setLkey(sendMr.getLkey());
		sgeList.clear();
		sgeList.add(sgeSend);
		sendWR = new IbvSendWR();
		sendWR.setWr_id(1002);
		sendWR.setSg_list(sgeList);
		sendWR.setOpcode(IbvSendWR.IBV_WR_SEND);
		sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
		wrList_send.clear();
		wrList_send.add(sendWR);		
		
		//let's post the final message
		commRdma.send(buffers, wrList_send, true, false);
	}
	

	public void launch(String[] args) throws Exception {
		String _userDriver = "siw";
		String _provider = "nat";

		String[] _args = args;
		if (args.length < 1) {
			System.exit(0);
		} else if (args[0].equals(VerbsClient.class.getCanonicalName())) {
			_args = new String[args.length - 1];
			for (int i = 0; i < _args.length; i++) {
				_args[i] = args[i + 1];
			}
		}

		GetOpt go = new GetOpt(_args, "t:a:f:s:l:k:m:b:go:x:u:pr:x:c:e:i:");
		go.optErr = true;
		int ch = -1;

		while ((ch = go.getopt()) != GetOpt.optEOF) {
			if ((char) ch == 'a') {
				ipAddress = go.optArgGet();
			} else if ((char) ch == 'u') {
				_userDriver = go.optArgGet();
				if (_userDriver.equalsIgnoreCase("siw")) {
					System.setProperty("com.ibm.jverbs.driver", "siw");
				} else if (_userDriver.equalsIgnoreCase("cxgb3")) {
					System.setProperty("com.ibm.jverbs.driver", "cxgb3");
				} else if (_userDriver.equalsIgnoreCase("mlx4")){
					System.setProperty("com.ibm.jverbs.driver", "mlx4");
				}
			} else if ((char) ch == 'e') {
				_provider = go.optArgGet();
				if (_provider.equalsIgnoreCase("mem")) {
					System.setProperty("com.ibm.jverbs.provider", "mem");
				} else if (_provider.equalsIgnoreCase("nat")) {
					System.setProperty("com.ibm.jverbs.provider", "nat");
				}
			} else {
				System.exit(1); // undefined option
			}
		}	
		
		this.run();
	}
	
	public static void main(String[] args) throws Exception { 
		VerbsClient verbsClient = new VerbsClient();
		verbsClient.launch(args);		
	}		
}

