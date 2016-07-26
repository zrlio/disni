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

package com.ibm.disni.examples.endpoints.read;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.ibm.disni.rdma.endpoints.RdmaActiveEndpointGroup;
import com.ibm.disni.rdma.endpoints.RdmaEndpointFactory;
import com.ibm.disni.rdma.verbs.IbvSendWR;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.rdma.verbs.SVCPostSend;
import com.ibm.disni.util.GetOpt;

public class JVerbsReadClient implements RdmaEndpointFactory<CustomClientEndpoint> {
	private RdmaActiveEndpointGroup<CustomClientEndpoint> endpointGroup;
	private String ipAddress; 
	
	public CustomClientEndpoint createClientEndpoint(RdmaCmId idPriv) throws IOException {
		return new CustomClientEndpoint(endpointGroup, idPriv);
	}	
	
	public void run() throws Exception {
		//create a EndpointGroup. The RdmaActiveEndpointGroup contains CQ processing and delivers CQ event to the endpoint.dispatchCqEvent() method.
		endpointGroup = new RdmaActiveEndpointGroup<CustomClientEndpoint>(1000, false, 128, 4, 128);
		endpointGroup.init(this);
		//we have passed our own endpoint factory to the group, therefore new endpoints will be of type CustomClientEndpoint
		//let's create a new client endpoint		
		CustomClientEndpoint endpoint = endpointGroup.createClientEndpoint();
		InetAddress localHost = InetAddress.getByName(ipAddress);
		InetSocketAddress address = new InetSocketAddress(localHost, 1919);
		
		//connect to the server
		endpoint.connect(address, 1000);
		InetSocketAddress _addr = (InetSocketAddress) endpoint.getDstAddr();
		System.out.println("ReadClient::client connected, address " + _addr.toString());
		
		//in our custom endpoints we make sure CQ events get stored in a queue, we now query that queue for new CQ events.
		//in this case a new CQ event means we have received some data, i.e., a message from the server
		endpoint.getWcEvents().take();
		ByteBuffer recvBuf = endpoint.getRecvBuf();
		//the message has been received in this buffer
		//it contains some RDMA information sent by the server
		recvBuf.clear();
		long addr = recvBuf.getLong();
		int length = recvBuf.getInt();
		int lkey = recvBuf.getInt();
		recvBuf.clear();		
		System.out.println("ReadClient::receiving rdma information, addr " + addr + ", length " + length + ", key " + lkey);
		System.out.println("ReadClient::preparing read operation...");		

		//the RDMA information above identifies a RDMA buffer at the server side
		//let's issue a one-sided RDMA read opeation to fetch the content from that buffer
		IbvSendWR sendWR = endpoint.getSendWR();
		sendWR.setWr_id(1001);
		sendWR.setOpcode(IbvSendWR.IBV_WR_RDMA_READ);
		sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
		sendWR.getRdma().setRemote_addr(addr);
		sendWR.getRdma().setRkey(lkey);		
		
		//post the operation on the endpoint
		SVCPostSend postSend = endpoint.postSend(endpoint.getWrList_send());
		for (int i = 10; i <= 100; ){
			postSend.getWrMod(0).getSgeMod(0).setLength(i);
			postSend.execute();
			//wait until the operation has completed
			endpoint.getWcEvents().take();
			
			//we should have the content of the remote buffer in our own local buffer now
			ByteBuffer dataBuf = endpoint.getDataBuf();
			dataBuf.clear();
			System.out.println("ReadClient::read memory from server: " + dataBuf.asCharBuffer().toString());		
			i += 10;
		}
		
		//let's prepare a final message to signal everything went fine
		sendWR.setWr_id(1002);
		sendWR.setOpcode(IbvSendWR.IBV_WR_SEND);
		sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
		sendWR.getRdma().setRemote_addr(addr);
		sendWR.getRdma().setRkey(lkey);
		
		//post that operation
		endpoint.postSend(endpoint.getWrList_send()).execute().free();
		
		//close everything
		System.out.println("closing endpoint");
		endpoint.close();
		System.out.println("closing endpoint, done");
		endpointGroup.close();
	}
	
	public void launch(String[] args) throws Exception {
		String[] _args = args;
		if (args.length < 1) {
			System.exit(0);
		} else if (args[0].equals(JVerbsReadServer.class.getCanonicalName())) {
			_args = new String[args.length - 1];
			for (int i = 0; i < _args.length; i++) {
				_args[i] = args[i + 1];
			}
		}
		
		GetOpt go = new GetOpt(_args, "a:");
		go.optErr = true;
		int ch = -1;
		
		while ((ch = go.getopt()) != GetOpt.optEOF) {
			if ((char) ch == 'a') {
				ipAddress = go.optArgGet();
			} 
		}	
		
		this.run();
	}
	
	public static void main(String[] args) throws Exception { 
		JVerbsReadClient simpleClient = new JVerbsReadClient();
		simpleClient.launch(args);		
	}
}

