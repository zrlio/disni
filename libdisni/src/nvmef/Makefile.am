noinst_LTLIBRARIES = libnvmef.la

INCLUDES = -I@jdk@/include -I@jdk@/include/linux -I@spdk@/include -I@spdk@/dpdk-16.11/include/dpdk -include @spdk@/config.h

libnvmef_la_SOURCES = com_ibm_disni_nvmef_spdk_NativeDispatcher.cpp

include_HEADERS = com_ibm_disni_nvmef_spdk_NativeDispatcher.h

libnvmef_la_LIBADD = -L@spdk@/build/lib -lspdk_nvme -lspdk_env_dpdk -lspdk_log -lspdk_util -L@spdk@/dpdk-16.11/lib -lrte_eal -lrte_mempool

