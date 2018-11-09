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

package com.ibm.disni;

import org.apache.commons.cli.*;

public class CmdLineCommon {

	private static final String IP_KEY = "a";
	private String ip;

	private static final String PORT_KEY = "p";
	private int port;
	private static final int DEFAULT_PORT = 1919;

	private final String appName;

	private final Options options;

	public CmdLineCommon(String appName) {
		this.appName = appName;

		this.options = new Options();
		Option address = Option.builder(IP_KEY).required().desc("ip address").hasArg().required().build();
		Option port = Option.builder(PORT_KEY).desc("port").hasArg().type(Number.class).build();

		options.addOption(address);
		options.addOption(port);
	}

	protected Options addOption(Option option) {
		return options.addOption(option);
	}

	public void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(appName, options);
	}

	protected void getOptionsValue(CommandLine line) throws ParseException {
		ip = line.getOptionValue(IP_KEY);
		if (line.hasOption(PORT_KEY)) {
			port = ((Number) line.getParsedOptionValue(PORT_KEY)).intValue();
		} else {
			port = DEFAULT_PORT;
		}
	}

	public void parse(String[] args) throws ParseException {
		CommandLineParser parser = new DefaultParser();
		CommandLine line = parser.parse(options, args);
		getOptionsValue(line);
	}


	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}
}
