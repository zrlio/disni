lib_LTLIBRARIES = libdisni.la

INCLUDES = -I@jdk@/include -I@jdk@/include/linux

libdisni_la_SOURCES = com_ibm_disni_verbs_impl_NativeDispatcher.cpp

include_HEADERS = com_ibm_disni_verbs_impl_NativeDispatcher.h

libdisni_la_LIBADD = -libverbs -lrdmacm 

