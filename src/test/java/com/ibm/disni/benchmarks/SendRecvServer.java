package com.ibm.disni.benchmarks;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;

import com.ibm.disni.rdma.RdmaEndpoint;
import com.ibm.disni.rdma.RdmaEndpointFactory;
import com.ibm.disni.rdma.RdmaPassiveEndpointGroup;
import com.ibm.disni.rdma.RdmaServerEndpoint;
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
import com.ibm.disni.util.GetOpt;

public class SendRecvServer implements RdmaEndpointFactory<SendRecvServer.SendRecvEndpoint> {
	private RdmaPassiveEndpointGroup<SendRecvEndpoint> group;
	private String host;
	private int size;
	private int loop;
	private int recvQueueSize;
	private int port;
	
	public SendRecvServer(String host, int size, int loop, int recvQueueSize, int port) throws IOException{
		this.group = new RdmaPassiveEndpointGroup<SendRecvServer.SendRecvEndpoint>(1, recvQueueSize, 1, recvQueueSize*4);
		this.group.init(this);
		this.host = host;
		this.size = size;
		this.loop = loop;
		this.recvQueueSize = recvQueueSize;
		this.port = port;
	}

	public SendRecvServer.SendRecvEndpoint createEndpoint(RdmaCmId id, boolean serverSide)
			throws IOException {
		return new SendRecvEndpoint(group, id, serverSide, size, recvQueueSize);
	}
	
	
	private void run() throws IOException, InterruptedException {
		System.out.println("SendRecvServer, size " + size + ", loop " + loop + ", recvQueueSize " + recvQueueSize + ", port " + port);
		
		RdmaServerEndpoint<SendRecvServer.SendRecvEndpoint> serverEndpoint = group.createServerEndpoint();
		InetAddress ipAddress = InetAddress.getByName(host);
		InetSocketAddress address = new InetSocketAddress(ipAddress, port);
		serverEndpoint.bind(address, 10);
		SendRecvServer.SendRecvEndpoint endpoint = serverEndpoint.accept();
		InetSocketAddress _addr = (InetSocketAddress) endpoint.getDstAddr();
		System.out.println("SendRecvServer, client connected, address " + _addr.toString());	
		
		int opCount = 0;
		while (opCount < loop){
			endpoint.pollRecvs();
			int remaining = loop - opCount;
			int actualRecvs = endpoint.installRecvs(remaining);
//			System.out.println("actualRecvs = " + actualRecvs);
			int actualSends = endpoint.replySends(actualRecvs);
//			System.out.println("actualSends = " + actualRecvs);
			opCount += actualSends;
//			System.out.println("opCount " + opCount);
		}
		
		endpoint.awaitRecvs();
		endpoint.awaitSends();
		
		//close everything
		endpoint.close();
		serverEndpoint.close();
		group.close();		
	}
	
	
	public static void main(String[] args) throws IOException, InterruptedException{
		String[] _args = args;
		if (args.length < 1) {
			System.exit(0);
		} else if (args[0].equals(ReadServer.class.getCanonicalName())) {
			_args = new String[args.length - 1];
			for (int i = 0; i < _args.length; i++) {
				_args[i] = args[i + 1];
			}
		}
		
		GetOpt go = new GetOpt(_args, "a:s:k:r:p:");
		go.optErr = true;
		int ch = -1;
		
		String ipAddress = "192.168.0.1";
		int size = 32;
		int loop = 1000;
		int recvQueueSize = 64;
		int port = 1919;
		while ((ch = go.getopt()) != GetOpt.optEOF) {
			if ((char) ch == 'a') {
				ipAddress = go.optArgGet();
			} else if ((char) ch == 's') {
				size = Integer.parseInt(go.optArgGet());
			} else if ((char) ch == 'k') {
				loop = Integer.parseInt(go.optArgGet());
			} else if ((char) ch == 'r') {
				recvQueueSize = Integer.parseInt(go.optArgGet());
			} else if ((char) ch == 'p') {
				port = Integer.parseInt(go.optArgGet());
			} 
		}		
		
		SendRecvServer server = new SendRecvServer(ipAddress, size, loop, recvQueueSize, port);
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

		public int replySends(int res) throws IOException {
			int ready = Math.min(res, sendBudget);
			for (int i = 0; i < ready; i++){
				int index = sendIndex % pipelineLength;
				sendIndex++;
//				System.out.println("sending response, wrid " + sendCall[index].getWrMod(0).getWr_id());
				sendCall[index].execute();
				this.sendBudget--;
				this.sendPending++;
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
			
			this.installRecvs(pipelineLength);
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
