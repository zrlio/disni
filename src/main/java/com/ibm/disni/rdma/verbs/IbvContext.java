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

package com.ibm.disni.rdma.verbs;

import java.io.IOException;


//struct ibv_context {
//        struct ibv_device      *device;
//        struct ibv_context_ops  ops;
//        int                     cmd_fd;
//        int                     async_fd;
//        int                     num_comp_vectors;
//        pthread_mutex_t         mutex;
//        void                   *abi_compat;
//};

/**
 * Represents a RDMA device context.
 */
public class IbvContext  {
	private RdmaVerbs verbs;
	protected int cmd_fd;
	
	protected IbvContext(int cmd_fd) throws IOException{
		this.verbs = RdmaVerbs.open();
		this.cmd_fd = cmd_fd;
	}

	/**
	 * Retrieves the file descriptor used to implement this device context.
	 *
	 * @return the cmd_fd
	 */
	public int getCmd_fd() {
		return cmd_fd;
	}
	
	//---------- oo-verbs
	
	public IbvPd allocPd() throws IOException{
		return verbs.allocPd(this);
	}
	
	public IbvCompChannel createCompChannel() throws IOException {
		return verbs.createCompChannel(this);
	}
	
	public IbvCQ createCQ(IbvCompChannel compChannel, int ncqe, int comp_vector) throws IOException {
		return verbs.createCQ(this, compChannel, ncqe, comp_vector);
	}
}
