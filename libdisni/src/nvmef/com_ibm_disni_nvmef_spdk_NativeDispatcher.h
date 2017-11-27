/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_ibm_disni_nvmef_spdk_NativeDispatcher */

#ifndef _Included_com_ibm_disni_nvmef_spdk_NativeDispatcher
#define _Included_com_ibm_disni_nvmef_spdk_NativeDispatcher
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _env_init
 * Signature: (J[I)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1env_1init
  (JNIEnv *, jobject, jlong, jintArray);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _log_set_trace_flag
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1log_1set_1trace_1flag
  (JNIEnv *, jobject, jstring);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _malloc
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1malloc
  (JNIEnv *, jobject, jlong, jlong);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _free
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1free
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_transport_available
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1transport_1available
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_probe
 * Signature: (IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1probe
  (JNIEnv *, jobject, jint, jint, jstring, jstring, jstring, jlongArray);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_detach
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1detach
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ctrlr_get_num_ns
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ctrlr_1get_1num_1ns
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ctrlr_get_ns
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ctrlr_1get_1ns
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ctrlr_get_data
 * Signature: (JJI)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ctrlr_1get_1data
  (JNIEnv *, jobject, jlong, jlong, jint);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ctrlr_get_opts
 * Signature: (JJI)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ctrlr_1get_1opts
  (JNIEnv *, jobject, jlong, jlong, jint);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ctrlr_alloc_io_qpair
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ctrlr_1alloc_1io_1qpair__J
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ctrlr_alloc_io_qpair
 * Signature: (JIII)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ctrlr_1alloc_1io_1qpair__JIII
  (JNIEnv *, jobject, jlong, jint, jint, jint);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ctrlr_free_io_qpair
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ctrlr_1free_1io_1qpair
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ctrlr_process_admin_completions
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ctrlr_1process_1admin_1completions
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ns_is_active
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ns_1is_1active
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ns_get_size
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ns_1get_1size
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ns_get_sector_size
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ns_1get_1sector_1size
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ns_get_max_io_xfer_size
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ns_1get_1max_1io_1xfer_1size
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ns_io_cmd
 * Signature: (JJJJIJZ)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ns_1io_1cmd
  (JNIEnv *, jobject, jlong, jlong, jlong, jlong, jint, jlong, jboolean);

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_qpair_process_completions
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1qpair_1process_1completions
  (JNIEnv *, jobject, jlong, jint);

#ifdef __cplusplus
}
#endif
#endif
