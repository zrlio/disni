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

## How to run a simple example

1. After building DiSNI, make sure DiSNI and its dependencies are in the classpath (e.g., disni-1.0-jar-with-dependencies.jar). Also add the DiSNI test jar (disni-1.0-tests.jar) which includes the examples.
2. Make sure libdisni.so and libaffinity are both in LD_LIBRARY_PATH
3. Run the server of the "read" example: java com.ibm.disni.examples.endpoints.read.JVerbsReadClient -a <Server IP Address>
4. Run the client of the "read" example: java com.ibm.disni.examples.endpoints.read.JVerbsReadClient -a <Server IP Address>

At the client, you should see an output similar to this:

ReadClient::preparing read operation...
ReadClient::read memory from server: This 
ReadClient::read memory from server: This is a 
ReadClient::read memory from server: This is a RDMA/
ReadClient::read memory from server: This is a RDMA/read 
ReadClient::read memory from server: This is a RDMA/read on st
ReadClient::read memory from server: This is a RDMA/read on stag 95
ReadClient::read memory from server: This is a RDMA/read on stag 9558435
ReadClient::read memory from server: This is a RDMA/read on stag 95584356 !
ReadClient::read memory from server: This is a RDMA/read on stag 95584356 !
ReadClient::read memory from server: This is a RDMA/read on stag 95584356 !
closing endpoint
closing endpoint, done

