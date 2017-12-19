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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.slf4j.Logger;

import com.ibm.disni.rdma.verbs.IbvPd;
import com.ibm.disni.rdma.verbs.IbvQP;
import com.ibm.disni.rdma.verbs.IbvQPInitAttr;
import com.ibm.disni.rdma.verbs.RdmaCm;
import com.ibm.disni.rdma.verbs.RdmaCmEvent;
import com.ibm.disni.rdma.verbs.RdmaCmId;
import com.ibm.disni.rdma.verbs.RdmaConnParam;
import com.ibm.disni.rdma.verbs.RdmaEventChannel;
import com.ibm.disni.util.DiSNILogger;
import com.ibm.disni.util.MemBuf;
import com.ibm.disni.util.MemoryAllocation;
import com.ibm.disni.util.NetUtils;


public class RdmaCmNat extends RdmaCm {
	private static final Logger logger = DiSNILogger.getLogger();
	
	private MemoryAllocation memAlloc;
	private NativeDispatcher nativeDispatcher;
	
	public RdmaCmNat(NativeDispatcher nativeDispatcher) { 
		this.memAlloc = MemoryAllocation.getInstance();
		this.nativeDispatcher = nativeDispatcher;
	}

	@Override
	public RdmaEventChannel createEventChannel() throws IOException {
		long objId = nativeDispatcher._createEventChannel();
		logger.info("createEventChannel, objId " + objId);
		
		NatRdmaEventChannel channel = null;
		if (objId >= 0){
			channel =  new NatRdmaEventChannel(objId, 0);
		}
		
		return channel;
	}

	@Override
	public RdmaCmId createId(RdmaEventChannel cmChannel, short rdma_ps)
			throws IOException {
		NatRdmaEventChannel channelImpl = (NatRdmaEventChannel) cmChannel;
		if (!channelImpl.isOpen()) {
			throw new IOException("Trying to create ID with closed channel.");
		}
		long objId = nativeDispatcher._createId(channelImpl.getObjId(), rdma_ps);
		logger.info("createId, id " + objId);
		
		NatCmaIdPrivate idPriv = null;
		if (objId >= 0) {
			idPriv = new NatCmaIdPrivate(objId, cmChannel, nativeDispatcher);
			channelImpl.addCmId(idPriv);
		}

		return idPriv;
	}

	@Override
	public IbvQP createQP(RdmaCmId id, IbvPd pd, IbvQPInitAttr attr)
			throws IOException {
		NatCmaIdPrivate idPriv = (NatCmaIdPrivate) id;
		NatIbvPd natPd = (NatIbvPd) pd;
		NatIbvCQ natSendCq = (NatIbvCQ) attr.getSend_cq();
		NatIbvCQ natRecvCq = (NatIbvCQ) attr.getRecv_cq();
		if (!idPriv.isOpen()) {
			throw new IOException("Trying to create QP with closed ID");
		}
		if (!pd.isOpen()) {
			throw new IOException("Trying to create QP with closed PD");
		}
		if (!natSendCq.isOpen()) {
			throw new IOException("Trying to create a QP with closed send CQ");
		}
		if (!natRecvCq.isOpen()) {
			throw new IOException("Trying to create a QP with closed receive CQ");
		}
		long objId = nativeDispatcher._createQP(idPriv.getObjId(), natPd.getObjId(), natSendCq.getObjId(), natRecvCq.getObjId(), attr.getQp_type(), attr.cap().getMax_send_wr(), attr.cap().getMax_recv_wr(), attr.cap().getMax_inline_data());
		logger.info("createQP, objId " + objId + ", send_wr size " + attr.cap().getMax_send_wr() + ", recv_wr_size " + attr.cap().getMax_recv_wr());
		
		NatIbvQP qp = null;
		if (objId >= 0){
			qp = new NatIbvQP(objId, nativeDispatcher);
			idPriv.setQp(qp);
		}
		
		return qp;
	}

	@Override
	public int bindAddr(RdmaCmId id, SocketAddress address)
			throws IOException {
        InetSocketAddress _address = (InetSocketAddress) address;
        short _sin_family = SockAddrIn.AF_INET;
        int _sin_addr = NetUtils.getIntIPFromInetAddress(_address.getAddress());
        short _sin_port = NetUtils.hostToNetworkByteOrder((short) _address.getPort());
        
        SockAddrIn addr = new SockAddrIn(_sin_family, _sin_addr, _sin_port);
        MemBuf sockBuf = memAlloc.allocate(SockAddrIn.CSIZE, MemoryAllocation.MemType.DIRECT, SockAddrIn.class.getCanonicalName());
        addr.writeBack(sockBuf.getBuffer());
        NatCmaIdPrivate idPriv = (NatCmaIdPrivate) id;
        if (!idPriv.isOpen()) {
            throw new IOException("Trying to bind() using a closed ID");
        }
        int ret = nativeDispatcher._bindAddr(idPriv.getObjId(), sockBuf.address());
        sockBuf.free();
        logger.info("bindAddr, address " + address.toString());

        return ret;
	}

	@Override
	public int listen(RdmaCmId id, int backlog) throws IOException {
		NatCmaIdPrivate idPriv = (NatCmaIdPrivate) id;
		if (!idPriv.isOpen()) {
		    throw new IOException("Trying to listen on closed ID");
		}
		int ret = nativeDispatcher._listen(idPriv.getObjId(), backlog);
		logger.info("listen, id " + id.getPs());
		
		return  ret;
	}

	@Override
	public int resolveAddr(RdmaCmId id, SocketAddress source,
			SocketAddress destination, int timeout) throws IOException {
        InetSocketAddress _dst = (InetSocketAddress) destination;
        SockAddrIn dst = new SockAddrIn(SockAddrIn.AF_INET, NetUtils.getIntIPFromInetAddress(_dst.getAddress()), NetUtils.hostToNetworkByteOrder((short) _dst.getPort()));
        MemBuf dstBuf = memAlloc.allocate(SockAddrIn.CSIZE, MemoryAllocation.MemType.DIRECT, SockAddrIn.class.getCanonicalName());
        dst.writeBack(dstBuf.getBuffer());
        NatCmaIdPrivate idPriv = (NatCmaIdPrivate) id;
        if (!idPriv.isOpen()) {
            throw new IOException("Trying to resolve address with closed ID");
        }
        int ret = nativeDispatcher._resolveAddr(idPriv.getObjId(), 0, dstBuf.address(), timeout);
        logger.info("resolveAddr, addres " + destination.toString());
        dstBuf.free();
        
        return ret;
	}

	@Override
	public int resolveRoute(RdmaCmId id, int timeout) throws IOException {
		NatCmaIdPrivate idPriv = (NatCmaIdPrivate) id;
        if (!idPriv.isOpen()) {
            throw new IOException("Trying to resolve route with closed ID");
        }
		int ret = nativeDispatcher._resolveRoute(idPriv.getObjId(), timeout);
		logger.info("resolveRoute, id " + id.getPs());
		
		return ret;
	}

	@Override
	public RdmaCmEvent getCmEvent(RdmaEventChannel cmChannel, int timeout) throws IOException {
		NatRdmaEventChannel channelImpl = (NatRdmaEventChannel) cmChannel;
		RdmaCmEvent cmEvent = null;
		
		MemBuf memBuf = memAlloc.allocate(2*8, MemoryAllocation.MemType.DIRECT, "getcm");
		ByteBuffer buf = memBuf.getBuffer();
		if (!channelImpl.isOpen()) {
			throw new IOException("Trying to get CM event on closed channel.");
		}
		int event = nativeDispatcher._getCmEvent(channelImpl.getObjId(), memBuf.address(), memBuf.address() + 8, timeout);
		
		if (event >= 0){
			long _listenId = buf.getLong();
			long _clientId = buf.getLong();  
			NatCmaIdPrivate idPriv = channelImpl.getCmId(_listenId);
			NatCmaIdPrivate clientId = channelImpl.getCmId(_clientId);
			if (event == RdmaCmEvent.EventType.RDMA_CM_EVENT_CONNECT_REQUEST.ordinal()){
				clientId = new NatCmaIdPrivate(_clientId, channelImpl, nativeDispatcher);
				clientId.setVerbs(idPriv.getVerbs());
				channelImpl.addCmId(clientId);
			} 
			cmEvent = new RdmaCmEvent(event, idPriv, clientId);
		}
		
		memBuf.free();
		
		return cmEvent;
	}

	@Override
	public int connect(RdmaCmId id, RdmaConnParam connParam)
			throws IOException {
		NatCmaIdPrivate idPriv = (NatCmaIdPrivate) id;
		if (!idPriv.isOpen()) {
			throw new IOException("Trying to call connect() with closed ID");
		}
		int ret = nativeDispatcher._connect(idPriv.getObjId(), connParam.getRetry_count(),
				connParam.getRnr_retry_count(), connParam.getPrivate_data(), connParam.getPrivate_data_len());
//		int ret = nativeDispatcher._connect(idPriv.getObjId(), (long) 0);
		logger.info("connect, id " + id.getPs());
		
		return ret;
	}

	@Override
	public int accept(RdmaCmId id, RdmaConnParam connParam)
			throws IOException {
		NatCmaIdPrivate idPriv = (NatCmaIdPrivate) id;
		if (!idPriv.isOpen()) {
			throw new IOException("Trying to call accept() with closed ID");
		}
		int ret = nativeDispatcher._accept(idPriv.getObjId(), connParam.getRetry_count(), connParam.getRnr_retry_count());
//		int ret = nativeDispatcher._accept(idPriv.getObjId(), (long) 0);
		logger.info("accept, id " + id.getPs());
		
		return ret;
	}

	@Override
	public int ackCmEvent(RdmaCmEvent cmEvent) {
		int ret = nativeDispatcher._ackCmEvent(cmEvent.getEvent());
		
		return ret;
	}

	public int disconnect(RdmaCmId id) throws IOException {
		NatCmaIdPrivate idPriv = (NatCmaIdPrivate) id;
		if (!idPriv.isOpen()) {
			throw new IOException("Trying to disconnect with closed ID");
		}
		int ret = nativeDispatcher._disconnect(idPriv.getObjId());
		logger.info("disconnect, id " + id.getPs());
		
		return ret;
	}

	public int destroyEventChannel(RdmaEventChannel cmChannel) throws IOException {
		logger.info("destroyEventChannel, channel " + cmChannel.getFd());
		NatRdmaEventChannel channelImpl = (NatRdmaEventChannel) cmChannel;
		if (!channelImpl.isOpen()) {
			throw new IOException("Trying to destroy an already destroyed channel.");
		}
		channelImpl.close();
		int ret = nativeDispatcher._destroyEventChannel(channelImpl.getObjId());
		return ret;
	}

	public int destroyCmId(RdmaCmId id) throws IOException {
		logger.info("destroyCmId, id " + id.getPs());
		NatCmaIdPrivate idPriv = (NatCmaIdPrivate) id;
		if (!idPriv.isOpen()) {
			throw new IOException("Trying to destroy an already destroyed ID");
		}
		idPriv.close();
		int ret = nativeDispatcher._destroyCmId(idPriv.getObjId());
		return ret;
	}

	public int destroyQP(RdmaCmId id) throws IOException {
		logger.info("destroyQP, id " + id.getPs());
		NatCmaIdPrivate idImpl = (NatCmaIdPrivate) id;
		if (!idImpl.isOpen()) {
			throw new IOException("Trying to destroy QP with closed ID.");
		}
		if (!idImpl.getQp().isOpen()) {
			throw new IOException("Trying to destroy an already destroyed QP.");
		}
		idImpl.getQp().close();
		int ret = nativeDispatcher._destroyQP(idImpl.getObjId());
		return ret;
	}
	
	public SocketAddress getSrcAddr(RdmaCmId id) throws IOException {
		NatCmaIdPrivate idPriv = (NatCmaIdPrivate) id;
		SockAddrIn srcAddr = new SockAddrIn();
		MemBuf sockBuf = memAlloc.allocate(SockAddrIn.CSIZE, MemoryAllocation.MemType.DIRECT, SockAddrIn.class.getCanonicalName());
		if (!idPriv.isOpen()) {
			throw new IOException("Trying to get source address with closed ID");
		}
		int ret = nativeDispatcher._getSrcAddr(idPriv.getObjId(), sockBuf.address());
		InetSocketAddress socketAddress = null;
		if (ret == 0){
			sockBuf.getBuffer().clear();
			srcAddr.update(sockBuf.getBuffer());
			
			ByteBuffer translater = ByteBuffer.allocate(4);
			translater.order(ByteOrder.BIG_ENDIAN);
			translater.putInt(0, srcAddr.getSin_addr());
			translater.order(ByteOrder.LITTLE_ENDIAN);
			int hostaddr = translater.getInt(0);
			InetAddress address = NetUtils.getInetAddressFromIntIP(hostaddr);
			
			translater.order(ByteOrder.BIG_ENDIAN);
			translater.putInt(0, 0);
			translater.putShort(0, srcAddr.getSin_port());
			translater.order(ByteOrder.LITTLE_ENDIAN);
			int hostport = translater.getInt(0);
			
			socketAddress = new InetSocketAddress(address, hostport);		
		}
		sockBuf.free();
		return socketAddress;
	}
	
	public SocketAddress getDstAddr(RdmaCmId id) throws IOException {
		NatCmaIdPrivate idPriv = (NatCmaIdPrivate) id;
		SockAddrIn dstAddr = new SockAddrIn();
		MemBuf sockBuf = memAlloc.allocate(SockAddrIn.CSIZE, MemoryAllocation.MemType.DIRECT, SockAddrIn.class.getCanonicalName());
		if (!idPriv.isOpen()) {
			throw new IOException("Trying to get destination address with closed ID");
		}
		int ret = nativeDispatcher._getDstAddr(idPriv.getObjId(), sockBuf.address());
		InetSocketAddress socketAddress = null;
		if (ret == 0){
			sockBuf.getBuffer().clear();
			dstAddr.update(sockBuf.getBuffer());
			
			ByteBuffer translater = ByteBuffer.allocate(4);
			translater.order(ByteOrder.BIG_ENDIAN);
			translater.putInt(0, dstAddr.getSin_addr());
			translater.order(ByteOrder.LITTLE_ENDIAN);
			int hostaddr = translater.getInt(0);
			InetAddress address = NetUtils.getInetAddressFromIntIP(hostaddr);
			
			translater.order(ByteOrder.BIG_ENDIAN);
			translater.putInt(0, 0);
			translater.putShort(0, dstAddr.getSin_port());
			translater.order(ByteOrder.LITTLE_ENDIAN);
			int hostport = translater.getInt(0);
			
			socketAddress = new InetSocketAddress(address, hostport);		
		}
		sockBuf.free();
		return socketAddress;
	}	
	
	public int destroyEp(RdmaCmId id) throws IOException {
		logger.info("destroyEp, id " + id.getPs());
		NatCmaIdPrivate idImpl = (NatCmaIdPrivate) id;
		if (!idImpl.isOpen()) {
			throw new IOException("Trying to destroy an endpoint with closed ID");
		}
		int ret = nativeDispatcher._destroyEp(idImpl.getObjId());
		return ret;
	}
}
