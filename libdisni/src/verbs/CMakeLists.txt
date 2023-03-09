add_library(verbs)

find_package(jni REQUIRED)

if (JNI_FOUND)
    message("Jni is found we got it include dir ${JNI_INCLUDE_DIRS}")
    include_directories(${JNI_INCLUDE_DIRS})

    message("Jni is found we got it library dir ${JNI_LIBRARIES}")
    include_directories(${JNI_LIBRARIES})
endif(Jni_FOUND)

target_sources(verbs
    PRIVATE
      com_ibm_disni_verbs_impl_NativeDispatcher.cpp
    PUBLIC 
      com_ibm_disni_verbs_impl_NativeDispatcher.h)