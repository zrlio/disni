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

import com.ibm.disni.rdma.RdmaActiveEndpointGroup;
import com.ibm.disni.rdma.RdmaEndpointFactory;
import com.ibm.disni.rdma.RdmaServerEndpoint;
import com.ibm.disni.rdma.verbs.IbvMr;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.util.GetOpt;

public class JVerbsReadServer implements RdmaEndpointFactory<CustomServerEndpoint> {
	private String ipAddress;
	private RdmaActiveEndpointGroup<CustomServerEndpoint> endpointGroup;
	
	public CustomServerEndpoint createEndpoint(RdmaCmId idPriv, boolean serverSide) throws IOException {
		return new CustomServerEndpoint(endpointGroup, idPriv, serverSide);
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
		System.out.println("ReadServer::server bound to address");
		
		//we can accept new connections
		CustomServerEndpoint endpoint = serverEndpoint.accept();
		System.out.println("ReadServer::connection accepted ");
		
		//let's prepare a message to be sent to the client
		//in the message we include the RDMA information of a local buffer which we allow the client to read using a one-sided RDMA operation
		ByteBuffer dataBuf = endpoint.getDataBuf();
		ByteBuffer sendBuf = endpoint.getSendBuf();
		IbvMr dataMr = endpoint.getDataMr();
		dataBuf.asCharBuffer().put("This is a RDMA/read on stag " + dataMr.getLkey() + " !");
		dataBuf.clear();		
		sendBuf.putLong(dataMr.getAddr());
		sendBuf.putInt(dataMr.getLength());
		sendBuf.putInt(dataMr.getLkey());
		sendBuf.clear();	
		
		//post the operation to send the message
		System.out.println("ReadServer::sending message");
		endpoint.postSend(endpoint.getWrList_send()).execute().free();
		//we have to wait for the CQ event, only then we know the message has been sent out
		endpoint.getWcEvents().take();
		
		//let's wait for the final message to be received. We don't need to check the message itself, just the CQ event is enough.
		endpoint.getWcEvents().take();
		System.out.println("ReadServer::final message");
		
		//close everything
		endpoint.close();
		serverEndpoint.close();
		endpointGroup.close();
	}	
	
	public void init(){
		
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
		JVerbsReadServer simpleServer = new JVerbsReadServer();
		simpleServer.launch(args);		
	}	
}

