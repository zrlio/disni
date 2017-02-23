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
import java.util.HashMap;

import org.slf4j.Logger;

import com.ibm.disni.rdma.verbs.IbvContext;
import com.ibm.disni.rdma.verbs.IbvPd;
import com.ibm.disni.util.DiSNILogger;

public class RdmaEndpointProvider {
	private static final Logger logger = DiSNILogger.getLogger();
	
	private static RdmaEndpointProvider provider = null;

	protected HashMap<Integer, IbvPd> pdMap;
	
	private RdmaEndpointProvider() throws IOException{
		this.pdMap = new HashMap<Integer, IbvPd>();
	}
	
	public static synchronized RdmaEndpointProvider getEndpointProvider() throws IOException{
		if (provider == null){
			provider = new RdmaEndpointProvider();
		}
		return provider;
	}
	
	public synchronized IbvPd createProtectionDomain(RdmaEndpoint endpoint) throws IOException {
		IbvContext context = endpoint.getIdPriv().getVerbs();
		return createProtectionDomain(context);
	}
	
	public synchronized IbvPd createProtectionDomain(RdmaServerEndpoint<?> endpoint) throws IOException {
		IbvContext context = endpoint.getIdPriv().getVerbs();
		return createProtectionDomain(context);
	}
	
	public synchronized IbvPd createProtectionDomain(IbvContext context) throws IOException {
		if (context != null) {
			IbvPd pd = null;
			int key = context.getCmd_fd();
			if (!pdMap.containsKey(key)) {
				pd = context.allocPd();
				pdMap.put(context.getCmd_fd(), pd);
			}
			pd = pdMap.get(context.getCmd_fd());
			logger.info("setting up protection domain, context " + context.getCmd_fd() + ", pd " + pd.getHandle());
			return pd;
		} else {
			throw new IOException("setting up protection domain, no context found");
		}			
	}
}
