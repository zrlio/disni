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


// TODO: Auto-generated Javadoc
//enum rdma_cm_event_type {
//        RDMA_CM_EVENT_ADDR_RESOLVED,
//        RDMA_CM_EVENT_ADDR_ERROR,
//        RDMA_CM_EVENT_ROUTE_RESOLVED,
//        RDMA_CM_EVENT_ROUTE_ERROR,
//        RDMA_CM_EVENT_CONNECT_REQUEST,
//        RDMA_CM_EVENT_CONNECT_RESPONSE,
//        RDMA_CM_EVENT_CONNECT_ERROR,
//        RDMA_CM_EVENT_UNREACHABLE,
//        RDMA_CM_EVENT_REJECTED,
//        RDMA_CM_EVENT_ESTABLISHED,
//        RDMA_CM_EVENT_DISCONNECTED,
//        RDMA_CM_EVENT_DEVICE_REMOVAL,
//        RDMA_CM_EVENT_MULTICAST_JOIN,
//        RDMA_CM_EVENT_MULTICAST_ERROR,
//        RDMA_CM_EVENT_ADDR_CHANGE,
//        RDMA_CM_EVENT_TIMEWAIT_EXIT
//};

//struct rdma_cm_event {
//        struct rdma_cm_id       *id;
//        struct rdma_cm_id       *listen_id;
//        enum rdma_cm_event_type  event;
//        int                      status;
//        union {
//                struct rdma_conn_param conn;
//                struct rdma_ud_param   ud;
//        } param;
//};

/**
 * Represents a communication event
 */
public class RdmaCmEvent {
	private RdmaCm cm;
	
	/**
	 * Different CM event types
	 */
	public enum EventType {
		RDMA_CM_EVENT_ADDR_RESOLVED, RDMA_CM_EVENT_ADDR_ERROR, RDMA_CM_EVENT_ROUTE_RESOLVED, RDMA_CM_EVENT_ROUTE_ERROR, RDMA_CM_EVENT_CONNECT_REQUEST, RDMA_CM_EVENT_CONNECT_RESPONSE, RDMA_CM_EVENT_CONNECT_ERROR, RDMA_CM_EVENT_UNREACHABLE, RDMA_CM_EVENT_REJECTED, RDMA_CM_EVENT_ESTABLISHED, RDMA_CM_EVENT_DISCONNECTED, RDMA_CM_EVENT_DEVICE_REMOVAL, RDMA_CM_EVENT_MULTICAST_JOIN, RDMA_CM_EVENT_MULTICAST_ERROR, RDMA_CM_EVENT_ADDR_CHANGE, RDMA_CM_EVENT_TIMEWAIT_EXIT
	};

	protected int event;
	protected int status;
	protected RdmaCmId listenIdPriv;
	protected RdmaCmId connIdPriv;
	protected RdmaConnParam conn;

	public RdmaCmEvent(int event, RdmaCmId listenId, RdmaCmId clientId) throws IOException {
		this.cm = RdmaCm.open();
		this.event = event;
		this.status = -1;
		this.listenIdPriv = listenId;
		this.connIdPriv = clientId;
		this.conn = new RdmaConnParam();		
	}

	/**
	 * Gets the type of the event
	 *
	 * @return the type of the event.
	 */
	public int getEvent() {
		return event;
	}

	/**
	 * Gets the status of the vent
	 *
	 * @return the status
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Gets the listen RdmaCmId for this event.
	 *
	 * @return the RDMA identifier of the listening connection
	 */
	public RdmaCmId getListenIdPriv() {
		return listenIdPriv;
	}

	/**
	 * Gets the connection RdmaCmId for this event.
	 *
	 * @return the RDMA identifier of the connection
	 */
	public RdmaCmId getConnIdPriv() {
		return connIdPriv;
	}

	/**
	 * Gets the connection parameters for this event.
	 *
	 * @return the connectin parameters
	 */
	public RdmaConnParam getConn() {
		return conn;
	}
	
	//---------- oo-verbs
	
	public void ackEvent() throws IOException{
		cm.ackCmEvent(this);
	}
}
