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
import java.util.ArrayList;
import java.util.List;

public class Nvme {

	private final NativeDispatcher nativeDispatcher;
	private final MemoryAllocation memoryAllocation;

	private NvmfTarget nvmfTarget;

	public Nvme(NvmeTransportType[] transportTypes, String hugePath, long[] socketMemoryMB) throws IllegalArgumentException {
		nativeDispatcher = new NativeDispatcher();
		memoryAllocation = MemoryAllocation.getInstance();

		boolean pcie = false;
		for(NvmeTransportType type : transportTypes) {
			if (!nativeDispatcher._nvme_transport_available(type.getNumVal())) {
				throw new IllegalArgumentException("Unsupported transport " + type.name());
			}
			if (type == NvmeTransportType.PCIE) {
				pcie = true;
			}
		}
		ArrayList<String> args = new ArrayList<String>();
		args.add("nvme");
		if (!pcie) {
			args.add("--no-pci");
		}

		if (hugePath == null) {
			//FIXME: this does not seem to work with the current SPDK build
			args.add("--no-huge");
			long totalMemory = 0;
			for (long memory : socketMemoryMB) {
				totalMemory += memory;
			}

			args.add("-m");
			args.add(Long.toString(totalMemory));
		} else {
			args.add("--huge-dir");
			args.add(hugePath);

			args.add("--socket-mem");
			if (socketMemoryMB == null || socketMemoryMB.length == 0) {
				throw new IllegalArgumentException("socketMemoryMB null or zero length");
			}
			StringBuilder sb = new StringBuilder();
			for (long memory : socketMemoryMB) {
				if (sb.length() > 0) {
					sb.append(',');
				}
				sb.append(Long.toString(memory));
			}
			args.add(sb.toString());
		}

		args.add("--proc-type");
		args.add("primary");

		int ret = nativeDispatcher._rte_eal_init(args.toArray(new String[args.size()]));
		if (ret < 0) {
			throw new IllegalArgumentException("rte_eal_init failed with " + ret);
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

	public NvmfTarget createNvmfTarget(short maxQueueDepth, short maxConnectionPerSession,
									   int inCapsuleDataSize, int maxIOSize) throws Exception {
		nvmfTarget = new NvmfTarget(nativeDispatcher, maxQueueDepth, maxConnectionPerSession, inCapsuleDataSize,
				maxIOSize);
		return nvmfTarget;
	}

	public NvmfTarget getNvmfTarget() throws Exception {
		if (nvmfTarget == null) {
			throw new Exception("Target not initialized");
		}
		return nvmfTarget;
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
