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

package com.ibm.disni.verbs;

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
	public interface SendWRMod {
		
		/**
		 * Modify the work-request id 
		 *
		 * @param wr_id the new wr_id
		 */
		void setWr_id(long wr_id);
		
		/**
		 * Modify the send flags 
		 *
		 * @param send_flags new flags
		 */		
		void setSend_flags(int send_flags);
		
		/**
		 * Retrieve the current work-request id 
		 *
		 * @return the wr_id
		 */
		long getWr_id();
		
		/**
		 * The number of scatter/gather elements in this work request. 
		 *
		 * @return the num_sge
		 */
		int getNum_sge();
		
		/**
		 * Retrieve the opcode of this work request 
		 *
		 * @return the opcode
		 */
		int getOpcode();
		
		/**
		 * Retrieve the send flags of this work request
		 *
		 * @return the send_flags
		 */
		int getSend_flags();
		
		/**
		 * Provides access to the rdma information in this work request
		 *
		 * @return RDMA information
		 */
		RdmaMod getRdmaMod();
		
		/**
		 * Returns a specific scatter/gather element of this work request.
		 *
		 * @param index the sg element.
		 * @return the sg element.
		 */
		SgeMod getSgeMod(int index);
	}
	
	/**
	 * Provides access methods to modify the RDMA information of a work request.
	 */
	public interface RdmaMod {
		
		/**
		 * Sets the remote_addr.
		 *
		 * @param remote_addr the new remote_addr
		 */
		void setRemote_addr(long remote_addr);
		
		/**
		 * Gets the remote_addr.
		 *
		 * @return the remote_addr
		 */
		long getRemote_addr();
		
		/**
		 * Sets the rkey.
		 *
		 * @param rkey the new rkey
		 */
		void setRkey(int rkey);
		
		/**
		 * Gets the rkey.
		 *
		 * @return the rkey
		 */
		int getRkey();
		
		/**
		 * Sets the reserved flag.
		 *
		 * @param reserved the new reserved flag.
		 */
		void setReserved(int reserved);
		
		/**
		 * Gets the reserved flag.
		 *
		 * @return the reserved flag.
		 */
		int getReserved();
	}
	
	/**
	 * Provides access methods to modify a given scatter/gather element.
	 */
	public interface SgeMod {
		
		/**
		 * Sets the addr.
		 *
		 * @param addr the new addr
		 */
		void setAddr(long addr);
		
		/**
		 * Gets the addr.
		 *
		 * @return the addr
		 */
		long getAddr();
		
		/**
		 * Sets the length.
		 *
		 * @param length the new length
		 */
		void setLength(int length);
		
		/**
		 * Gets the length.
		 *
		 * @return the length
		 */
		int getLength();
		
		/**
		 * Sets the lkey.
		 *
		 * @param lkey the new lkey
		 */
		void setLkey(int lkey);
		
		/**
		 * Gets the lkey.
		 *
		 * @return the lkey
		 */
		int getLkey();
	}
}
