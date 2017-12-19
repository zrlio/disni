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

import com.ibm.disni.rdma.verbs.IbvWC;
import com.ibm.disni.util.DiSNILogger;

public class NativeDispatcher {
	private static final Logger logger = DiSNILogger.getLogger();
	private static int JVERBS_VERSION = 32;

	static {
	    System.loadLibrary("disni");
	}

	NativeDispatcher() throws IOException{
		logger.info("jverbs jni version " + _getVersion());
		if (_getVersion() != JVERBS_VERSION){
			logger.info("jverbs outdated, version found " + _getVersion() + ", version required " + JVERBS_VERSION);
			throw new IOException("jverbs outdated, version found " + _getVersion() + ", version required " + JVERBS_VERSION);
		}
		if (_getSockAddrInSize() != SockAddrIn.CSIZE){
			logger.info("sock_addr_in size mismatch, jverbs size " + SockAddrIn.CSIZE + ", native size " +_getSockAddrInSize());
			SockAddrIn.CSIZE = _getSockAddrInSize();
		} else {
			logger.info("sock_addr_in size match, jverbs size " + SockAddrIn.CSIZE + ", native size " +_getSockAddrInSize());
		}
		if (_getIbvRecvWRSize() != NatIbvRecvWR.CSIZE){
			logger.info("IbvRecvWR size mismatch, jverbs size " + NatIbvRecvWR.CSIZE + ", native size " +_getIbvRecvWRSize());
			NatIbvRecvWR.CSIZE = _getIbvRecvWRSize();
		} else {
			logger.info("IbvRecvWR size match, jverbs size " + NatIbvRecvWR.CSIZE + ", native size " +_getIbvRecvWRSize());
		}
		if (_getIbvSendWRSize() != NatIbvSendWR.CSIZE){
			logger.info("IbvSendWR size mismatch, jverbs size " + NatIbvSendWR.CSIZE + ", native size " +_getIbvSendWRSize());
			NatIbvSendWR.CSIZE = _getIbvSendWRSize();
		} else {
			logger.info("IbvSendWR size match, jverbs size " + NatIbvSendWR.CSIZE + ", native size " +_getIbvSendWRSize());
		}
		if (_getIbvWCSize() != IbvWC.CSIZE){
			logger.info("IbvWC size mismatch, jverbs size " + IbvWC.CSIZE + ", native size " +_getIbvWCSize());
			IbvWC.CSIZE = _getIbvWCSize();
		} else {
			logger.info("IbvWC size match, jverbs size " + IbvWC.CSIZE + ", native size " +_getIbvWCSize());
		}
		if (_getIbvSgeSize() != NatIbvSge.CSIZE){
			logger.info("IbvSge size mismatch, jverbs size " + NatIbvSge.CSIZE + ", native size " +_getIbvSgeSize());
			NatIbvSge.CSIZE = _getIbvSgeSize();
		} else {
			logger.info("IbvSge size match, jverbs size " + NatIbvSge.CSIZE + ", native size " +_getIbvSgeSize());
		}

		if (_getRemoteAddressOffset() != NatIbvSendWR.REMOTEADDR_OFFSET){
			logger.info("Remote addr offset mismatch, jverbs size " + NatIbvSendWR.REMOTEADDR_OFFSET + ", native size " +_getRemoteAddressOffset());
			NatIbvSendWR.REMOTEADDR_OFFSET = _getRemoteAddressOffset();
		} else {
			logger.info("Remote addr offset match, jverbs size " + NatIbvSendWR.REMOTEADDR_OFFSET + ", native size " +_getRemoteAddressOffset());
		}
		if (_getRKeyOffset() != NatIbvSendWR.RKEY_OFFSET){
			logger.info("Rkey offset mismatch, jverbs size " + NatIbvSendWR.RKEY_OFFSET + ", native size " +_getRKeyOffset());
			NatIbvSendWR.RKEY_OFFSET = _getRKeyOffset();
		} else {
			logger.info("Rkey offset match, jverbs size " + NatIbvSendWR.RKEY_OFFSET + ", native size " +_getRKeyOffset());
		}
	}

	//rdmacm
	public native long _createEventChannel();
	public native long _createId(long channel, short rdma_ps);
	public native long _createQP(long id, long pd, long sendcq, long recvcq, int qptype, int maxsendwr, int maxrecvwr, int maxinline);
	public native int _bindAddr(long id, long addr);
	public native int _listen(long id, int backlog);
	public native int _resolveAddr(long id, long src, long dst, int timeout);
	public native int _resolveRoute(long id, int timeout);
	public native int _getCmEvent(long channel, long listenid, long clientid, int timeout);
	public native int _connect(long id, int retrycount, int rnrretrycount, long privdataaddr, byte privdatalen);
	public native int _accept(long id, int retrycount, int rnrretrycount);
	public native int _ackCmEvent(int cmEvent);
	public native int _disconnect(long id);
	public native int _destroyEventChannel(long fd);
	public native int _destroyCmId(long natid);
	public native int _destroyQP(long id);
	public native int _getSrcAddr(long id, long address);
	public native int _getDstAddr(long id, long address);
	public native int _destroyEp(long natid);

	//ibverbs
	public native long _allocPd(long context);
	public native long _createCompChannel(long context);
	public native long _createCQ(long context, long compChannel, int ncqe, int comp_vector);
	public native int _modifyQP(long qp, long attr);
	public native long _regMr(long pd, long addr, int len, int access, long lkey, long rkey, long handle);
	public native int _queryOdpSupport(long context);
	public native int _expPrefetchMr(long handle, long addr, int len);
	public native int _deregMr(long handle);
	public native int _postSend(long qp, long wrList);
	public native int _postRecv(long qp, long wrList);
	public native int _getCqEvent(long compChannel, int timeout);
	public native int _pollCQ(long cq, int ne, long wclist);
	public native int _reqNotifyCQ(long cq, int solicited_only);
	public native int _ackCqEvent(long cq, int nevents);
	public native int _destroyCompChannel(long fd);
	public native int _deallocPd(long handle);
	public native int _destroyCQ(long handle);

	//field lookup
	public native long _getContext(long id);
	public native int _getQpNum(long id);
	public native int _getContextFd(long objId);
	public native int _getContextNumCompVectors(long objId);
	public native int _getPdHandle(long objId);

	//struct verification
	public native int _getSockAddrInSize();
	public native int _getIbvRecvWRSize();
	public native int _getIbvSendWRSize();
	public native int _getIbvSgeSize();
	public native int _getIbvWCSize();
	public native int _getRemoteAddressOffset();
	public native int _getRKeyOffset();

	//version
	public native int _getVersion();

}
