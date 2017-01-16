/*
 * jVerbs: RDMA verbs support for the Java Virtual Machine
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

package com.ibm.disni.benchmarks;

import com.ibm.disni.nvmef.spdk.*;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;

public class Nvmef {

	public static void main(String[] args) throws Exception {
		Nvme nvme = new Nvme();
		NvmeTransportId tid = new NvmeTransportId(NvmeTransportType.RDMA, NvmfAddressFamily.IPV4, "10.40.0.17", "4420",
				"nqn.2014-08.org.nvmexpress.discovery");
		ArrayList<NvmeController> controllers = new ArrayList<NvmeController>();
		nvme.probe(tid, controllers);
		NvmeController controller = controllers.get(0);
		NvmeNamespace namespace = controller.getNamespace(1);
		NvmeQueuePair qpair = controller.allocQueuePair();

		Random rand = new Random(System.nanoTime());
		// 128KB = maximum transfer size
		ByteBuffer buffer = ByteBuffer.allocateDirect(1024*128);
		byte bytes[] = new byte[buffer.capacity()];
		rand.nextBytes(bytes);
		buffer.put(bytes);

		final int ITERS = 10000;
		IOCompletion completion;
		long start = System.nanoTime();
		for (int i = 0; i < ITERS; i++) {
			completion = namespace.read(qpair, ((DirectBuffer)buffer).address(), 100, 1);
			do {
				qpair.processCompletions(1);
				completion.update();
			} while (!completion.done());
		}
		long end = System.nanoTime();
		System.out.println("Read latency (same location) = " + (end - start)/ITERS + "ns");

		start = System.nanoTime();
		for (int i = 0; i < ITERS; i++) {
			completion = namespace.read(qpair, ((DirectBuffer)buffer).address(), rand.nextInt(1024*1024), 1);
			do {
				qpair.processCompletions(1);
				completion.update();
			} while (!completion.done());
		}
		end = System.nanoTime();
		System.out.println("Read latency (random location) = " + (end - start)/ITERS + "ns");

		start = System.nanoTime();
		for (int i = 0; i < ITERS; i++) {
			completion = namespace.write(qpair, ((DirectBuffer)buffer).address(), 1024, 1);
			do {
				qpair.processCompletions(1);
				completion.update();
			} while (!completion.done());
		}
		end = System.nanoTime();
		System.out.println("Write latency (same location) = " + (end - start)/ITERS + "ns");

		start = System.nanoTime();
		for (int i = 0; i < ITERS; i++) {
			completion = namespace.write(qpair, ((DirectBuffer)buffer).address(), rand.nextInt(1024*1024), 1);
			do {
				qpair.processCompletions(1);
				completion.update();
			} while (!completion.done());
		}
		end = System.nanoTime();
		System.out.println("Write latency (random location) = " + (end - start)/ITERS + "ns");


		start = System.nanoTime();
		for (int i = 0; i < ITERS; i++) {
			completion = namespace.read(qpair, ((DirectBuffer)buffer).address(), rand.nextInt(1024*1024), 262144);
			do {
				qpair.processCompletions(1);
				completion.update();
			} while (!completion.done());
		}
		end = System.nanoTime();
		System.out.println("Read qd = 1 / 128KB (random location) = " + (end - start)/ITERS + "ns");
	}
}
