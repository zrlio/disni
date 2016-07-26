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

import com.ibm.disni.examples.endpoints.read.CustomServerEndpoint;
import com.ibm.disni.rdma.endpoints.RdmaActiveEndpointGroup;
import com.ibm.disni.rdma.endpoints.RdmaEndpointFactory;
import com.ibm.disni.rdma.endpoints.RdmaServerEndpoint;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.util.GetOpt;

public class JVerbsSimpleServer implements RdmaEndpointFactory<CustomServerEndpoint> {
	private String ipAddress;
	RdmaActiveEndpointGroup<CustomServerEndpoint> endpointGroup;
	
	public CustomServerEndpoint createClientEndpoint(RdmaCmId idPriv) throws IOException {
		return new CustomServerEndpoint(endpointGroup, idPriv);
	}	
	
	public void run() throws Exception {
		//create a EndpointGroup. The RdmaActiveEndpointGroup contains CQ processing and delivers CQ event to the endpoint.dispatchCqEvent() method.
		endpointGroup = new RdmaActiveEndpointGroup<CustomServerEndpoint>(1000, false, 128, 4, 128);
		endpointGroup.init(this);
		//create a server endpoint
		RdmaServerEndpoint<CustomServerEndpoint> serverEndpoint = endpointGroup.createServerEndpoint();		
		InetAddress localHost = InetAddress.getByName(ipAddress);
		InetSocketAddress address = new InetSocketAddress(localHost, 1919);

		//we can call bind on a server endpoint, just like we do with sockets
		serverEndpoint.bind(address, 10);
		System.out.println("SimpleServer::servers bound to address " + localHost);
		
		//we can accept new connections
		CustomServerEndpoint clientEndpoint = serverEndpoint.accept();
		//we have previously passed our own endpoint factory to the group, therefore new endpoints will be of type CustomServerEndpoint
		System.out.println("SimpleServer::client connection accepted");

		//in our custom endpoints we have prepared (memory registration and work request creation) some memory buffers beforehand. 
		ByteBuffer sendBuf = clientEndpoint.getSendBuf();
		sendBuf.asCharBuffer().put("Hello from the server");
		sendBuf.clear();		
		
		//in our custom endpoints we make sure CQ events get stored in a queue, we now query that queue for new CQ events.
		//in this case a new CQ event means we have received data, i.e., a message from the client.
		clientEndpoint.getWcEvents().take();
		System.out.println("SimpleServer::message received");
		ByteBuffer recvBuf = clientEndpoint.getRecvBuf();
		recvBuf.clear();
		System.out.println("Message from the client: " + recvBuf.asCharBuffer().toString());			
		//let's respond with a message 
		clientEndpoint.postSend(clientEndpoint.getWrList_send()).execute().free();
		//when receiving the CQ event we know the message has been sent
		clientEndpoint.getWcEvents().take();
		System.out.println("SimpleServer::message sent");
		
		//close everything
		clientEndpoint.close();
		System.out.println("client endpoint closed");
		serverEndpoint.close();
		System.out.println("server endpoint closed");
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
		JVerbsSimpleServer simpleServer = new JVerbsSimpleServer();
		simpleServer.launch(args);		
		
	
	}	
}

