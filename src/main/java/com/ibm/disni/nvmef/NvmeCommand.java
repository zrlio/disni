package com.ibm.disni.nvmef;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.ibm.disni.nvmef.spdk.IOCompletion;
import com.ibm.disni.nvmef.spdk.NvmeNamespace;
import com.ibm.disni.nvmef.spdk.NvmeQueuePair;
import sun.nio.ch.DirectBuffer;

public class NvmeCommand {
	private final NvmeEndpoint endpoint;
	private final NvmeNamespace namespace;
	private final NvmeQueuePair queuePair;
	private ByteBuffer buffer;
	private long bufferAddress;
	private long linearBlockAddress;
	private int sectorCount;
	private final IOCompletion completion;
	private boolean isWrite;
	
	NvmeCommand(NvmeEndpoint endpoint, ByteBuffer buffer, long linearBlockAddress,
				IOCompletion completion, boolean isWrite) {
		this(endpoint, completion);
		this.linearBlockAddress = linearBlockAddress;
		this.isWrite = isWrite;
		setBuffer(buffer);
	}

	NvmeCommand(NvmeEndpoint endpoint, IOCompletion completion) {
		this.endpoint = endpoint;
		this.namespace = endpoint.getNamespace();
		this.queuePair = endpoint.getQueuePair();
		this.completion = completion;
	}
	
	public void execute() throws IOException {
		if (bufferAddress == 0) {
			throw new IllegalArgumentException("Buffer not set");
		}

		synchronized (endpoint) {
			if (!endpoint.isOpen()) {
				throw new IOException("Endpoint not open!");
			}
			if (isWrite) {
				namespace.write(queuePair, bufferAddress, linearBlockAddress, sectorCount, completion);
			} else {
				namespace.read(queuePair, bufferAddress, linearBlockAddress, sectorCount, completion);
			}
		}
	}
	
	public boolean isDone(){
		return completion.done();
	}

	public boolean isPending() {  return completion.isPending(); }
	
	public void free() {
		completion.free();
	}

	public NvmeCommand setLinearBlockAddress(long linearBlockAddress) {
		this.linearBlockAddress = linearBlockAddress;
		return this;
	}

	public NvmeCommand setBuffer(ByteBuffer buffer) {
		this.sectorCount = buffer.remaining() / namespace.getSectorSize();
		this.bufferAddress = ((DirectBuffer) buffer).address() + buffer.position();
		this.buffer = buffer;
		return this;
	}

	public NvmeCommand read() {
		this.isWrite = false;
		return this;
	}

	public NvmeCommand write() {
		this.isWrite = true;
		return this;
	}

	public NvmeCommand setId(long id) {
		completion.setId(id);
		return this;
	}

	public long getId() {
		return completion.getId();
	}

	public IOCompletion getCompletion() {
		return completion;
	}
}
