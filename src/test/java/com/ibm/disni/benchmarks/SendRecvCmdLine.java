/*
 * DiSNI: Direct Storage and Networking Interface
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

package com.ibm.disni.benchmarks;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

public class SendRecvCmdLine extends RdmaBenchmarkCmdLine {

	private int queueDepth;
	private final static String QUEUEDEPTH_KEY = "q";
	private final static int QUEUEDEPTH_DEFAULT = 64;

	public SendRecvCmdLine(String appName) {
		super(appName);

		addOption(Option.builder(QUEUEDEPTH_KEY).desc("queue depth")
				.hasArg().type(Number.class).build());
	}

	@Override
	protected void getOptionsValue(CommandLine line) throws ParseException {
		super.getOptionsValue(line);

		if (line.hasOption(QUEUEDEPTH_KEY)) {
			queueDepth = ((Number)line.getParsedOptionValue(QUEUEDEPTH_KEY)).intValue();
		} else {
			queueDepth = QUEUEDEPTH_DEFAULT;
		}
	}

	public int getQueueDepth() {
		return queueDepth;
	}
}
