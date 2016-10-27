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
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;

import org.slf4j.Logger;

import com.ibm.disni.util.DiSNILogger;
import com.ibm.disni.util.StopWatch;
import com.ibm.disni.verbs.IbvCQ;
import com.ibm.disni.verbs.IbvCompChannel;
import com.ibm.disni.verbs.IbvContext;
import com.ibm.disni.verbs.IbvMr;
import com.ibm.disni.verbs.IbvPd;
import com.ibm.disni.verbs.IbvQP;
import com.ibm.disni.verbs.IbvQPInitAttr;
import com.ibm.disni.verbs.IbvRecvWR;
import com.ibm.disni.verbs.IbvSendWR;
import com.ibm.disni.verbs.IbvSge;
import com.ibm.disni.verbs.RdmaCm;
import com.ibm.disni.verbs.RdmaCmEvent;
import com.ibm.disni.verbs.RdmaCmId;
import com.ibm.disni.verbs.RdmaConnParam;
import com.ibm.disni.verbs.RdmaEventChannel;
import com.ibm.disni.verbs.RdmaVerbs;

public class RdmaClient extends BenchmarkBase implements IBenchmarkTask { 
	private static final Logger logger = DiSNILogger.getLogger();

	private String clientIP; 
	private int size;
	private StopWatch stopWatchThroughput;
	private SocketChannel controlChannel;
	private int loop;
	private AppLauncher.TestCase testCase;

	private short data_port; 
	private short control_port;
	private int timeout;
	private RdmaCmEvent cmEvent;
	private int ncqe;
	private int comp_vector;
	private RdmaVerbs verbs;
	private RdmaCm cm;
	private RdmaEventChannel cmChannel = null;
	private RdmaCmId idPriv;
	private IbvContext context;
	private IbvPd pd;
	private IbvCompChannel compChannel;
	private IbvCQ cq;
	private IbvQP qp;
	private IbvQPInitAttr attr;
	private int access;
	private RdmaConnParam connParam;
	private boolean inlineData;
	private boolean polling;
	private RpcStub stub;
	
	public RdmaClient(AppLauncher applauncher) throws Exception {
		this.clientIP = applauncher.getIpAddress();
		this.size = applauncher.getSize();
		this.stopWatchThroughput = new StopWatch();
		this.loop = applauncher.getLoop();
		this.testCase = applauncher.getTestCase();

		this.control_port = applauncher.getControlPort();
		this.data_port = applauncher.getDataPort();
		this.timeout = 2000;
		this.ncqe = 50;
		this.comp_vector = 0;
		this.verbs = RdmaVerbs.open();
		this.cm = RdmaCm.open();
		this.access = IbvMr.IBV_ACCESS_REMOTE_WRITE
				| IbvMr.IBV_ACCESS_LOCAL_WRITE;
		this.controlChannel = SocketChannel.open();
		
		this.polling = applauncher.usePolling();
		this.inlineData = applauncher.useInline();
		this.stub = new RpcStub(size);
	}

	public void run() {
		try {
			// init data parameters
			int ret = -1;
			InetAddress _dst = InetAddress.getByName(clientIP);
			InetSocketAddress dst = new InetSocketAddress(_dst, data_port);			
			InetAddress localHost = InetAddress.getByName(clientIP);
			InetSocketAddress controlAddress = new InetSocketAddress(localHost,
					this.control_port);

			cmChannel = cm.createEventChannel();
			if (cmChannel == null){
				logger.info("cmChannel null");
				return;
			}
			
			idPriv = cm.createId(cmChannel, RdmaCm.RDMA_PS_TCP);
			if (idPriv == null){
				logger.info("idPriv null");
				return;
			}
			
			// connecting data channel 
			ret = cm.resolveAddr(idPriv, null, dst, timeout);
			if (ret < 0){
				logger.info("resolveAddr failed");
				return;
			}
			
			cmEvent = cm.getCmEvent(cmChannel, -1);
			if (cmEvent == null){
				logger.info("cmEvent null");
				return;
			} else if (cmEvent.getEvent() != RdmaCmEvent.EventType.RDMA_CM_EVENT_ADDR_RESOLVED
					.ordinal()) {
				logger.info("wrong event received: " + cmEvent.getEvent());
				return;
			} 
			cm.ackCmEvent(cmEvent);
			
			ret = cm.resolveRoute(idPriv, timeout);
			if (ret < 0){
				logger.info("resolveRoute failed");
				return;
			}
			
			cmEvent = cm.getCmEvent(cmChannel, -1);
			if (cmEvent == null){
				logger.info("cmEvent null");
				return;
			} else if (cmEvent.getEvent() != RdmaCmEvent.EventType.RDMA_CM_EVENT_ROUTE_RESOLVED
					.ordinal()) {
				logger.info("wrong event received: " + cmEvent.getEvent());
				return;
			} 
			cm.ackCmEvent(cmEvent); 

			context = idPriv.getVerbs();
			
			pd = verbs.allocPd(context);
			if (pd == null){
				logger.info("pd null");
				return;
			}
			
			compChannel = verbs.createCompChannel(context);
			if (compChannel == null){
				logger.info("compChannel null");
				return;
			}
			
			logger.info("compchannel set up");
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
			
			logger.info("about to create qp");
			qp = cm.createQP(idPriv, pd, attr);
			if (qp == null){
				logger.info("qp null");
				return;
			}
			
			logger.info("RdmaClient::main: clientIP=" + clientIP);

			logger.info("setting control channel");
			// connecting control channel
			controlChannel.configureBlocking(true);
			controlChannel.connect(controlAddress);
			logger.info("setting up control channel");

			connParam = new RdmaConnParam();
			connParam.setInitiator_depth((byte) 5);
			connParam.setResponder_resources((byte) 5);
			connParam.setRetry_count((byte) 2);
			ret = cm.connect(idPriv, connParam);
			if (ret < 0){
				logger.info("connect failed");
				return;
			}
			
			cmEvent = cm.getCmEvent(cmChannel, -1);
			logger.info("after connect, event " + cmEvent.getEvent() + ", idPriv.handle " + idPriv);
			if (cmEvent == null){
				logger.info("cmEvent null");
				return;
			} else if (cmEvent.getEvent() != RdmaCmEvent.EventType.RDMA_CM_EVENT_ESTABLISHED
					.ordinal()) {
				logger.info("wrong event received: " + cmEvent.getEvent());
				return;
			}
			cm.ackCmEvent(cmEvent);
			
			logger.info("back from connect"); 
			
			ByteBuffer[] recvFragments = BufferUtils.getBufferFragments("", size, size, true);
			ByteBuffer[] sendFragments = BufferUtils.getBufferFragments("", recvFragments.length*4, 4, true);
			
			if (testCase == AppLauncher.TestCase.WRITE) {
				logger.info("preparing test case write");
			} else if (testCase == AppLauncher.TestCase.READ) {
				logger.info("preparing test case read");
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
					logger.info("registered send buffer, addr " + sendMr.getAddr() + ", length " + sendMr.getLength() + ", stag " + sendMr.getLkey());
				}
				
				ByteBuffer recvBuffer = recvFragments[i];
				long recvAddress = ((sun.nio.ch.DirectBuffer) recvBuffer).address();
				IbvMr recvMr = verbs.regMr(pd, recvBuffer, access).execute().free().getMr();
				if (recvMr == null){
					logger.info("registration failed");
					return;
				} else {
					logger.info("registered recv buffer, addr " + recvMr.getAddr() + ", length " + recvMr.getLength() + ", stag " + recvMr.getLkey());
				}

				//preparing for send
				IbvSge sgeSend = new IbvSge();
				LinkedList<IbvSge> sgeListSend = new LinkedList<IbvSge>();
				if (testCase == AppLauncher.TestCase.PING){
					sgeSend.setAddr(sendAddress);
					sgeSend.setLength(sendBuffer.capacity());
					sgeSend.setLkey(sendMr.getLkey());
				} else if (testCase == AppLauncher.TestCase.READ){
					sgeSend.setAddr(recvAddress);
					sgeSend.setLength(recvBuffer.capacity());
					sgeSend.setLkey(recvMr.getLkey());
				}
				sgeListSend.add(sgeSend);

				IbvSendWR sendWR = new IbvSendWR();
				sendWR.setWr_id(55+i);
				sendWR.setSg_list(sgeListSend);
				sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
				if (inlineData){
					sendWR.setSend_flags(sendWR.getSend_flags() | IbvSendWR.IBV_SEND_INLINE);
					logger.info("setting inline flag " + sendWR.getSend_flags());
				} else {
					logger.info("not setting inline flag " + sendWR.getSend_flags());
				}
				if (testCase == AppLauncher.TestCase.WRITE || testCase == AppLauncher.TestCase.PING) {
					sendWR.setOpcode(IbvSendWR.IbvWrOcode.IBV_WR_SEND.ordinal());
				} else if (testCase == AppLauncher.TestCase.READ) {
					logger.info("test case is read");
					sendWR.setOpcode(IbvSendWR.IbvWrOcode.IBV_WR_RDMA_READ.ordinal());
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
			
			ByteBuffer[] rpcBuf = BufferUtils.getBufferFragments("", size, size, true);
			ReadyToReceive client2server = new ReadyToReceive(size,	size, loop, mrList_recv);
			ReadyToReceive server2client = new ReadyToReceive(size, size, loop, mrList_send);
			
			ByteBuffer[] client2serverBuffer = BufferUtils.getBufferFragments("", client2server.size(), client2server.size(), true);
			ByteBuffer[] server2clientBuffer = BufferUtils.getBufferFragments("", server2client.size(), server2client.size(), true);
			
			CommRdma dataPlane = new CommRdma(verbs, context, compChannel, qp, cq);
			CommNio controlPlane = new CommNio(controlChannel);
			
			logger.info("\n");
			logger.info("starting handshake");
			
			for (IbvMr mr : client2server.getStagList()) {
				logger.info("used addr " + mr.getAddr());
				logger.info("used stag " + mr.getLkey());
			}
			
			client2serverBuffer[0].clear();
			client2server.writeBack(client2serverBuffer[0]);
			controlPlane.startNextRound(client2serverBuffer[0]);
			
			controlPlane.waitForNextRound(server2clientBuffer[0]);
			server2clientBuffer[0].clear();
			server2client.update(server2clientBuffer[0]);		
			
			if (testCase == AppLauncher.TestCase.WRITE) {
				logger.info("test case is write");
				for (IbvMr mr : server2client.getStagList()) {
					logger.info("received addr " + mr.getAddr());
					logger.info("received stag " + mr.getLkey());
				}				
			} else if (testCase == AppLauncher.TestCase.READ) {
				logger.info("test case is read");
				for (int i = 0; i < wrList_send.size(); i++) {
					IbvSendWR currentWR = wrList_send.get(i);
					IbvMr mr = server2client.getStagList().get(i);
					currentWR.getRdma().setRemote_addr(mr.getAddr());
					currentWR.getRdma().setRkey(mr.getLkey());
					logger.info("received addr " + mr.getAddr());
					logger.info("received stag " + mr.getLkey());
				}
			}
			
			logger.info("handshake done\n");
			
			stopWatchThroughput.start();
			double sumbytes = 0;
			double ops = 0;
			for (int i = 0; i < loop; i++) {
				if (testCase == AppLauncher.TestCase.WRITE) {
					dataPlane.initSGRecv(recvFragments, wrList_recv);
					controlPlane.startNextRound(client2serverBuffer[0]);
					boolean ok = dataPlane.completeSGRecv(recvFragments, wrList_recv, polling);
					if (!ok){
						logger.info("complete recv failed");
						return;
					}
					ok = BufferUtils.checkTag(recvFragments, i + 1);

					if (ok) {
						sumbytes += ((double) size);
						this.writeOps++;
					} else {
						logger.info("incorrect data received");
						this.errorOps += 1.0;
					}
				}  else if (testCase == AppLauncher.TestCase.PING || testCase == AppLauncher.TestCase.RPC_INT || testCase == AppLauncher.TestCase.RPC_ARRAY || testCase == AppLauncher.TestCase.RPC_COMPLEX) {
					dataPlane.initSGRecv(recvFragments, wrList_recv);
					if (testCase == AppLauncher.TestCase.RPC_INT){
						stub.marshallInt(rpcBuf);
					} else if (testCase == AppLauncher.TestCase.RPC_ARRAY){
						stub.marshallArray(rpcBuf);
					}
					
					boolean ok = dataPlane.send(recvFragments, wrList_send, true, polling);
					if (!ok){
						logger.info("send failed");
						return;
					}
					
					ok = dataPlane.completeSGRecv(recvFragments, wrList_recv, polling);
					if (!ok){
						logger.info("complete recv failed");
						return;
					} 
					
					if (testCase == AppLauncher.TestCase.RPC_INT){
						stub.demarshallInt(rpcBuf);
					} else if (testCase == AppLauncher.TestCase.RPC_ARRAY){
						stub.demarshallArray(rpcBuf);
					}					
				} else if (testCase == AppLauncher.TestCase.READ) {
					boolean ok = dataPlane.send(recvFragments, wrList_send, true, polling);
					if (!ok){
						logger.info("send failed");
						return;
					}
					sumbytes += ((double) size);
				}
				ops += 1.0;
			}
			logger.info("starting shutdown handshake");
			double executionTime = (double) stopWatchThroughput.getExecutionTime() / 1000.0;
			double sumbits = sumbytes * 8.0;
			logger.info("execution time " + executionTime + " sumbits " + sumbits);
			if (executionTime > 0) {
				this.throughput = sumbits / executionTime / 1000.0 / 1000.0;
				this.latency = 1000000 * executionTime / ((double) ops);
			}
			
			controlPlane.startNextRound(client2serverBuffer[0]);
			controlPlane.waitForNextRound(server2clientBuffer[0]);
			
			logger.info("final handshake done");
			logger.info("throughput " + throughput + " Mbit/s");

			cm.disconnect(idPriv);
			cmEvent = cm.getCmEvent(cmChannel, -1);
			if (cmEvent.getEvent() != RdmaCmEvent.EventType.RDMA_CM_EVENT_DISCONNECTED
					.ordinal()) {
				logger.info("wrong event received: " + cmEvent.getEvent());
				return;
			} else {
				logger.info("got disconnect event");
			}
			cm.ackCmEvent(cmEvent);
			
			for (Iterator<IbvMr> iter = mrList_send.iterator(); iter.hasNext();) {
				IbvMr mr = iter.next();
				verbs.deregMr(mr).execute().free();
			}

			logger.info("done...");
		} catch (Exception e) {
			logger.warn("Exception e=" + e.toString());
			logger.warn("StackTrace:\n");
			e.printStackTrace();
		}
	}

	public void close() throws Exception {
	}
}
