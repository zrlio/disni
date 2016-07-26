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
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executors;

import org.slf4j.Logger;

import com.ibm.disni.examples.benchmarks.AppLauncher.BenchmarkType;
import com.ibm.disni.util.DiSNILogger;
import com.ibm.disni.util.StopWatch;

public class Nio2Server extends BenchmarkBase implements IBenchmarkTask {
	private static final Logger logger = DiSNILogger.getLogger();
	
	private ServerSocketChannel controlServerChannel;
	private SocketChannel controlChannel;

	private AsynchronousChannelGroup channelGroup;
	private AsynchronousServerSocketChannel serverChannelNio2;
	private AsynchronousSocketChannel clientChannelNio2;
	private ServerSocketChannel serverChannelNio;
	private SocketChannel clientChannelNio;
	
	private CommNio controlPlane;
	private CommNio dataPlaneNio;
	private CommNio2 dataPlaneNio2;	
	
	private String serverIP;
	private int size;
	private StopWatch stopWatchThroughput;
	private int loop;
	private BenchmarkType benchmarkType;
	private AppLauncher.TestCase testCase;
	
	public Nio2Server(AppLauncher applauncher) throws Exception {
		this.serverIP = applauncher.getIpAddress();
		this.size = applauncher.getSize();
		this.loop = applauncher.getLoop();
		this.stopWatchThroughput = new StopWatch();
		this.benchmarkType = applauncher.getBechmarkType();
		
		this.controlServerChannel = ServerSocketChannel.open();
		if (benchmarkType == BenchmarkType.JAVA_NIO_SERVER){
			logger.info("launching nio server socket");
			this.serverChannelNio = ServerSocketChannel.open();
		} else if (benchmarkType == BenchmarkType.JAVA_NIO2_SERVER){
			logger.info("launching nio2 group");
			this.channelGroup = AsynchronousChannelGroup.withThreadPool(Executors
					.newFixedThreadPool(10));
			logger.info("launching nio2 server socket");
			this.serverChannelNio2 = AsynchronousServerSocketChannel.open(channelGroup);
		} else {
			throw new Exception("not a valid benchmark type");
		}
		this.controlChannel = null;
		this.clientChannelNio2 = null;
		this.testCase = applauncher.getTestCase();
	}

	public void close() throws IOException {
		//closing control channels
		if (controlChannel != null) {
			logger.info("closing control channel");
			controlChannel.close();
		}
		if (controlServerChannel != null){
			logger.info("closing control server channel");
			this.controlServerChannel.close();
		}
		
		//closing data channels nio2
		if (clientChannelNio2 != null) {
			logger.info("closing client channel nio2");
			clientChannelNio2.close();
		}
		if (serverChannelNio2 != null){
			logger.info("closing server channel nio2");
			this.serverChannelNio2.close();
		}
		if (channelGroup != null){
			logger.info("closing channel group nio2");
			this.channelGroup.shutdown();
		}
		
		//closing data channels nio
		if (clientChannelNio != null) {
			logger.info("closing client channel nio");
			clientChannelNio.close();
		}
		if (serverChannelNio != null){
			logger.info("closing server channel nio");
			this.serverChannelNio.close();
		}
	}

	public void run() throws Exception {
		logger.info("Nio2Server::run: serverIP=" + serverIP);
		InetAddress localHost = InetAddress.getByName(serverIP);
		InetSocketAddress address = new InetSocketAddress(localHost, 1919);
		InetSocketAddress controlAddress = new InetSocketAddress(localHost,
				2020);
		ByteBuffer[] fragments = BufferUtils.getBufferFragments("", size,
				size, true);
		
		logger.info("binding control channel");
		controlServerChannel.bind(controlAddress);
		
		logger.info("binding data channel");
		if (benchmarkType == BenchmarkType.JAVA_NIO_SERVER){
			serverChannelNio.bind(address);
		} else {
			serverChannelNio2.bind(address);
		}
		logger.info("servers bound to address");

		logger.info("accepting control channel");
		controlChannel = controlServerChannel.accept();
		logger.info("control channel accepted!");
		
		if (benchmarkType == BenchmarkType.JAVA_NIO_SERVER){
			clientChannelNio = serverChannelNio.accept();
		} else {
			clientChannelNio2 = serverChannelNio2.accept().get();
		}
		logger.info("data channel accepted");
		
		ReadyToReceive readyToReceive = new ReadyToReceive(size, size,
				loop);
		ByteBuffer client2serverBuffer = ByteBuffer.allocateDirect(readyToReceive
				.size());
		ByteBuffer server2clientBuffer = ByteBuffer.allocateDirect(readyToReceive
				.size());
		readyToReceive.writeBack(client2serverBuffer);
		readyToReceive.writeBack(server2clientBuffer);
		client2serverBuffer.clear();
		
		controlPlane = new CommNio(controlChannel);
		if (benchmarkType == BenchmarkType.JAVA_NIO_SERVER){
			logger.info("creating nio data plane");
			dataPlaneNio = new CommNio(clientChannelNio);
		} else {
			logger.info("creating nio2 data plane");
			dataPlaneNio2 = new CommNio2(clientChannelNio2);
		}	
		
		logger.info("\n");
		logger.info("waiting for handshake init");
		controlPlane.waitForNextRound(client2serverBuffer);
		logger.info("sending handshake response");
		controlPlane.startNextRound(server2clientBuffer);
		logger.info("init handshake done");
		logger.info("\n");
		
		this.readOps = 0;
		double sumbytes = 0;
		this.errorOps = 0;
		stopWatchThroughput.start();
		for (int i = 0; i < loop; i++) {
			if (testCase == AppLauncher.TestCase.READ){
				BufferUtils.tagBuffer(fragments, i+1);
				controlPlane.waitForNextRound(client2serverBuffer);
				if (benchmarkType == BenchmarkType.JAVA_NIO_SERVER){
					dataPlaneNio.writeSG(fragments, size, i);
				} else {
					dataPlaneNio2.writeSG(fragments, size, i);
				}
			} else {
				BufferUtils.tagBuffer(fragments, i+1);
				if (benchmarkType == BenchmarkType.JAVA_NIO_SERVER){
					dataPlaneNio.writeSG(fragments, size, i);
				} else {
					dataPlaneNio2.writeSG(fragments, size, i);
				}
			}
		}
		logger.info("done loop, waiting for final handshake\n");
		double executionTime = (double) stopWatchThroughput.getExecutionTime() / 1000.0;
		double sumbits = sumbytes * 8.0;
		if (executionTime > 0) {
			this.throughput = sumbits / executionTime / 1000.0 / 1000.0;
		}
		
		controlPlane.waitForNextRound(client2serverBuffer);
		controlPlane.startNextRound(server2clientBuffer);
		
		logger.info("final handshake done\n");
		logger.info("throughput " + throughput + " Mbit/s");
	}
}
