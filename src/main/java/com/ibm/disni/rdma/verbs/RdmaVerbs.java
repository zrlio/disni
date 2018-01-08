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
import java.nio.ByteBuffer;
import java.util.List;

/**
 * The RDMA verbs interface for data transfer operations.
 */
public abstract class RdmaVerbs {  
	
	/**
	 * Open the verbs interface.
	 *
	 * @return the actual verbs interface.
	 * @throws Exception on failure
	 */
	public static RdmaVerbs open() throws IOException {
		return RdmaProvider.provider().openVerbs(); 
	}
	
	/**
	 * Retrieves the version of jVerbs
	 * @throws IOException 
	 */	
	public static int getVersion() {
		int version = 0;
		try {
			version = RdmaProvider.provider().getVersion();
		} catch(Exception e){
		}
		return version;
	}	
	
	/**
	 * Allocates a protection domain for the RDMA device context context.
	 * 
	 * Protection domains are a container for memory registrations. 
	 * 
	 * The device context should be obtained from the RdmaCmId.
	 *
	 * @param context the device context. 
	 * @return the ibv pd
	 * @throws Exception on failure.
	 */
	public abstract IbvPd allocPd(IbvContext context) throws IOException;

	/**
	 * Creates a completion event channel for the RDMA device context.
	 * 
	 * A completion channel is essentially a file descriptor that is used to deliver completion notifications to a userspace process. When a completion event is generated for a completion queue (CQ), the event is delivered via the completion channel attached to that CQ.  This may be useful to steer completion events to different threads by using multiple completion channels. 
	 *
	 * @param context the device context. 
	 * @return the completion channel.
	 * @throws Exception on failure.
	 */
	public abstract IbvCompChannel createCompChannel(IbvContext context)
			throws IOException;

	/**
	 * Creates a completion queue (CQ) with at least ncqe entries for the RDMA device context.
	 *
	 * @param context the device context. 
	 * @param compChannel the completion channel to be associated with the CQ.
	 * @param ncqe the number of CQ entries this CQ can possibly hold.
	 * @param comp_vector used for signaling completion events. Must be at least zero.
	 * @return the completion queue (CQ).
	 * @throws Exception on failure.
	 */
	public abstract IbvCQ createCQ(IbvContext context,
			IbvCompChannel compChannel, int ncqe, int comp_vector)
			throws IOException;
	
	/**
	 * Waits for the next completion event in the completion event channel channel
	 *
	 * @param compChannel the completion channel to be used.
	 * @param cq Returns the CQ which caused the event. Unsupported.
	 * @return a stateful verb call (SVC) ready to execute this call. 
	 * @throws Exception on failure.
	 */
	public abstract boolean getCqEvent(IbvCompChannel compChannel, IbvCQ cq, int timeout) throws IOException;
	

	/**
	 * Registers the memory of a ByteBuffer with the RDMA device.
	 *
	 * @param pd the protection domain this memory registratin will be associated with.
	 * @param buffer the actual memory to be registered.
	 * @param access the access rights.
	 * @return a stateful  verb call (SVC) ready to execute the memory registration. 
	 * @throws Exception on failure.
	 */
	public abstract SVCRegMr regMr(IbvPd pd, ByteBuffer buffer, int access) throws IOException;
	
	public abstract SVCRegMr regMr(IbvPd pd, long address, int length, int access) throws IOException;

	/**
	 * Query device RC capability for on demand paging support
	 *
	 * @param context the device context.
	 * @return odp_caps.per_transport_caps.rc_odp_caps property if ODP is supported, -1 otherwise.
	 * @throws Exception on failure.
	 */
	public abstract int queryOdpSupport(IbvContext context) throws IOException;

	/**
	 * Prefetch part of a memory region.
	 * Can be used only with MRs registered with IBV_EXP_ACCESS_ON_DEMAND
	 *
	 * @param  address address of the area to prefetch the device context.
	 * @param  length length of the area to prefetch.
	 * @return return 0 on success.
	 * ENOSYS libibverbs or provider driver doesn't support the prefetching verb.
	 * EFAULT when the range requested is out of the memory region bounds, or when
	 *   parts of it are not part of the process address space.
	 * EINVAL when the MR is invalid.
	 * @throws Exception on failure.
	 */
	public abstract int expPrefetchMr(IbvMr ibvMr, long address, int length) throws IOException;

	/**
	 * Deregister memory.
	 *
	 * @param mr the memory region to be de-registered.
	 * @return a stateful  verb call (SVC) ready to execute the memory de-registration. 
	 * @throws Exception on failure.
	 */
	public abstract SVCDeregMr deregMr(IbvMr mr)
			throws IOException;

	/**
	 * Post a send operation on this endpoint.
	 *
	 * @param qp the QP this operation should be executed on.
	 * @param wrList list of send requests.
	 * @param badwrList output list of request that could not be processed. Not supported.
	 * @return a stateful verb call (SVC) ready to execute the send operations. 
	 * @throws Exception on failure.
	 */
	public abstract SVCPostSend postSend(IbvQP qp, List<IbvSendWR> wrList, List<IbvSendWR> badwrList) throws IOException;

	/**
	 * Post recv.
	 *
	 * @param qp the QP this operation should be executed on.
	 * @param wrList list of receive requests.
	 * @param badwrList output list of request that could not be processed. Not supported.
	 * @return a stateful verb call (SVC) ready to execute the receive operations. 
	 * @throws Exception on failure.
	 */
	public abstract SVCPostRecv postRecv(IbvQP qp, List<IbvRecvWR> wrList, List<IbvRecvWR> badwrList) throws IOException;

	/**
	 * Poll on the CQ until a new event is received.
	 *
	 * @param cq the completion queue to be polled.
	 * @param wcList an array of completion events to be used by this method to return completion events.
	 * @param ne the maximum number of completion events to be polled.
	 * @return a stateful verb call (SVC) ready to execute a polling call.
	 * @throws Exception on failure.
	 */
	public abstract SVCPollCq pollCQ(IbvCQ cq, IbvWC[] wcList, int ne) throws IOException;

	/**
	 * Requests a completion notification on the completion queue (CQ).
	 *
	 * @param cq the completion queue to be used.
	 * @param solicited_only if the argument solicited_only is zero, a completion event is generated for any new CQE. If solicited_only is non-zero, an event is only generated for a new CQE with that is considered "solicited".
	 * @return a stateful verb call (SVC) ready to execute this call.
	 * @throws Exception on failure.
	 */
	public abstract SVCReqNotify reqNotifyCQ(IbvCQ cq, boolean solicited_only) throws IOException;

	/**
	 * Acknowledges nevents events on the CQ.
	 *
	 * @param cq the completion queue that generated the events in the first place.
	 * @param nevents the number of events to be acknowledged.
	 * @return returns 0 on success.
	 * @throws IOException
	 */
	public abstract int ackCqEvents(IbvCQ cq, int nevents) throws IOException;
	
	/**
	 * Destroys the completion event channel
	 *
	 * @param compChannel the completion channel to be destroyed.
	 * @return return 0 on success.
	 * @throws Exception on failure.
	 */
	public abstract int destroyCompChannel(IbvCompChannel compChannel) throws IOException;
	
	/**
	 * Deallocates the protection domain.
	 *
	 * @param pd the protection domain to be de-allocated.
	 * @return return 0 on success.
	 * @throws Exception on failure.
	 */
	public abstract int deallocPd(IbvPd pd) throws IOException;
	
	/**
	 * Destroys the completion queue.
	 *
	 * @param cq the completion queue to be destroyed.
	 * @return return 0 on success.
	 * @throws Exception on failure.
	 */
	public abstract int destroyCQ(IbvCQ cq) throws IOException;
}
