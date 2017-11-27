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
