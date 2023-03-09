add_library(affinity)

find_package(jni REQUIRED)

if (JNI_FOUND)
    message("Jni is found we got it include dir ${JNI_INCLUDE_DIRS}")
    #include_directories(${JNI_INCLUDE_DIRS})
    include_directories(/Library/Java/JavaVirtualMachines/jdk1.8.0_351.jdk/Contents/Home/include)
    #include_directories(/Library/Java/JavaVirtualMachines/jdk1.8.0_351.jdk/Contents/Home/include/darwin)

    message("Jni is found we got it library dir ${JNI_LIBRARIES}")
    include_directories(/Library/Java/JavaVirtualMachines/jdk1.8.0_351.jdk/Contents/Home/jre/lib/server/libjvm.dylibs)
else() 
    message("Add Jni by manual")
   include_directories(/opt/hostedtoolcache/Java_Oracle_jdk/17/x64/include)
   include_directories(/opt/hostedtoolcache/Java_Oracle_jdk/17/x64/include/darwin)
   include_directories(/opt/hostedtoolcache/Java_Oracle_jdk/17/x64/jre/lib)
   include_directories(/opt/hostedtoolcache/Java_Oracle_jdk/17/x64/jre/lib/server)
endif(Jni_FOUND)

target_sources(affinity
    PRIVATE
      com_ibm_disni_util_NativeAffinity.c
    PUBLIC 
      com_ibm_disni_util_NativeAffinity.h
)
