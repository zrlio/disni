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

public class VerbsServer {
	private String ipAddress;
	
	public void run() throws Exception {
		System.out.println("VerbsServer::starting...");
		
		//create a communication channel for receiving CM events 
		RdmaEventChannel cmChannel = RdmaEventChannel.createEventChannel();
		if (cmChannel == null){
			System.out.println("VerbsServer::CM channel null");
			return;
		}
		
		//create a RdmaCmId for the server
		RdmaCmId idPriv = cmChannel.createId(RdmaCm.RDMA_PS_TCP);
		if (idPriv == null){
			System.out.println("idPriv null");
			return;
		}
		
		InetAddress _src = InetAddress.getByName(ipAddress);
		InetSocketAddress src = new InetSocketAddress(_src, 1919);	
		int ret = idPriv.bindAddr(src);
		if (ret < 0){
			System.out.println("VerbsServer::binding not sucessfull");
		}
		
		//listen on the id
		ret = idPriv.listen(10);
		if (ret < 0){
			System.out.println("VerbsServer::listen not successfull");
		}

		//wait for new connect requests
		RdmaCmEvent cmEvent = cmChannel.getCmEvent(-1);
		if (cmEvent == null){
			System.out.println("cmEvent null");
			return;
		}
		else if (cmEvent.getEvent() != RdmaCmEvent.EventType.RDMA_CM_EVENT_CONNECT_REQUEST
				.ordinal()) {
			System.out.println("VerbsServer::wrong event received: " + cmEvent.getEvent());
			return;
		} 
		//always acknowledge CM events 
		cmEvent.ackEvent();
		
		//get the id of the newly connection
		RdmaCmId connId = cmEvent.getConnIdPriv();
		if (connId == null){
			System.out.println("VerbsServer::connId null");
			return;
		}
		
		//get the device context of the new connection, typically the same as with the server id
		IbvContext context = connId.getVerbs();
		if (context == null){
			System.out.println("VerbsServer::context null");
			return;
		}
		
		//create a new protection domain, we will use the pd later when registering memory
		IbvPd pd = context.allocPd();
		if (pd == null){
			System.out.println("VerbsServer::pd null");
			return;
		}
		
		//the comp channel is used to get CQ notifications
		IbvCompChannel compChannel = context.createCompChannel();
		if (compChannel == null){
			System.out.println("VerbsServer::compChannel null");
			return;
		}
		
		//create a completion queue
		IbvCQ cq = context.createCQ(compChannel, 50, 0);
		if (cq == null){
			System.out.println("VerbsServer::cq null");
			return;
		}
		//request to be notified on that CQ
		cq.reqNotification(false).execute().free();

		//prepare a new queue pair
		IbvQPInitAttr attr = new IbvQPInitAttr();
		attr.cap().setMax_recv_sge(1);
		attr.cap().setMax_recv_wr(10);
		attr.cap().setMax_send_sge(1);
		attr.cap().setMax_send_wr(10);
		attr.setQp_type(IbvQP.IBV_QPT_RC);
		attr.setRecv_cq(cq);
		attr.setSend_cq(cq);
		//create the queue pair for the client connection
		IbvQP qp = connId.createQP(pd, attr);
		if (qp == null){
			System.out.println("VerbsServer::qp null");
			return;
		}	
		
		int buffercount = 3;
		int buffersize = 100;
		ByteBuffer buffers[] = new ByteBuffer[buffercount];
		IbvMr mrlist[] = new IbvMr[buffercount];
		int access = IbvMr.IBV_ACCESS_LOCAL_WRITE | IbvMr.IBV_ACCESS_REMOTE_WRITE | IbvMr.IBV_ACCESS_REMOTE_READ; 
		
		RdmaConnParam connParam = new RdmaConnParam();
		connParam.setInitiator_depth((byte) 5);
		connParam.setResponder_resources((byte) 5);
		connParam.setRetry_count((byte) 2);
		//once the client id is set up, accept the connection
		ret = connId.accept(connParam);
		if (ret < 0){
			System.out.println("VerbsServer::accept failed");
			return;
		}
		//wait until the connection is officially switched into established mode
		cmEvent = cmChannel.getCmEvent(-1);
		if (cmEvent.getEvent() != RdmaCmEvent.EventType.RDMA_CM_EVENT_ESTABLISHED
				.ordinal()) {
			System.out.println("VerbsServer::wrong event received: " + cmEvent.getEvent());
			return;
		}
		//always ack CM events
		cmEvent.ackEvent();
		
		//register some buffers to be used later
		for (int i = 0; i < buffercount; i++){
			buffers[i] = ByteBuffer.allocateDirect(buffersize);
			mrlist[i] = pd.regMr(buffers[i], access).execute().free().getMr();
		}
		
		ByteBuffer dataBuf = buffers[0];
		IbvMr dataMr = mrlist[0];
		ByteBuffer sendBuf = buffers[1];
		IbvMr sendMr = mrlist[1];
		IbvMr recvMr = mrlist[2];
		
		dataBuf.asCharBuffer().put("This is a RDMA/read on stag " + dataMr.getLkey() + " !");
		dataBuf.clear();		

		sendBuf.putLong(dataMr.getAddr());
		sendBuf.putInt(dataMr.getLength());
		sendBuf.putInt(dataMr.getLkey());
		sendBuf.clear();

		//this class is a thin wrapper over some of the data operations in jverbs
		//we use it to issue data transfer operations
		VerbsTools commRdma = new VerbsTools(context, compChannel, qp, cq);
		LinkedList<IbvSendWR> wrList_send = new LinkedList<IbvSendWR>();	
		
		//let's preopare some work requests for sending
		IbvSge sgeSend = new IbvSge();
		sgeSend.setAddr(sendMr.getAddr());
		sgeSend.setLength(sendMr.getLength());
		sgeSend.setLkey(sendMr.getLkey());
		LinkedList<IbvSge> sgeList = new LinkedList<IbvSge>();
		sgeList.add(sgeSend);
		IbvSendWR sendWR = new IbvSendWR();
		sendWR.setWr_id(2000);
		sendWR.setSg_list(sgeList);
		sendWR.setOpcode(IbvSendWR.IBV_WR_SEND);
		sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
		wrList_send.add(sendWR);

		LinkedList<IbvRecvWR> wrList_recv = new LinkedList<IbvRecvWR>();	
		
		//let's preopare some work requests for receiving
		IbvSge sgeRecv = new IbvSge();
		sgeRecv.setAddr(recvMr.getAddr());
		sgeRecv.setLength(recvMr.getLength());
		int lkey = recvMr.getLkey();
		sgeRecv.setLkey(lkey);
		LinkedList<IbvSge> sgeListRecv = new LinkedList<IbvSge>();
		sgeListRecv.add(sgeRecv);	
		IbvRecvWR recvWR = new IbvRecvWR();
		recvWR.setSg_list(sgeListRecv);
		recvWR.setWr_id(2001);
		wrList_recv.add(recvWR);		
		
		//post a receive call
		commRdma.initSGRecv(wrList_recv);		
		System.out.println("VerbsServer::initiated recv, about to send stag info");
		//post a send call, here we send a message which include the RDMA information of a data buffer
		commRdma.send(buffers, wrList_send, true, false);
		System.out.println("VerbsServer::stag info sent");
		
		//wait for the final message from the server
		commRdma.completeSGRecv(wrList_recv, false);
		
		System.out.println("VerbsServer::done");
	}

	public void launch(String[] args) throws Exception {
		String _userDriver = "siw";
		String _provider = "nat";

		String[] _args = args;
		if (args.length < 1) {
			System.exit(0);
		} else if (args[0].equals(VerbsServer.class.getCanonicalName())) {
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
		VerbsServer verbsServer = new VerbsServer();
		verbsServer.launch(args);		
	}		
}

