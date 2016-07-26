/*
 * jVerbs: RDMA verbs support for the Java Virtual Machine
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

package com.ibm.disni.examples.endpoints.common;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import com.ibm.disni.rdma.endpoints.RdmaActiveClientEndpoint;
import com.ibm.disni.rdma.endpoints.RdmaActiveEndpointGroup;
import com.ibm.disni.rdma.verbs.IbvWC;
import com.ibm.disni.rdma.verbs.RdmaCmId;

public class CustomEndpoint extends RdmaActiveClientEndpoint {
	private ArrayBlockingQueue<IbvWC> wcEvents;

	public CustomEndpoint(RdmaActiveEndpointGroup<?> endpointGroup, RdmaCmId idPriv) throws IOException {
		super(endpointGroup, idPriv);
		
		this.wcEvents = new ArrayBlockingQueue<IbvWC>(10);
	}
	
	public void dispatchCqEvent(IbvWC wc) throws IOException {
		wcEvents.add(wc);
	}
	
	public ArrayBlockingQueue<IbvWC> getWcEvents() {
		return wcEvents;
	}
}
