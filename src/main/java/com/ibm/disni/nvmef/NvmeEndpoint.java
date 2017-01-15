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
    private AtomicLong issuedOperations;
    private AtomicLong completedOperations;
	
	public NvmeEndpoint(NvmeEndpointGroup group){
		this.group = group;
		this.queuePair = null;
		this.namespace = null;
		this.issuedOperations = new AtomicLong(0);
		this.completedOperations = new AtomicLong(0);
	}
	
	public synchronized void connect(SocketAddress dst, int timeout) throws IOException {
		InetSocketAddress inetAddress = (InetSocketAddress) dst;
		String host = inetAddress.getAddress().getHostAddress();
		int port = inetAddress.getPort();
		NvmeController controller = group.probe(host, 0);
		this.namespace = controller.getNamespace(1);
		this.queuePair = controller.allocQueuePair();
	}
	
	public IOCompletion write(ByteBuffer buffer) throws IOException{
		IOCompletion completion = namespace.write(queuePair, ((DirectBuffer) buffer).address(), 0, 1);
		long id = issuedOperations.getAndIncrement();
		//completion.attachId(id);
		return completion;
	}
	
	public IOCompletion read(ByteBuffer buffer) throws IOException{
		IOCompletion completion = namespace.read(queuePair, ((DirectBuffer) buffer).address(), 0, 1);
		long id = issuedOperations.getAndIncrement();
		//completion.attachId(id);
		return completion;
	}	
	
	public boolean poll(IOCompletion completion) throws IOException{
		int ret = queuePair.processCompletions(10);
		if (ret > 0){
			completedOperations.incrementAndGet();
		}
		/*
		 * if (completedId.get() >= completion.id()){
		 * return true;
		 * } else {
		 * return false;
		 * }
		 */
		return true;
	}
	
	public synchronized void close() throws IOException, InterruptedException {
		
	}
}
