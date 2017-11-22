/*
 * DiSNI: Direct Storage and Networking Interface
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


//FIXME: spdk is missing extern C in some headers
extern "C" {
#include <spdk/log.h>
#include <spdk/nvmf.h>
//XXX
#include <nvme_internal.h>
#include <env_internal.h>
#include <spdk_internal/event.h>
}


#include <rte_config.h>
#include <rte_lcore.h>
#include <rte_errno.h>

#include <sched.h>
#include <unistd.h>

#include <iostream>
#include <sstream>
#include <vector>

#include <cstdio>
#include <cstring>
#include <cerrno>
#include <cstdlib>

struct probe_ctx {
    size_t probe_count;
    size_t attach_idx;
    jlong* ctrl_ids;
    jsize size;
};

struct completed_array {
    int index;
    long ids[0];
};

struct io_completion {
    int status_code_type;
    int status_code;
    const long id;
    completed_array* completed;
};

class JNIString {
    private:
        jstring str_;
        JNIEnv* env_;
        const char* c_str_;
    public:
        JNIString(JNIEnv* env, jstring str) : str_(str), env_(env),
        c_str_(env_->GetStringUTFChars(str, NULL)) {}

        ~JNIString() {
            if (c_str_ != NULL) {
                env_->ReleaseStringUTFChars(str_, c_str_);
            }
        }

        const char* c_str() const {
            return c_str_;
        }
};

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _env_init
 * Signature: (Ljava/lang/String;J[I)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1env_1init
  (JNIEnv* env, jobject thiz, jstring huge_path, jlong memory_size_MB, jintArray transport_types) {
    spdk_env_opts opts;
    spdk_env_opts_init(&opts);

    jsize jtransport_types_size = env->GetArrayLength(transport_types);
    jint* jtransport_types = env->GetIntArrayElements(transport_types, NULL);
    bool pcie = false;
    for (jsize i = 0; i < jtransport_types_size; i++) {
        if (jtransport_types[i] == SPDK_NVME_TRANSPORT_PCIE) {
            pcie = true;
        }
    }
    env->ReleaseIntArrayElements(transport_types, jtransport_types, 0);

    if (env->IsSameObject(huge_path, NULL)) {
        return -EFAULT;
    }
    JNIString jHugePath(env, huge_path);
    if (jHugePath.c_str() == NULL) {
        return -EFAULT;
    }
    int args_idx = 0;
    std::vector<const char*> cargs;
    cargs.push_back("--huge-dir");
    cargs.push_back(jHugePath.c_str());

    if (!pcie) {
        cargs.push_back("--no-pci");
    }

    cargs.push_back("--proc");
    cargs.push_back("primary");

    cargs.push_back("--file-prefix");
    pid_t pid = getpid();
    std::stringstream prefix;
    prefix << "rte_" << pid;
    std::string prefix_str = prefix.str();
    cargs.push_back(prefix_str.c_str());

    cargs.push_back("-m");
    std::stringstream memory_size_ss;
    memory_size_ss << memory_size_MB;
    std::string memory_size_str = memory_size_ss.str();
    cargs.push_back(memory_size_str.c_str());

    //XXX we should call spdk_env_init but it does not allow to set --no-pci etc
    int ret = rte_eal_init(cargs.size(), const_cast<char**>(cargs.data()));
    spdk_vtophys_register_dpdk_mem();
    return ret < 0 ? rte_errno : ret;
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _malloc
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1malloc
  (JNIEnv* env, jobject thiz, jlong size, jlong alignment) {
    return reinterpret_cast<jlong>(spdk_malloc(size, alignment, NULL));
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _free
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1free
  (JNIEnv* env, jobject thiz, jlong address) {
    spdk_free(reinterpret_cast<void*>(address));
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _log_set_trace_flag
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1log_1set_1trace_1flag
  (JNIEnv* env, jobject thiz, jstring name) {
    JNIString flag(env, name);
    return spdk_log_set_trace_flag(flag.c_str());
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_transport_available
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1transport_1available
  (JNIEnv* env, jobject thiz, jint transport_type) {
    spdk_nvme_transport_type trtype =
        static_cast<spdk_nvme_transport_type>(transport_type);
    return spdk_nvme_transport_available(trtype);
}

static bool probe_cb(void *cb_ctx, const struct spdk_nvme_transport_id *trid,
        struct spdk_nvme_ctrlr_opts *opts) {
    probe_ctx* ctx = reinterpret_cast<probe_ctx*>(cb_ctx);
    return ctx->probe_count++ < ctx->size;
}

static void attach_cb(void *cb_ctx, const struct spdk_nvme_transport_id *trid,
        struct spdk_nvme_ctrlr *ctrlr, const struct spdk_nvme_ctrlr_opts *opts) {
    probe_ctx* ctx = reinterpret_cast<probe_ctx*>(cb_ctx);
    ctx->ctrl_ids[ctx->attach_idx++] = reinterpret_cast<jlong>(ctrlr);
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_probe
 * Signature: (IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1probe
  (JNIEnv* env, jobject thiz, jint type, jint address_family, jstring address,
   jstring service_id, jstring subsystemNQN, jlongArray controller_ids) {
    spdk_nvme_transport_id trid = {};
    trid.trtype = static_cast<spdk_nvme_transport_type>(type);
    trid.adrfam = static_cast<spdk_nvmf_adrfam>(address_family);
    if (env->IsSameObject(address, NULL)) {
        return -EFAULT;
    }
    JNIString addr(env, address);
    if (addr.c_str() == NULL) {
        return -EFAULT;
    }
    strncpy(trid.traddr, addr.c_str(), sizeof(trid.traddr));

    if (env->IsSameObject(service_id, NULL)) {
        return -EFAULT;
    }
    JNIString svcid(env, service_id);
    if (svcid.c_str() == NULL) {
        return -EFAULT;
    }
    strncpy(trid.trsvcid, svcid.c_str(), sizeof(trid.trsvcid));

    if (env->IsSameObject(subsystemNQN, NULL)) {
        return -EFAULT;
    }
    JNIString subnqn(env, subsystemNQN);
    if (subnqn.c_str() == NULL) {
        return -EFAULT;
    }
    strncpy(trid.subnqn, subnqn.c_str(), sizeof(trid.subnqn));

    jboolean is_copy;
    jlong* ctrl_ids = env->GetLongArrayElements(controller_ids, &is_copy);
    if (ctrl_ids == NULL) {
        return -EFAULT;
    }
    probe_ctx ctx = {0, 0, ctrl_ids, env->GetArrayLength(controller_ids)};
    int ret = spdk_nvme_probe(&trid, &ctx, probe_cb, attach_cb, NULL);
    env->ReleaseLongArrayElements(controller_ids, ctrl_ids, 0);
    return ret < 0 ? ret : ctx.attach_idx;
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_detach
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1detach
  (JNIEnv* env, jobject thiz, jlong controller_id) {
    spdk_nvme_ctrlr* ctrlr = reinterpret_cast<spdk_nvme_ctrlr*>(controller_id);
    return spdk_nvme_detach(ctrlr);
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
 * Method:    _nvme_ctrlr_get_opts
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ctrlr_1get_1opts
  (JNIEnv* env, jobject thiz, jlong controller_id, jlong address) {
    spdk_nvme_ctrlr* ctrlr = reinterpret_cast<spdk_nvme_ctrlr*>(controller_id);
    const spdk_nvme_ctrlr_opts* ctrlr_opts = &ctrlr->opts;
    memcpy(reinterpret_cast<void*>(address), ctrlr_opts, sizeof(*ctrlr_opts));
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
 * Method:    _nvme_ctrlr_free_io_qpair
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ctrlr_1free_1io_1qpair
  (JNIEnv* env, jobject thiz, jlong qpair_id) {
    spdk_nvme_qpair* qpair = reinterpret_cast<spdk_nvme_qpair*>(qpair_id);
    return spdk_nvme_ctrlr_free_io_qpair(qpair);
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
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ns_1get_1sector_1size
  (JNIEnv* env, jobject thiz, jlong namespace_id) {
    spdk_nvme_ns* ns = reinterpret_cast<spdk_nvme_ns*>(namespace_id);
    return spdk_nvme_ns_get_sector_size(ns);
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_ns_get_max_io_xfer_size
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1ns_1get_1max_1io_1xfer_1size
  (JNIEnv* env, jobject thiz, jlong namespace_id) {
    spdk_nvme_ns* ns = reinterpret_cast<spdk_nvme_ns*>(namespace_id);
    return spdk_nvme_ns_get_max_io_xfer_size(ns);
}

static void command_cb(void* cb_data, const struct spdk_nvme_cpl* nvme_completion) {
    volatile io_completion* completion = reinterpret_cast<volatile io_completion*>(cb_data);
    completion->status_code = nvme_completion->status.sc;
    completion->status_code_type = nvme_completion->status.sct;
    completed_array* ca = completion->completed;
    ca->ids[ca->index++] = completion->id;
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

