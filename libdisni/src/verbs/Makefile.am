noinst_LTLIBRARIES = libverbs.la

AM_CPPFLAGS = -I@jdk@/include -I@jdk@/include/linux

libverbs_la_SOURCES = com_ibm_disni_rdma_verbs_impl_NativeDispatcher.cpp

include_HEADERS = com_ibm_disni_rdma_verbs_impl_NativeDispatcher.h

libverbs_la_LIBADD = -libverbs -lrdmacm 

