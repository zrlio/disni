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

import java.util.LinkedList;

// TODO: Auto-generated Javadoc
//struct ibv_send_wr {
//    uint64_t                wr_id;
//    struct ibv_send_wr     *next;
//    struct ibv_sge         *sg_list;
//    int                     num_sge;
//    enum ibv_wr_opcode      opcode;
//    int                     send_flags;
//    uint32_t                imm_data;       /* in network byte order */
//    union {
//            struct {
//                    uint64_t        remote_addr;
//                    uint32_t        rkey;
//            } rdma;
//            struct {
//                    uint64_t        remote_addr;
//                    uint64_t        compare_add;
//                    uint64_t        swap;
//                    uint32_t        rkey;
//            } atomic;
//            struct {
//                    struct ibv_ah  *ah;
//                    uint32_t        remote_qpn;
//                    uint32_t        remote_qkey;
//            } ud;
//    } wr;
//};

/**
 * Represent a send work request. Applications create work request and post them onto queue pairs for execution.
 * 
 * Each work request is composed of several scatter/gather elements, each of which referring to one single buffer.
 */
public class IbvSendWR {
	public enum IbvWrOcode {
		IBV_WR_RDMA_WRITE, IBV_WR_RDMA_WRITE_WITH_IMM, IBV_WR_SEND, IBV_WR_SEND_WITH_IMM, IBV_WR_RDMA_READ, IBV_WR_ATOMIC_CMP_AND_SWP, IBV_WR_ATOMIC_FETCH_AND_ADD
	};
	
	public static final int IBV_WR_RDMA_WRITE = 0;
	public static final int IBV_WR_RDMA_WRITE_WITH_IMM = 1;
	public static final int IBV_WR_SEND = 2;
	public static final int IBV_WR_SEND_WITH_IMM = 3;
	public static final int IBV_WR_RDMA_READ = 4;
	public static final int IBV_WR_ATOMIC_CMP_AND_SWP = 5;
	public static final int IBV_WR_ATOMIC_FETCH_AND_ADD = 6;

	public static int IBV_SEND_FENCE = 1 << 0;
	public static int IBV_SEND_SIGNALED = 1 << 1;
	public static int IBV_SEND_SOLICITED = 1 << 2;
	public static int IBV_SEND_INLINE = 1 << 3;

	protected long wr_id;
	protected LinkedList<IbvSge> sg_list;
	protected int num_sge;
	protected int opcode;
	protected int send_flags;
	protected int imm_data; /* in network byte order */

	protected Rdma rdma;
	protected Atomic atomic;
	protected Ud ud;

	public IbvSendWR() {
		rdma = new Rdma();
		atomic = new Atomic();
		ud = new Ud();
		sg_list = new LinkedList<IbvSge>();
	}
	
	protected IbvSendWR(Rdma rdma, Atomic atomic, Ud ud, LinkedList<IbvSge> sg_list) {
		this.rdma = rdma;
		this.atomic = atomic;
		this.ud = ud;
		this.sg_list = sg_list;
	}	

	/**
	 * A unique identifier for this work request. A subsequent completion event will have a matching id. 
	 *
	 * @return the wr_id
	 */
	public long getWr_id() {
		return wr_id;
	}

	/**
	 * Allows setting the work request identifier. A subsequent completion event will have a matching id.
	 *
	 * @param wr_id the new wr_id
	 */
	public void setWr_id(long wr_id) {
		this.wr_id = wr_id;
	}

	/**
	 * Gets the scatter/gather elements of this work request. Each scatter/gather element refers to one single buffer.
	 *
	 * @return the sg_list
	 */
	public LinkedList<IbvSge> getSg_list() {
		return sg_list;
	}
	
	/**
	 * Gets a specific scatter/gather element of this work request.
	 *
	 * @return the sg element
	 */	
	public IbvSge getSge(int index){
		return sg_list.get(index);
	}	

	/**
	 * Sets the scatter/gather elements of this work request. Each scatter/gather element refers to one single buffer.
	 *
	 * @param sg_list the new sg_list
	 */
	public void setSg_list(LinkedList<IbvSge> sg_list) {
		this.sg_list.clear();
		this.sg_list.addAll(sg_list);
		this.num_sge = sg_list.size();
	}

	/**
	 * The number of scatter/gather elements in this work request.
	 *
	 * @return the num_sge
	 */
	public int getNum_sge() {
		return num_sge;
	}

	/**
	 * Unsupported.
	 *
	 * @param num_sge the new num_sge
	 */
	public void setNum_sge(int num_sge) {
		this.num_sge = num_sge;
	}

	/**
	 * Returns the opcode of this work request.
	 * 
	 * A opcode can be either IBV_WR_RDMA_WRITE, IBV_WR_SEND, or IBV_WR_RDMA_READ.
	 *
	 * @return the opcode
	 */
	public int getOpcode() {
		return opcode;
	}

	/**
	 * Returns the opcode of this work request.
	 * 
	 * A opcode can be either IBV_WR_RDMA_WRITE, IBV_WR_SEND, or IBV_WR_RDMA_READ.
	 *
	 * @param opcode the new opcode
	 */
	public void setOpcode(int opcode) {
		this.opcode = opcode;
	}

	/**
	 * Gets the send flags for this work request. Unsupported.
	 *
	 * @return the send_flags
	 */
	public int getSend_flags() {
		return send_flags;
	}

	/**
	 * Sets the send flags for this work request. Unsupported.
	 *
	 * @param send_flags the new send_flags
	 */
	public void setSend_flags(int send_flags) {
		this.send_flags = send_flags;
	}

	/**
	 * Unsupported.
	 *
	 * @return the imm_data
	 */
	public int getImm_data() {
		return imm_data;
	}

	/**
	 * Unsupported.
	 *
	 * @param imm_data the new imm_data
	 */
	public void setImm_data(int imm_data) {
		this.imm_data = imm_data;
	}

	/**
	 * Gets the RDMA section of of this work request.
	 * 
	 * The RDMA part is required for one sided-operations such as IBV_WR_RDMA_WRITE, or IBV_WR_RDMA_READ.
	 *
	 * @return the rdma
	 */
	public Rdma getRdma() {
		return rdma;
	}

	/**
	 * Unsupported. 
	 *
	 * @return the atomic
	 */
	public Atomic getAtomic() {
		return atomic;
	}

	/**
	 * Unsupported.
	 *
	 * @return the ud
	 */
	public Ud getUd() {
		return ud;
	}
	
	/**
	 * Specifies the remote buffer to be used in READ or WRITE operations. 
	 */
	public static class Rdma  {
		protected long remote_addr;
		protected int rkey;
		protected int reserved;

		public Rdma() {
		}

		/**
		 * Gets the address of the remote buffer;
		 *
		 * @return the remote_addr
		 */
		public long getRemote_addr() {
			return remote_addr;
		}

		/**
		 * Sets the address of the remote buffer;
		 *
		 * @param remote_addr the new remote_addr
		 */
		public void setRemote_addr(long remote_addr) {
			this.remote_addr = remote_addr;
		}

		/**
		 * Gets the key of the remote buffer;
		 *
		 * @return the rkey
		 */
		public int getRkey() {
			return rkey;
		}

		/**
		 * Sets the key of the remote buffer;
		 *
		 * @param rkey the new rkey
		 */
		public void setRkey(int rkey) {
			this.rkey = rkey;
		}

		/**
		 * Unsupported.
		 *
		 * @return the reserved
		 */
		public int getReserved() {
			return reserved;
		}

		/**
		 * Unsupported.
		 *
		 * @param reserved the new reserved
		 */
		public void setReserved(int reserved) {
			this.reserved = reserved;
		}

		public String getClassName() {
			return Rdma.class.getCanonicalName();
		}
	}

	/**
	 * Unsupported.
	 */
	public static class Atomic  {
		protected long remote_addr;
		protected long compare_add;
		protected long swap;
		protected int rkey;
		protected int reserved;

		public Atomic() {
		}

		public long getRemote_addr() {
			return remote_addr;
		}

		public void setRemote_addr(long remote_addr) {
			this.remote_addr = remote_addr;
		}

		public long getCompare_add() {
			return compare_add;
		}

		public void setCompare_add(long compare_add) {
			this.compare_add = compare_add;
		}

		public long getSwap() {
			return swap;
		}

		public void setSwap(long swap) {
			this.swap = swap;
		}

		public int getRkey() {
			return rkey;
		}

		public void setRkey(int rkey) {
			this.rkey = rkey;
		}

		public int getReserved() {
			return reserved;
		}

		public void setReserved(int reserved) {
			this.reserved = reserved;
		}

		public String getClassName() {
			return Atomic.class.getCanonicalName();
		}
	}

	/**
	 * Unsupported.
	 */
	public static class Ud  {
		protected int ah; // 24
		protected int remote_qpn; // 28
		protected int remote_qkey; // 32
		protected int reserved; // 36

		public Ud() {
		}

		public int getAh() {
			return ah;
		}

		public void setAh(int ah) {
			this.ah = ah;
		}

		public int getRemote_qpn() {
			return remote_qpn;
		}

		public void setRemote_qpn(int remote_qpn) {
			this.remote_qpn = remote_qpn;
		}

		public int getRemote_qkey() {
			return remote_qkey;
		}

		public void setRemote_qkey(int remote_qkey) {
			this.remote_qkey = remote_qkey;
		}

		public int getReserved() {
			return reserved;
		}

		public void setReserved(int reserved) {
			this.reserved = reserved;
		}

		public String getClassName() {
			return Ud.class.getCanonicalName();
		}
	}
	
}
