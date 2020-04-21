/*
 * DiSNI: Direct Storage and Networking Interface
 *
 * Author: Peter Rudenko <peterr@mellanox.com>
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

import com.ibm.disni.examples.SendRecvClient.CustomClientEndpoint;
import com.ibm.disni.RdmaActiveEndpointGroup;
import com.ibm.disni.RdmaEndpointFactory;
import com.ibm.disni.verbs.*;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class RDMAvsTcpBenchmarkClient implements RdmaEndpointFactory<CustomClientEndpoint> {
  RdmaActiveEndpointGroup<CustomClientEndpoint> endpointGroup;
  private InetSocketAddress rdmaAddress;
  private InetSocketAddress tcpAddress;
  private int bufferSize;
  private int loopCount;

  public CustomClientEndpoint createEndpoint(RdmaCmId idPriv, boolean serverSide) throws IOException {
    return new CustomClientEndpoint(endpointGroup, idPriv, serverSide, bufferSize);
  }

  public void runTCP() throws Exception {
    SocketChannel socketChannel = SocketChannel.open(tcpAddress);
    socketChannel.configureBlocking(true);
    socketChannel.socket().setReceiveBufferSize(bufferSize);
    socketChannel.socket().setSendBufferSize(bufferSize);
    ByteBuffer sendBuf = ByteBuffer.allocateDirect(bufferSize);
    ByteBuffer recvBuf = ByteBuffer.allocateDirect(bufferSize);
    sendBuf.asCharBuffer().put("PING").clear();
    long startTime = System.nanoTime();
    for (int i = 0; i < loopCount; i++){
      int read = 0;
      int written = 0;

      // Send PING
      sendBuf.clear();
      do {
        written += socketChannel.write(sendBuf);
      } while (written != bufferSize);

      // Recv PONG
      do {
        read += socketChannel.read(recvBuf);
      } while (read != bufferSize);
      recvBuf.clear();
    }
    System.out.println("TCP result:");
    printResults(startTime);
    socketChannel.close();
  }

  public void runRDMA() throws Exception {
    //create a EndpointGroup. The RdmaActiveEndpointGroup contains CQ processing and delivers CQ event to the endpoint.dispatchCqEvent() method.
    endpointGroup = new RdmaActiveEndpointGroup<CustomClientEndpoint>(1000, false, 128, 4, 128);
    endpointGroup.init(this);

    //we have passed our own endpoint factory to the group, therefore new endpoints will be of type CustomClientEndpoint
    //let's create a new client endpoint
    CustomClientEndpoint endpoint = endpointGroup.createEndpoint();

    IbvDeviceAttr deviceAttr = endpoint.getIdPriv().getVerbs().queryDevice();

    int maxResponderResources = deviceAttr.getMax_qp_rd_atom();
    int maxInitiatorDepth = deviceAttr.getMax_qp_init_rd_atom();

    RdmaConnParam connParam = new RdmaConnParam();
    connParam.setResponder_resources((byte) maxResponderResources);
    connParam.setInitiator_depth((byte) maxInitiatorDepth);
    endpointGroup.setConnParam(connParam);

    //connect to the server
    endpoint.connect(rdmaAddress, 1000);
    System.out.println("RDMAvsTcpBenchmarkClient::client channel set up ");

    //in our custom endpoints we have prepared (memory registration and work request creation) some memory
    //buffers beforehand.
    //let's send one of those buffers out using a send operation
    ByteBuffer sendBuf = endpoint.getSendBuf();
    ByteBuffer recvBuf = endpoint.getRecvBuf();
    sendBuf.asCharBuffer().put("PING");
    SVCPostSend postSend = endpoint.postSend(endpoint.getWrList_send());
    SVCPostRecv postRecv = endpoint.postRecv(endpoint.getWrList_recv());
    long startTime = System.nanoTime();
    for (int i = 0; i < loopCount + 1; i++) {
      // Send PING
      sendBuf.clear();
      postSend.execute();
      endpoint.getWcEvents().take();

      // Recv PONG
      postRecv.execute();
      endpoint.getWcEvents().take();
      recvBuf.clear();
    }
    System.out.println("RDMA result:");
    printResults(startTime);
    //close everything
    endpoint.close();
    endpointGroup.close();
  }

  private void printResults(long startTime){
    long duration = System.nanoTime() - startTime;
    long totalSize = (long)bufferSize * 2 * loopCount;
    System.out.println("Total time: " + duration / 1e6 + " ms");
    System.out.println("Bidirectional bandwidth: " + totalSize * 1e9/1024/1024/1024/duration + " Gb/s");
    System.out.println("Bidirectional average latency: " + duration  / (1e6 * loopCount) + " ms");
  }

  public void launch(String[] args) throws Exception {
    RdmaBenchmarkCmdLine cmdLine = new RdmaBenchmarkCmdLine("RDMAvsTcpBenchmarkClient");

    try {
      cmdLine.parse(args);
    } catch (ParseException e) {
      cmdLine.printHelp();
      System.exit(-1);
    }
    String host = cmdLine.getIp();
    Integer port = cmdLine.getPort();

    InetAddress ipAddress = InetAddress.getByName(host);
    rdmaAddress = new InetSocketAddress(ipAddress, port);
    tcpAddress = new InetSocketAddress(ipAddress, port + 1);
    bufferSize = cmdLine.getSize();
    loopCount = cmdLine.getLoop();

    this.runRDMA();
    this.runTCP();
    System.exit(0);
  }

  public static void main(String[] args) throws Exception {
    RDMAvsTcpBenchmarkClient pingPongClient = new RDMAvsTcpBenchmarkClient();
    pingPongClient.launch(args);
  }
}

