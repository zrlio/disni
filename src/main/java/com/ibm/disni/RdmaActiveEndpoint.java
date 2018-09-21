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

package com.ibm.disni;

import java.io.IOException;

import com.ibm.disni.verbs.IbvWC;
import com.ibm.disni.verbs.RdmaCmId;

public abstract class RdmaActiveEndpoint extends RdmaEndpoint {
	RdmaActiveEndpointGroup<? extends RdmaActiveEndpoint> agroup;
	
	public RdmaActiveEndpoint(RdmaActiveEndpointGroup<? extends RdmaActiveEndpoint> group, RdmaCmId idPriv, boolean serverSide) throws IOException {
		super(group, idPriv, serverSide);
		this.agroup = group;
	}

	/* (non-Javadoc)
	 * @see com.ibm.jverbs.endpoints.ICqConsumer#dispatchCqEvent(com.ibm.jverbs.verbs.IbvWC)
	 */
	public abstract void dispatchCqEvent(IbvWC wc) throws IOException;

	@Override
	public void close() throws IOException, InterruptedException {
		super.close();
		agroup.close(this);
	}
	
}
