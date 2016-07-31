# DiSNI: Direct Storage and Networking Interface

DiSNI is a Java library for direct storage and networking access from userpace. It currently provides a RDMA interface. Support for other userpace storage and networking interfaces, such as DPDK or SPDK, are in planning. 

## RDMA APIs

The RDMA APIs enable the development of Java applications for high performance RDMA networks, such as InfiniBand, iWARP, or RoCE. There exist two sets of APIs, the low level verbs API and the higher level endpoint API. The verbs interface matches the well known OFED verbs interface available in the native C language and thereby offers maximum control over all RDMA resources. The endpoint API, on the other hand, offers a simplified programming interface. Both APIs offers the fundamental RDMA operations such as two-sided and one-sided operations, zero-copy transmission and direct data placement. 

The RDMA APIs in DiSNI are implemented based on the Open Fabrics Enterprise Distribution (OFED) RDMA user libraries. A thin JNI layer is used to bridge between Java code and the OFED user libraries. To avoid any performance impacts that are associated with passing complex RDMA parameters and arrays through the JNI interface, the DiSNI library implements a concept called stateful verbs method (SVM). With this approach, each JNI serialization state for a verb call is cached in the context of an SVM object and can be reused many times.

## Building DiSNI

Building the source requires [Apache Maven](http://maven.apache.org/) and [GNU/autotools](http://www.gnu.org/software/autoconf/autoconf.html).
To build DiSNI and its example programs, execute the following steps:

1. Obtain a copy of DiSNI from [Github](https://github.com/zrlio/disni)
2. Build libdisni and libaffinity using: ./autoprepare.sh; ./configure --with-jdk=path-to-jdk; make install
3. Run: mvn -DskipTests install

## How to Run the READ Example

1. After building DiSNI, make sure DiSNI and its dependencies are in the classpath (e.g., disni-1.0-jar-with-dependencies.jar). Also add the DiSNI test jar (disni-1.0-tests.jar) which includes the examples.
2. Make sure libdisni and libaffinity are both part of the LD_LIBRARY_PATH
3. Make sure the RDMA network interface is configured and up on the test machines (run ibv\_devices to see the list of RDMA NICs). If your machine does not have RDMA hardware, you can also use SoftiWARP from [Github](https://github.com/zrlio/softiwarp). 
4. Run the server\: java com.ibm.disni.examples.endpoints.read.JVerbsReadClient -a \<server IP\>
5. Run the client\: java com.ibm.disni.examples.endpoints.read.JVerbsReadClient -a \<server IP\>

## What are Stateful Verb Calls (SVCs)

Stateful verb calls are objects representing RDMA operations. SVCs encapsulate the serialization state of the network operation (just the operation, not the data). If an operation is executed multiple times (typically with the content of the source or sink buffer changing), the serialization overhead can be saved, which in turn will lead to the highest possible performance. SVCs can also be modified, for instance when transmitting data at different offsets. Modifications to SVC objects are efficient as they do not require serialization. 

A good example showcasing the use of SVCs can be found in JVerbsReadClient.java\:

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




