noinst_LTLIBRARIES = libnvmef.la

if SPDK
AM_CPPFLAGS = -I@jdk@/include -I@jdk@/include/linux -I@spdk@/include -I@dpdk@/include/dpdk -include @spdk@/config.h -I@spdk@/lib/nvme
include_HEADERS = com_ibm_disni_nvmef_spdk_NativeDispatcher.h
libnvmef_la_SOURCES = com_ibm_disni_nvmef_spdk_NativeDispatcher.cpp
libnvmef_la_LIBADD = -L@spdk@/build/lib -lspdk_nvmf -lspdk_nvme -lspdk_env_dpdk -lspdk_log -lspdk_util -lspdk_trace
endif
