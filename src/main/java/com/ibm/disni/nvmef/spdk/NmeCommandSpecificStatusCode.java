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

public enum NmeCommandSpecificStatusCode {
	COMPLETION_QUEUE_INVALID(0x00),
	INVALID_QUEUE_IDENTIFIER(0x01),
	MAXIMUM_QUEUE_SIZE_EXCEEDED(0x02),
	ABORT_COMMAND_LIMIT_EXCEEDED(0x03),
	ASYNC_EVENT_REQUEST_LIMIT_EXCEEDED(0x05),
	INVALID_FIRMWARE_SLOT(0x06),
	INVALID_FIRMWARE_IMAGE(0x07),
	INVALID_INTERRUPT_VECTOR(0x08),
	INVALID_LOG_PAGE(0x09),
	INVALID_FORMAT(0x0a),
	FIRMWARE_REQ_CONVENTIONAL_RESET(0x0b),
	INVALID_QUEUE_DELETION(0x0c),
	FEATURE_ID_NOT_SAVEABLE(0x0d),
	FEATURE_NOT_CHANGEABLE(0x0e),
	FEATURE_NOT_NAMESPACE_SPECIFIC(0x0f),
	FIRMWARE_REQ_NVM_RESET(0x10),
	FIRMWARE_REQ_RESET(0x11),
	FIRMWARE_REQ_MAX_TIME_VIOLATION(0x12),
	FIRMWARE_ACTIVATION_PROHIBITED(0x13),
	OVERLAPPING_RANGE(0x14),
	NAMESPACE_INSUFFICIENT_CAPACITY(0x15),
	NAMESPACE_ID_UNAVAILABLE(0x16),
	NAMESPACE_ALREADY_ATTACHED(0x18),
	NAMESPACE_IS_PRIVATE(0x19),
	NAMESPACE_NOT_ATTACHED(0x1a),
	THINPROVISIONING_NOT_SUPPORTED(0x1b),
	CONTROLLER_LIST_INVALID(0x1c),
	CONFLICTING_ATTRIBUTES(0x80),
	INVALID_PROTECTION_INFO(0x81),
	ATTEMPTED_WRITE_TO_RO_PAGE(0x82);

	private int numVal;

	NmeCommandSpecificStatusCode(int numVal) { this.numVal = numVal; }

	public int getNumVal() { return numVal; }

	public static NmeCommandSpecificStatusCode valueOf(int numVal) {
		for (NmeCommandSpecificStatusCode statusCode : NmeCommandSpecificStatusCode.values()) {
			if (statusCode.getNumVal() == numVal) {
				return statusCode;
			}
		}
		throw new IllegalArgumentException();
	}
}
