add_library(verbs)

message("Add Jni by manual")
include_directories(/opt/hostedtoolcache/Java_Oracle_jdk/17/x64/include/linux)
include_directories(/opt/hostedtoolcache/Java_Oracle_jdk/17/x64/include)
include_directories(/opt/hostedtoolcache/Java_Oracle_jdk/17/x64/jre/lib/server)
include_directories(/opt/hostedtoolcache/Java_Oracle_jdk/17/x64/jre/lib)

message("Add rdma includes directory ")
include_directories(/tmp/rdma-core-44.0/build/include)
message("Add rdma libraries directory")
include_directories(/tmp/rdma-core-44.0/build/lib)

target_sources(verbs
    PRIVATE
      com_ibm_disni_verbs_impl_NativeDispatcher.cpp
    PUBLIC 
      com_ibm_disni_verbs_impl_NativeDispatcher.h)
