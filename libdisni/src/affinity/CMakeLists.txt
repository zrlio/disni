add_library(affinity)

find_package(jni REQUIRED)

if (JNI_FOUND)
    message("Jni is found we got it include dir ${JNI_INCLUDE_DIRS}")
    #include_directories(${JNI_INCLUDE_DIRS})
    include_directories(/Library/Java/JavaVirtualMachines/jdk1.8.0_351.jdk/Contents/Home/include)
    #include_directories(/Library/Java/JavaVirtualMachines/jdk1.8.0_351.jdk/Contents/Home/include/darwin)

    message("Jni is found we got it library dir ${JNI_LIBRARIES}")
    include_directories(/Library/Java/JavaVirtualMachines/jdk1.8.0_351.jdk/Contents/Home/jre/lib/server/libjvm.dylibs)
endif(Jni_FOUND)

target_sources(affinity
    PRIVATE
      com_ibm_disni_util_NativeAffinity.c
    PUBLIC 
      com_ibm_disni_util_NativeAffinity.h
)