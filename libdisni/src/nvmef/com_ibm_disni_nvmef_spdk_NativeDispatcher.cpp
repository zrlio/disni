/*
 * jVerbs: RDMA verbs support for the Java Virtual Machine
 *
 * Author: Jonas Pfefferle <jpf@zurich.ibm.com>
 *
 * Copyright (C) 2016, IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#include "com_ibm_disni_nvmef_spdk_NativeDispatcher.h"

#include <spdk/nvme.h>
#include <spdk/env.h>
#include <spdk/nvme_intel.h>
#include <spdk/nvmf_spec.h>
#include <spdk/pci_ids.h>

#include <rte_config.h>
#include <rte_lcore.h>

#include <iostream>

#include <cstdio>
#include <cstring>
#include <cerrno>
#include <cstdlib>

#define PACKAGE_NAME "com/ibm/disni/nvmef/spdk"

static const char *ealargs[] = {
	"identify",
	"-c 0x1",
	"-n 4",
	"-m 512",
	"--proc-type=auto",
};

struct probe_ctx {
    size_t idx;
    jlong* ctrl_ids;
    jsize size;
};

struct io_completion {
    int status_code_type;
    int status_code;
};

static void initialize_dpdk() {
    static bool dpdk_initialized = false;
    if (!dpdk_initialized) {
        int ret = rte_eal_init(sizeof(ealargs) / sizeof(ealargs[0]),
                  (char **)(void *)(uintptr_t)ealargs);
        if (ret < 0) {
            std::cerr << "could not initialize dpdk\n";
            exit(1);
        }
        dpdk_initialized = true;
    }
}

static bool probe_cb(void *cb_ctx, const struct spdk_nvme_transport_id *trid,
        struct spdk_nvme_ctrlr_opts *opts) {
    probe_ctx* ctx = reinterpret_cast<probe_ctx*>(cb_ctx);
    return ctx->idx++ < ctx->size;
}

static void attach_cb(void *cb_ctx, const struct spdk_nvme_transport_id *trid,
        struct spdk_nvme_ctrlr *ctrlr, const struct spdk_nvme_ctrlr_opts *opts) {
    probe_ctx* ctx = reinterpret_cast<probe_ctx*>(cb_ctx);
    ctx->ctrl_ids[ctx->idx - 1] = reinterpret_cast<jlong>(ctrlr);
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_probe
 * Signature: (IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1probe
  (JNIEnv* env, jobject thiz, jint type, jint address_family, jstring address,
   jstring service_id, jstring subsystemNQN, jlongArray controller_ids) {
    initialize_dpdk();
    spdk_nvme_transport_id trid;
    trid.trtype = static_cast<spdk_nvme_transport_type>(type);
    trid.adrfam = static_cast<spdk_nvmf_adrfam>(address_family);
    if (env->IsSameObject(address, NULL)) {
        return -EFAULT;
    }
    const char* addr = env->GetStringUTFChars(address, NULL);
    if (addr == NULL) {
        return -EFAULT;
    }
    strncpy(trid.traddr, addr, sizeof(trid.traddr));
    env->ReleaseStringUTFChars(address, addr);

    if (env->IsSameObject(service_id, NULL)) {
        return -EFAULT;
    }
    const char* svcid = env->GetStringUTFChars(service_id, NULL);
    if (svcid == NULL) {
        return -EFAULT;
    }
    strncpy(trid.trsvcid, svcid, sizeof(trid.trsvcid));
    env->ReleaseStringUTFChars(service_id, svcid);

    if (env->IsSameObject(subsystemNQN, NULL)) {
        return -EFAULT;
    }
    const char* subnqn = env->GetStringUTFChars(subsystemNQN, NULL);
    if (subnqn == NULL) {
        return -EFAULT;
    }
    strncpy(trid.subnqn, subnqn, sizeof(trid.subnqn));
    env->ReleaseStringUTFChars(subsystemNQN, subnqn);

    jboolean is_copy;
    jlong* ctrl_ids = env->GetLongArrayElements(controller_ids, &is_copy);
    if (ctrl_ids == NULL) {
        return -EFAULT;
    }
    probe_ctx ctx = {0, ctrl_ids, env->GetArrayLength(controller_ids)};
    int ret = spdk_nvme_probe(&trid, &ctx, probe_cb, attach_cb, NULL);
    env->ReleaseLongArrayElements(controller_ids, ctrl_ids, 0);
    return ret < 0 ? ret : ctx.idx;
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ctrlr_get_num_ns
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ctrlr_1get_1num_1ns
  (JNIEnv* env, jobject thiz, jlong controller_id) {
    spdk_nvme_ctrlr* ctrlr = reinterpret_cast<spdk_nvme_ctrlr*>(controller_id);
    return spdk_nvme_ctrlr_get_num_ns(ctrlr);
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ctrlr_get_ns
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ctrlr_1get_1ns
  (JNIEnv* env, jobject thiz, jlong controller_id, jint nid) {
    spdk_nvme_ctrlr* ctrlr = reinterpret_cast<spdk_nvme_ctrlr*>(controller_id);
    spdk_nvme_ns* ns = spdk_nvme_ctrlr_get_ns(ctrlr, nid);
    return reinterpret_cast<jlong>(ns);
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ctrlr_get_data
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ctrlr_1get_1data
  (JNIEnv* env, jobject thiz, jlong controller_id, jlong address) {
    spdk_nvme_ctrlr* ctrlr = reinterpret_cast<spdk_nvme_ctrlr*>(controller_id);
    const spdk_nvme_ctrlr_data* ctrlr_data = spdk_nvme_ctrlr_get_data(ctrlr);
    //FIXME: complete data
    memcpy(reinterpret_cast<void*>(address), ctrlr_data, 1024);
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ctrlr_alloc_io_qpair
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ctrlr_1alloc_1io_1qpair
  (JNIEnv* env, jobject thiz, jlong controller_id, jint priority) {
    spdk_nvme_ctrlr* ctrlr = reinterpret_cast<spdk_nvme_ctrlr*>(controller_id);
    spdk_nvme_qpair* qpair = spdk_nvme_ctrlr_alloc_io_qpair(ctrlr, static_cast<spdk_nvme_qprio>(0));
    return reinterpret_cast<jlong>(qpair);
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ns_is_active
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ns_1is_1active
  (JNIEnv* env, jobject thiz, jlong namespace_id) {
    spdk_nvme_ns* ns = reinterpret_cast<spdk_nvme_ns*>(namespace_id);
    return spdk_nvme_ns_is_active(ns);
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ns_get_size
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ns_1get_1size
  (JNIEnv* env, jobject thiz, jlong namespace_id) {
    spdk_nvme_ns* ns = reinterpret_cast<spdk_nvme_ns*>(namespace_id);
    return spdk_nvme_ns_get_size(ns);
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ns_get_sector_size
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ns_1get_1sector_1size
  (JNIEnv* env, jobject thiz, jlong namespace_id) {
    spdk_nvme_ns* ns = reinterpret_cast<spdk_nvme_ns*>(namespace_id);
    return spdk_nvme_ns_get_sector_size(ns);
}

static void command_cb(void* cb_data, const struct spdk_nvme_cpl* nvme_completion) {
    io_completion* completion = reinterpret_cast<io_completion*>(cb_data);
    completion->status_code = nvme_completion->status.sc;
    completion->status_code_type = nvme_completion->status.sct;
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ns_io_cmd
 * Signature: (JJJJIJZ)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ns_1io_1cmd
  (JNIEnv* env, jobject thiz, jlong namespace_id, jlong qpair_id, jlong address,
   jlong linearBlockAddress, jint count, jlong completion_address, jboolean write) {
    spdk_nvme_ns* ns = reinterpret_cast<spdk_nvme_ns*>(namespace_id);
    spdk_nvme_qpair* qpair = reinterpret_cast<spdk_nvme_qpair*>(qpair_id);

    void* cb_data = reinterpret_cast<io_completion*>(completion_address);
    void* payload = reinterpret_cast<void*>(address);
    int ret;
    if (write) {
        ret = spdk_nvme_ns_cmd_write(ns, qpair, payload, linearBlockAddress,
                count, command_cb, cb_data, 0);
    } else {
        ret = spdk_nvme_ns_cmd_read(ns, qpair, payload, linearBlockAddress,
                count, command_cb, cb_data, 0);
    }
    return ret;
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_qpair_process_completions
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1qpair_1process_1completions
  (JNIEnv* env, jobject thiz, jlong qpair_id, jint max_completions) {
    spdk_nvme_qpair* qpair = reinterpret_cast<spdk_nvme_qpair*>(qpair_id);
    return spdk_nvme_qpair_process_completions(qpair, max_completions);
}

