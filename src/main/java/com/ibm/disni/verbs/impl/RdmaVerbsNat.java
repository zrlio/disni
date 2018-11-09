/*
 * DiSNI: Direct Storage and Networking Interface
 *
 * Author: Patrick Stuedi <stu@zurich.ibm.com>
 *
 * Copyright (C) 2016-2018, IBM Corporation
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

package com.ibm.disni.verbs.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;

import com.ibm.disni.verbs.IbvCQ;
import com.ibm.disni.verbs.IbvCompChannel;
import com.ibm.disni.verbs.IbvContext;
import com.ibm.disni.verbs.IbvMr;
import com.ibm.disni.verbs.IbvPd;
import com.ibm.disni.verbs.IbvQP;
import com.ibm.disni.verbs.IbvQpAttr;
import com.ibm.disni.verbs.IbvRecvWR;
import com.ibm.disni.verbs.IbvSendWR;
import com.ibm.disni.verbs.IbvWC;
import com.ibm.disni.verbs.RdmaVerbs;
import com.ibm.disni.verbs.SVCDeregMr;
import com.ibm.disni.verbs.SVCPollCq;
import com.ibm.disni.verbs.SVCPostRecv;
import com.ibm.disni.verbs.SVCPostSend;
import com.ibm.disni.verbs.SVCRegMr;
import com.ibm.disni.verbs.SVCReqNotify;
import com.ibm.disni.util.DiSNILogger;
import com.ibm.disni.util.MemoryAllocation;


public class RdmaVerbsNat extends RdmaVerbs {
	private static final Logger logger = DiSNILogger.getLogger();
	
	private MemoryAllocation memAlloc;
	private NativeDispatcher nativeDispatcher;
	
	public RdmaVerbsNat(NativeDispatcher nativeDispatcher) {
		this.memAlloc = MemoryAllocation.getInstance();
		this.nativeDispatcher = nativeDispatcher;
	}

	public IbvPd allocPd(IbvContext context) throws IOException {
		NatIbvContext natContext = (NatIbvContext) context;
		if (!natContext.isOpen()) {
			throw new IOException("Trying to allocate PD on closed context.");
		}
		long objId = nativeDispatcher._allocPd(natContext.getObjId());
		logger.info("allocPd, objId " + objId);
		
		NatIbvPd pd = null;
		if (objId >= 0){
			pd = new NatIbvPd(objId, context, nativeDispatcher);
		}
		
		return pd;
	}

	public IbvCompChannel createCompChannel(IbvContext context)
			throws IOException {
		NatIbvContext natContext = (NatIbvContext) context;
		if (!natContext.isOpen()) {
			throw new IOException("Trying to create completion channel with closed channel.");
		}
		long objId = nativeDispatcher._createCompChannel(natContext.getObjId());
		logger.info("createCompChannel, context " + natContext.getObjId());
		
		NatIbvCompChannel channel = null;
		if (objId >= 0){
			channel = new NatIbvCompChannel(objId, 0, context);
		}
		
		return channel;
	}

	public IbvCQ createCQ(IbvContext context, IbvCompChannel compChannel,
			int ncqe, int comp_vector) throws IOException {
		NatIbvContext natContext = (NatIbvContext) context;
		NatIbvCompChannel natCompChannel = (NatIbvCompChannel) compChannel;
		if (!natContext.isOpen()) {
			throw new IOException("Trying to create CQ with closed context.");
		}
		if (!natCompChannel.isOpen()) {
			throw new IOException("Trying to create CQ with closed completion channel.");
		}
		long objId = nativeDispatcher._createCQ(natContext.getObjId(), natCompChannel.getObjId(), ncqe, comp_vector);
		logger.info("createCQ, objId " + objId + ", ncqe " + ncqe);
		
		NatIbvCQ cq = null;
		if (objId >= 0){
			cq = new NatIbvCQ(objId, context, compChannel, 0);
		}
		return cq;
	}

	public IbvQP modifyQP(IbvQP qp, IbvQpAttr attr)
			throws IOException {
		NatIbvQP natQP = (NatIbvQP) qp;
		if (!qp.isOpen()) {
			throw new IOException("Trying to modify closed QP");
		}
		int ret = nativeDispatcher._modifyQP(natQP.getObjId(), (long) 0);
		logger.info("modifyQP, qpnum " + qp.getQp_num());
		
		if (ret >= 0){
			return qp;
		}
		return null;
	}

	public SVCRegMr regMr(IbvPd pd, ByteBuffer buffer, int access) {
		return new NatRegMrCall(this, nativeDispatcher, memAlloc, pd, buffer, access);
	}

	public SVCRegMr regMr(IbvPd pd, long address, int length, int access) {
		return new NatRegMrCall(this, nativeDispatcher, memAlloc, pd, address, length, access);

	}

	public int queryOdpSupport(IbvContext context){
		NatIbvContext natContext = (NatIbvContext) context;
		return nativeDispatcher._queryOdpSupport(natContext.getObjId());
	}

	public int expPrefetchMr(IbvMr ibvMr, long address, int length) throws IOException {
		return nativeDispatcher._expPrefetchMr(((NatIbvMr)ibvMr).getObjId(), address, length);
	}

	public SVCDeregMr deregMr(IbvMr mr) {
		return new NatDeregMrCall(this, nativeDispatcher, mr);
	}

	public SVCPostSend postSend(IbvQP qp, List<IbvSendWR> wrList, List<IbvSendWR> badwrList) {
		return new NatPostSendCall(this, nativeDispatcher, memAlloc, qp, wrList);
	}

	public SVCPostRecv postRecv(IbvQP qp, List<IbvRecvWR> wrList, List<IbvRecvWR> badwrList) {
		return new NatPostRecvCall(this, nativeDispatcher, memAlloc, qp, wrList);
	}

	public boolean getCqEvent(IbvCompChannel compChannel, IbvCQ cq, int timeout) throws IOException {
		NatIbvCompChannel natChannel = (NatIbvCompChannel) compChannel;
		if (!natChannel.isOpen()) {
			throw new IOException("Trying to get CQ event on closed completion channel.");
		}
		int ret = nativeDispatcher._getCqEvent(natChannel.getObjId(), timeout);
		return ret >= 0;
	}

	public SVCPollCq pollCQ(IbvCQ cq, IbvWC[] wcList, int ne) {
		return new NatPollCqCall(this, nativeDispatcher, memAlloc, cq, wcList, ne);
	}

	public SVCReqNotify reqNotifyCQ(IbvCQ cq, boolean solicited_only) {
		return new NatReqNotifyCall(this, nativeDispatcher, cq, solicited_only);
	}

	public int ackCqEvents(IbvCQ cq, int nevents) throws IOException {
		NatIbvCQ natCQ = (NatIbvCQ) cq;
		if (!cq.isOpen()) {
			throw new IOException("Trying to acknowledge events on closed CQ.");
		}
		int ret = nativeDispatcher._ackCqEvent(natCQ.getObjId(), nevents);
		return ret;
	}

	public int destroyCompChannel(IbvCompChannel compChannel) throws IOException {
		logger.info("destroyCompChannel, compChannel " + compChannel.getFd());
		NatIbvCompChannel compChannelImpl = (NatIbvCompChannel) compChannel;
		if (!compChannelImpl.isOpen()) {
			throw new IOException("Trying to destroy an already destroyed completion channel.");
		}
		compChannelImpl.close();
		int ret = nativeDispatcher._destroyCompChannel(compChannelImpl.getObjId());
		return ret;
	}

	@Override
	public int deallocPd(IbvPd pd) throws IOException {
		logger.info("deallocPd, pd " + pd.getHandle());
		NatIbvPd pdImpl = (NatIbvPd) pd;
		if (!pdImpl.isOpen()) {
			throw new IOException("Trying to deallocate a closed protection domain.");
		}
		pdImpl.close();
		int ret = nativeDispatcher._deallocPd(pdImpl.getObjId());
		return ret;
	}

	@Override
	public int destroyCQ(IbvCQ cq) throws IOException {
		NatIbvCQ cqImpl = (NatIbvCQ) cq;
		logger.info("destroyCQ, cq " + cqImpl.getObjId());
		if (!cqImpl.isOpen()) {
			throw new IOException("Trying to destroy already destroyed CQ.");
		}
		cqImpl.close();
		int ret = nativeDispatcher._destroyCQ(cqImpl.getObjId());
		return ret;
	}
}
