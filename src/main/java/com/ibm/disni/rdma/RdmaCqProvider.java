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

import org.slf4j.Logger;

import com.ibm.disni.rdma.verbs.IbvCQ;
import com.ibm.disni.rdma.verbs.IbvCompChannel;
import com.ibm.disni.rdma.verbs.IbvContext;
import com.ibm.disni.util.DiSNILogger;

/**
 * Responsible for creating a completion queue. Used as a base class in RdmaCqClient and RdmaCqProcessor.
 */
public class RdmaCqProvider {
	private static final Logger logger = DiSNILogger.getLogger();
	
	protected IbvContext context;
	protected IbvCompChannel compChannel;	
	protected IbvCQ cq;	
	protected int cqSize;
	
	public RdmaCqProvider(IbvContext context, int cqSize) throws IOException {
		logger.info("new endpoint CQ processor");
		this.context = context;
		this.compChannel = context.createCompChannel();
		this.cqSize = cqSize;
		this.cq = context.createCQ(compChannel, cqSize, 0);
	}
	
	public void close() throws IOException, InterruptedException {
		logger.info("shutting cq provider, destroying cq1");
		compChannel.destroyCompChannel();
		logger.info("compChannel destroyed");
		cq.destroyCQ();
		logger.info("cq destroyed");
	}	
	
	public IbvCQ getCQ() {
		return cq;
	}

	public int getCqSize() {
		return cqSize;
	}
}
