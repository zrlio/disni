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

package com.ibm.disni.benchmarks;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.ibm.disni.rdma.RdmaEndpoint;
import com.ibm.disni.rdma.RdmaEndpointFactory;
import com.ibm.disni.rdma.RdmaEndpointGroup;
import com.ibm.disni.rdma.RdmaPassiveEndpointGroup;
import com.ibm.disni.rdma.verbs.IbvCQ;
import com.ibm.disni.rdma.verbs.IbvMr;
import com.ibm.disni.rdma.verbs.IbvRecvWR;
import com.ibm.disni.rdma.verbs.IbvSendWR;
import com.ibm.disni.rdma.verbs.IbvSge;
import com.ibm.disni.rdma.verbs.IbvWC;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.rdma.verbs.SVCPollCq;
import com.ibm.disni.rdma.verbs.SVCPostSend;

public class ReadClient implements RdmaEndpointFactory<ReadClient.ReadClientEndpoint> {
	private RdmaPassiveEndpointGroup<ReadClientEndpoint> group;
	private String host;
	private int port;
	private int size;
	private int loop;
	
	public ReadClient(String host, int port, int size, int loop) throws IOException{
		this.group = new RdmaPassiveEndpointGroup<ReadClient.ReadClientEndpoint>(1, 10, 4, 40);
		this.group.init(this);
		this.host = host;
		this.port = port;
		this.size = size;
		this.loop = loop;
	}

	public ReadClient.ReadClientEndpoint createEndpoint(RdmaCmId id, boolean serverSide)
			throws IOException {
		return new ReadClientEndpoint(group, id, serverSide, size);
	}
	
	private void run() throws Exception {
		System.out.println("ReadClient, size " + size + ", loop " + loop);
		
		ReadClient.ReadClientEndpoint endpoint = group.createEndpoint();
		endpoint.connect(URI.create("rdma://" + host + ":" + port));
		System.out.println("ReadClient, client connected, address " + host + ", port " + port);	
		
		//in our custom endpoints we make sure CQ events get stored in a queue, we now query that queue for new CQ events.
		//in this case a new CQ event means we have received some data, i.e., a message from the server
		endpoint.pollUntil();
		ByteBuffer recvBuf = endpoint.getRecvBuf();
		//the message has been received in this buffer
		//it contains some RDMA information sent by the server
		recvBuf.clear();
		long addr = recvBuf.getLong();
		int length = recvBuf.getInt();
		int lkey = recvBuf.getInt();
		recvBuf.clear();		
		System.out.println("ReadClient, receiving rdma information, addr " + addr + ", length " + length + ", key " + lkey);
		System.out.println("ReadClient, preparing read operation...");		

		//the RDMA information above identifies a RDMA buffer at the server side
		//let's issue a one-sided RDMA read opeation to fetch the content from that buffer
		IbvSendWR sendWR = endpoint.getSendWR();
		sendWR.setWr_id(1001);
		sendWR.setOpcode(IbvSendWR.IBV_WR_RDMA_READ);
		sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
		sendWR.getRdma().setRemote_addr(addr);
		sendWR.getRdma().setRkey(lkey);		
		
		//post the operation on the endpoint
		SVCPostSend postSend = endpoint.newPostSend();
		long start = System.nanoTime();
		for (int i = 0; i < loop; i++){
			postSend.execute();
			endpoint.pollUntil();
			
		}
		long end = System.nanoTime();
		long duration = end - start;
		long latency = duration / loop / 1000;
		
		
		//let's prepare a final message to signal everything went fine
		sendWR.setWr_id(1002);
		sendWR.setOpcode(IbvSendWR.IBV_WR_SEND);
		sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
		sendWR.getRdma().setRemote_addr(addr);
		sendWR.getRdma().setRkey(lkey);
		
		//post that operation
		endpoint.newPostSend().execute().free();
		endpoint.pollUntil();
		
		//close everything
		endpoint.close();
		group.close();		
		
		System.out.println("ReadClient, latency " + latency);
	}	
	
	public static void main(String[] args) throws Exception {
		String ipAddress = "192.168.0.1";
		int port = 1919;
		int size = 32;
		int loop = 1000;		
		
		Option addressOption = Option.builder("a").required().desc("address of the server").hasArg().build();
		Option portOption = Option.builder("p").desc("server port").hasArg().build();
		Option sizeOption = Option.builder("s").desc("size of the data to exchange").hasArg().build();
		Option loopOption = Option.builder("k").desc("number of iterations").hasArg().build();
		Options options = new Options();
		options.addOption(addressOption);
		options.addOption(portOption);
		options.addOption(sizeOption);
		options.addOption(loopOption);
		CommandLineParser parser = new DefaultParser();
		
		try {
			CommandLine line = parser.parse(options, Arrays.copyOfRange(args, 0, args.length));
			ipAddress = line.getOptionValue(addressOption.getOpt());
			
			if (line.hasOption(portOption.getOpt())) {
				port = Integer.parseInt(line.getOptionValue(portOption.getOpt()));
			}				
			if (line.hasOption(sizeOption.getOpt())) {
				size = Integer.parseInt(line.getOptionValue(sizeOption.getOpt()));
			}	
			if (line.hasOption(sizeOption.getOpt())) {
				loop = Integer.parseInt(line.getOptionValue(loopOption.getOpt()));
			}				
		} catch (ParseException e) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("ReadClient", options);
			System.exit(-1);
		}
		
		ReadClient client = new ReadClient(ipAddress, port, size, loop);
		client.run();
	}

	public static class ReadClientEndpoint extends RdmaEndpoint {
		private ByteBuffer buffers[];
		private IbvMr mrlist[];
		private int buffersize;
		
		private ByteBuffer dataBuf;
		private IbvMr dataMr;
		private ByteBuffer sendBuf;
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
		
		private IbvWC[] wcList;
		private SVCPollCq poll;	
		
		protected ReadClientEndpoint(RdmaEndpointGroup<? extends RdmaEndpoint> group, RdmaCmId idPriv, boolean serverSide, int size) throws IOException {
			super(group, idPriv, serverSide);
			
			this.buffersize = size;
			buffers = new ByteBuffer[3];
			this.mrlist = new IbvMr[3];
			
			for (int i = 0; i < 3; i++){
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
		
		public ByteBuffer getDataBuf() {
			return dataBuf;
		}

		public SVCPostSend newPostSend() throws IOException {
			return this.postSend(wrList_send);
		}

		public IbvSendWR getSendWR() {
			return sendWR;
		}

		public ByteBuffer getRecvBuf() {
			return recvBuf;
		}

		public void init() throws IOException{
			super.init();
			
			IbvCQ cq = getCqProvider().getCQ();
			this.wcList = new IbvWC[getCqProvider().getCqSize()];
			for (int i = 0; i < wcList.length; i++){
				wcList[i] = new IbvWC();
			}		
			this.poll = cq.poll(wcList, wcList.length);				
			
			for (int i = 0; i < 3; i++){
				mrlist[i] = registerMemory(buffers[i]).execute().free().getMr();
			}
			
			this.dataBuf = buffers[0];
			this.dataMr = mrlist[0];
			this.sendBuf = buffers[1];
			this.recvBuf = buffers[2];
			this.recvMr = mrlist[2];
			
			dataBuf.clear();		
			sendBuf.clear();

			sgeSend.setAddr(dataMr.getAddr());
			sgeSend.setLength(dataMr.getLength());
			sgeSend.setLkey(dataMr.getLkey());
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
			
			this.postRecv(wrList_recv).execute().free();	
		}
		
		private int pollUntil() throws IOException {
			int res = 0;
			while (res == 0) {
				res = poll.execute().getPolls();
			}
			return res;
		}		
	}
}
