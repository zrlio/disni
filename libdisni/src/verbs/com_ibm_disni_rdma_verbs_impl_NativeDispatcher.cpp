/*
 * DiSNI: Direct Storage and Networking Interface
 *
 * Author: Patrick Stuedi <stu@zurich.ibm.com>
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

#if HAVE_CONFIG_H
#  include <config.h>
#endif /* HAVE_CONFIG_H */

/* Expose the PRI* macros in inttypes.h */
#define __STDC_FORMAT_MACROS

#include "com_ibm_disni_rdma_verbs_impl_NativeDispatcher.h"
#include <rdma/rdma_cma.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <sys/types.h>
#include <netinet/tcp.h>
#include <string.h>
#include <errno.h>
#include <pthread.h>
#include <unistd.h>
#include <fcntl.h>
#include <poll.h>
#include <inttypes.h>
#include <stddef.h>

#ifdef HAVE_ODP_MR_PREFETCH
#include <infiniband/verbs_exp.h>
#endif

//#define MAX_WR 200;
#define MAX_SGE 4;
//#define N_CQE 200
#define JVERBS_JNI_VERSION 32;

//global resource id counter
static unsigned long long counter = 0;

static pthread_mutex_t mut_counter = PTHREAD_MUTEX_INITIALIZER;

#ifdef ENABLE_LOGGING
#define log(...)\
{\
	printf(__VA_ARGS__); \
}
#else
#define log(...)\
{\
}
#endif

static unsigned long long createObjectId(void *obj){
	unsigned long long obj_id = (unsigned long long) obj;
	return obj_id;
	/*
	int id = 0;
	pthread_mutex_lock(&mut_counter);
	counter++;
	id = counter;
	pthread_mutex_unlock(&mut_counter);
	return id;
	*/
}



/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _createEventChannel
 * Signature: ()I
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1createEventChannel(
		JNIEnv *env, jobject obj) {
	struct rdma_event_channel *cm_channel = NULL;
	unsigned long long obj_id = -1;

	cm_channel = rdma_create_event_channel();
	if (cm_channel != NULL){
		int flags = fcntl(cm_channel->fd, F_GETFL);
		int rc = fcntl(cm_channel->fd, F_SETFL, flags | O_NONBLOCK);
		obj_id = createObjectId(cm_channel);
		log("j2c::createEventChannel: obj_id %llu\n", obj_id);
	} else {
		log("j2c::createEventChannel: rdma_create_event_channel failed\n");
	}

	return obj_id;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _createId
 * Signature: (IIS)I
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1createId(JNIEnv *env,
		jobject obj, jlong channel, jshort rdma_ps) {
	struct rdma_cm_id *cm_listen_id;
	struct rdma_event_channel *cm_channel = NULL;
	unsigned long long obj_id = -1;

	cm_channel = (struct rdma_event_channel *)channel;
	if (cm_channel != NULL){
		int ret = rdma_create_id(cm_channel, &cm_listen_id, NULL, RDMA_PS_TCP);
		if (ret == 0){
			obj_id = createObjectId(cm_listen_id);
			//obj_id = (unsigned long long) cm_listen_id;
			log("j2c::createId: ret %i, obj_id %p\n", ret, (void *)cm_listen_id);
		} else {
			log("j2c::createId: rdma_create_id failed\n");
		}
	} else {
		log("j2c::createId: cm_channel (%p) \n", cm_channel);
	}

	return obj_id;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _createQP
 * Signature: (IIJ)I
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1createQP
  (JNIEnv *env, jobject obj, jlong id, jlong pd, jlong sendcq, jlong recvcq, jint qptype, jint maxsendwr, jint maxrecvwr, jint maxinline){
	struct rdma_cm_id *cm_listen_id = NULL;
	struct ibv_pd *protection = NULL;
	struct ibv_cq *send_cq = NULL;
	struct ibv_cq *recv_cq = NULL;
	struct ibv_qp_init_attr qp_init_attr;
	unsigned long long obj_id = -1;
	int _maxsendwr = maxsendwr;
	int _maxrecvwr = maxrecvwr;
	enum ibv_qp_type _qptype = (enum ibv_qp_type) qptype;

	cm_listen_id = (struct rdma_cm_id *)id;
	protection = (struct ibv_pd *)pd;
	send_cq = (struct ibv_cq *)sendcq;
	recv_cq = (struct ibv_cq *)recvcq;

	if (cm_listen_id != NULL && protection != NULL && send_cq != NULL && recv_cq != NULL){
		memset(&qp_init_attr, 0, sizeof qp_init_attr);
		qp_init_attr.cap.max_recv_sge = MAX_SGE;
		qp_init_attr.cap.max_recv_wr = _maxrecvwr;
		qp_init_attr.cap.max_send_sge = MAX_SGE;
		qp_init_attr.cap.max_send_wr = _maxsendwr;
		qp_init_attr.qp_type = _qptype;
		qp_init_attr.cap.max_inline_data = maxinline;
		//qp_init_attr.qp_type = IBV_QPT_RC;
		qp_init_attr.send_cq = send_cq;
		qp_init_attr.recv_cq = recv_cq;

		int ret = rdma_create_qp(cm_listen_id, protection, &qp_init_attr);
		if (ret == 0){
			struct ibv_qp *qp = cm_listen_id->qp;
			obj_id = createObjectId(qp);
			log("j2c::createQP: obj_id %llu, qpnum %u, send_wr %u, recv_wr %u \n", obj_id, cm_listen_id->qp->qp_num, qp_init_attr.cap.max_send_wr, qp_init_attr.cap.max_recv_wr);

			struct ibv_qp_attr tmp_attr;
			struct ibv_qp_init_attr tmp_init_attr;
			ret = ibv_query_qp(qp, &tmp_attr, IBV_QP_STATE, &tmp_init_attr);
		} else {
			log("j2c::createQP: rdma_create_qp failed %s\n", strerror(errno));
		}
	} else{
		log("j2c::createQP: cm_listen_id or protection or cq null\n");
	}

	return obj_id;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _bindAddr
 * Signature: (IJ)V
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1bindAddr
  (JNIEnv *env, jobject obj, jlong id, jlong address){
	struct rdma_cm_id *cm_listen_id = NULL;
	struct sockaddr_in *s_addr = (struct sockaddr_in *) address;	
	jint ret = -1;

	cm_listen_id = (struct rdma_cm_id *)id;

	if (cm_listen_id != NULL){
		ret = rdma_bind_addr(cm_listen_id, (struct sockaddr*) s_addr);
		if (ret == 0){
			log("j2c::bind: ret %i, cm_listen_id %p\n", ret, (void *)cm_listen_id);
		} else {
			log("j2c::bind: rdma_bind_addr failed, cm_listen_id %p \n", (void *)cm_listen_id);
		}
	} else {
		log("j2c::bind: cm_listen_id null\n");
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _listen
 * Signature: (II)V
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1listen
  (JNIEnv *env, jobject obj, jlong id, jint backlog){
	struct rdma_cm_id *cm_listen_id = NULL;
	jint ret = -1;

	cm_listen_id = (struct rdma_cm_id *)id;
	if (cm_listen_id != NULL){
		ret = rdma_listen(cm_listen_id, backlog);
		if (ret == 0){
			log("j2c::listen: ret %i\n", ret);
		} else {
			log("j2c::listen: rdma_listen failed\n");
		}
	} else {
		log("j2c::listen: cm_listen_id null\n");
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _resolveAddr
 * Signature: (IJJI)V
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1resolveAddr
  (JNIEnv *env, jobject obj, jlong id, jlong src, jlong dst, jint timeout){
	struct rdma_cm_id *cm_listen_id = NULL;
	struct sockaddr_in *d_addr = (struct sockaddr_in *) dst;
	jint ret = -1;

	cm_listen_id = (struct rdma_cm_id *)id;
	if (cm_listen_id != NULL){
		ret = rdma_resolve_addr(cm_listen_id, NULL, (struct sockaddr*) d_addr, (int) timeout);
		if (ret == 0){
			log("j2c::resolveAddr: ret %i\n", ret);
		} else {
			log("j2c::resolveAddr: rdma_resolve_addr failed\n");
		}
	} else {
		log("j2c::resolveAddr: cm_listen_id null\n");
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _resolveRoute
 * Signature: (II)V
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1resolveRoute
  (JNIEnv *env, jobject obj, jlong id, jint timeout){
	struct rdma_cm_id *cm_listen_id = NULL;
	jint ret = -1;
	
	cm_listen_id = (struct rdma_cm_id *)id;
	if (cm_listen_id != NULL){
		ret = rdma_resolve_route(cm_listen_id, (int) timeout);
		if (ret == 0){
			log("j2c::resolveRoute: ret %i\n", ret);
		} else {
			log("j2c::resolveRoute: rdma_resolve_route failed\n");
		}
	} else {
		log("j2c::resolveRoute: cm_listen_id null\n");
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _getCmEvent
 * Signature: (IJJJ)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getCmEvent
  (JNIEnv *env, jobject obj, jlong channel, jlong listen_id, jlong client_id, jint timeout){
	struct rdma_event_channel *cm_channel = NULL;
	struct rdma_cm_event *cm_event;
	jint event = -1;

	cm_channel = (struct rdma_event_channel *)channel;
	if (cm_channel != NULL){
		struct pollfd pollfdcm;
		pollfdcm.fd = cm_channel->fd;
		pollfdcm.events  = POLLIN;
		pollfdcm.revents = 0;
		int ret = poll(&pollfdcm, 1, (int) timeout);
		if (ret > 0){
			ret = rdma_get_cm_event(cm_channel, &cm_event);
			if (ret == 0){
				event = cm_event->event;

				unsigned long long *_listen_id = (unsigned long long *) listen_id;
				if (cm_event->listen_id != NULL){
					*_listen_id = (unsigned long long) cm_event->listen_id;
				} else {
					*_listen_id = -1;
				}

				unsigned long long *_client_id = (unsigned long long *) client_id;
				if (cm_event->id != NULL){
					*_client_id = (unsigned long long) cm_event->id;
				} else {
					*_client_id = -1;
				}
				rdma_ack_cm_event(cm_event);
			}
		} 
	}
	
	return event;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _connect
 * Signature: (IJ)V
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1connect
  (JNIEnv *env, jobject obj, jlong id, jint retry, jint rnr_retry, jlong private_data_addr, jbyte private_data_len){
	struct rdma_cm_id *cm_listen_id = NULL;
	struct rdma_conn_param conn_param;
	jint ret = -1;

	cm_listen_id = (struct rdma_cm_id *)id;
	struct ibv_device_attr dev_attr;
	ibv_query_device(cm_listen_id->verbs, &dev_attr);

	if (cm_listen_id != NULL){
		memset(&conn_param, 0, sizeof(conn_param));
		conn_param.initiator_depth = dev_attr.max_qp_rd_atom;
		conn_param.responder_resources = dev_attr.max_qp_rd_atom;
		conn_param.retry_count = (unsigned char) retry;
		conn_param.rnr_retry_count = (unsigned char) rnr_retry;
		conn_param.private_data = (void *)private_data_addr;
		conn_param.private_data_len = (unsigned char)private_data_len;
		ret = rdma_connect(cm_listen_id, &conn_param);
		if (ret == 0){
			log("j2c::connect: ret %i, guid %" PRIu64 "\n", ret, ibv_get_device_guid(cm_listen_id->verbs->device));
		} else {
			log("j2c::connect: rdma_connect failed\n");
		}
	} else {
		log("j2c:connect: cm_listen_id null\n");
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _accept
 * Signature: (IJ)V
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1accept(JNIEnv *env,
		jobject obj, jlong id, jint retry, jint rnr_retry) {
	struct rdma_cm_id *cm_listen_id = NULL;
	struct rdma_conn_param conn_param;
	jint ret = -1;

	cm_listen_id = (struct rdma_cm_id *)id;
        struct ibv_device_attr dev_attr;
        ibv_query_device(cm_listen_id->verbs, &dev_attr);

	if (cm_listen_id != NULL){
		memset(&conn_param, 0, sizeof(conn_param));
		conn_param.initiator_depth = dev_attr.max_qp_rd_atom;
		conn_param.responder_resources = dev_attr.max_qp_rd_atom;
		conn_param.retry_count = (unsigned char) retry;
		conn_param.rnr_retry_count = (unsigned char) rnr_retry;
		ret = rdma_accept(cm_listen_id, &conn_param);
		log("j2c::accept: ret %i\n", ret);
		if (ret == 0){
		} else {
			log("j2c::accept: rdma_accept failed\n");
		}
	} else {
		log("j2c::accept: cm_listen_id null\n");
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _ackCmEvent
 * Signature: (I)V
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1ackCmEvent
  (JNIEnv *env, jobject obj, jint event){
	return 0;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _disconnect
 * Signature: (I)V
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1disconnect
  (JNIEnv *env, jobject obj, jlong id){
	struct rdma_cm_id *cm_listen_id = NULL;
	jint ret = -1;

	cm_listen_id = (struct rdma_cm_id *)id;

	if (cm_listen_id != NULL){
		ret = rdma_disconnect(cm_listen_id);
	} else {
		log("j2c:disconnect: cm_listen_id null\n");
	}

	return ret;
}

JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1destroyEventChannel
  (JNIEnv *env, jobject obj, jlong channel){
	struct rdma_event_channel *cm_channel = NULL;
	jint ret = -1;

	cm_channel = (struct rdma_event_channel *)channel;

	if (cm_channel != NULL){
		rdma_destroy_event_channel(cm_channel);
		ret = 0;
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _destroyCmId
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1destroyCmId
  (JNIEnv *env, jobject obj, jlong id){
	struct rdma_cm_id *cm_listen_id = NULL;
	jint ret = -1;

	cm_listen_id = (struct rdma_cm_id *)id;

	if (cm_listen_id != NULL){
		ret = rdma_destroy_id(cm_listen_id);
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _destroyQP
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1destroyQP
  (JNIEnv *, jobject, jlong id){
	struct rdma_cm_id *cm_listen_id = NULL;
	jint ret = -1;

	cm_listen_id = (struct rdma_cm_id *)id;

	if (cm_listen_id != NULL && cm_listen_id->qp != NULL){
		jlong obj_id = createObjectId(cm_listen_id->qp);

		rdma_destroy_qp(cm_listen_id);
		ret = 0;
	}

	return ret;
}



/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _destroyEp
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1destroyEp
  (JNIEnv *, jobject, jlong id){
	struct rdma_cm_id *cm_listen_id = NULL;
	jint ret = -1;

	cm_listen_id = (struct rdma_cm_id *)id;

	if (cm_listen_id != NULL){
		log("j2c::destroyEp: id %p\n", (void *)id);
		rdma_destroy_ep(cm_listen_id);
		ret = 0;
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _getSrcAddr
 * Signature: (JJ)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getSrcAddr
  (JNIEnv *env, jobject obj, jlong id, jlong addr){
        struct rdma_cm_id *cm_listen_id = NULL;
        jint ret = -1;

        cm_listen_id = (struct rdma_cm_id *)id;

        if (cm_listen_id != NULL){
        	struct sockaddr *sock = rdma_get_local_addr(cm_listen_id);
        	void *_addr = (void *) addr;
        	memcpy(_addr, sock, sizeof(*sock));
        	ret = 0;
        }


	return ret;
}


/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _getDstAddr
 * Signature: (JJ)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getDstAddr
  (JNIEnv *, jobject, jlong id, jlong addr){
        struct rdma_cm_id *cm_listen_id = NULL;
        jint ret = -1;

        cm_listen_id = (struct rdma_cm_id *)id;

        if (cm_listen_id != NULL){
                struct sockaddr *sock = rdma_get_peer_addr(cm_listen_id);
		//struct sockaddr_in *sockin = (struct sockaddr_in *) sock;
		//log("j2c:getDstAddr: portx %hu\n", sockin->sin_port);
                void *_addr = (void *) addr;
                memcpy(_addr, sock, sizeof(*sock));
                ret = 0;
        }


        return ret;
}


/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _getContext
 * Signature: ()I
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getContext
  (JNIEnv *env, jobject obj, jlong id){
	struct rdma_cm_id *cm_listen_id = NULL;
	//jint cmd_fd = -1;
	unsigned long long obj_id = -1;

	cm_listen_id = (struct rdma_cm_id *)id;

	if (cm_listen_id != NULL){
		struct ibv_context *context = cm_listen_id->verbs;
                if (context != NULL){
			obj_id = createObjectId(context);
                        log("j2c::getContext: obj_id %llu\n", obj_id);
                } else {
                        log("j2c::getContext: context_list empty %p\n", (void *)cm_listen_id);
                }
	} else {
                log("j2c::getContext: rdma_get_devices failed\n");
	}

	return obj_id;
}

/*
 * Class:     jrdma_nat_NativeDispatcher
 * Method:    _allocPd
 * Signature: (I)I
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1allocPd
  (JNIEnv *env, jobject obj, jlong ctx){
	struct ibv_context *context = NULL;
	unsigned long long obj_id = -1;

	context = (struct ibv_context *)ctx;

	if (context != NULL){
		struct ibv_pd *pd = ibv_alloc_pd(context);
		if (pd != NULL){
			obj_id = createObjectId(pd);
			log("j2c::allocPd: obj_id %llu\n", obj_id);
		} else {
			log("j2c::allocPd: ibv_alloc_pd failed\n");
		}
	} else {
		log("j2c::allocPd: context null\n");
	}

	return obj_id;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _createCompChannel
 * Signature: (I)I
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1createCompChannel
  (JNIEnv *env, jobject obj, jlong ctx){
	struct ibv_context *context = NULL;
	unsigned long long obj_id = -1;

	context = (struct ibv_context *)ctx;
	if (context != NULL){
		struct ibv_comp_channel *comp_channel = ibv_create_comp_channel(context);
		if (comp_channel != NULL){
			int flags = fcntl(comp_channel->fd, F_GETFL);
			int rc = fcntl(comp_channel->fd, F_SETFL, flags | O_NONBLOCK);
			obj_id = createObjectId(comp_channel);
			log("j2c::createCompChannel: obj_id %llu\n", obj_id);
		} else {
			log("j2c::createCompChannel: ibv_create_comp_channel failed\n");
		}
	} else {
		log("j2c::createCompChannel: context null\n");
	}

	return obj_id;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _createCQ
 * Signature: (IIII)I
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1createCQ
  (JNIEnv *env, jobject obj, jlong ctx, jlong channel, jint ncqe, jint comp_vector){
	struct ibv_context *context = NULL;
	struct ibv_comp_channel *comp_channel = NULL;
	unsigned long long obj_id = -1;
	int _ncqe = ncqe;
	int _comp_vector = comp_vector;
	
	context = (struct ibv_context *)ctx;
	comp_channel = (struct ibv_comp_channel *)channel;

	if (context != NULL && comp_channel != NULL){
		struct ibv_cq *cq = ibv_create_cq(context, _ncqe, NULL, comp_channel, _comp_vector);
		if (cq != NULL){
			obj_id = createObjectId(cq);
			log("j2c::createCQ: obj_id %p, cq %p, cqnum %u, size %u\n", (void *)obj_id, (void *)cq, cq->handle, _ncqe);
		} else {
			log("j2c::createCQ: ibv_create_cq failed\n");
		}
	} else {
		log("j2c::createCQ: context or comp_channel null\n");
	}

	//return handle;
	return obj_id;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _modifyQP
 * Signature: (IIJ)V
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1modifyQP
  (JNIEnv *env, jobject obj, jlong qp, jlong addr){
	struct ibv_qp *queuepair = NULL;
	struct ibv_qp_attr attr;
	int attr_mask;	
	jint ret = -1;

	queuepair = (struct ibv_qp *)qp;
	if (queuepair != NULL){
		ret = ibv_modify_qp(queuepair, &attr, attr_mask);
		if (ret == 0){
			log("j2c::modify_qp: ret %i\n", ret);
		} else {
			log("j2c::modify_qp: ibv_modify_qp failed\n");
		}
	} else {
		log("j2c::modify_qp: queuepair null\n");
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _queryOdpSupport
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1queryOdpSupport
  (JNIEnv *env, jobject obj, jlong id){
       jint ret = -1;
       #ifdef HAVE_ODP_MR_PREFETCH
           struct ibv_context *context = (struct ibv_context *)id;
           struct ibv_device_attr_ex dev_attr;
           ret = ibv_query_device_ex(context, NULL, &dev_attr);
           if (ret == 0) {
            if (dev_attr.odp_caps.general_caps & IBV_ODP_SUPPORT) {
                ret = dev_attr.odp_caps.per_transport_caps.rc_odp_caps;
                log("j2c::queryOdpSupport: supported");
            } else {
                ret = -1;
                log("j2c::queryOdpSupport: not supported");
            }
           }
       #endif
       return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _expPrefetchMr
 * Signature: (JJI)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1expPrefetchMr
  (JNIEnv *env, jobject obj, jlong handle, jlong addr, jint length){
       jint ret = -1;

       #ifdef HAVE_ODP_MR_PREFETCH
           struct ibv_mr *mr = NULL;
           mr = (struct ibv_mr *)handle;
           struct ibv_exp_prefetch_attr prefetch_attr;
           prefetch_attr.flags = IBV_EXP_PREFETCH_WRITE_ACCESS;
           prefetch_attr.addr = (void *)addr;
           prefetch_attr.length = (size_t)length;
           prefetch_attr.comp_mask = 0;

           if (mr != NULL){
                   ret = ibv_exp_prefetch_mr(mr, &prefetch_attr);
                   if (ret == 0){
                           log("j2c::expPrefetchMr: ret %i\n", ret);
                   } else {
                           log("j2c::expPrefetchMr:  ibv_exp_prefetch_mr failed, error %s\n", strerror(errno));
                   }
           } else {
                   log("j2c::expPrefetchMr: mr null\n");
           }
       #else
            log("j2c::expPrefetchMr: ODP not supported\n");
       #endif
       return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _regMr
 * Signature: (IIJIJJJ)V
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1regMr
  (JNIEnv *env, jobject obj, jlong pd, jlong address, jint len, jint access, jlong lkey, jlong rkey, jlong handle){
	struct ibv_pd *protection = NULL;
	void *addr = (void *) address;
	//jint ret = -1;
	unsigned long long obj_id = -1;
	
	protection = (struct ibv_pd *)pd;
	if (protection != NULL){
		struct ibv_mr *mr = ibv_reg_mr(protection, addr, len, access);
		if (mr != NULL){
			obj_id = createObjectId(mr);

			int *_lkey = (int *) lkey;
			int *_rkey = (int *) rkey;
			int *_handle = (int *) handle;

			*_lkey = mr->lkey;
			*_rkey = mr->rkey;
			*_handle = mr->handle;

			log("j2c::regMr: obj_id %p, mr %p\n", (void *)obj_id, (void *)mr);
		} else {
			log("j2c::regMr: ibv_reg_mr failed\n");
		}
	} else {
		log("j2c::regMr: protection null\n");
	}

	return obj_id;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _deregMr
 * Signature: (IJ)V
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1deregMr
  (JNIEnv *env, jobject obj, jlong handle){
	//int _handle = (int) handle;
	struct ibv_mr *mr = NULL;
	jint ret = -1;

	mr = (struct ibv_mr *)handle;

	if (mr != NULL){
		ret = ibv_dereg_mr(mr);
		if (ret == 0){
			log("j2c::deregMr: ret %i\n", ret);
		} else {
			log("j2c::deregMr:  ibv_dereg_failed, error %s\n", strerror(errno));
		}
	} else {
		log("j2c::deregMr: mr null\n");
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _postSend
 * Signature: (IIJ)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1postSend
  (JNIEnv *env, jobject obj, jlong qp, jlong wrList){
	struct ibv_qp *queuepair = NULL;
	struct ibv_send_wr *wr = (struct ibv_send_wr *) wrList;
	struct ibv_send_wr *bad_wr;
	jint ret = -1;

	queuepair = (struct ibv_qp *)qp;
	if (queuepair != NULL){
		ret = ibv_post_send(queuepair, wr, &bad_wr);
		if (ret == 0){
			//log("j2c::post_send: ret %i\n", ret);
		} else {
			log("j2c::post_send: ibv_post_send failed %s\n", strerror(ret));
		}
 
	} else {
		log("j2c::post_send: queuepair null\n");
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _postRecv
 * Signature: (IIJ)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1postRecv
  (JNIEnv *env, jobject obj, jlong qp, jlong wrList){
	struct ibv_qp *queuepair = NULL;
	struct ibv_recv_wr *wr = (struct ibv_recv_wr *) wrList;
	struct ibv_recv_wr *bad_wr;
	jint ret = -1;

	queuepair = (struct ibv_qp *)qp;
	if (queuepair != NULL){
		//log("j2c::post_recv: sizeof wr %zu, wrid %llu\n", sizeof *wr, wr->wr_id);
		ret = ibv_post_recv(queuepair, wr, &bad_wr);
		if (ret == 0){
			//log("j2c::post_recv: ret %i\n", ret);
		} else {
			log("j2c::post_recv: ibv_post_recv failed %s\n", strerror(ret));
		}
	} else {
		log("j2c::post_recv: queuepair null\n");
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _getCqEvent
 * Signature: (III)V
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getCqEvent
  (JNIEnv *env, jobject obj, jlong channel, int timeout){
	struct ibv_comp_channel *comp_channel = NULL;
	struct ibv_cq *dst_cq;
	void *dst_cq_ctx;
	jint ret = -1;

	comp_channel = (struct ibv_comp_channel *)channel;

	if (comp_channel != NULL){
		struct pollfd pollfdcomp;
		pollfdcomp.fd = comp_channel->fd;
		pollfdcomp.events  = POLLIN;
		pollfdcomp.revents = 0;
		ret = poll(&pollfdcomp, 1, timeout);
		if (ret > 0){
			ret = ibv_get_cq_event(comp_channel, &dst_cq, &dst_cq_ctx);
		} else {
			ret = -1;
		}
	} else {
		//log("j2c::getCqEvent: comp_channel null, channel %llu\n", channel);
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _pollCQ
 * Signature: (IIJ)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1pollCQ
  (JNIEnv *env, jobject obj, jlong cq, jint ne, jlong wc_list){
	struct ibv_cq *completionqueue = NULL;
	struct ibv_wc *wc = (struct ibv_wc *) wc_list;
	int num_entries = (int) ne;	
	jint ret = -1;

	completionqueue = (struct ibv_cq*)cq;
	if (completionqueue != NULL){
		ret = ibv_poll_cq(completionqueue, num_entries, wc);
		//log("j2c::pollCQ: ret %i\n", ret);
	} else {
		//log("j2c::pollCQ: completionqueue null\n");
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _reqNotifyCQ
 * Signature: (II)V
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1reqNotifyCQ
  (JNIEnv *env, jobject obj, jlong cq, jint solicited){
	struct ibv_cq *completionqueue = NULL;
	int solicited_only = (int) solicited;
	jint ret = -1;

	completionqueue = (struct ibv_cq*)cq;

	if (completionqueue != NULL){
		ret = ibv_req_notify_cq(completionqueue, solicited_only);
		//log("j2c::reqNotify: ret %i\n", ret);
	} else {
		//log("j2c::reqNotify:  completionqueue null\n");
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _ackCqEvent
 * Signature: (II)V
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1ackCqEvent
  (JNIEnv *env, jobject obj, jlong cq, jint nevents){
	struct ibv_cq *completionqueue = NULL;
	unsigned int num_events = (unsigned int) nevents;
	jint ret = -1;
	
	completionqueue = (struct ibv_cq*)cq;

	if (completionqueue != NULL){
		ibv_ack_cq_events(completionqueue, num_events);
		ret = 0;
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _destroyCompChannel
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1destroyCompChannel
  (JNIEnv *env, jobject obj, jlong channel){
	struct ibv_comp_channel *comp_channel = NULL;
	jint ret = -1;

	comp_channel = (struct ibv_comp_channel *)channel;
	if (comp_channel != NULL){
		ret = ibv_destroy_comp_channel(comp_channel);
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _deallocPd
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1deallocPd
  (JNIEnv *env, jobject obj, jlong pd){
	struct ibv_pd *protection = NULL;
	jint ret = -1;

	protection = (struct ibv_pd *)pd;
	if (protection != NULL){
		ret = ibv_dealloc_pd(protection);
	}

	return ret;
}

/*
 * Class:     com_ibm_jverbs_nat_NativeDispatcher
 * Method:    _destroyCQ
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1destroyCQ
  (JNIEnv *env, jobject obj, jlong cq){
	struct ibv_cq *completionqueue = NULL;
	jint ret = -1;

	completionqueue = (struct ibv_cq*)cq;
	if (completionqueue != NULL){
		ret = ibv_destroy_cq(completionqueue);
	}

	return ret;
}

/*
 * Class:     com_ibm_zac_jverbs_impl_nat_NativeDispatcher
 * Method:    _getQpNum
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getQpNum
  (JNIEnv *env, jobject obj, jlong obj_id){
	jint qpnum = -1;

	struct ibv_qp * qp = (struct ibv_qp *)obj_id;
	if (qp != NULL){
		qpnum = qp->qp_num;
		log("j2c::getQpNum: obj_id %p, qpnum %i\n", (void *)obj_id, qpnum);
	} else {
		log("j2c::getQpNum: failed, obj_id %p\n", (void *)obj_id);
	}

	return qpnum;
}

/*
 * Class:     com_ibm_zac_jverbs_impl_nat_NativeDispatcher
 * Method:    _getContextFd
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getContextFd
  (JNIEnv *env, jobject obj, jlong obj_id){
        jint cmd_fd = -1;

        struct ibv_context *context = (struct ibv_context *)obj_id;
        if (context != NULL){
                cmd_fd = context->cmd_fd;
                log("j2c::getContextFd: obj_id %p, fd %i\n", (void *)obj_id, cmd_fd);
        } else {
                log("j2c::getContext: failed, obj_id %p\n", (void *)obj_id);
        }

        return cmd_fd;
}

/*
 * Class:     com_ibm_disni_rdma_verbs_impl_nat_NativeDispatcher
 * Method:    _getContextNumCompVectors
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getContextNumCompVectors
  (JNIEnv *env, jobject obj, jlong obj_id) {
    jint num_comp_vectors = -1;

    struct ibv_context *context = (struct ibv_context *)obj_id;
    if (context != NULL) {
        num_comp_vectors = context->num_comp_vectors;
        log("j2c::getContextNumCompVectors: obj_id %llu, num_comp_vectors %i\n", obj_id, num_comp_vectors);
    } else {
        log("j2c::getContextNumCompVectors: failed, obj_id %llu\n", obj_id);
    }

    return num_comp_vectors;
}

/*
 * Class:     com_ibm_zac_jverbs_impl_nat_NativeDispatcher
 * Method:    _getPdHandle
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getPdHandle
  (JNIEnv *env, jobject obj, jlong pd){
	jint handle = -1;

	struct ibv_pd *protection = (struct ibv_pd *)pd;
	if (protection != NULL){
		handle = protection->handle;
		log("j2c::getPdHandle: obj_id %p, handle %i\n", (void *)pd, handle);
	} else {
		log("j2c::getPdHandle: failed, obj_id %p\n", (void *)pd);
	}

	return handle;
}



/*
 * Class:     com_ibm_zac_jverbs_impl_nat_NativeDispatcher
 * Method:    _getSockAddrInSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getSockAddrInSize
  (JNIEnv *, jobject){
	return sizeof(struct sockaddr_in);
}

/*
 * Class:     com_ibm_zac_jverbs_impl_nat_NativeDispatcher
 * Method:    _getIbvRecvWRSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getIbvRecvWRSize
  (JNIEnv *, jobject){
	return sizeof(struct ibv_recv_wr);
}

/*
 * Class:     com_ibm_zac_jverbs_impl_nat_NativeDispatcher
 * Method:    _getIbvSendWRSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getIbvSendWRSize
  (JNIEnv *, jobject){
	return sizeof(struct ibv_send_wr);
}

/*
 * Class:     com_ibm_zac_jverbs_impl_nat_NativeDispatcher
 * Method:    _getIbvSgeSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getIbvSgeSize
  (JNIEnv *, jobject){
	return sizeof(struct ibv_sge);
}


/*
 * Class:     com_ibm_zac_jverbs_impl_nat_NativeDispatcher
 * Method:    _getIbvWCSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getIbvWCSize
  (JNIEnv *, jobject){
	return sizeof(struct ibv_wc);
}

/*
 * Class:     com_ibm_zac_jverbs_impl_nat_NativeDispatcher
 * Method:    _getRemoteAddressOffset
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getRemoteAddressOffset
  (JNIEnv *, jobject){
	return offsetof(struct ibv_send_wr, wr.rdma.remote_addr);
}

/*
 * Class:     com_ibm_zac_jverbs_impl_nat_NativeDispatcher
 * Method:    _getRKeyOffset
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getRKeyOffset
  (JNIEnv *, jobject){
	return offsetof(struct ibv_send_wr, wr.rdma.rkey);	
}

/*
 * Class:     com_ibm_zac_jverbs_impl_nat_NativeDispatcher
 * Method:    _getVersion
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getVersion
  (JNIEnv *, jobject){
	int version = JVERBS_JNI_VERSION;
	return version;
}

