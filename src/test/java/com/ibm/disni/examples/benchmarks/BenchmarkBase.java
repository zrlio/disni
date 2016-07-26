/*
 * jVerbs: RDMA verbs support for the Java Virtual Machine
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

package com.ibm.disni.examples.benchmarks;

public abstract class BenchmarkBase implements IBenchmarkTask {
	protected double throughput;
	protected double latency;
	protected double readOps;
	protected double writeOps;
	protected double errorOps;

	public double getThroughput() {
		return throughput;
	}

	public double getLatency() {
		return this.latency;
	}

	public double getReadOps() {
		return this.readOps;
	}

	public double getWriteOps() {
		return this.writeOps;
	}

	public double getErrorOps() {
		return this.errorOps;
	}
}
