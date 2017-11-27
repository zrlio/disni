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
import java.util.ArrayList;
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
import com.ibm.disni.rdma.RdmaPassiveEndpointGroup;
import com.ibm.disni.rdma.verbs.IbvCQ;
import com.ibm.disni.rdma.verbs.IbvMr;
import com.ibm.disni.rdma.verbs.IbvRecvWR;
import com.ibm.disni.rdma.verbs.IbvSendWR;
import com.ibm.disni.rdma.verbs.IbvSge;
import com.ibm.disni.rdma.verbs.IbvWC;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.rdma.verbs.SVCPollCq;
import com.ibm.disni.rdma.verbs.SVCPostRecv;
import com.ibm.disni.rdma.verbs.SVCPostSend;

public class SendRecvClient implements RdmaEndpointFactory<SendRecvClient.SendRecvEndpoint> {
	private RdmaPassiveEndpointGroup<SendRecvEndpoint> group;
	private String host;
	private int port;
	private int size;
	private int loop;
	private int recvQueueSize;
	
	public SendRecvClient(String host, int port, int size, int loop, int recvQueueSize) throws IOException{
		this.group = new RdmaPassiveEndpointGroup<SendRecvClient.SendRecvEndpoint>(1, recvQueueSize, 1, recvQueueSize*2);
		this.group.init(this);
		this.host = host;
		this.port = port;
		this.size = size;
		this.loop = loop;
		this.recvQueueSize = recvQueueSize;
	}

	public SendRecvClient.SendRecvEndpoint createEndpoint(RdmaCmId id, boolean serverSide)
			throws IOException {
		return new SendRecvEndpoint(group, id, serverSide, size, recvQueueSize);
	}
	
	
	private void run() throws Exception {
		System.out.println("SendRecvClient, size " + size + ", loop " + loop + ", recvQueueSize " + recvQueueSize + ", port " + port);
		
		SendRecvClient.SendRecvEndpoint endpoint = group.createEndpoint();
		endpoint.connect(URI.create("rdma://" + host + ":" + 1919));
		System.out.println("SendRecvClient, client connected, address " + host + ", port " + 1919);	
		
		int opCount = 0;
		long start = System.nanoTime();
		while (opCount < loop){
			endpoint.pollRecvs();
			int remaining = loop - opCount;
			int actualRecvs = endpoint.installRecvs(remaining);
//			System.out.println("actualRecvs = " + actualRecvs);
			int actualSends = endpoint.sendRequest(actualRecvs);
//			System.out.println("actualSends = " + actualSends);
			opCount += actualSends;
//			System.out.println("opCount " + opCount);
		}
		endpoint.awaitRecvs();
		endpoint.awaitSends();
		long end = System.nanoTime();
		long duration = end - start;
		double _ops = (double) loop;
		double _duration = (double) duration;
		double _seconds = _duration / 1000 / 1000 / 1000;
		double iops = _ops / _seconds;
		System.out.println("iops " + iops);
		
		
		//close everything
		endpoint.close();
		group.close();		
	}
	
	
	public static void main(String[] args) throws Exception {
		String ipAddress = "192.168.0.1";
		int port = 1919;
		int size = 32;
		int loop = 1000;
		int recvQueueSize = 64;
		
		Option addressOption = Option.builder("a").required().desc("address of the server").hasArg().build();
		Option portOption = Option.builder("p").desc("server port").hasArg().build();
		Option sizeOption = Option.builder("s").desc("size of the data to exchange").hasArg().build();
		Option loopOption = Option.builder("k").desc("number of iterations").hasArg().build();
		Option queueOption = Option.builder("q").desc("queue depth").hasArg().build();
		Options options = new Options();
		options.addOption(addressOption);
		options.addOption(portOption);
		options.addOption(sizeOption);
		options.addOption(loopOption);
		options.addOption(queueOption);
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
			if (line.hasOption(queueOption.getOpt())) {
				recvQueueSize = Integer.parseInt(line.getOptionValue(queueOption.getOpt()));
			}				
		} catch (ParseException e) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("SendRecvClient", options);
			System.exit(-1);
		}
		
		SendRecvClient server = new SendRecvClient(ipAddress, port, size, loop, recvQueueSize);
		server.run();
	}
	
	public static class SendRecvEndpoint extends RdmaEndpoint {
		private int bufferSize;
		private int pipelineLength;
		private ByteBuffer[] recvBufs;
		private ByteBuffer[] sendBufs;
		private IbvMr[] recvMRs;
		private IbvMr[] sendMRs;
		private SVCPostRecv[] recvCall;
		private SVCPostSend[] sendCall;
		private IbvWC[] wcList;
		private SVCPollCq poll;	
		private int recvIndex;
		private int sendIndex;
		private int sendBudget;
		private int recvBudget;	
		private int sendPending;
		private int recvPending;
		
		protected SendRecvEndpoint(RdmaPassiveEndpointGroup<? extends RdmaEndpoint> group, RdmaCmId idPriv, boolean serverSide, int bufferSize, int recvQueueSize) throws IOException {
			super(group, idPriv, serverSide);
			this.bufferSize = bufferSize;
			this.pipelineLength = recvQueueSize;
			this.recvBufs = new ByteBuffer[pipelineLength];
			this.sendBufs = new ByteBuffer[pipelineLength];
			this.recvCall = new SVCPostRecv[pipelineLength];
			this.sendCall = new SVCPostSend[pipelineLength];
			this.recvMRs = new IbvMr[pipelineLength];
			this.sendMRs = new IbvMr[pipelineLength];
			this.recvIndex = 0;
			this.sendIndex = 0;
			this.sendBudget = pipelineLength;
			this.recvBudget = pipelineLength;
			this.sendPending = 0;
			this.recvPending = 0;
		}

		public int sendRequest(int res) throws IOException {
			int ready = Math.min(res, sendBudget);
			for (int i = 0; i < ready; i++){
				int index = sendIndex % pipelineLength;
				sendIndex++;
//				System.out.println("sending request, wrid " + sendCall[index].getWrMod(0).getWr_id());
				sendCall[index].execute();
				sendBudget--;
				sendPending++;
			}
			return ready;
		}

		public int installRecvs(int res) throws IOException {
			int ready = Math.min(res, recvBudget);
			for (int i = 0; i < ready; i++){
				int index = recvIndex % pipelineLength;
				recvIndex++;
//				System.out.println("installing recv, wrid " + recvCall[index].getWrMod(0).getWr_id());
				recvCall[index].execute();
				recvBudget--;
				recvPending++;
			}
			return ready;
		}
		
		private int pollRecvs() throws IOException {
			if (recvPending == 0){
				return 0;
			}
			
			int recvCount = 0;
			while (recvCount == 0){
				int res = 0;
				while (res == 0) {
					res = poll.execute().getPolls();
				}
				for (int i = 0; i < res; i++){
					if (wcList[i].getOpcode() == 128){
//						System.out.println("recv from wrid " + wcList[i].getWr_id());
						recvCount++;
						recvBudget++;
						recvPending--;
					} else {
						sendBudget++;
						sendPending--;
					}
				}
			}
			return recvCount;
		}	
		
		private void awaitRecvs() throws IOException {
			while (recvPending > 0) {
				int res = poll.execute().getPolls();
				if (res > 0){
					for (int i = 0; i < res; i++){
						if (wcList[i].getOpcode() == 128){
//							System.out.println("recv from wrid " + wcList[i].getWr_id());
							recvBudget++;
							recvPending--;
						} else {
							sendBudget++;
							sendPending--;
						}					
					}
				}
			}
		}
		
		private void awaitSends() throws IOException {
			while (sendPending > 0) {
				int res = poll.execute().getPolls();
				if (res > 0){
					for (int i = 0; i < res; i++){
						if (wcList[i].getOpcode() == 128){
//							System.out.println("recv from wrid " + wcList[i].getWr_id());
							recvBudget++;
							recvPending--;
						} else {
							sendBudget++;
							sendPending--;
						}					
					}
				}
			}
		}			
		
		@Override
		protected synchronized void init() throws IOException {
			super.init();
			
			IbvCQ cq = getCqProvider().getCQ();
			this.wcList = new IbvWC[getCqProvider().getCqSize()];
			for (int i = 0; i < wcList.length; i++){
				wcList[i] = new IbvWC();
			}		
			this.poll = cq.poll(wcList, wcList.length);				
			
			for(int i = 0; i < pipelineLength; i++){
				recvBufs[i] = ByteBuffer.allocateDirect(bufferSize);
				sendBufs[i] = ByteBuffer.allocateDirect(bufferSize);
				this.recvCall[i] = setupRecvTask(recvBufs[i], i);
				this.sendCall[i] = setupSendTask(sendBufs[i], i);
			}
		}
		
		public synchronized void close() throws IOException, InterruptedException {
			super.close();
			for(int i = 0; i < pipelineLength; i++){
				deregisterMemory(recvMRs[i]);
				deregisterMemory(sendMRs[i]);
			}
		}	
		
		private SVCPostSend setupSendTask(ByteBuffer sendBuf, int wrid) throws IOException {
			ArrayList<IbvSendWR> sendWRs = new ArrayList<IbvSendWR>(1);
			LinkedList<IbvSge> sgeList = new LinkedList<IbvSge>();
			
			IbvMr mr = registerMemory(sendBuf).execute().free().getMr();
			sendMRs[wrid] = mr;
			IbvSge sge = new IbvSge();
			sge.setAddr(mr.getAddr());
			sge.setLength(mr.getLength());
			int lkey = mr.getLkey();
			sge.setLkey(lkey);
			sgeList.add(sge);
		
			IbvSendWR sendWR = new IbvSendWR();
			sendWR.setSg_list(sgeList);
			sendWR.setWr_id(wrid);
			sendWRs.add(sendWR);
			sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
			sendWR.setOpcode(IbvSendWR.IbvWrOcode.IBV_WR_SEND.ordinal());
			
			return postSend(sendWRs);
		}

		private SVCPostRecv setupRecvTask(ByteBuffer recvBuf, int wrid) throws IOException {
			ArrayList<IbvRecvWR> recvWRs = new ArrayList<IbvRecvWR>(1);
			LinkedList<IbvSge> sgeList = new LinkedList<IbvSge>();
			
			IbvMr mr = registerMemory(recvBuf).execute().free().getMr();
			recvMRs[wrid] = mr;
			IbvSge sge = new IbvSge();
			sge.setAddr(mr.getAddr());
			sge.setLength(mr.getLength());
			int lkey = mr.getLkey();
			sge.setLkey(lkey);
			sgeList.add(sge);
			
			IbvRecvWR recvWR = new IbvRecvWR();
			recvWR.setWr_id(wrid);
			recvWR.setSg_list(sgeList);
			recvWRs.add(recvWR);
		
			return postRecv(recvWRs);
		}	
	}		
}
