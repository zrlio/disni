# DiSNI: Direct Storage and Networking Interface

DiSNI is a Java library for direct storage and networking access from userpace. It currently provides an RDMA interface to access remote memory, and an NVMf interface to access remote NVMe storage. DiSNI enables the development of Java applications for high performance RDMA networks, such as InfiniBand, iWARP, or RoCE. The RDMA API is implemented based on the Open Fabrics Enterprise Distribution (OFED) RDMA user libraries. The NVMf APIs are implemented on top of the Storage Performance Development Kit ([SPDK](http://www.spdk.io)). Both APIs provide RDMA semantics including asynchronous operations, zero-copy transmission and direct data placement. 

## Building DiSNI

Building the source requires [Apache Maven](http://maven.apache.org/) and [GNU/autotools](http://www.gnu.org/software/autoconf/autoconf.html) and Java version 8 or higher.
To build DiSNI and its example programs, execute the following steps:

1) Obtain a copy of DiSNI from [Github](https://github.com/zrlio/disni)<br/>

**To compile with RDMA support only**<br/>
2) Build libdisni using: ./autoprepare.sh; ./configure --with-jdk=path-to-jdk; make install<br/>
**Or to compile with RDMA and NVMf support**<br/>
2.1) Obtain spdk from [Github](https://github.com/spdk/spdk) and follow build instructions<br/>
2.2) Compile dpdk with `CONFIG_RTE_BUILD_SHARED_LIB=y`<br/>
2.3) Build libdisni using: ./autoprepare.sh; ./configure --with-jdk=path-to-jdk --with-spdk=path-to-spdk --with-dpdk=path-to-dpdk; make install<br/>

3) Run: mvn -DskipTests install

## How to Run the Examples

Common steps:

1. After building DiSNI, make sure DiSNI and its dependencies are in the classpath (e.g., disni-1.0-jar-with-dependencies.jar). Also add the DiSNI test jar (disni-1.0-tests.jar) which includes the examples.<br/>
2. Make sure libdisni is part of the LD_LIBRARY_PATH

### RDMA example
1. Make sure the RDMA network interface is configured and up on the test machines (run ibv\_devices to see the list of RDMA NICs). If your machine does not have RDMA hardware, you can also use SoftiWARP from [Github](https://github.com/zrlio/softiwarp). 
2. Run the server\: java com.ibm.disni.examples.ReadServer -a \<server IP\>
3. Run the client\: java com.ibm.disni.examples.ReadClient -a \<server IP\>

### NVMf example
1. Include DPDK library dependencies in LD_LIBRARY_PATH (\*.so files are required)
2. Run the server\: java com.ibm.disni.benchmarks.NvmfEndpointServer \<local NVMe PCI address\> \<NVMe qualified name\> \<server ip\> \<port\>
3. Run the client\: java.com.ibm.disni.benchmarks.NvmfEndpointClient \<server ip\> \<port\> \<NVMe qualified name\>

## Programming with DiSNI

To use DiSNI in your maven application use the following snippet in your pom.xml file (you need to build DiSNI before to update your local maven repo):

    <dependency>
      <groupId>com.ibm.disni</groupId>
      <artifactId>disni</artifactId>
      <version>1.0</version>
    </dependency>
    
Here are the basic steps that are necessary to develop an RDMA client/server application using endpoints:

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
	postSend.execute();
```
A completion event is created by the network interface after the data buffer has been DMA's to the NIC. Depending on which type of endpoint group that is used, the event is signaled either through a callback, or has to be polled manually by the application. Once the completion event has been consumed, the data buffer can be reused.

### What are Stateful Verb Calls (SVCs)

To avoid any performance impacts that are associated with passing complex RDMA parameters and arrays through the JNI interface, the DiSNI library implements a concept called stateful verbs call (SVC). With this approach, each JNI serialization state for a verb call is cached in the context of an SVM object and can be reused many times.

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
		postSend.free();
```
### Choosing the EndpointGroup 

EndpointGroups are containers and factories for RDMA connections (RdmaEndpoint). There are two types of groups available and which type works best depends on the application. The RdmaActiveEndpointGroup actively processes network events caused by RDMA messages being transmitted or received. Events are signaled by calling dispatchCqEvent() which can be overriden by the custom endpoint of the application. The RdmaPassiveEndpointGroup provides a polling interface that allows the application to directly reap completion events from the network queue (completion queue). As such, the passive mode has typically lower latency but may suffer from contention in case of large numbers of threads operating on the same connection. The active mode, on the other hand, is more robust under large numbers of threads, but has higher latencies. Often it is the best option to use active endpoints at the server, and passive connections at the client. Passive endpoints are typically the right choice if the application knows when messages will be received and, thus, can poll the completion queue accordingly. 

## Contributions

PRs are always welcome. Please fork, and make necessary modifications 
you propose, and let us know. 

## Contact 

If you have questions or suggestions, feel free to post at:

https://groups.google.com/forum/#!forum/zrlio-users

or email: zrlio-users@googlegroups.com
