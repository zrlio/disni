add_library(verbs)

message("Add Jni by manual")
include_directories(/opt/hostedtoolcache/Java_Oracle_jdk/17/x64/include/darwin)
include_directories(/opt/hostedtoolcache/Java_Oracle_jdk/17/x64/include)
include_directories(/opt/hostedtoolcache/Java_Oracle_jdk/17/x64/jre/lib/server)
include_directories(/opt/hostedtoolcache/Java_Oracle_jdk/17/x64/jre/lib)

target_sources(verbs
    PRIVATE
      com_ibm_disni_verbs_impl_NativeDispatcher.cpp
    PUBLIC 
      com_ibm_disni_verbs_impl_NativeDispatcher.h)
