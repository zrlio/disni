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
