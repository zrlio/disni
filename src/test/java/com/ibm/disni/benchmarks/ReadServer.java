package com.ibm.disni.benchmarks;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;

import com.ibm.disni.examples.endpoints.read.JVerbsReadServer;
import com.ibm.disni.rdma.RdmaActiveEndpoint;
import com.ibm.disni.rdma.RdmaActiveEndpointGroup;
import com.ibm.disni.rdma.RdmaEndpoint;
import com.ibm.disni.rdma.RdmaEndpointFactory;
import com.ibm.disni.rdma.RdmaServerEndpoint;
import com.ibm.disni.rdma.verbs.IbvMr;
import com.ibm.disni.rdma.verbs.IbvRecvWR;
import com.ibm.disni.rdma.verbs.IbvSendWR;
import com.ibm.disni.rdma.verbs.IbvSge;
import com.ibm.disni.rdma.verbs.IbvWC;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.util.GetOpt;

public class ReadServer implements RdmaEndpointFactory<ReadServer.ReadServerEndpoint> {
	private RdmaActiveEndpointGroup<ReadServerEndpoint> group;
	private String host;
	private int size;
	private int loop;
	
	public ReadServer(String host, int size, int loop) throws IOException{
		this.group = new RdmaActiveEndpointGroup<ReadServer.ReadServerEndpoint>(1, false, 128, 4, 128);
		this.group.init(this);
		this.host = host;
		this.size = size;
		this.loop = loop;
	}

	public ReadServer.ReadServerEndpoint createEndpoint(RdmaCmId id, boolean serverSide)
			throws IOException {
		return new ReadServerEndpoint(group, id, serverSide, size);
	}
	
	
	private void run() throws IOException, InterruptedException {
		System.out.println("ReadServer, size " + size + ", loop " + loop);
		
		RdmaServerEndpoint<ReadServer.ReadServerEndpoint> serverEndpoint = group.createServerEndpoint();
		InetAddress ipAddress = InetAddress.getByName(host);
		InetSocketAddress address = new InetSocketAddress(ipAddress, 1919);
		serverEndpoint.bind(address, 10);
		ReadServer.ReadServerEndpoint endpoint = serverEndpoint.accept();
		InetSocketAddress _addr = (InetSocketAddress) endpoint.getDstAddr();
		System.out.println("ReadServer, client connected, address " + _addr.toString());	
		
		//let's send a message to the client
		//in the message we include the RDMA information of a local buffer which we allow the client to read using a one-sided RDMA operation
		System.out.println("ReadServer, sending message");
		endpoint.sendMessage();
		//we have to wait for the CQ event, only then we know the message has been sent out
		endpoint.takeEvent();
		
		//let's wait for the final message to be received. We don't need to check the message itself, just the CQ event is enough.
		endpoint.takeEvent();
		System.out.println("ReadServer, final message");
		
		//close everything
		endpoint.close();
		serverEndpoint.close();
		group.close();		
	}
	
	
	public static void main(String[] args) throws IOException, InterruptedException{
		String[] _args = args;
		if (args.length < 1) {
			System.exit(0);
		} else if (args[0].equals(JVerbsReadServer.class.getCanonicalName())) {
			_args = new String[args.length - 1];
			for (int i = 0; i < _args.length; i++) {
				_args[i] = args[i + 1];
			}
		}
		
		GetOpt go = new GetOpt(_args, "a:s:k:");
		go.optErr = true;
		int ch = -1;
		
		String ipAddress = "192.168.0.1";
		int size = 32;
		int loop = 1000;
		while ((ch = go.getopt()) != GetOpt.optEOF) {
			if ((char) ch == 'a') {
				ipAddress = go.optArgGet();
			} else if ((char) ch == 's') {
				size = Integer.parseInt(go.optArgGet());
			} else if ((char) ch == 'k') {
				loop = Integer.parseInt(go.optArgGet());
			} 
		}		
		
		ReadServer server = new ReadServer(ipAddress, size, loop);
		server.run();
	}
	
	public static class ReadServerEndpoint extends RdmaActiveEndpoint {
		private ArrayBlockingQueue<IbvWC> wcEvents;
		
		private ByteBuffer buffers[];
		private IbvMr mrlist[];
		private int buffersize;
		
		private IbvMr dataMr;
		private IbvMr sendMr;
		private IbvMr recvMr;	
		
		private LinkedList<IbvSendWR> wrList_send;
		private IbvSge sgeSend;
		private LinkedList<IbvSge> sgeList;
		private IbvSendWR sendWR;
		
		private LinkedList<IbvRecvWR> wrList_recv;
		private IbvSge sgeRecv;
		private LinkedList<IbvSge> sgeListRecv;
		private IbvRecvWR recvWR;	
		
		
		protected ReadServerEndpoint(RdmaActiveEndpointGroup<? extends RdmaEndpoint> group, RdmaCmId idPriv, boolean serverSide, int size) throws IOException {
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
			
			this.wcEvents = new ArrayBlockingQueue<IbvWC>(10);
		}

		public void sendMessage() throws IOException {
			this.postSend(wrList_send).execute().free();
		}

		@Override
		protected synchronized void init() throws IOException {
			super.init();
			for (int i = 0; i < 3; i++){
				mrlist[i] = registerMemory(buffers[i]).execute().free().getMr();
			}
			
			this.dataMr = mrlist[0];
			this.sendMr = mrlist[1];
			this.recvMr = mrlist[2];
			
			ByteBuffer sendBuf = buffers[1];
			sendBuf.putLong(dataMr.getAddr());
			sendBuf.putInt(dataMr.getLength());
			sendBuf.putInt(dataMr.getLkey());
			sendBuf.clear();				
			
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
			
			this.postRecv(wrList_recv).execute();
		}
		
		public void dispatchCqEvent(IbvWC wc) throws IOException {
			wcEvents.add(wc);
		}		
		
		public IbvWC takeEvent() throws InterruptedException{
			return wcEvents.take();
		}
		
	}		
}
