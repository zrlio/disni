package com.ibm.disni.nvmef;

import java.io.IOException;

import com.ibm.disni.nvmef.spdk.IOCompletion;
import com.ibm.disni.nvmef.spdk.NvmeNamespace;
import com.ibm.disni.nvmef.spdk.NvmeQueuePair;

public class NvmeOperation {
	private NvmeNamespace namespace;
	private NvmeQueuePair queuePair;
	private long bufferAddress;
	private long linearBlockAddress;
	private int sectorCount;
	private IOCompletion completion;
	private boolean isWrite;
	
	public NvmeOperation(NvmeNamespace namespace, NvmeQueuePair queuePair, long bufferAddress, long linearBlockAddress,
			int sectorCount, IOCompletion completion, boolean isWrite) {
		this.namespace = namespace;
		this.queuePair = queuePair;
		this.bufferAddress = bufferAddress;
		this.linearBlockAddress = linearBlockAddress;
		this.sectorCount = sectorCount;
		this.completion = completion;
		this.isWrite = isWrite;
	}
	
	public void execute() throws IOException {
		if (completion.isPending()){
			throw new IOException("Operation is already pending");
		}
		
		if (isWrite){
			namespace.write(queuePair, bufferAddress, linearBlockAddress, sectorCount, completion);
		} else {
			namespace.read(queuePair, bufferAddress, linearBlockAddress, sectorCount, completion);
		}
	}
	
	public boolean isDone(){
		return completion.done();
	}
	
	public void free(){
	}

	public void setLba(long lba) {
		this.linearBlockAddress = lba;
	}	
}
