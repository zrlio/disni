# DiSNI: Direct Storage and Networking Interface

DiSNI is a Java library for direct storage and networking access from userpace. It currently provides an RDMA interface. Support for other userpace storage and networking interfaces, such as DPDK or SPDK, are in planning. 

## RDMA APIs

DiSNI's RDMA pillar is the open source version of the [IBM jVerbs](http://www.ibm.com/support/knowledgecenter/SSYKE2_7.0.0/com.ibm.java.lnx.70.doc/diag/understanding/rdma_jverbs.html) stack. The RDMA APIs enable the development of Java applications for high performance RDMA networks, such as InfiniBand, iWARP, or RoCE. There exist two sets of APIs, the low level verbs API and the higher level endpoint API. The verbs interface matches the well known OFED verbs interface available in the native C language and thereby offers maximum control over all RDMA resources. The endpoint API, on the other hand, offers a simplified programming interface. Both APIs provide the full set of RDMA operations such as two-sided and one-sided operations, zero-copy transmission and direct data placement. DiSNI runs on Infiniband, RoCE, iWARP and [SoftiWARP](https://github.com/zrlio/softiwarp). If specifically low-latency RPC is required, then [DaRPC](https://github.com/zrlio/darpc) can be used, an RPC library built on top of DiSNI.

The RDMA APIs in DiSNI are implemented based on the Open Fabrics Enterprise Distribution (OFED) RDMA user libraries. A thin JNI layer is used to bridge between Java code and the OFED user libraries. To avoid any performance impacts that are associated with passing complex RDMA parameters and arrays through the JNI interface, the DiSNI library implements a concept called stateful verbs call (SVC). With this approach, each JNI serialization state for a verb call is cached in the context of an SVM object and can be reused many times.

## Building DiSNI

Building the source requires [Apache Maven](http://maven.apache.org/) and [GNU/autotools](http://www.gnu.org/software/autoconf/autoconf.html) and Java version 8 or higher.
To build DiSNI and its example programs, execute the following steps:

1. Obtain a copy of DiSNI from [Github](https://github.com/zrlio/disni)
2. Build libdisni and libaffinity using: ./autoprepare.sh; ./configure --with-jdk=path-to-jdk; make install
3. Run: mvn -DskipTests install

## How to Run the READ Example

1. After building DiSNI, make sure DiSNI and its dependencies are in the classpath (e.g., disni-1.0-jar-with-dependencies.jar). Also add the DiSNI test jar (disni-1.0-tests.jar) which includes the examples.
2. Make sure libdisni and libaffinity are both part of the LD_LIBRARY_PATH
3. Make sure the RDMA network interface is configured and up on the test machines (run ibv\_devices to see the list of RDMA NICs). If your machine does not have RDMA hardware, you can also use SoftiWARP from [Github](https://github.com/zrlio/softiwarp). 
4. Run the server\: java com.ibm.disni.examples.endpoints.read.JVerbsReadServer -a \<server IP\>
5. Run the client\: java com.ibm.disni.examples.endpoints.read.JVerbsReadClient -a \<server IP\>

## Maven Integration

To use DiSNI in your maven application use the following snippet in your pom.xml file (you need to build DiSNI before to update your local maven repo):

    <dependency>
      <groupId>com.ibm.disni</groupId>
      <artifactId>disni</artifactId>
      <version>1.0</version>
    </dependency>


## What are Stateful Verb Calls (SVCs)

Stateful verb calls are objects representing RDMA operations. SVCs encapsulate the serialization state of the network operation (just the operation, not the data). If an operation is executed multiple times (typically with the content of the source or sink buffer changing), the serialization overhead can be saved, which in turn will lead to the highest possible performance. SVCs can also be modified, for instance when transmitting data at different offsets. Modifications to SVC objects are efficient as they do not require serialization. It is key that SVCs object are re-used whenver possible to avoid garbage collection overheads. 

A good example showcasing the use of SVCs can be found in JVerbsReadClient.java\:
```
		SVCPostSend postSend = endpoint.postSend(endpoint.getWrList_send());
		for (int i = 10; i <= 100; ){
			postSend.getWrMod(0).getSgeMod(0).setLength(i);
			postSend.execute();
			//wait until the operation has completed
			endpoint.getWcEvents().take();
			
			//we should have the content of the remote buffer in our own local buffer now
			ByteBuffer dataBuf = endpoint.getDataBuf();
			dataBuf.clear();
			System.out.println("ReadClient::read memory from server: " + dataBuf.asCharBuffer().toString());		
			i += 10;
		}
```
## Basic Steps in Developing a DiSNI RDMA-based Application

The recommended way to program RDMA with DiSNI is to use the endpoint API. In extreme cases, the low level verbs API can be used which matches almost exactly the OFED native verbs interface. The verbs interface, however, requires the application to implement complex event handling for connection events just to set up proper network queues and connect them. The endpoint API relieves the application from that burden, while still offering the same powerful API for programming data operations. Here are the basic steps that are necessary to develop an RDMA client/server application using endpoints:

Define your own custom endpoints by extending either extending RdmaClientEndpoint or RdmaActiveClientEndpoint
```
	public class CustomServerEndpoint extends RdmaActiveClientEndpoint {
		public void init() throws IOException{
			super.init();
			//allocate and register buffers
			//initiate postRecv call to pre-post some recvs if necessary
			//...
		}
	}
```
Implement a factory for your custom endpoints
```
	public class CustomFactory implements RdmaEndpointFactory<CustomServerEndpoint> {
		private RdmaActiveEndpointGroup<CustomServerEndpoint> endpointGroup;
	
		public CustomServerEndpoint createClientEndpoint(RdmaCmId idPriv) throws IOException {
			return new CustomServerEndpoint(endpointGroup, idPriv);
		}	
	}
```
At the server, allocate an endpoint group and initialize it with the factory, create a server endpoint, bind it and accept connections
```
	RdmaActiveEndpointGroup endpointGroup = new RdmaActiveEndpointGroup<CustomServerEndpoint>();
	CustomFactory factory = new CustomFactory(endpointGroup);
	endpointGroup.init(factory);
	RdmaServerEndpoint<CustomServerEndpoint> endpoint = endpointGroup.createServerEndpoint();
	endpoint.bind(address);
	CustomServerEndpoint endpoint = serverEndpoint.accept();
```
At the client, also create a custom endpoint and factory (not shown) and connect your endpoint to the server
```
	RdmaActiveEndpointGroup endpointGroup = new RdmaActiveEndpointGroup<CustomClientEndpoint>();
	CustomFactory factory = new CustomFactory(endpointGroup);
	endpointGroup.init(factory);
	CustomClientEndpoint endpoint = endpointGroup.createClientEndpoint();
	endpoint.connect(address);
```
Once an endpoint is connected, RDMA data operations can be issued. For this, a descriptor that encodes the operation will have to be prepared. The descriptor encodes the type of operation (read, write, send, recv) and points to the data buffer that is involved.
```
	IbvMr mr = endpoint.registerMemory(buffer).execute().free();
	IbvSendWR sendWR = endpoint.getSendWR();
	sendWR.setOpcode(IbvSendWR.IBV_WR_RDMA_READ);
	sendWR.getRdma().setRemote_addr(mr.getAddr());
	sendWR.getRdma().setRkey(lkey);
```
To trigger the operation, a list of descriptors will have to be posted onto the connection. Each descriptor may further have multiple scatter/gather elements.
```
	SVCPostSend postSend = endpoint.postSend(decriptorList);
	postSend.execute().free()
```
A completion event is created by the network interface after the data buffer has been DMA's to the NIC. Depending on which type of endpoint group that is used, the event is signaled either through a callback, or has to be polled manually by the application. Once the completion event has been consumed, the data buffer can be reused.

## Choosing the EndpointGroup 

EndpointGroups are containers and factories for RDMA connections (RdmaEndpoint). There are two types of groups available and which type works best depends on the application. The RdmaActiveEndpointGroup actively processes network events caused by RDMA messages being transmitted or received. Events are signaled by calling dispatchCqEvent() which can be overriden by the custom endpoint of the application. The RdmaPassiveEndpointGroup provides a polling interface that allows the application to directly reap completion events from the network queue (completion queue). As such, the passive mode has typically lower latency but may suffer from contention in case of large numbers of threads operating on the same connection. The active mode, on the other hand, is more robust under large numbers of threads, but has higher latencies. 
