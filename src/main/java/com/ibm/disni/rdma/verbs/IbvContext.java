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

//enum ibv_odp_transport_cap_bits {
//        IBV_ODP_SUPPORT_SEND     = 1 << 0, /* Send operations support on-demand paging */
//        IBV_ODP_SUPPORT_RECV     = 1 << 1, /* Receive operations support on-demand paging */
//        IBV_ODP_SUPPORT_WRITE    = 1 << 2, /* RDMA-Write operations support on-demand paging */
//        IBV_ODP_SUPPORT_READ     = 1 << 3, /* RDMA-Read operations support on-demand paging */
//        IBV_ODP_SUPPORT_ATOMIC   = 1 << 4, /* RDMA-Atomic operations support on-demand paging */
//};

/**
 * Represents a RDMA device context.
 */
public class IbvContext  {
	private RdmaVerbs verbs;
	protected int cmd_fd;
	protected volatile boolean isOpen;
	protected int numCompVectors;

	//ODP capabilities
	public static int IBV_ODP_SUPPORT_SEND     = 1 << 0;
	public static int IBV_ODP_SUPPORT_RECV     = 1 << 1;
	public static int IBV_ODP_SUPPORT_WRITE    = 1 << 2;
	public static int IBV_ODP_SUPPORT_READ     = 1 << 3;
	public static int IBV_ODP_SUPPORT_ATOMIC   = 1 << 4;
	
	protected IbvContext(int cmd_fd, int numCompVectors) throws IOException {
		this.verbs = RdmaVerbs.open();
		this.cmd_fd = cmd_fd;
		this.isOpen = true;
		this.numCompVectors = numCompVectors;
	}

	/**
	 * Retrieves the file descriptor used to implement this device context.
	 *
	 * @return the cmd_fd
	 * @throws IOException
	 */
	public int getCmd_fd() throws IOException {
		return cmd_fd;
	}

	public int getNumCompVectors() {
		return numCompVectors;
	}
	
	public boolean isOpen() {
		return isOpen;
	}

	public void close() {
		isOpen = false;
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

	public int queryOdpSupport() throws IOException { return verbs.queryOdpSupport(this); }
}
