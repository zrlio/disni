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

import com.ibm.disni.util.MemoryAllocation;
import sun.nio.ch.DirectBuffer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.List;

public class Nvme {

	private final NativeDispatcher nativeDispatcher;
	private final MemoryAllocation memoryAllocation;

	public Nvme(NvmeTransportType[] transportTypes, long memorySizeMB) throws IllegalArgumentException {
		nativeDispatcher = new NativeDispatcher();
		memoryAllocation = MemoryAllocation.getInstance();


		int[] transportTypesInt = new int[transportTypes.length];
		for(int i = 0; i < transportTypes.length; i++) {
			NvmeTransportType type = transportTypes[i];
			if (!nativeDispatcher._nvme_transport_available(type.getNumVal())) {
				throw new IllegalArgumentException("Unsupported transport " + type.name());
			}
			transportTypesInt[i] = type.getNumVal();
		}

		int ret = nativeDispatcher._env_init(memorySizeMB, transportTypesInt);
		if (ret < 0) {
			throw new IllegalArgumentException("spdk_env_init failed with " + ret);
		}
	}

	public void logEnableTrace() {
		nativeDispatcher._log_set_trace_flag("all");
	}

	public void probe(NvmeTransportId id, List<NvmeController> controller) throws IOException {
		long controllerIds[] = new long[8];
		int i;
		int controllers;
		do {
			controllers = nativeDispatcher._nvme_probe(id.getType().getNumVal(), id.getAddressFamily().getNumVal(),
					id.getAddress(), id.getServiceId(),
					id.getSubsystemNQN(), controllerIds);
			if (controllers < 0) {
				throw new IOException("spdk_nvme_probe failed with " + controllers);
			}

			for (i = 0; i < Math.min(controllers, controllerIds.length); i++) {
				controller.add(new NvmeController(controllerIds[i], nativeDispatcher, memoryAllocation));
			}
		} while (controllers > controllerIds.length);
	}

	public ByteBuffer allocateBuffer(int size, int alignment) {
		if (size < 0) {
			throw new IllegalArgumentException("negative size");
		}
		if (alignment < 0) {
			throw new IllegalArgumentException("negative alignment");
		}
		long address = nativeDispatcher._malloc(size, alignment);
		if (address == 0) {
			throw new OutOfMemoryError("No more space in SPDK mempool");
		}

		Class directByteBufferClass;
		try {
			directByteBufferClass = Class.forName("java.nio.DirectByteBuffer");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("No class java.nio.DirectByteBuffer");
		}
		Constructor<Object> constructor = null;
		try {
			constructor = directByteBufferClass.getDeclaredConstructor(Long.TYPE, Integer.TYPE);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("No constructor (long, int) in java.nio.DirectByteBuffer");
		}
		constructor.setAccessible(true);
		ByteBuffer buffer;
		try {
			buffer = (ByteBuffer)constructor.newInstance(address, size);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}

		return buffer;
	}

	public void freeBuffer(ByteBuffer buffer) {
		nativeDispatcher._free(((DirectBuffer)buffer).address());
	}
}
