add_library(affinity)

message("Add Jni by manual")
include_directories(/opt/hostedtoolcache/Java_Oracle_jdk/17/x64/include)
include_directories(/opt/hostedtoolcache/Java_Oracle_jdk/17/x64/include/darwin)
include_directories(/opt/hostedtoolcache/Java_Oracle_jdk/17/x64/jre/lib)
include_directories(/opt/hostedtoolcache/Java_Oracle_jdk/17/x64/jre/lib/server)

target_sources(affinity
    PRIVATE
      com_ibm_disni_util_NativeAffinity.c
    PUBLIC 
      com_ibm_disni_util_NativeAffinity.h
)
