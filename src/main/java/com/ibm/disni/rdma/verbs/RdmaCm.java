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
import java.net.SocketAddress;

/**
 * The RDMA connection management (CM) interface.
 */
public abstract class RdmaCm {
	
	/** Unsupported. */
	public static final short RDMA_PS_IPOIB = 0x0002;
	
	
	/** Provides reliable, connection-oriented QP communication.  Unlike TCP, the RDMA port space provides message, not stream, based communication. */
	public static final short RDMA_PS_TCP = 0x0106;
	
	/** Unsupported */
	public static final short RDMA_PS_UDP = 0x0111;	
	
	/**
	 * Open the connection management (CM) interface.
	 *
	 * @return the actual CM interface.
	 * @throws Exception if no implementation exists. 
	 */
	public static RdmaCm open() throws IOException {
		return RdmaProvider.provider().openCm(); 
	} 

	/**
	 * Asynchronous events are reported to users through event channels.
	 * 
	 * Event channels are used to direct all events on an RdmaCmId. For many clients, a single event channel may be sufficient, however, when managing a large number of connections or id's. users may find it useful to direct events for different id's to different channels for processing.
	 * 
	 * All created event channels must be destroyed by calling destroyEventChannel. Users should call getCmEvent to retrieve events on an event channel.
	 *
	 * @return the rdma event channel
	 * @throws Exception the exception
	 */
	public abstract RdmaEventChannel createEventChannel() throws IOException;

	/**
	 * Creates an identifier that is used to track communication information.
	 * 
	 * RdmaCmId's are conceptually equivalent to a socket for RDMA communication.  The difference is that RDMA communication requires explicitly binding to a specified RDMA device before communication can occur, and most operations are asynchronous in nature.  Asynchronous communication events on an id are reported through the associated event channel.  
	 * 
	 * Users must release the id by calling destroyCmId.
	 *
	 * @param cmChannel the communication channel that events associated with the allocated RdmaCmId will be reported on.
	 * @param rdma_ps the RDMA port space. Only RDMA_PS_TCP supported.
	 * @return the newly created Id 
	 * @throws Exception on failure.
	 */
	public abstract RdmaCmId createId(RdmaEventChannel cmChannel, short rdma_ps) throws IOException;

	/**
	 * Allocate a QP associated with the specified RdmaCmId and transition it for sending and receiving.
	 * 
	 * The RdmaCmId must be bound to a local RDMA device before calling this function, and the protection domain must be for that same device.  QPs allocated to an id are automatically transitioned by the librdmacm through their states.  After being allocated, the QP will be ready to handle posting of receives. If the QP is unconnected, it will be ready to post sends.
	 *
	 * @param id the RDMA identifier.
	 * @param pd the protection domain. 
	 * @param attr the attr.
	 * @return the actual QP.
	 * @throws Exception on failure.
	 */
	public abstract IbvQP createQP(RdmaCmId id, IbvPd pd,
			IbvQPInitAttr attr) throws IOException;

	/**
	 * Associates a source address with an RdmaCmId.
	 *
	 * The address may be wildcarded. If binding to a specific local address, the RdmaCmId will also be bound to a local RDMA device.
	 * 
	 * @param id the RDMA identifier.
	 * @param addr local address information.
	 * @return returns 0 on success.
	 * @throws Exception on failure.
	 */
	public abstract int bindAddr(RdmaCmId id, SocketAddress addr)
			throws IOException;

	/**
	 * Initiates a listen for incoming connection requests.
	 * 
	 * The listen will be restricted to the locally bound source address.
	 *
	 * @param id the RDMA identifier. 
	 * @param backlog the backlog of listen call. 
	 * @return returns 0 on success. 
	 * @throws Exception on failure. 
	 */
	public abstract int listen(RdmaCmId id, int backlog)
			throws IOException;

	/**
	 * Resolve destination and optional source addresses from IP addresses to an RDMA address.
	 * 
	 * If successful, the specified RdmaCmId will be bound to a local device.
	 *
	 * @param id the RDMA identifier.
	 * @param src source address information. 
	 * @param dst destination address information
	 * @param timeout time to wait for resolution to complete.
	 * @return returns 0 on success. 
	 * @throws Exception on failure.
	 */
	public abstract int resolveAddr(RdmaCmId id, SocketAddress src,
			SocketAddress dst, int timeout) throws IOException;

	/**
	 * Resolves an RDMA route to the destination address in order to establish a connection.
	 * 
	 * The destination address must have already been resolved by calling resolveAddr.
	 * 
	 * @param id the RDMA identifier.
	 * @param timeout time to wait for resolution to complete.
	 * @return returns 0 on success. 
	 * @throws Exception on failure.
	 */
	public abstract int resolveRoute(RdmaCmId id, int timeout)
			throws IOException;

	/**
	 * Retrieves a communication event. If no events are pending, by default, the call will block until an event is received.
	 * 
	 * All events that are reported must be acknowledged by calling ackCmEvent.
	 *
	 * @param cmChannel event channel to check for events.
	 * @return information about the next communication event.
	 * @throws Exception on failure.
	 */
	public abstract RdmaCmEvent getCmEvent(RdmaEventChannel cmChannel, int timeout)
			throws IOException;

	/**
	 * For an RdmaCmId of type RDMA_PS_TCP, this call initiates a connection request to a remote destination.
	 * 
	 * Users must have resolved a route to the destination address by having called resolveRoute before calling this method.
	 *  
	 * The following properties are used to configure the communication and specified by the connParam parameter when connecting.
	 * 
	 * <p>
	 * 
	 * private_data: a user-controlled data buffer. The contents of the buffer are copied and transparently passed to the remote side as part of the communication request.
	 * 
	 * <p>
	 * 
	 * private_data_len: Specifies the size of the user-controlled data buffer
	 * 
	 * <p>
	 * 
	 * initiator_depth: The maximum number of outstanding RDMA read and atomic operations that the local side will have to the remote side.
	 * 
	 * <p>
	 * 
	 * flow_control: Specifies if hardware flow control is available. This value is exchanged with the remote peer and is not used to configure the QP. Applies only to RDMA_PS_TCP.
	 *
	 * @param id the RDMA identifier. 
	 * @param connParam connection parameters. See above for details.
	 * @return returns 0 on success. 
	 * @throws Exception on failure.
	 */
	public abstract int connect(RdmaCmId id, RdmaConnParam connParam)
			throws IOException;

	/**
	 * Called from the listening side to accept a connection.
	 * 
	 * The following properties are used to configure the communication and specified by the connParam parameter when connecting.
	 * 
	 * <p>
	 * 
	 * private_data: a user-controlled data buffer. The contents of the buffer are copied and transparently passed to the remote side as part of the communication request.
	 * 
	 * <p>
	 * 
	 * private_data_len: Specifies the size of the user-controlled data buffer
	 * 
	 * <p>
	 * 
	 * initiator_depth: The maximum number of outstanding RDMA read and atomic operations that the local side will have to the remote side.
	 * 
	 * <p>
	 * 
	 * flow_control: Specifies if hardware flow control is available. This value is exchanged with the remote peer and is not used to configure the QP. Applies only to RDMA_PS_TCP.
	 *
	 *
	 * @param id the connection identifier associated with the request
	 * @param connParam Information needed to establish the connection. See above for details.
	 * @return the int
	 * @throws Exception the exception
	 */
	public abstract int accept(RdmaCmId id, RdmaConnParam connParam)
			throws IOException;

	/**
	 * All events which are allocated by getCmEvent must be released, there should be a one-to-one correspondence between successful gets and acks.
	 *
	 * @param cmEvent the event to be released.
	 * @return return 0 on success, 1 on failure.
	 */
	public abstract int ackCmEvent(RdmaCmEvent cmEvent);

	/**
	 * Disconnects a connection and transitions any associated QP to the error state.
	 * 
	 * Flushes any posted work requests to the completion queue.  This routine may be called by both the client and server side of a connection.  After successfully disconnecting, an RDMA_CM_EVENT_DISCONNECTED event will be generated on both sides of the connection.
	 *
	 * @param id the RDMA identifier.
	 * @return returns 0 on success.
	 * @throws Exception on failure. 
	 */
	public abstract int disconnect(RdmaCmId id) throws IOException;

	/**
	 * Release all resources associated with an event channel.
	 *
	 * @param cmChannel The communication channel to destroy.
	 * @return returns 0 on success.
	 * @throws Exception on failure.
	 */
	public abstract int destroyEventChannel(RdmaEventChannel cmChannel) throws IOException;
	
	/**
	 * Destroys the specified RdmaCmId and cancels any outstanding asynchronous operation.
	 *
	 * @param id the RDMA identifier to be destroyed. 
	 * @return return 0 on success. 
	 * @throws Exception on failure. 
	 */
	public abstract int destroyCmId(RdmaCmId id) throws IOException;	
	
	/**
	 * Destroy a QP allocated on the RdmaCmId.
	 *
	 * @param id the RDMA identifier.
	 * @return returns 0 on success. 
	 * @throws Exception on failure. 
	 */
	public abstract int destroyQP(RdmaCmId id) throws IOException;
	
	/**
	 * Returns the local IP address for an RdmaCmId that has been bound to a local device.
	 *
	 * @param id the RDMA identifier. 
	 * @return the local IP address for an RdmaCmId that has been bound to a local device.
	 * @throws Exception on failure.
	 */
	public abstract SocketAddress getSrcAddr(RdmaCmId id) throws IOException;
	
	/**
	 * Returns the remote IP address associated with an RdmaCmId.
	 *
	 * @param id the RDMA identifier. 
	 * @return the SocketAddress address of the connected peer. If the RdmaCmId is not connected the call returns NULL.
	 * @throws Exception on failure.
	 */
	public abstract SocketAddress getDstAddr(RdmaCmId id) throws IOException;
	
	/**
	 * Destroys the specified RdmaCmId and all associated resources.
	 *
	 * @param id the RDMA identifier
	 * @return return 0 on success
	 * @throws Exception on failure
	 */
	public abstract int destroyEp(RdmaCmId id) throws IOException;
	
}
