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

import com.ibm.disni.CmdLineCommon;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

public class RdmaBenchmarkCmdLine extends CmdLineCommon {

	private static final String SIZE_KEY = "s";
	private int size;
	private static final int SIZE_DEFAULT = 32;

	private static final String LOOP_KEY = "k";
	private int loop;
	private static final int LOOP_DEFAULT = 1000;

	public RdmaBenchmarkCmdLine(String appName) {
		super(appName);

		addOption(Option.builder(SIZE_KEY).desc("size of the data to exchange")
				.hasArg().type(Number.class).build());
		addOption(Option.builder(LOOP_KEY).desc("number of iterations")
				.hasArg().type(Number.class).build());
	}

	@Override
	protected void getOptionsValue(CommandLine line) throws ParseException {
		super.getOptionsValue(line);

		if (line.hasOption(SIZE_KEY)) {
			size = ((Number)line.getParsedOptionValue(SIZE_KEY)).intValue();
		} else {
			size = SIZE_DEFAULT;
		}

		if (line.hasOption(LOOP_KEY)) {
			loop = ((Number)line.getParsedOptionValue(LOOP_KEY)).intValue();
		} else {
			loop = LOOP_DEFAULT;
		}
	}


	public int getSize() {
		return size;
	}

	public int getLoop() {
		return loop;
	}
}
