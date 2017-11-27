/*
 * DiSNI: Direct Storage and Networking Interface
 *
 * Author: Jonas Pfefferle <jpf@zurich.ibm.com>
 *         Patrick Stuedi  <stu@zurich.ibm.com>
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
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.ibm.disni.DiSNIGroup;
import com.ibm.disni.DiSNIServerEndpoint;
import com.ibm.disni.nvmef.spdk.Nvme;
import com.ibm.disni.nvmef.spdk.NvmeController;
import com.ibm.disni.nvmef.spdk.NvmeTransportId;
import com.ibm.disni.nvmef.spdk.NvmeTransportType;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class NvmeEndpointGroup implements DiSNIGroup<NvmeEndpoint> {
	private Nvme nvme;
	
	public NvmeEndpointGroup(NvmeTransportType[] transportTypes, long memorySizeMB){
		this.nvme = new Nvme(transportTypes, memorySizeMB);
	}

	public DiSNIServerEndpoint<NvmeEndpoint> createServerEndpoint() throws Exception {
		throw new NotImplementedException();
	}

	public NvmeEndpoint createEndpoint(){
		return new NvmeEndpoint(this);
	}
	
	//--------------- internal ------------------

	NvmeController probe(NvmeTransportId transportId, int index) throws IOException {
		ArrayList<NvmeController> controllers = new ArrayList<NvmeController>();
		nvme.probe(transportId, controllers);
		return controllers.get(index);
	}

	public ByteBuffer allocateBuffer(int size, int alignment) {
		return nvme.allocateBuffer(size, alignment);
	}

	public void freeBuffer(ByteBuffer buffer) {
		nvme.freeBuffer(buffer);
	}
}
