
#include "com_ibm_disni_nvmef_spdk_NativeDispatcher.h"

#include <spdk/nvme.h>
#include <spdk/env.h>
#include <spdk/nvme_intel.h>
#include <spdk/nvmf_spec.h>
#include <spdk/pci_ids.h>

#include <cstdio>
#include <cstring>
#include <cerrno>

#define PACKAGE_NAME "com/ibm/disni/nvmef/spdk"

struct probe_ctx {
    size_t idx;
    jlong* ctrl_ids;
    size_t size;
};

static bool probe_cb(void *cb_ctx, const struct spdk_nvme_transport_id *trid,
        struct spdk_nvme_ctrlr_opts *opts) {
    probe_ctx* ctx = reinterpret_cast<probe_ctx*>(cb_ctx);
    return ctx->idx < ctx->size;
}

static void attach_cb(void *cb_ctx, const struct spdk_nvme_transport_id *trid,
        struct spdk_nvme_ctrlr *ctrlr, const struct spdk_nvme_ctrlr_opts *opts) {
    probe_ctx* ctx = reinterpret_cast<probe_ctx*>(cb_ctx);
    ctx->ctrl_ids[ctx->idx++] = reinterpret_cast<jlong>(ctrlr);
}

/*
 * Class:     com_ibm_disni_nvmef_spdk_NativeDispatcher
 * Method:    _nvme_probe
 * Signature: (IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_nvmef_spdk_NativeDispatcher__1nvme_1probe
  (JNIEnv* env, jobject thiz, jint type, jint address_family, jstring address, jstring service_id, jstring subsystemNQN, jlongArray controller_ids) {
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
    int ret = spdk_nvme_probe(&trid, reinterpret_cast<probe_ctx*>(&ctx),
            probe_cb, attach_cb, NULL);
    env->ReleaseLongArrayElements(controller_ids, ctrl_ids, 0);
    return ret;
}

