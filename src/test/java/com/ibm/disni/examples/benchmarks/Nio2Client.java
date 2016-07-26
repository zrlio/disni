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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executors;

import org.slf4j.Logger;

import com.ibm.disni.examples.benchmarks.AppLauncher.BenchmarkType;
import com.ibm.disni.util.DiSNILogger;
import com.ibm.disni.util.StopWatch;

public class Nio2Client extends BenchmarkBase implements IBenchmarkTask {
	private static final Logger logger = DiSNILogger.getLogger();

	private SocketChannel controlChannel;
	
	private AsynchronousChannelGroup channelGroup;
	private AsynchronousSocketChannel clientChannelNio2;
	private SocketChannel clientChannelNio;
	
	private CommNio controlPlane;
	private CommNio dataPlaneNio;
	private CommNio2 dataPlaneNio2;
	
	private String clientIP;
	private int size;
	private StopWatch stopWatchThroughput;
	private int loop;
	private AppLauncher.TestCase testCase;
	private BenchmarkType benchmarkType;
	private ReadyToReceive readyToReceive;

	public Nio2Client(AppLauncher applauncher) throws IOException {
		this.clientIP = applauncher.getIpAddress();
		this.size = applauncher.getSize();
		this.loop = applauncher.getLoop();
		this.testCase = applauncher.getTestCase();
		this.benchmarkType = applauncher.getBechmarkType();
		this.readyToReceive = null;
		this.stopWatchThroughput = new StopWatch();
		
		this.controlChannel = SocketChannel.open();
		if (benchmarkType == BenchmarkType.JAVA_NIO_CLIENT){
			logger.info("launching client socket");
			this.clientChannelNio = SocketChannel.open();
			logger.info("launching client socket done!");
		} else if (benchmarkType == BenchmarkType.JAVA_NIO2_CLIENT){
			logger.info("launching group");
			this.channelGroup = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(10));
			logger.info("launching client socket");
			this.clientChannelNio2 = AsynchronousSocketChannel.open(channelGroup);
			logger.info("launching client socket done!");
		}
		this.dataPlaneNio = null;
		this.dataPlaneNio2 = null;
	}

	public void close() throws IOException {
		if (clientChannelNio2 != null){
			logger.info("closing client channel nio2");
			clientChannelNio2.close();
		}
		
		if (clientChannelNio != null){
			logger.info("closing client channel nio");
			clientChannelNio.close();
		}
		
		if (controlChannel != null){
			logger.info("closing control channel nio");
			controlChannel.close();
		}
		
		if (channelGroup != null){
			logger.info("closing channel group nio");
			channelGroup.shutdown();
		}
	}

	public void run() throws Exception {
		logger.info("Nio2Client::run: clientIP=" + clientIP);
		InetAddress localHost = InetAddress.getByName(clientIP);
		InetSocketAddress address = new InetSocketAddress(localHost, 1919);
		InetSocketAddress controlAddress = new InetSocketAddress(localHost,
				2020);
		ByteBuffer[] fragments = BufferUtils.getBufferFragments("", size,
				size, true);

		logger.info("connecting control channel");
		controlChannel.connect(controlAddress);
		logger.info("control channel connected!");
		
		logger.info("connecting data channel");
		if (benchmarkType == BenchmarkType.JAVA_NIO_CLIENT){
			clientChannelNio.connect(address);
		} else {
			clientChannelNio2.connect(address).get();
		}
		logger.info("data channel connect");
		
		readyToReceive = new ReadyToReceive(size, size, loop);
		ByteBuffer client2serverBuffer = ByteBuffer.allocateDirect(readyToReceive
				.size());
		ByteBuffer server2clientBuffer = ByteBuffer.allocateDirect(readyToReceive
				.size());
		
		controlPlane = new CommNio(controlChannel);
		if (benchmarkType == BenchmarkType.JAVA_NIO_CLIENT){
			dataPlaneNio = new CommNio(clientChannelNio);
		} else {
			dataPlaneNio2 = new CommNio2(clientChannelNio2);
		}	
		
		logger.info("\n");
		logger.info("sending handshake init");
		controlPlane.startNextRound(client2serverBuffer);
		logger.info("waiting for handshake response");
		controlPlane.waitForNextRound(server2clientBuffer);
		logger.info("handshake done");
		
		logger.info("\n");
		this.writeOps = 0;
		double sumbytes = 0; 
		double ops = 0;
		stopWatchThroughput.start();
		for (int i = 0; i < loop; i++) {
			if (testCase == AppLauncher.TestCase.READ){
				boolean res = false;
				if (benchmarkType == BenchmarkType.JAVA_NIO_CLIENT){
					controlPlane.startNextRound(client2serverBuffer);
					dataPlaneNio.read(fragments, size, i);
				} else {
					dataPlaneNio2.initSGRead(fragments);
					controlPlane.startNextRound(client2serverBuffer);
					res = dataPlaneNio2.completeSGRead(fragments, size, i);
				}
				
				res = BufferUtils.checkTag(fragments, i+1);
				if (res) {
					sumbytes += (double) size;
				} else {
					this.errorOps += 1.0;
				}
				ops += 1.0;
			} else {
				boolean res = false;
				if (benchmarkType == BenchmarkType.JAVA_NIO_CLIENT){
					dataPlaneNio.read(fragments, size, i);
				} else {
					dataPlaneNio2.initSGRead(fragments);
					res = dataPlaneNio2.completeSGRead(fragments, size, i);
				}
				
				res = BufferUtils.checkTag(fragments, i+1);
				if (res) {
					sumbytes += (double) size;
				} else {
					this.errorOps += 1.0;
				}
				ops += 1.0;
			}
		}
		logger.info("done loop, issuing final handshake\n");
		double executionTime = (double) stopWatchThroughput.getExecutionTime() / 1000.0;
		double sumbits = sumbytes * 8.0;
		if (executionTime > 0) {
			this.throughput = sumbits / executionTime / 1000.0 / 1000.0;
			this.latency = 1000000 * executionTime / ((double) ops);
		}
		
		controlPlane.startNextRound(client2serverBuffer);
		controlPlane.waitForNextRound(server2clientBuffer);
		
		logger.info("final handshake done\n");
		logger.info("throughput " + throughput + " Mbit/s");
	}
}
