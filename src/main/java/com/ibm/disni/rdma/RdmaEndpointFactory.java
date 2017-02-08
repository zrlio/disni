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

import com.ibm.disni.rdma.verbs.RdmaCmId;

public interface RdmaEndpointFactory <C extends RdmaEndpoint> {
	
	/**
	 * Creates a new client endpoint.
	 *
	 * @param group the RdmaEndpoint group the newly created endpoint is going to be part of.
	 * @param id the RDMA identifier.
	 * @return the newly created endpoint.
	 * @throws Exception on failure.
	 */
	public C createEndpoint(RdmaCmId id, boolean serverSide) throws IOException;
	
}
