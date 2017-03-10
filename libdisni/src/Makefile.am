SUBDIRS = verbs affinity nvmef
lib_LTLIBRARIES = libdisni.la
libdisni_la_SOURCES =
nodist_EXTRA_libdisni_la_SOURCES = dummy.cpp
libdisni_la_LIBADD = \
  verbs/libverbs.la \
  affinity/libaffinity.la \
  nvmef/libnvmef.la
if SPDK
#XXX: libtools has no better way to deal with whole-archive
libdisni_la_LDFLAGS = -Wl,--whole-archive,@dpdk@/lib/librte_eal.a,@dpdk@/lib/librte_mempool.a,@dpdk@/lib/librte_ring.a,--no-whole-archive
endif

