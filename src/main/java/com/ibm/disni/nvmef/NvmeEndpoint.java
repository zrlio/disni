package com.ibm.disni.nvmef;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import sun.nio.ch.DirectBuffer;

import com.ibm.disni.nvmef.spdk.IOCompletion;
import com.ibm.disni.nvmef.spdk.NvmeController;
import com.ibm.disni.nvmef.spdk.NvmeNamespace;
import com.ibm.disni.nvmef.spdk.NvmeQueuePair;

public class NvmeEndpoint {
	private NvmeEndpointGroup group;
    private NvmeQueuePair queuePair;
    private NvmeNamespace namespace;
	
	public NvmeEndpoint(NvmeEndpointGroup group){
		this.group = group;
		this.queuePair = null;
		this.namespace = null;
	}
	
	public void connect(String address, String port, int controller, int namespace) throws IOException {
		NvmeController nvmecontroller = group.probe(address, port, controller);
		this.namespace = nvmecontroller.getNamespace(namespace);
		this.queuePair = nvmecontroller.allocQueuePair();		
	}	
	
	public IOCompletion write(ByteBuffer buffer, long linearBlockAddress, int count) throws IOException{
		IOCompletion completion = namespace.write(queuePair, ((DirectBuffer) buffer).address(), linearBlockAddress, count);
		return completion;
	}
	
	public IOCompletion read(ByteBuffer buffer, long linearBlockAddress, int count) throws IOException{
		IOCompletion completion = namespace.read(queuePair, ((DirectBuffer) buffer).address(), linearBlockAddress, count);
		return completion;
	}	
	
	public int processCompletions(int length) throws IOException {
		return queuePair.processCompletions(length);
	}
	
	public int getSectorSize() { 
		return namespace.getSectorSize();
	}

	public long getSize() {
		return namespace.getSize();
	}



}
