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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;

import org.slf4j.Logger;

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
import com.ibm.disni.rdma.verbs.RdmaVerbs;
import com.ibm.disni.util.DiSNILogger;
import com.ibm.disni.util.StopWatch;

public class RdmaServer extends BenchmarkBase implements IBenchmarkTask { 
	private static final Logger logger = DiSNILogger.getLogger();

	private String serverIP;
	private short data_port;
	private short control_port;
	private int backlog;
	private RdmaCmEvent cmEvent;
	private int ncqe = 50;
	private int comp_vector = 0;
	private RdmaCm cm;
	private RdmaVerbs verbs;
	private RdmaEventChannel cmChannel = null;
	private RdmaCmId idPriv = null;
	private IbvContext context = null;
	private IbvPd pd = null;
	private IbvCompChannel compChannel = null;
	private IbvCQ cq = null;
	private IbvQP qp = null;
	private IbvQPInitAttr attr = null;
	private int access;
	private RdmaConnParam connParam = null;
	private RdmaCmId connId = null;

	private ServerSocketChannel controlServerChannel;
	private SocketChannel controlChannel;
	private int size;
	private StopWatch stopWatchThroughput;
	private int loop;
	private AppLauncher.TestCase testCase;
	private RpcStub stub;

	private boolean inlineData;
	private boolean polling;

	public RdmaServer(AppLauncher applauncher) throws Exception {
		this.serverIP = applauncher.getIpAddress();
		this.size = applauncher.getSize();
		this.loop = applauncher.getLoop();
		this.testCase = applauncher.getTestCase();

		this.control_port = applauncher.getControlPort();
		this.data_port = applauncher.getDataPort();
		this.backlog = 1;
		this.ncqe = 50;
		this.comp_vector = 0;
		this.access = IbvMr.IBV_ACCESS_LOCAL_WRITE
				| IbvMr.IBV_ACCESS_REMOTE_WRITE
				| IbvMr.IBV_ACCESS_REMOTE_READ;
		this.verbs = RdmaVerbs.open();
		this.cm = RdmaCm.open();
		this.stopWatchThroughput = new StopWatch();
		this.controlServerChannel = ServerSocketChannel.open();

		this.inlineData = applauncher.useInline();
		this.polling = applauncher.usePolling();
		this.stub = new RpcStub(size);
	}

	public void run() { 
		try {
			// init program data channel
			int ret = -1;
			InetAddress _src = InetAddress.getByName(serverIP);
			InetSocketAddress src = new InetSocketAddress(_src, data_port);			
			InetAddress localHost = InetAddress.getByName(serverIP);
			InetSocketAddress controlAddress = new InetSocketAddress(localHost,
					control_port);
			
			cmChannel = cm.createEventChannel();
			if (cmChannel == null){
				logger.info("Cm Channel null");
				return;
			}
			
			idPriv = cm.createId(cmChannel, RdmaCm.RDMA_PS_TCP);
			if (idPriv == null){
				logger.info("idPriv null");
				return;
			}
			
			logger.info("server id, handle.. " + idPriv);
			logger.info("serverIP=" + serverIP);

			ret = cm.bindAddr(idPriv, src);
			if (ret < 0){
				logger.info("binding not sucessfull");
			}
			
			ret = cm.listen(idPriv, backlog);
			if (ret < 0){
				logger.info("listen not successfull");
			}

			// accept control channel;
			controlServerChannel.bind(controlAddress);
			controlChannel = controlServerChannel.accept();
			controlChannel.configureBlocking(true);
			logger.info("control channel set up");
			
			// accept data channel;
			cmEvent = cm.getCmEvent(cmChannel, -1);
			if (cmEvent == null){
				logger.info("cmEvent null");
				return;
			}
			else if (cmEvent.getEvent() != RdmaCmEvent.EventType.RDMA_CM_EVENT_CONNECT_REQUEST
					.ordinal()) {
				logger.warn("wrong event received: " + cmEvent.getEvent());
				return;
			} 
			cm.ackCmEvent(cmEvent); 
			
			connId = cmEvent.getConnIdPriv();
			if (connId == null){
				logger.info("connId null");
				return;
			}
			logger.info("new clientID with handle " + connId);
			context = connId.getVerbs();
			if (context == null){
				logger.info("context null");
				return;
			}
			
			pd = verbs.allocPd(context);
			if (pd == null){
				logger.info("pd null");
				return;
			}
			
			logger.info("server id, handle " + idPriv);
			compChannel = verbs.createCompChannel(context);
			if (compChannel == null){
				logger.info("compChannel null");
				return;
			}
			
			cq = verbs.createCQ(context, compChannel, ncqe, comp_vector);
			if (cq == null){
				logger.info("cq null");
				return;
			}
			verbs.reqNotifyCQ(cq, false).execute().free();

			attr = new IbvQPInitAttr();
			attr.cap().setMax_recv_sge(CommRdma.MAX_SGE);
			attr.cap().setMax_recv_wr(CommRdma.MAX_WR);
			attr.cap().setMax_send_sge(CommRdma.MAX_SGE);
			attr.cap().setMax_send_wr(CommRdma.MAX_WR);
			attr.setQp_type(IbvQP.IBV_QPT_RC);
			attr.setRecv_cq(cq);
			attr.setSend_cq(cq);
			if (inlineData){
				logger.info("mode is inline");
				attr.cap().setMax_inline_data(size);
			}			
			
			qp = cm.createQP(connId, pd, attr);
			if (qp == null){
				logger.info("qp null");
				return;
			}
			
			ByteBuffer[] sendFragments = BufferUtils.getBufferFragments("", size, size, true);	
			ByteBuffer[] recvFragments = BufferUtils.getBufferFragments("", sendFragments.length*4, 4, true);	
			ByteBuffer[] rpcBuf = BufferUtils.getBufferFragments("", size, size, true);
			
			if (testCase == AppLauncher.TestCase.WRITE) {
				logger.info("preparing test case write");
			} else if (testCase == AppLauncher.TestCase.READ) {
				logger.info("preparing test case read");
				for (int i = 0; i < sendFragments.length; i++){
					BufferUtils.setString(sendFragments[i], i);
				}
			} else if (testCase == AppLauncher.TestCase.PING){
			}
			
			LinkedList<IbvMr> mrList_recv = new LinkedList<IbvMr>();
			LinkedList<IbvRecvWR> wrList_recv = new LinkedList<IbvRecvWR>();			
			LinkedList<IbvMr> mrList_send = new LinkedList<IbvMr>();
			LinkedList<IbvSendWR> wrList_send = new LinkedList<IbvSendWR>();
			
			for (int i = 0; i < sendFragments.length; i++) {
				ByteBuffer sendBuffer = sendFragments[i];
				long sendAddress = ((sun.nio.ch.DirectBuffer) sendBuffer).address();
				IbvMr sendMr = verbs.regMr(pd, sendBuffer, access).execute().free().getMr();	
				if (sendMr == null){
					logger.info("registration failed");
					return;
				} else {
					logger.info("registered send buffer*, addr " + sendMr.getAddr() + ", length " + sendMr.getLength() + ", stag " + sendMr.getLkey());
				}
				
				
				
				ByteBuffer recvBuffer = recvFragments[i];
				long recvAddress = ((sun.nio.ch.DirectBuffer) recvBuffer).address();
				IbvMr recvMr = verbs.regMr(pd, recvBuffer, access).execute().free().getMr();
				if (recvMr == null){
					logger.info("registration failed");
					return;
				} else {
					logger.info("registered recv buffer*, addr " + recvMr.getAddr() + ", length " + recvMr.getLength() + ", stag " + recvMr.getLkey());
				}
				
				IbvSge sge = new IbvSge();
				sge.setAddr(sendAddress);
				sge.setLength(sendBuffer.capacity());
				sge.setLkey(sendMr.getLkey());
				LinkedList<IbvSge> sgeList = new LinkedList<IbvSge>();
				sgeList.add(sge);

				IbvSendWR sendWR = new IbvSendWR();
				sendWR.setWr_id(55+i);
				sendWR.setSg_list(sgeList);
				sendWR.setOpcode(IbvSendWR.IbvWrOcode.IBV_WR_SEND.ordinal());
				sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
				if (inlineData){
					sendWR.setSend_flags(sendWR.getSend_flags() | IbvSendWR.IBV_SEND_INLINE);
					logger.info("setting inline flag " + sendWR.getSend_flags());
				} else {
					logger.info("not setting inline flag " + sendWR.getSend_flags());
				}
				
				wrList_send.add(sendWR);
				mrList_send.add(sendMr);				
				
				//preparing for recv
				IbvSge sgeRecv = new IbvSge();
				sgeRecv.setAddr(recvAddress);
				sgeRecv.setLength(recvBuffer.capacity());
				int lkey = recvMr.getLkey();
				sgeRecv.setLkey(lkey);
				LinkedList<IbvSge> sgeListRecv = new LinkedList<IbvSge>();
				sgeListRecv.add(sgeRecv);

				IbvRecvWR recvWR = new IbvRecvWR();
				recvWR.setSg_list(sgeListRecv);
				recvWR.setWr_id(77+i);
				wrList_recv.add(recvWR);
				mrList_recv.add(recvMr);				
				
			}			
			
			ReadyToReceive client2server = new ReadyToReceive(size, size, loop, mrList_send);
			ReadyToReceive server2client = new ReadyToReceive(size, size, loop, mrList_send);
			
			ByteBuffer[] client2serverBuffer = BufferUtils.getBufferFragments("", client2server.size(), client2server.size(), true);
			ByteBuffer[] server2clientBuffer = BufferUtils.getBufferFragments("", server2client.size(), server2client.size(), true);
			
			CommRdma dataPlane = new CommRdma(verbs, context, compChannel, qp, cq);
			CommNio controlPlane = new CommNio(controlChannel);
			
			connParam = new RdmaConnParam();
			connParam.setInitiator_depth((byte) 5);
			connParam.setResponder_resources((byte) 5);
			connParam.setRetry_count((byte) 2);
			ret = cm.accept(connId, connParam);
			if (ret < 0){
				logger.info("accept failed");
				return;
			}
			
			cmEvent = cm.getCmEvent(cmChannel, -1);
			if (cmEvent.getEvent() != RdmaCmEvent.EventType.RDMA_CM_EVENT_ESTABLISHED
					.ordinal()) {
				logger.warn("wrong event received: " + cmEvent.getEvent());
				return;
			} 
			cm.ackCmEvent(cmEvent);
			
			logger.info("back from accept..\n");			

			logger.info("starting handshake, server2client->size " + server2client.size());
			
			controlPlane.waitForNextRound(client2serverBuffer[0]);
			
			client2serverBuffer[0].clear();
			client2server.update(client2serverBuffer[0]);
			
			for (IbvMr mr : client2server.getStagList()){
				logger.info("received addr " + mr.getAddr());				
				logger.info("received stag " + mr.getLkey());
			}
			
			dataPlane.initSGRecv(sendFragments, wrList_recv); 
				
			server2clientBuffer[0].clear();
			server2client.writeBack(server2clientBuffer[0]);
			controlPlane.startNextRound(server2clientBuffer[0]);
			
			for (IbvMr mr : server2client.getStagList()){
				logger.info("used addr " + mr.getAddr());						
				logger.info("used stag " + mr.getLkey());
			}

			logger.info("handshake done\n");
			
			stopWatchThroughput.start();
			double sumbytes = 0;
			for (int i = 0; i < loop; i++) { 
				if (testCase == AppLauncher.TestCase.WRITE) {
					controlPlane.waitForNextRound(client2serverBuffer[0]);
					BufferUtils.tagBuffer(sendFragments, i + 1);
					boolean ok = dataPlane.send(sendFragments, wrList_send, true, polling);
					if (!ok){
						logger.info("rdma send failed");
					}
				} else if (testCase == AppLauncher.TestCase.PING || testCase == AppLauncher.TestCase.RPC_INT || testCase == AppLauncher.TestCase.RPC_ARRAY || testCase == AppLauncher.TestCase.RPC_COMPLEX) {
					boolean ok = dataPlane.completeSGRecv(sendFragments, wrList_recv, polling);
					if (!ok){
						return;						
					} 
					
					if (testCase == AppLauncher.TestCase.RPC_INT){
						stub.demarshallInt(rpcBuf);
						stub.marshallInt(rpcBuf);
					} else if (testCase == AppLauncher.TestCase.RPC_ARRAY){
						stub.demarshallArray(rpcBuf);
						stub.marshallArray(rpcBuf);
					}					
					ok = dataPlane.initSGRecv(sendFragments, wrList_recv);
					if (!ok){
						return;						
					}
					
					ok = dataPlane.send(sendFragments, wrList_send, true, polling);
					if (!ok){
						return;						
					}					
				} else if (testCase == AppLauncher.TestCase.READ) {
					break;
				}

			}
			
			logger.info("starting shutdown handshake");
			double executionTime = (double) stopWatchThroughput
					.getExecutionTime() / 1000.0;
			double sumbits = sumbytes * 8.0;
			if (executionTime > 0) {
				this.throughput = sumbits / executionTime / 1000.0 / 1000.0;
			}
			
			controlPlane.waitForNextRound(client2serverBuffer[0]);
			controlPlane.startNextRound(server2clientBuffer[0]);
			logger.warn("final handshake done");
			logger.info("throughput " + throughput + " Mbit/s");

			cmEvent = cm.getCmEvent(cmChannel, -1);
			if (cmEvent.getEvent() != RdmaCmEvent.EventType.RDMA_CM_EVENT_DISCONNECTED
					.ordinal()) {
				logger.warn("wrong event received: " + cmEvent.getEvent());
				return;
			} else {
				logger.warn("got disconnect event");
			}
			cm.ackCmEvent(cmEvent);

			for (Iterator<IbvMr> iter = mrList_send.iterator(); iter.hasNext();) {
				IbvMr mr = iter.next();
				verbs.deregMr(mr).execute().free();
			}

			logger.info("done...");
		} catch (Exception e) {
			System.out.println("Exception e=" + e.toString());
			System.out.println("StackTrace:\n");
			e.printStackTrace();
		}
	}

	@Override
	public void close() throws Exception {
	}
	
}
