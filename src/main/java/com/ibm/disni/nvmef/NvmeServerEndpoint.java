package com.ibm.disni.nvmef;

import java.io.IOException;
import java.net.SocketAddress;

//this class is most likely not used
public class NvmeServerEndpoint {
	private NvmeEndpointGroup group;
	
	public NvmeServerEndpoint(NvmeEndpointGroup group){
		this.group = group;
	}
	
	public synchronized NvmeServerEndpoint bind(SocketAddress src, int backlog) throws IOException {
		return this;
	}
	
	public NvmeEndpoint accept() throws IOException {
		return null;
	}
	
	public synchronized void close() throws IOException, InterruptedException {
		
	}
}
