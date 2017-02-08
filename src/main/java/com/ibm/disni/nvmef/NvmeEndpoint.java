/*
 * DiSNI: Direct Storage and Networking Interface
 *
 * Author: Jonas Pfefferle <jpf@zurich.ibm.com>
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

package com.ibm.disni.nvmef;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;
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
	
	//rdma://<host>:<port>
	//nvmef:://<host>:<port>/controller/namespace"
	public void connect(URI url) throws IOException {
		if (!url.getScheme().equalsIgnoreCase("nvmef")){
			throw new IOException("URL has wrong protocol " + url.getScheme());
		}
		
		String address = url.getHost();
		String port = Integer.toString(url.getPort());
		int controller = 0;
		int namespace = 1;
		
		String path = url.getPath();
		if (path != null){
			StringTokenizer tokenizer = new StringTokenizer(path, "/");
			if (tokenizer.countTokens() > 2){
				throw new IOException("URL format error, too many elements in path");
			}
			for (int i = 0; tokenizer.hasMoreTokens(); i++){
				switch(i) {
				case 0:
					controller = Integer.parseInt(tokenizer.nextToken());
					break;
				case 1:
					namespace = Integer.parseInt(tokenizer.nextToken());
					break;
				}
			}
			
//			String tokens[] = new String[tokenizer.countTokens()];
//			int i = 0;
//			while(tokenizer.hasMoreTokens()){
//				tokens[i] = tokenizer.nextToken();
//				System.out.println("parsing token " + tokens[i]);
//				i++;
//			}
//			if (tokens.length > 0){
//				controller = Integer.parseInt(tokens[0]);
//			}
//			if (tokens.length > 1){
//				namespace = Integer.parseInt(tokens[1]);
//			}
		}
		
		System.out.println("connecting to address " + address);
		NvmeController nvmecontroller = group.probe(address, port, controller);
		this.namespace = nvmecontroller.getNamespace(namespace);
		this.queuePair = nvmecontroller.allocQueuePair();		
	}	
	
	public IOCompletion write(ByteBuffer buffer, long linearBlockAddress) throws IOException{
		if (buffer.remaining() % namespace.getSectorSize() != 0){
			throw new IOException("buffer must a multiple of sector size");
		}
		
		int sectorCount = buffer.remaining() / namespace.getSectorSize();
		IOCompletion completion = namespace.write(queuePair, ((DirectBuffer) buffer).address(), linearBlockAddress, sectorCount);
		return completion;
	}
	
	public IOCompletion read(ByteBuffer buffer, long linearBlockAddress) throws IOException{
		if (buffer.remaining() % namespace.getSectorSize() != 0){
			throw new IOException("buffer must a multiple of sector size");
		}		
		
		int sectorCount = buffer.remaining() / namespace.getSectorSize();
		IOCompletion completion = namespace.read(queuePair, ((DirectBuffer) buffer).address(), linearBlockAddress, sectorCount);
		return completion;
	}	
	
	public int processCompletions(int length) throws IOException {
		return queuePair.processCompletions(length);
	}
	
	public int getSectorSize() { 
		return namespace.getSectorSize();
	}

	public long getNamespaceSize() {
		return namespace.getSize();
	}
	
	public int getMaxTransferSize() {
		return namespace.getMaxIOTransferSize();
	}	
}
