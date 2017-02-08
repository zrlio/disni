/*
 * DiSNI: Direct Storage and Networking Interface
 *
 * Author: Jonas Pfefferle <jpf@zurich.ibm.com>
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

package com.ibm.disni.nvmef.spdk;

public enum NvmeGenericCommandStatusCode {
	SUCCESS(0x00),
	INVALID_OPCODE(0x01),
	INVALID_FIELD(0x02),
	COMMAND_ID_CONFLICT(0x03),
	DATA_TRANSFER_ERROR(0x04),
	ABORTED_POWER_LOSS(0x05),
	INTERNAL_DEVICE_ERROR(0x06),
	ABORTED_BY_REQUEST(0x07),
	ABORTED_SQ_DELETION(0x08),
	ABORTED_FAILED_FUSED(0x09),
	ABORTED_MISSING_FUSED(0x0a),
	INVALID_NAMESPACE_OR_FORMAT(0x0b),
	COMMAND_SEQUENCE_ERROR(0x0c),
	INVALID_SGL_SEG_DESCRIPTOR(0x0d),
	INVALID_NUM_SGL_DESCIRPTORS(0x0e),
	DATA_SGL_LENGTH_INVALID(0x0f),
	METADATA_SGL_LENGTH_INVALID(0x10),
	SGL_DESCRIPTOR_TYPE_INVALID(0x11),
	INVALID_CONTROLLER_MEM_BUF(0x12),
	INVALID_PRP_OFFSET(0x13),
	ATOMIC_WRITE_UNIT_EXCEEDED(0x14),
	INVALID_SGL_OFFSET(0x16),
	INVALID_SGL_SUBTYPE(0x17),
	HOSTID_INCONSISTENT_FORMAT(0x18),
	KEEP_ALIVE_EXPIRED(0x19),
	KEEP_ALIVE_INVALID(0x1a),
	LBA_OUT_OF_RANGE(0x80),
	CAPACITY_EXCEEDED(0x81),
	NAMESPACE_NOT_READY(0x82),
	RESERVATION_CONFLICT(0x83),
	FORMAT_IN_PROGRESS(0x84);

	private int numVal;

	NvmeGenericCommandStatusCode(int numVal) { this.numVal = numVal; }

	public int getNumVal() { return numVal; }

	public static NvmeGenericCommandStatusCode valueOf(int numVal) {
		for (NvmeGenericCommandStatusCode statusCode : NvmeGenericCommandStatusCode.values()) {
			if (statusCode.getNumVal() == numVal) {
				return statusCode;
			}
		}
		throw new IllegalArgumentException();
	}
}
