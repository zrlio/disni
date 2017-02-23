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

/**
 * The Class SVCPostSend.
 * 
 * This class is a stateful representation of the post-send verb call (RdmaVerbs.postSend). 
 */
public abstract class SVCPostSend implements StatefulVerbCall<SVCPostSend> {
	
	/**
	 * Access a specific work request of this SVC object.
	 *
	 * @param index the work request
	 * @return the work request
	 * @throws Exception in case the index is out-of-bound.
	 */
	public abstract SendWRMod getWrMod(int index) throws IOException;
	
	/**
	 * Provides access methods to modify a given work-request belonging to this SVC object.
	 */
	public static interface SendWRMod {
		
		/**
		 * Modify the work-request id 
		 *
		 * @param wr_id the new wr_id
		 */
		public void setWr_id(long wr_id);
		
		/**
		 * Modify the send flags 
		 *
		 * @param send_flags new flags
		 */		
		public void setSend_flags(int send_flags);		
		
		/**
		 * Retrieve the current work-request id 
		 *
		 * @return the wr_id
		 */
		public long getWr_id();
		
		/**
		 * The number of scatter/gather elements in this work request. 
		 *
		 * @return the num_sge
		 */
		public int getNum_sge();
		
		/**
		 * Retrieve the opcode of this work request 
		 *
		 * @return the opcode
		 */
		public int getOpcode();
		
		/**
		 * Retrieve the send flags of this work request
		 *
		 * @return the send_flags
		 */
		public int getSend_flags();
		
		/**
		 * Provides access to the rdma information in this work request
		 *
		 * @return RDMA information
		 */
		public RdmaMod getRdmaMod();
		
		/**
		 * Returns a specific scatter/gather element of this work request.
		 *
		 * @param index the sg element.
		 * @return the sg element.
		 */
		public SgeMod getSgeMod(int index);
	}
	
	/**
	 * Provides access methods to modify the RDMA information of a work request.
	 */
	public static interface RdmaMod {
		
		/**
		 * Sets the remote_addr.
		 *
		 * @param remote_addr the new remote_addr
		 */
		public void setRemote_addr(long remote_addr);
		
		/**
		 * Gets the remote_addr.
		 *
		 * @return the remote_addr
		 */
		public long getRemote_addr();
		
		/**
		 * Sets the rkey.
		 *
		 * @param rkey the new rkey
		 */
		public void setRkey(int rkey);
		
		/**
		 * Gets the rkey.
		 *
		 * @return the rkey
		 */
		public int getRkey();
		
		/**
		 * Sets the reserved flag.
		 *
		 * @param reserved the new reserved flag.
		 */
		public void setReserved(int reserved);
		
		/**
		 * Gets the reserved flag.
		 *
		 * @return the reserved flag.
		 */
		public int getReserved();
	}
	
	/**
	 * Provides access methods to modify a given scatter/gather element.
	 */
	public static interface SgeMod {
		
		/**
		 * Sets the addr.
		 *
		 * @param addr the new addr
		 */
		public void setAddr(long addr);
		
		/**
		 * Gets the addr.
		 *
		 * @return the addr
		 */
		public long getAddr();
		
		/**
		 * Sets the length.
		 *
		 * @param length the new length
		 */
		public void setLength(int length);
		
		/**
		 * Gets the length.
		 *
		 * @return the length
		 */
		public int getLength();
		
		/**
		 * Sets the lkey.
		 *
		 * @param lkey the new lkey
		 */
		public void setLkey(int lkey);
		
		/**
		 * Gets the lkey.
		 *
		 * @return the lkey
		 */
		public int getLkey();
	}
}
