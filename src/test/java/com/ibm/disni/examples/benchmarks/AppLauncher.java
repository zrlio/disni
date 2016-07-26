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

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.slf4j.*;
import com.ibm.disni.util.DiSNILogger;
import com.ibm.disni.util.GetOpt;

public class AppLauncher {         
	private static final Logger logger = DiSNILogger.getLogger();
	
	private String ipAddress = "127.0.0.1"; // of  
	private int size = 1024;
	private int loop = 1;
	private BenchmarkType benchmarkType = BenchmarkType.UNDEFINED;
	private TestCase testCase = TestCase.WRITE;
	private UserDriver userDriver = UserDriver.SIW;  
	private short controlPort = 2020;
	private short dataPort = 1919;
	private boolean polling = false;   
	private boolean inline_data = false;
	
	public static enum BenchmarkType {
		UNDEFINED, JAVA_NIO_SERVER, JAVA_NIO_CLIENT, JAVA_NIO2_SERVER, JAVA_NIO2_CLIENT, JAVA_RDMA_SERVER, JAVA_RDMA_CLIENT, JAVA_ZK_TCP_CLIENT, JAVA_ZK_RDMA_SERVER, JAVA_ZK_RDMA_CLIENT, JAVA_HDFS_TCP_CLIENT, JAVA_HDFS_RDMA_SERVER, JAVA_HDFS_RDMA_CLIENT, JAVA_MC_CLIENT, JAVA_RPC_SERVER, JAVA_RPC_CLIENT, JAVA_VERIFICATION_SERVER, JAVA_VERIFICATION_CLIENT
	};

	public static enum TestCase {
		UNDEFINED, WRITE, READ, PING, RPC_INT, RPC_ARRAY, RPC_COMPLEX, MC_GET, MC_MULTIGET, ZK_EXIST, ZK_DATA, HDFS_EXIST, HDFS_LOCATION
	}; 

	public static enum UserDriver {
		SIW, CXGB3, MLX4
	};

	public void launchBenchmark(String[] args) throws Exception {
		String _bechmarkType = "";
		String _testCase = "";
		String dataFilename;
		FileOutputStream dataStream;
		FileChannel dataChannel;
		IBenchmarkTask benchmarkTask = null;

		String[] _args = args;
		if (args.length < 1) {
			System.exit(0);
		} else if (args[0].equals(AppLauncher.class.getCanonicalName())) {
			_args = new String[args.length - 1];
			for (int i = 0; i < _args.length; i++) {
				_args[i] = args[i + 1];
			}
		}

		GetOpt go = new GetOpt(_args, "t:a:s:k:o:pci");
		go.optErr = true;
		int ch = -1;

		while ((ch = go.getopt()) != GetOpt.optEOF) {
			if ((char) ch == 't') {
				_bechmarkType = go.optArgGet();
			} else if ((char) ch == 'a') {
				ipAddress = go.optArgGet();
			} else if ((char) ch == 's') {
				size = Integer.parseInt(go.optArgGet());
			} else if ((char) ch == 'k') {
				loop = Integer.parseInt(go.optArgGet());
			} else if ((char) ch == 'o') {
				_testCase = go.optArgGet();
				if (_testCase.equalsIgnoreCase("Write")) {
					testCase = TestCase.WRITE;
				} else if (_testCase.equalsIgnoreCase("Read")) {
					testCase = TestCase.READ;
				} else if (_testCase.equalsIgnoreCase("Ping")) {
					testCase = TestCase.PING;
				} else if (_testCase.equalsIgnoreCase("Int")) {
					testCase = TestCase.RPC_INT;
				} else if (_testCase.equalsIgnoreCase("Array")) {
					testCase = TestCase.RPC_ARRAY;
				} else if (_testCase.equalsIgnoreCase("Complex")) {
					testCase = TestCase.RPC_COMPLEX;
				} else if (_testCase.equalsIgnoreCase("Get")) {
					testCase = TestCase.MC_GET;
				} else if (_testCase.equalsIgnoreCase("MGet")) {
					testCase = TestCase.MC_MULTIGET;
				} else if (_testCase.equalsIgnoreCase("ZExist")) {
					testCase = TestCase.ZK_EXIST;
				} else if (_testCase.equalsIgnoreCase("ZData")) {
					testCase = TestCase.ZK_DATA;
				} else if (_testCase.equalsIgnoreCase("HExist")) {
					testCase = TestCase.HDFS_EXIST;
				} else if (_testCase.equalsIgnoreCase("HLocation")) {
					testCase = TestCase.HDFS_LOCATION;
				}
			} else if ((char) ch == 'p') {
				polling = true;
				System.setProperty("cqpolling", "yes");
			} else if ((char) ch == 'i') {
				inline_data = true;
			} else if ((char) ch == 'c') {
				int caching = Integer.parseInt(go.optArgGet());
				if (caching == 0){
					CommRdma.CachingON = false;
				}
			} else {
				System.exit(1); // undefined option
			}
		}
		
		logger.info("property " + System.getProperty("com.ibm.jverbs.provider", "nat"));
		
		logger.info("New logger configuration...");
		try {
			if (_bechmarkType.equals("java-nio-tcp-server")) {
				logger.info("starting java-nio-tcp-server");
				benchmarkType = BenchmarkType.JAVA_NIO_SERVER;
				benchmarkTask = new Nio2Server(this);
			} else if (_bechmarkType.equals("java-nio-tcp-client")) {
				logger.info("starting java-nio-tcp-client");
				benchmarkType = BenchmarkType.JAVA_NIO_CLIENT;
				benchmarkTask = new Nio2Client(this);			
			} else if (_bechmarkType.equals("java-rdma-server")) {
				logger.info("starting java-rdma-server");
				benchmarkType = BenchmarkType.JAVA_RDMA_SERVER;
				benchmarkTask = new RdmaServer(this);
			} else if (_bechmarkType.equals("java-rdma-client")) {
				logger.info("starting java-rdma-client");
				benchmarkType = BenchmarkType.JAVA_RDMA_CLIENT;
				benchmarkTask = new RdmaClient(this);
			} else {
				System.out.println("No valid apptype, type " + _bechmarkType);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

		benchmarkTask.run();
		benchmarkTask.close();  

		dataFilename = "datalog";
		if (_bechmarkType.contains("client")) {
			dataFilename += "-client.dat";
		} else {
			dataFilename += "-server.dat";
		}
		dataStream = new FileOutputStream(dataFilename, true);
		dataChannel = dataStream.getChannel();
		if (benchmarkTask != null) {
			String logdata = _bechmarkType + "\t\t" + benchmarkType.ordinal()
					+ "\t" + loop + "\t" + size + "\t\t"
					+ benchmarkTask.getReadOps() + "\t\t"
					+ benchmarkTask.getWriteOps() + "\t\t"
					+ benchmarkTask.getThroughput() + "\t\t"
					+ benchmarkTask.getErrorOps() + "\t\t" + size
					+ "\t\t" + benchmarkTask.getLatency() + "\n";
			ByteBuffer buffer = ByteBuffer.wrap(logdata.getBytes());
			dataChannel.write(buffer);
		}
		dataChannel.close();
		dataStream.close();

		System.exit(0);
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception { 
		try {
			AppLauncher appLauncher = new AppLauncher();
			appLauncher.launchBenchmark(args);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getCause().getMessage());
		}
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public int getSize() {
		return size;
	}

	public int getLoop() {
		return loop;
	}

	public BenchmarkType getBechmarkType() {
		return benchmarkType;
	}

	public TestCase getTestCase() {
		return testCase;
	}

	public UserDriver getUserDriver() {
		return userDriver;
	}

	public short getControlPort() {
		return controlPort;
	}

	public short getDataPort() {
		return dataPort;
	}

	public boolean usePolling() {
		return polling;
	}

	public boolean useInline() {
		return inline_data;
	}
}
