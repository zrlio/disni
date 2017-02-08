/*
 * DiSNI: Direct Storage and Networking Interface
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

package com.ibm.disni.util;

public class GetOpt {

	private String[] theArgs = null;
	private int argCount = 0;
	private String optString = null;

	public GetOpt(String[] args, String opts) {
		theArgs = args;
		argCount = theArgs.length;
		optString = opts;
	}

	// user can toggle this to control printing of error messages
	public boolean optErr = false;

	public int processArg(String arg, int n) {
		int value;
		try {
			value = Integer.parseInt(arg);
		} catch (NumberFormatException e) {
			if (optErr)
				System.err.println("processArg cannot process " + arg // NOI18N
						+ " as an integer"); // NOI18N
			return n;
		}
		return value;
	}

	public int tryArg(int k, int n) {
		int value;
		try {
			value = processArg(theArgs[k], n);
		} catch (ArrayIndexOutOfBoundsException e) {
			if (optErr)
				System.err.println("tryArg: no theArgs[" + k + "]"); // NOI18N
			return n;
		}
		return value;
	}

	public long processArg(String arg, long n) {
		long value;
		try {
			value = Long.parseLong(arg);
		} catch (NumberFormatException e) {
			if (optErr)
				System.err.println("processArg cannot process " + arg // NOI18N
						+ " as a long"); // NOI18N
			return n;
		}
		return value;
	}

	public long tryArg(int k, long n) {
		long value;
		try {
			value = processArg(theArgs[k], n);
		} catch (ArrayIndexOutOfBoundsException e) {
			if (optErr)
				System.err.println("tryArg: no theArgs[" + k + "]"); // NOI18N
			return n;
		}
		return value;
	}

	public double processArg(String arg, double d) {
		double value;
		try {
			value = Double.valueOf(arg).doubleValue();
		} catch (NumberFormatException e) {
			if (optErr)
				System.err.println("processArg cannot process " + arg // NOI18N
						+ " as a double"); // NOI18N
			return d;
		}
		return value;
	}

	public double tryArg(int k, double d) {
		double value;
		try {
			value = processArg(theArgs[k], d);
		} catch (ArrayIndexOutOfBoundsException e) {
			if (optErr)
				System.err.println("tryArg: no theArgs[" + k + "]"); // NOI18N
			return d;
		}
		return value;
	}

	public float processArg(String arg, float f) {
		float value;
		try {
			value = Float.valueOf(arg).floatValue();
		} catch (NumberFormatException e) {
			if (optErr)
				System.err.println("processArg cannot process " + arg // NOI18N
						+ " as a float"); // NOI18N
			return f;
		}
		return value;
	}

	public float tryArg(int k, float f) {
		float value;
		try {
			value = processArg(theArgs[k], f);
		} catch (ArrayIndexOutOfBoundsException e) {
			if (optErr)
				System.err.println("tryArg: no theArgs[" + k + "]"); // NOI18N
			return f;
		}
		return value;
	}

	public boolean processArg(String arg, boolean b) {
		// `true' in any case mixture is true; anything else is false
		return Boolean.valueOf(arg).booleanValue();
	}

	public boolean tryArg(int k, boolean b) {
		boolean value;
		try {
			value = processArg(theArgs[k], b);
		} catch (ArrayIndexOutOfBoundsException e) {
			if (optErr)
				System.err.println("tryArg: no theArgs[" + k + "]"); // NOI18N
			return b;
		}
		return value;
	}

	public String tryArg(int k, String s) {
		String value;
		try {
			value = theArgs[k];
		} catch (ArrayIndexOutOfBoundsException e) {
			if (optErr)
				System.err.println("tryArg: no theArgs[" + k + "]"); // NOI18N
			return s;
		}
		return value;
	}

	private static void writeError(String msg, char ch) {
		System.err.println("GetOpt: " + msg + " -- " + ch); // NOI18N
	}

	public static final int optEOF = -1;

	private int optIndex = 0;

	public int optIndexGet() {
		return optIndex;
	}

	public void optIndexSet(int i) {
		optIndex = i;
	}

	private String optArg = null;

	public String optArgGet() {
		return optArg;
	}

	private int optPosition = 1;

	public int getopt() {
		optArg = null;
		if (theArgs == null || optString == null)
			return optEOF;
		if (optIndex < 0 || optIndex >= argCount)
			return optEOF;
		String thisArg = theArgs[optIndex];
		int argLength = thisArg.length();
		// handle special cases
		if (argLength <= 1 || thisArg.charAt(0) != '-') {
			// e.g., "", "a", "abc", or just "-"
			return optEOF;
		} else if (thisArg.equals("--")) {// NOI18N
			// end of non-option args
			optIndex++;
			return optEOF;
		}
		// get next "letter" from option argument
		char ch = thisArg.charAt(optPosition);
		// find this option in optString
		int pos = optString.indexOf(ch);
		if (pos == -1 || ch == ':') {
			if (optErr) {
				writeError("illegal option", ch); // NOI18N
			}
			ch = '?';
		} else { // handle colon, if present
			if (pos < optString.length() - 1
					&& optString.charAt(pos + 1) == ':') {
				if (optPosition != argLength - 1) {
					// take rest of current arg as optArg
					optArg = thisArg.substring(optPosition + 1);
					optPosition = argLength - 1; // force advance to next arg
													// below
				} else { // take next arg as optArg
					optIndex++;
					if (optIndex < argCount
							&& (theArgs[optIndex].charAt(0) != '-' || theArgs[optIndex]
									.length() >= 2
									&& (optString.indexOf(theArgs[optIndex]
											.charAt(1)) == -1 || theArgs[optIndex]
											.charAt(1) == ':'))) {
						optArg = theArgs[optIndex];
					} else {
						if (optErr) {
							writeError("option requires an argument", ch); // NOI18N
						}
						optArg = null;
						ch = ':'; // Linux man page for getopt(3) says : not ?
					}
				}
			}
		}
		// advance to next option argument,
		// which might be in thisArg or next arg
		optPosition++;
		if (optPosition >= argLength) {
			optIndex++;
			optPosition = 1;
		}
		return ch;
	}
}
