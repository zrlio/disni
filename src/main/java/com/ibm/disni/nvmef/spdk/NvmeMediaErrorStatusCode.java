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


public enum NvmeMediaErrorStatusCode {
	WRITE_FAULTS(0x80),
	UNRECOVERED_READ_ERROR(0x81),
	GUARD_CHECK_ERROR(0x82),
	APPLICATION_TAG_CHECK_ERROR(0x83),
	REFERENCE_TAG_CHECK_ERROR(0x84),
	COMPARE_FAILURE(0x85),
	ACCESS_DENIED(0x86),
	DEALLOCATED_OR_UNWRITTEN_BLOCK(0x87);

	private int numVal;

	NvmeMediaErrorStatusCode(int numVal) { this.numVal = numVal; }

	public int getNumVal() { return numVal; }

	public static NvmeMediaErrorStatusCode valueOf(int numVal) {
		for (NvmeMediaErrorStatusCode statusCode : NvmeMediaErrorStatusCode.values()) {
			if (statusCode.getNumVal() == numVal) {
				return statusCode;
			}
		}
		throw new IllegalArgumentException();
	}
}
