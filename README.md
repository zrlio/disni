# DiSNI: Direct Storage and Networking Interface

DiSNI is a Java library for direct storage and networking access from userpace. It currently provides an RDMA interface to access remote memory, and an NVMf interface to access remote NVMe storage. DiSNI enables the development of Java applications for high performance RDMA networks, such as InfiniBand, iWARP, or RoCE. The RDMA API is implemented based on the Open Fabrics Enterprise Distribution (OFED) RDMA user libraries. The NVMf APIs are implemented on top of the Storage Performance Development Kit ([SPDK](http://www.spdk.io)). Both APIs provide RDMA semantics including asynchronous operations, zero-copy transmission and direct data placement. 

## Building DiSNI

Building the source requires [Apache Maven](http://maven.apache.org/) and [GNU/autotools](http://www.gnu.org/software/autoconf/autoconf.html) and Java version 8 or higher.
To build DiSNI and its example programs, obtain a copy of DiSNI from [Github](https://github.com/zrlio/disni) and execute the following steps:

1. Compile the Java sources using: mvn -DskipTests install
2. Compile libdisni using: cd libdisni; ./autoprepare.sh; ./configure --with-jdk=\<path\>; make install

By default DiSNI will only build with RDMA support. To enable NVMf support execute the following steps when compiling libdisni:

2. Obtain dpdk from [dpdk.org](http://dpdk.org/download) (version 17.02)\
3. Build dpdk using: make install T=x86_64-native-linuxapp-gcc DESTDIR=. CONFIG_RTE_BUILD_SHARED_LIB=y
4. Obtain spdk from [Github](https://github.com/spdk/spdk) (version 17.03)
5. Build spdk using: make DPDK_DIR=\<dpdk-path/x86_64-native-linuxapp-gcc\> CONFIG_RDMA=y
6. Configure libdisni for NVMf: ./configure --with-jdk=\<path\> --with-spdk=\<path\> --with-dpdk=\<path/x86_64-native-linuxapp-gcc\>
7. Build libdisni: make install
8. Make sure shared libraries of DPDK are in the LD_LIBRARY_PATH

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

DiSNI is part of maven central, therefore the simplest way to use DiSNI in your maven application is to add the following snippet to your application pom.xml file.

    <dependency>
      <groupId>com.ibm.disni</groupId>
      <artifactId>disni</artifactId>
      <version>1.1</version>
    </dependency>
    
The RDMA and NVMf APIs in DiSNI are both following the Group/Endpoint model which is based on three key data types (interfaces):

* DiSNIServerEndpoint: 
  * represents a listerning server waiting for new connections
  * contains methods to bind() to a specific port and to accept() new connections 
* DiSNIEndpoint: 
  * represents a connection to a remote (or local) resource (e.g., RDMA or NVMf) 
  * offers non-blocking methods to read() or write() the resource
* DiSNIGroup: 
  * a container and a factory for both client and server endpoints 
  
Specific implementations of these interface in DiSNI/RDMA and DiSNI/NVMf are offering extra functionality tailored to their purpose. 

### Stateful Operations

To avoid any performance impacts that are associated with passing complex parameters and arrays through the JNI interface, the DiSNI library implements stateful method calls (SMC). With this approach, the JNI serialization state for a particular call is cached in the context of an SMC object and can be reused many times. SMC objects can also be modified, for instance when transmitting data at different offsets. Modifications to SMC objects are efficient as they do not require serialization. It is key that SMC objecs are re-used whenever possible to avoid garbage collection overheads. 

### Programming NVMf using DiSNI

Let's have a look at a simple client/server application using the DiSNI NVMf API. Here is an example of a simple NVMf server exporting a NVMe device with PCI address 0000:86:00.0. The server is listening on port 5050 and waits for new connections:
```
NvmeTransportType transports[] = new NvmeTransportType[]{NvmeTransportType.PCIE, NvmeTransportType.RDMA}; 
String hugePages = "/dev/hugepages";
long perSocketMemory[] = new long[]{256,256};
NvmeEndpointGroup group = new NvmeEndpointGroup(transports, hugePages, perSocketMemory);
NvmeServerEndpoint serverEndpoint = group.createServerEndpoint();
URI url = new URI("nvmef://192.168.0.1:5050/0/1?subsystem=nqn.2016-06.io.spdk:cnode1&pci=0000:86:00.0");
serverEndpoint.bind(url);
		
while(true){
	NvmeEndpoint endpoint = serverEndpoint.accept();
}
```

Here is an example of a simple NVMf client. The client issues a 512 byte read operation against the remote flash resource exposed by the server above. 
```
NvmeTransportType transports[] = new NvmeTransportType[]{NvmeTransportType.RDMA}; 
String hugePages = "/dev/hugepages";
long perSocketMemory[] = new long[]{256,256};	
NvmeEndpointGroup group = new NvmeEndpointGroup(transports, hugePages, perSocketMemory);
NvmeEndpoint endpoint = group.createEndpoint();
		
URI uri = new URI("nvmef://192.168.0.1:5050/0/1?subsystem=nqn.2016-06.io.spdk:cnode1");
endpoint.connect(uri);
ByteBuffer buffer = ByteBuffer.allocateDirect(512);
NvmeCommand command = endpoint.read(buffer, 0);
command.execute();
```

### Programming RDMA using DiSNI
    
Here are the basic steps that are necessary to develop an RDMA client/server application using DiSNI. First, define your own custom endpoints by extending either extending RdmaClientEndpoint or RdmaActiveClientEndpoint
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

A good example showcasing the use of SMC can be found in JVerbsReadClient.java\:
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

As mentioned earlier, EndpointGroups are containers and factories for RDMA connections (RdmaEndpoint). There are two types of groups available in the RDMA API, and which type works best depends on the application. The RdmaActiveEndpointGroup actively processes network events caused by RDMA messages being transmitted or received. Events are signaled by calling dispatchCqEvent() which can be overriden by the custom endpoint of the application. The RdmaPassiveEndpointGroup provides a polling interface that allows the application to directly reap completion events from the network queue (completion queue). As such, the passive mode has typically lower latency but may suffer from contention in case of large numbers of threads operating on the same connection. The active mode, on the other hand, is more robust under large numbers of threads, but has higher latencies. Often it is the best option to use active endpoints at the server, and passive connections at the client. Passive endpoints are typically the right choice if the application knows when messages will be received and, thus, can poll the completion queue accordingly. 

## Contributions

PRs are always welcome. Please fork, and make necessary modifications 
you propose, and let us know. 

## Contact 

If you have questions or suggestions, feel free to post at:

https://groups.google.com/forum/#!forum/zrlio-users

or email: zrlio-users@googlegroups.com
