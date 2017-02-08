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

package com.ibm.disni.rdma.verbs.impl;

import java.io.IOException;

import org.slf4j.Logger;

import com.ibm.disni.rdma.verbs.RdmaCm;
import com.ibm.disni.rdma.verbs.RdmaProvider;
import com.ibm.disni.rdma.verbs.RdmaVerbs;
import com.ibm.disni.util.DiSNILogger;


public class RdmaProviderNat extends RdmaProvider {
	private static final Logger logger = DiSNILogger.getLogger();
	
	private NativeDispatcher nativeDispatcher;
	private RdmaVerbsNat verbs;
	private RdmaCmNat cm;
	
	public RdmaProviderNat() throws IOException{
		logger.info("creating  RdmaProvider of type 'nat'");
		this.nativeDispatcher = new NativeDispatcher();
		this.verbs = new RdmaVerbsNat(nativeDispatcher);
		this.cm = new RdmaCmNat(nativeDispatcher);		
	}
	
	public RdmaCm openCm() throws IOException {
		return cm;
	}
	
	public RdmaVerbs openVerbs() throws IOException {
		return verbs;
	}

	@Override
	public int getVersion() {
		return nativeDispatcher._getVersion();
	}
}
