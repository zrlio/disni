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

package com.ibm.disni.examples.endpoints.sendrecv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import com.ibm.disni.endpoints.RdmaActiveEndpointGroup;
import com.ibm.disni.examples.endpoints.common.CustomEndpoint;
import com.ibm.disni.verbs.IbvMr;
import com.ibm.disni.verbs.IbvRecvWR;
import com.ibm.disni.verbs.IbvSendWR;
import com.ibm.disni.verbs.IbvSge;
import com.ibm.disni.verbs.RdmaCmId;

public class CustomServerEndpoint extends CustomEndpoint {
	private ByteBuffer buffers[];
	private IbvMr mrlist[];
	private int buffercount = 3;
	private int buffersize = 100;
	
	private ByteBuffer dataBuf;
	private IbvMr dataMr;
	private ByteBuffer sendBuf;
	private IbvMr sendMr;
	private ByteBuffer recvBuf;
	private IbvMr recvMr;	
	
	private LinkedList<IbvSendWR> wrList_send;
	private IbvSge sgeSend;
	private LinkedList<IbvSge> sgeList;
	private IbvSendWR sendWR;
	
	private LinkedList<IbvRecvWR> wrList_recv;
	private IbvSge sgeRecv;
	private LinkedList<IbvSge> sgeListRecv;
	private IbvRecvWR recvWR;

	public CustomServerEndpoint(RdmaActiveEndpointGroup<CustomServerEndpoint> endpointGroup, RdmaCmId idPriv) throws IOException {	
		super(endpointGroup, idPriv);
		this.buffercount = 3;
		this.buffersize = 100;
		buffers = new ByteBuffer[buffercount];
		this.mrlist = new IbvMr[buffercount];
		
		for (int i = 0; i < buffercount; i++){
			buffers[i] = ByteBuffer.allocateDirect(buffersize);
		}
		
		this.wrList_send = new LinkedList<IbvSendWR>();	
		this.sgeSend = new IbvSge();
		this.sgeList = new LinkedList<IbvSge>();
		this.sendWR = new IbvSendWR();
		
		this.wrList_recv = new LinkedList<IbvRecvWR>();	
		this.sgeRecv = new IbvSge();
		this.sgeListRecv = new LinkedList<IbvSge>();
		this.recvWR = new IbvRecvWR();			
	}
	
	//important: we override the init method to prepare some buffers (memory registration, post recv, etc). 
	//This guarantees that at least one recv operation will be posted at the moment this endpoint is connected. 	
	public void init() throws IOException{
		super.init();
		
		for (int i = 0; i < buffercount; i++){
			mrlist[i] = registerMemory(buffers[i]).execute().free().getMr();
		}
		
		this.dataBuf = buffers[0];
		this.dataMr = mrlist[0];
		this.sendBuf = buffers[1];
		this.sendMr = mrlist[1];
		this.recvBuf = buffers[2];
		this.recvMr = mrlist[2];
		
		sgeSend.setAddr(sendMr.getAddr());
		sgeSend.setLength(sendMr.getLength());
		sgeSend.setLkey(sendMr.getLkey());
		sgeList.add(sgeSend);
		sendWR.setWr_id(2000);
		sendWR.setSg_list(sgeList);
		sendWR.setOpcode(IbvSendWR.IBV_WR_SEND);
		sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
		wrList_send.add(sendWR);
		
		sgeRecv.setAddr(recvMr.getAddr());
		sgeRecv.setLength(recvMr.getLength());
		int lkey = recvMr.getLkey();
		sgeRecv.setLkey(lkey);
		sgeListRecv.add(sgeRecv);	
		recvWR.setSg_list(sgeListRecv);
		recvWR.setWr_id(2001);
		wrList_recv.add(recvWR);
		
		System.out.println("SimpleServer::initiated recv");
		this.postRecv(wrList_recv).execute().free();		
	}

	public LinkedList<IbvSendWR> getWrList_send() {
		return wrList_send;
	}

	public LinkedList<IbvRecvWR> getWrList_recv() {
		return wrList_recv;
	}	
	
	public ByteBuffer getDataBuf() {
		return dataBuf;
	}

	public ByteBuffer getSendBuf() {
		return sendBuf;
	}

	public ByteBuffer getRecvBuf() {
		return recvBuf;
	}

	public IbvSendWR getSendWR() {
		return sendWR;
	}

	public IbvRecvWR getRecvWR() {
		return recvWR;
	}

	public IbvMr getDataMr() {
		return dataMr;
	}

	public IbvMr getSendMr() {
		return sendMr;
	}

	public IbvMr getRecvMr() {
		return recvMr;
	}		
}
