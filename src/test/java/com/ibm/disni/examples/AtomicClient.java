/*
 * DiSNI: Direct Storage and Networking Interface
 *
 * Author: Konstantin Taranov <ktaranov@inf.ethz.ch>
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

import com.ibm.disni.CmdLineCommon;
import com.ibm.disni.verbs.*;
import org.apache.commons.cli.ParseException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

public class AtomicClient {
	private String ipAddress;
	private int port;

	public void run() throws Exception {
		System.out.println("AtomicClient::starting...");
		//open the CM and the verbs interfaces

		//create a communication channel for receiving CM events
		RdmaEventChannel cmChannel = RdmaEventChannel.createEventChannel();
		if (cmChannel == null){
			System.out.println("AtomicClient::cmChannel null");
			return;
		}

		//create a RdmaCmId for this client
		RdmaCmId idPriv = cmChannel.createId(RdmaCm.RDMA_PS_TCP);
		if (idPriv == null){
			System.out.println("AtomicClient::id null");
			return;
		}

		//before connecting, we have to resolve addresses
		InetAddress _dst = InetAddress.getByName(ipAddress);
		InetSocketAddress dst = new InetSocketAddress(_dst, port);
		idPriv.resolveAddr(null, dst, 2000);

		//resolve addr returns an event, we have to catch that event
		RdmaCmEvent cmEvent = cmChannel.getCmEvent(-1);
		if (cmEvent == null){
			System.out.println("AtomicClient::cmEvent null");
			return;
		} else if (cmEvent.getEvent() != RdmaCmEvent.EventType.RDMA_CM_EVENT_ADDR_RESOLVED
				.ordinal()) {
			System.out.println("AtomicClient::wrong event received: " + cmEvent.getEvent());
			return;
		}
		cmEvent.ackEvent();

		//we also have to resolve the route
		idPriv.resolveRoute(2000);
		//and catch that event too
		cmEvent = cmChannel.getCmEvent(-1);
		if (cmEvent == null){
			System.out.println("AtomicClient::cmEvent null");
			return;
		} else if (cmEvent.getEvent() != RdmaCmEvent.EventType.RDMA_CM_EVENT_ROUTE_RESOLVED
				.ordinal()) {
			System.out.println("AtomicClient::wrong event received: " + cmEvent.getEvent());
			return;
		}
		cmEvent.ackEvent();

		//let's create a device context
		IbvContext context = idPriv.getVerbs();

		//and a protection domain, we use that one later for registering memory
		IbvPd pd = context.allocPd();
		if (pd == null){
			System.out.println("AtomicClient::pd null");
			return;
		}

		//the comp channel is used for getting CQ events
		IbvCompChannel compChannel = context.createCompChannel();
		if (compChannel == null){
			System.out.println("AtomicClient::compChannel null");
			return;
		}

		//let's create a completion queue
		IbvCQ cq = context.createCQ(compChannel, 50, 0);
		if (cq == null){
			System.out.println("AtomicClient::cq null");
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
			System.out.println("AtomicClient::qp null");
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

		IbvDeviceAttr deviceAttr = context.queryDevice();

		int maxResponderResources = deviceAttr.getMax_qp_rd_atom();
		int maxInitiatorDepth = deviceAttr.getMax_qp_init_rd_atom();

		//now let's connect to the server
		RdmaConnParam connParam = new RdmaConnParam();
		connParam.setRetry_count((byte) 2);
		connParam.setResponder_resources((byte) maxResponderResources);
		connParam.setInitiator_depth((byte) maxInitiatorDepth);
		idPriv.connect(connParam);		

 
		//wait until we are really connected
		cmEvent = cmChannel.getCmEvent(-1);
		if (cmEvent == null){
			System.out.println("AtomicClient::cmEvent null");
			return;
		} else if (cmEvent.getEvent() != RdmaCmEvent.EventType.RDMA_CM_EVENT_ESTABLISHED
				.ordinal()) {
			System.out.println("AtomicClient::wrong event received: " + cmEvent.getEvent());
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
		System.out.println("AtomicClient::receiving rdma information, addr " + addr + ", length " + length + ", key " + lkey);
		System.out.println("AtomicClient::preparing atomic operation...");
		
		dataBuf.order(ByteOrder.LITTLE_ENDIAN);
		System.out.println("AtomicClient::initial value in the buffer: " + dataBuf.getLong());

		//let's prepare a one-sided RDMA atomic operation to fetch the content of that remote buffer
		LinkedList<IbvSendWR> wrList_send = new LinkedList<IbvSendWR>();
		IbvSge sgeSend = new IbvSge();
		sgeSend.setAddr(dataMr.getAddr());
		sgeSend.setLength(8);
		sgeSend.setLkey(dataMr.getLkey());
		LinkedList<IbvSge> sgeList = new LinkedList<IbvSge>();
		sgeList.add(sgeSend);
		IbvSendWR sendWR = new IbvSendWR();
		sendWR.setWr_id(1001);
		sendWR.setSg_list(sgeList);
		sendWR.setOpcode(IbvSendWR.IBV_WR_ATOMIC_FETCH_AND_ADD);
		sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
		sendWR.getAtomic().setRemote_addr(addr);
		sendWR.getAtomic().setRkey(lkey);
		sendWR.getAtomic().setCompare_add(10);
		wrList_send.add(sendWR);

		//now we post the operation, the RDMA/atomic operation will take off
		//the wrapper class will also wait of the CQ event
		//once the CQ event is received we know the RDMA/atomic operation has completed
		//we should have the content of the remote buffer stored in our own local buffer
		//let's print it
		commRdma.send(buffers, wrList_send, true, false);
		dataBuf.clear();
		System.out.println("AtomicClient::the remote server values has been incremented by 10");
		System.out.println("AtomicClient::the fetched value from the remote server: " + dataBuf.getLong());
		
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
		CmdLineCommon cmdLine = new CmdLineCommon("AtomicClient");

		try {
			cmdLine.parse(args);
		} catch (ParseException e) {
			cmdLine.printHelp();
			System.exit(-1);
		}
		ipAddress = cmdLine.getIp();
		port = cmdLine.getPort();

		this.run();
	}

	public static void main(String[] args) throws Exception {
		AtomicClient AtomicClient = new AtomicClient();
		AtomicClient.launch(args);
	}
}

