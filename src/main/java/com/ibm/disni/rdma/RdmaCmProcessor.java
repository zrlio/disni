/*
 * DiSNI: Direct Storage and Networking Interface
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

package com.ibm.disni.rdma;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import com.ibm.disni.rdma.verbs.RdmaCm;
import com.ibm.disni.rdma.verbs.RdmaCmEvent;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.rdma.verbs.RdmaEventChannel;
import com.ibm.disni.util.DiSNILogger;

/**
 * Responsible for processing communication events. 
 */
public class RdmaCmProcessor implements Runnable {
	private static final Logger logger = DiSNILogger.getLogger();
	
	private RdmaEventChannel cmChannel;
	private RdmaEndpointGroup<? extends RdmaEndpoint> cmConsumer;
	private Thread thread;
	private AtomicBoolean closed;
	private int timeout;
	
	public RdmaCmProcessor(RdmaEndpointGroup<? extends RdmaEndpoint> cmConsumer, int timeout) throws IOException {
		this.cmChannel = RdmaEventChannel.createEventChannel();
		if (cmChannel == null){
			throw new IOException("No RDMA device configured!");
		}
		this.cmConsumer = cmConsumer;		
		this.thread = new Thread(this);
		closed = new AtomicBoolean(true);
		this.timeout = timeout;
	}
	
	public synchronized void start(){
		closed.set(false);
		thread.start();
	}	

	public void run() {
		logger.info("launching cm processor, cmChannel " + cmChannel.getFd());
		RdmaCmEvent cmEvent;
		while (!closed.get()) {
			try {
				cmEvent = cmChannel.getCmEvent(timeout);
				if (cmEvent != null){
					cmConsumer.dispatchCmEvent(cmEvent);
					cmEvent.ackEvent();
				}
			} catch(Throwable e){
				if (cmConsumer.isClosed()){
					logger.info("cm looping closes, group is shutdown!!");
					break;
				} else {
					logger.info("cm processing, caught exception but keep going " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
		logger.info("terminating cm polling, closed " + closed);
	}

	/**
	 * Close the event loop.
	 *
	 * @throws Exception the exception
	 */
	public synchronized void close() throws IOException, InterruptedException {
		logger.info("shutting down cm processor");
		if (closed.get()){
			return;
		}
		
		closed.set(true);
		thread.join();
		logger.info("cm processor down");
		cmChannel.destroyEventChannel();
		logger.info("cm channel down");
	}

	RdmaCmId createId(short rdmaPsTcp) throws IOException {
		RdmaCmId idPriv = cmChannel.createId(RdmaCm.RDMA_PS_TCP);
		return idPriv;
	}
}
