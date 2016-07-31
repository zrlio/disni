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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.ibm.disni.endpoints.RdmaActiveEndpointGroup;
import com.ibm.disni.endpoints.RdmaEndpointFactory;
import com.ibm.disni.examples.endpoints.read.CustomClientEndpoint;
import com.ibm.disni.util.GetOpt;
import com.ibm.disni.verbs.IbvWC;
import com.ibm.disni.verbs.RdmaCmId;
import com.ibm.disni.verbs.SVCPostSend;


public class JVerbsSimpleClient implements RdmaEndpointFactory<CustomClientEndpoint> { 
	private String ipAddress;
	RdmaActiveEndpointGroup<CustomClientEndpoint> endpointGroup;
	
	public CustomClientEndpoint createClientEndpoint(RdmaCmId idPriv) throws IOException {
		return new CustomClientEndpoint(endpointGroup, idPriv);
	}	
	
	public void run() throws Exception {
		//create a EndpointGroup. The RdmaActiveEndpointGroup contains CQ processing and delivers CQ event to the endpoint.dispatchCqEvent() method.
		endpointGroup = new RdmaActiveEndpointGroup<CustomClientEndpoint>(1000, false, 128, 4, 128);
		endpointGroup.init(this);
		//we have passed our own endpoint factory to the group, therefore new endpoints will be of type CustomClientEndpoint
		//let's create a new client endpoint
		CustomClientEndpoint endpoint = (CustomClientEndpoint) endpointGroup.createClientEndpoint();
		InetAddress localHost = InetAddress.getByName(ipAddress);
		InetSocketAddress address = new InetSocketAddress(localHost, 1919);		
		
		//connect to the server
		endpoint.connect(address, 1000);
		System.out.println("SimpleClient::client channel set up ");
		
		//in our custom endpoints we have prepared (memory registration and work request creation) some memory 
		//buffers beforehand.
		//let's send one of those buffers out using a send operation
		ByteBuffer sendBuf = endpoint.getSendBuf();
		sendBuf.asCharBuffer().put("Hello from the client");
		sendBuf.clear();			
		SVCPostSend postSend = endpoint.postSend(endpoint.getWrList_send());
		postSend.getWrMod(0).setWr_id(4444);
		postSend.execute().free();
		//in our custom endpoints we make sure CQ events get stored in a queue, we now query that queue for new CQ events.
		//in this case a new CQ event means we have sent data, i.e., the message has been sent to the server
		IbvWC wc = endpoint.getWcEvents().take();
		System.out.println("SimpleClient::message sent, wr_id " + wc.getWr_id());
		//in this case a new CQ event means we have received data
		endpoint.getWcEvents().take();
		System.out.println("SimpleClient::message received");
		
		//the response should be received in this buffer, let's print it
		ByteBuffer recvBuf = endpoint.getRecvBuf();
		recvBuf.clear();
		System.out.println("Message from the server: " + recvBuf.asCharBuffer().toString());		
		
		//close everything
		endpoint.close();
		System.out.println("endpoint closed");
		endpointGroup.close();
		System.out.println("group closed");
//		System.exit(0);
	}
	
	public void launch(String[] args) throws Exception {
		String[] _args = args;
		if (args.length < 1) {
			System.exit(0);
		} else if (args[0].equals(JVerbsSimpleClient.class.getCanonicalName())) {
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
		JVerbsSimpleClient simpleClient = new JVerbsSimpleClient();
		simpleClient.launch(args);		
	}		
	
}

