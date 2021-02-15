/*
 * DiSNI: Direct Storage and Networking Interface
 *
 * Author: Konstantin Taranov <ktaranov@inf.ethz.ch>
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
import java.nio.ByteBuffer;

//struct ibv_device_attr {
//               char                    fw_ver[64];             /* FW version */
//               uint64_t                node_guid;              /* Node GUID (in network byte order) */
//               uint64_t                sys_image_guid;         /* System image GUID (in network byte order) */
//               uint64_t                max_mr_size;            /* Largest contiguous block that can be registered */
//               uint64_t                page_size_cap;          /* Supported memory shift sizes */
//               uint32_t                vendor_id;              /* Vendor ID, per IEEE */
//               uint32_t                vendor_part_id;         /* Vendor supplied part ID */
//               uint32_t                hw_ver;                 /* Hardware version */
//               int                     max_qp;                 /* Maximum number of supported QPs */
//               int                     max_qp_wr;              /* Maximum number of outstanding WR on any work queue */
//               unsigned int            device_cap_flags;       /* HCA capabilities mask */
//               int                     max_sge;                /* Maximum number of s/g per WR for SQ & RQ of QP for non RDMA Read operations */
//               int                     max_sge_rd;             /* Maximum number of s/g per WR for RDMA Read operations */
//               int                     max_cq;                 /* Maximum number of supported CQs */
//               int                     max_cqe;                /* Maximum number of CQE capacity per CQ */
//               int                     max_mr;                 /* Maximum number of supported MRs */
//               int                     max_pd;                 /* Maximum number of supported PDs */
//               int                     max_qp_rd_atom;         /* Maximum number of RDMA Read & Atomic operations that can be outstanding per QP */
//               int                     max_ee_rd_atom;         /* Maximum number of RDMA Read & Atomic operations that can be outstanding per EEC */
//               int                     max_res_rd_atom;        /* Maximum number of resources used for RDMA Read & Atomic operations by this HCA as the Target */
//               int                     max_qp_init_rd_atom;    /* Maximum depth per QP for initiation of RDMA Read & Atomic operations */
//               int                     max_ee_init_rd_atom;    /* Maximum depth per EEC for initiation of RDMA Read & Atomic operations */
//               enum ibv_atomic_cap     atomic_cap;             /* Atomic operations support level */
//               int                     max_ee;                 /* Maximum number of supported EE contexts */
//               int                     max_rdd;                /* Maximum number of supported RD domains */
//               int                     max_mw;                 /* Maximum number of supported MWs */
//               int                     max_raw_ipv6_qp;        /* Maximum number of supported raw IPv6 datagram QPs */
//               int                     max_raw_ethy_qp;        /* Maximum number of supported Ethertype datagram QPs */
//               int                     max_mcast_grp;          /* Maximum number of supported multicast groups */
//               int                     max_mcast_qp_attach;    /* Maximum number of QPs per multicast group which can be attached */
//               int                     max_total_mcast_qp_attach;/* Maximum number of QPs which can be attached to multicast groups */
//               int                     max_ah;                 /* Maximum number of supported address handles */
//               int                     max_fmr;                /* Maximum number of supported FMRs */
//               int                     max_map_per_fmr;        /* Maximum number of (re)maps per FMR before an unmap operation in required */
//               int                     max_srq;                /* Maximum number of supported SRQs */
//               int                     max_srq_wr;             /* Maximum number of WRs per SRQ */
//               int                     max_srq_sge;            /* Maximum number of s/g per SRQ */
//               uint16_t                max_pkeys;              /* Maximum number of partitions */
//               uint8_t                 local_ca_ack_delay;     /* Local CA ack delay */
//               uint8_t                 phys_port_cnt;          /* Number of physical ports */
//};


/**
 * RDMA device's attributes
 */
public class IbvDeviceAttr {
	protected byte fw_ver[];
	protected long node_guid;
	protected long sys_image_guid;
	protected long max_mr_size;
	protected long page_size_cap;
	protected int vendor_id;
	protected int vendor_part_id;
	protected int hw_ver;
	protected int max_qp;
	protected int max_qp_wr;
	protected int device_cap_flags;
	protected int max_sge;
	protected int max_sge_rd;
	protected int max_cq;
	protected int max_cqe;
	protected int max_mr;
	protected int max_pd;
	protected int max_qp_rd_atom;
	protected int max_ee_rd_atom;
	protected int max_res_rd_atom;
	protected int max_qp_init_rd_atom;
	protected int max_ee_init_rd_atom;
	protected int atomic_cap;// not supported 
	protected int max_ee;
	protected int max_rdd;
	protected int max_mw;
	protected int max_raw_ipv6_qp;
	protected int max_raw_ethy_qp;
	protected int max_mcast_grp;
	protected int max_mcast_qp_attach;
	protected int max_total_mcast_qp_attach;
	protected int max_ah;
	protected int max_fmr;
	protected int max_map_per_fmr;
	protected int max_srq;
	protected int max_srq_wr;
	protected int max_srq_sge;
	protected short max_pkeys;
	protected byte local_ca_ack_delay;
	protected byte phys_port_cnt;
	protected int reserved;

	public static int CSIZE = 232;

	public IbvDeviceAttr(){
		this.fw_ver = new byte[64];
		this.node_guid = 0;
		this.sys_image_guid = 0;
		this.max_mr_size = 0;
		this.page_size_cap = 0;
		this.vendor_id = 0;
		this.vendor_part_id = 0;
		this.hw_ver = 0;
		this.max_qp = 0;
		this.max_qp_wr = 0;
		this.device_cap_flags = 0;
		this.max_sge = 0;
		this.max_sge_rd = 0;
		this.max_cq = 0;
		this.max_cqe = 0;
		this.max_mr = 0;
		this.max_pd = 0;
		this.max_qp_rd_atom = 0;
		this.max_ee_rd_atom = 0;
		this.max_res_rd_atom = 0;
		this.max_qp_init_rd_atom = 0;
		this.max_ee_init_rd_atom = 0;
		this.atomic_cap = 0; 
		this.max_ee = 0;
		this.max_rdd = 0;
		this.max_mw = 0;
		this.max_raw_ipv6_qp = 0;
		this.max_raw_ethy_qp = 0;
		this.max_mcast_grp = 0;
		this.max_mcast_qp_attach = 0;
		this.max_total_mcast_qp_attach = 0;
		this.max_ah = 0;
		this.max_fmr = 0;
		this.max_map_per_fmr = 0;
		this.max_srq = 0;
		this.max_srq_wr = 0;
		this.max_srq_sge = 0;
		this.max_pkeys = 0;
		this.local_ca_ack_delay = 0;
		this.phys_port_cnt = 0;
		this.reserved = 0;
	}
 
	/**
	 * Gets the fw_ver.
	 *
	 * @return the fw_ver
	 */
	public byte[] getFw_ver() {
		return fw_ver;
	}

	/**
	 * Gets the node_guid.
	 *
	 * @return the node_guid
	 */
	public long getNode_guid() {
		return node_guid;
	}

	/**
	 * Gets the sys_image_guid.
	 *
	 * @return the sys_image_guid
	 */
	public long getSys_image_guid() {
		return sys_image_guid;
	}

 	/**
	 * Gets the max_mr_size.
	 *
	 * @return the max_mr_size
	 */
	public long getMax_mr_size() {
		return max_mr_size;
	}
	
	/**
	 * Gets the page_size_cap.
	 *
	 * @return the page_size_cap
	 */
	public long getPage_size_cap() {
		return page_size_cap;
	}
	
	/**
	 * Gets the vendor_id.
	 *
	 * @return the vendor_id
	 */
	public int getVendor_id() {
		return vendor_id;
	}

	/**
	 * Gets the vendor_part_id.
	 *
	 * @return the vendor_part_id
	 */
	public int getVendor_part_id() {
		return vendor_part_id;
	}

	/**
	 * Gets the hw_ver.
	 *
	 * @return the hw_ver
	 */
	public int getHw_ver() {
		return hw_ver;
	}

	/**
	 * Gets the max_qp.
	 *
	 * @return the max_qp
	 */
	public int getMax_qp() {
		return max_qp;
	} 

	/**
	 * Gets the max_qp_wr.
	 *
	 * @return the max_qp_wr
	 */
	public int getMax_qp_wr() {
		return max_qp_wr;
	} 

	/**
	 * Gets the device_cap_flags.
	 *
	 * @return the device_cap_flags
	 */
	public int getDevice_cap_flags() {
		return device_cap_flags;
	} 

	/**
	 * Gets the max_sge.
	 *
	 * @return the max_sge
	 */
	public int getMax_sge() {
		return max_sge;
	} 

	/**
	 * Gets the max_sge_rd.
	 *
	 * @return the max_sge_rd
	 */
	public int getMax_sge_rd() {
		return max_sge_rd;
	}


	/**
	 * Gets the max_cq.
	 *
	 * @return the max_cq
	 */
	public int getMax_cq() {
		return max_cq;
	}

	/**
	 * Gets the max_cqe.
	 *
	 * @return the max_cqe
	 */
	public int getMax_cqe() {
		return max_cqe;
	}

	/**
	 * Gets the max_mr.
	 *
	 * @return the max_mr
	 */
	public int getMax_mr() {
		return max_mr;
	}

	/**
	 * Gets the max_pd.
	 *
	 * @return the max_pd
	 */
	public int getMax_pd() {
		return max_pd;
	}

	/**
	 * Gets the max_qp_rd_atom.
	 *
	 * @return the max_qp_rd_atom
	 */
	public int getMax_qp_rd_atom() {
		return max_qp_rd_atom;
	}

	/**
	 * Gets the max_ee_rd_atom.
	 *
	 * @return the max_ee_rd_atom
	 */
	public int getMax_ee_rd_atom() {
		return max_ee_rd_atom;
	}

	/**
	 * Gets the max_res_rd_atom.
	 *
	 * @return the max_res_rd_atom
	 */
	public int getMax_res_rd_atom() {
		return max_res_rd_atom;
	}

	/**
	 * Gets the max_qp_init_rd_atom.
	 *
	 * @return the max_qp_init_rd_atom
	 */
	public int getMax_qp_init_rd_atom() {
		return max_qp_init_rd_atom;
	}


	/**
	 * Gets the max_ee_init_rd_atom.
	 *
	 * @return the max_ee_init_rd_atom
	 */
	public int getMax_ee_init_rd_atom() {
		return max_ee_init_rd_atom;
	}

	/**
	 * Gets the atomic_cap.
	 *
	 * @return the atomic_cap
	 */
	public int getAtomic_cap() {
		return atomic_cap;
	}

	/**
	 * Gets the max_ee.
	 *
	 * @return the max_ee
	 */
	public int getMax_ee() {
		return max_ee;
	}

	/**
	 * Gets the max_rdd.
	 *
	 * @return the max_rdd
	 */
	public int getMax_rdd() {
		return max_rdd;
	}

	/**
	 * Gets the max_mw.
	 *
	 * @return the max_mw
	 */
	public int getMax_mw() {
		return max_mw;
	}

	/**
	 * Gets the max_raw_ipv6_qp.
	 *
	 * @return the max_raw_ipv6_qp
	 */
	public int getMax_raw_ipv6_qp() {
		return max_raw_ipv6_qp;
	}

	/**
	 * Gets the max_raw_ethy_qp.
	 *
	 * @return the max_raw_ethy_qp
	 */
	public int getMax_raw_ethy_qp() {
		return max_raw_ethy_qp;
	}

	/**
	 * Gets the max_mcast_grp.
	 *
	 * @return the max_mcast_grp
	 */
	public int getMax_mcast_grp() {
		return max_mcast_grp;
	} 

	/**
	 * Gets the max_mcast_qp_attach.
	 *
	 * @return the max_mcast_qp_attach
	 */
	public int getMax_mcast_qp_attach() {
		return max_mcast_qp_attach;
	}

	/**
	 * Gets the max_total_mcast_qp_attach.
	 *
	 * @return the max_total_mcast_qp_attach
	 */
	public int getMax_total_mcast_qp_attach() {
		return max_total_mcast_qp_attach;
	}
 
	/**
	 * Gets the max_ah.
	 *
	 * @return the max_ah
	 */
	public int getMax_ah() {
		return max_ah;
	}

	/**
	 * Gets the max_fmr.
	 *
	 * @return the max_fmr
	 */
	public int getMax_fmr() {
		return max_fmr;
	}

	/**
	 * Gets the max_map_per_fmr.
	 *
	 * @return the max_map_per_fmr
	 */
	public int getMax_map_per_fmr() {
		return max_map_per_fmr;
	}

	/**
	 * Gets the max_srq.
	 *
	 * @return the max_srq
	 */
	public int getMax_srq() {
		return max_srq;
	}

	/**
	 * Gets the max_srq_wr.
	 *
	 * @return the max_srq_wr
	 */
	public int getMax_srq_wr() {
		return max_srq_wr;
	}

	/**
	 * Gets the max_srq_sge.
	 *
	 * @return the max_srq_sge
	 */
	public int getMax_srq_sge() {
		return max_srq_sge;
	}

	/**
	 * Gets the max_pkeys.
	 *
	 * @return the max_pkeys
	 */
	public short getMax_pkeys() {
		return max_pkeys;
	}

	/**
	 * Gets the local_ca_ack_delay.
	 *
	 * @return the local_ca_ack_delay
	 */
	public byte getLocal_ca_ack_delay() {
		return local_ca_ack_delay;
	}

	/**
	 * Gets the local_ca_ack_delay.
	 *
	 * @return the local_ca_ack_delay
	 */
	public byte getPhys_port_cnt() {
		return phys_port_cnt;
	}

	/**
	 * Gets the reserved.
	 *
	 * @return the reserved
	 */
	public int getReserved() {
		return reserved;
	}
 
	public void update(ByteBuffer buffer) {
		buffer.get(this.fw_ver);
		this.node_guid = buffer.getLong();
		this.sys_image_guid = buffer.getLong();
		this.max_mr_size = buffer.getLong();
		this.page_size_cap = buffer.getLong();
		this.vendor_id = buffer.getInt();
		this.vendor_part_id = buffer.getInt();
		this.hw_ver = buffer.getInt();
		this.max_qp = buffer.getInt();
		this.max_qp_wr = buffer.getInt();
		this.device_cap_flags = buffer.getInt();
		this.max_sge = buffer.getInt();
		this.max_sge_rd = buffer.getInt();
		this.max_cq = buffer.getInt();
		this.max_cqe = buffer.getInt();
		this.max_mr = buffer.getInt();
		this.max_pd = buffer.getInt();
		this.max_qp_rd_atom = buffer.getInt();
		this.max_ee_rd_atom = buffer.getInt();
		this.max_res_rd_atom = buffer.getInt();
		this.max_qp_init_rd_atom = buffer.getInt();
		this.max_ee_init_rd_atom = buffer.getInt();
		this.atomic_cap = buffer.getInt();
		this.max_ee = buffer.getInt();
		this.max_rdd = buffer.getInt();
		this.max_mw = buffer.getInt();
		this.max_raw_ipv6_qp = buffer.getInt();
		this.max_raw_ethy_qp = buffer.getInt();
		this.max_mcast_grp = buffer.getInt();
		this.max_mcast_qp_attach = buffer.getInt();
		this.max_total_mcast_qp_attach = buffer.getInt();
		this.max_ah = buffer.getInt();
		this.max_fmr = buffer.getInt();
		this.max_map_per_fmr =buffer.getInt();
		this.max_srq = buffer.getInt();
		this.max_srq_wr = buffer.getInt();
		this.max_srq_sge = buffer.getInt();
		this.max_pkeys = buffer.getShort();
		this.local_ca_ack_delay = buffer.get();
		this.phys_port_cnt = buffer.get();
		this.reserved = buffer.getInt();
	}

	public int size() {
		return CSIZE;
	}
}
