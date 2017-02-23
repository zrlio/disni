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

#include "com_ibm_disni_rdma_verbs_impl_NativeDispatcher.h"
#include <rdma/rdma_cma.h>
#include <map>
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
#include <stddef.h>

using namespace std;

//#define MAX_WR 200;
#define MAX_SGE 4;
//#define N_CQE 200
#define JVERBS_JNI_VERSION 27;

//global resource id counter
static unsigned long long counter = 0;
//event channel: system wide
static map<unsigned long long, struct rdma_event_channel *> map_cm_channel;
//cm id: device specific
static map<unsigned long long, struct rdma_cm_id *> map_cm_id;
//pd: device specific
static map<unsigned long long, struct ibv_pd *> map_pd;
//device: system wide
static map<unsigned long long, struct ibv_context *> map_context;
//comp_channel: system wide
static map<unsigned long long, struct ibv_comp_channel *> map_comp_channel;
//cq: device specific
static map<unsigned long long, struct ibv_cq *> map_cq;
//qp: device specific
static map<unsigned long long, struct ibv_qp *> map_qp;
//mr: device specific
static map<unsigned long long, struct ibv_mr *> map_mr;

static pthread_mutex_t mut_counter = PTHREAD_MUTEX_INITIALIZER;
static pthread_rwlock_t mut_cm_channel = PTHREAD_RWLOCK_INITIALIZER;
static pthread_rwlock_t mut_cm_id = PTHREAD_RWLOCK_INITIALIZER;
static pthread_rwlock_t mut_pd = PTHREAD_RWLOCK_INITIALIZER;
static pthread_rwlock_t mut_context = PTHREAD_RWLOCK_INITIALIZER;
static pthread_rwlock_t mut_comp_channel = PTHREAD_RWLOCK_INITIALIZER;
static pthread_rwlock_t mut_cq = PTHREAD_RWLOCK_INITIALIZER;
static pthread_rwlock_t mut_qp = PTHREAD_RWLOCK_INITIALIZER;
static pthread_rwlock_t mut_mr = PTHREAD_RWLOCK_INITIALIZER;

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
		pthread_rwlock_wrlock(&mut_cm_channel);
		map_cm_channel[obj_id] = cm_channel;
		pthread_rwlock_unlock(&mut_cm_channel);
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

	pthread_rwlock_rdlock(&mut_cm_channel);
	cm_channel = map_cm_channel[channel];
	pthread_rwlock_unlock(&mut_cm_channel);
	if (cm_channel != NULL){
		int ret = rdma_create_id(cm_channel, &cm_listen_id, NULL, RDMA_PS_TCP);
		if (ret == 0){
			obj_id = createObjectId(cm_listen_id);
			//obj_id = (unsigned long long) cm_listen_id;
			pthread_rwlock_wrlock(&mut_cm_id);
			map_cm_id[obj_id] = cm_listen_id;
			pthread_rwlock_unlock(&mut_cm_id);
			log("j2c::createId: ret %i, obj_id %llu\n", ret, cm_listen_id);
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

	pthread_rwlock_rdlock(&mut_cm_id);
	cm_listen_id = map_cm_id[id];
	pthread_rwlock_unlock(&mut_cm_id);
	pthread_rwlock_rdlock(&mut_pd);
	protection = map_pd[pd];
	pthread_rwlock_unlock(&mut_pd);
	pthread_rwlock_rdlock(&mut_cq);
	send_cq = map_cq[sendcq];
	pthread_rwlock_unlock(&mut_cq);
	pthread_rwlock_rdlock(&mut_cq);
	recv_cq = map_cq[recvcq];
	pthread_rwlock_unlock(&mut_cq);

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
			pthread_rwlock_wrlock(&mut_qp);
			map_qp[obj_id] = qp;
			pthread_rwlock_unlock(&mut_qp);
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

	pthread_rwlock_rdlock(&mut_cm_id);
	cm_listen_id = map_cm_id[id];
	pthread_rwlock_unlock(&mut_cm_id);

	if (cm_listen_id != NULL){
		ret = rdma_bind_addr(cm_listen_id, (struct sockaddr*) s_addr);
		if (ret == 0){
			log("j2c::bind: ret %i, cm_listen_id %llu\n", ret, cm_listen_id);
		} else {
			log("j2c::bind: rdma_bind_addr failed, cm_listen_id %llu \n", cm_listen_id);
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

	pthread_rwlock_rdlock(&mut_cm_id);
	cm_listen_id = map_cm_id[id];
	pthread_rwlock_unlock(&mut_cm_id);
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

	pthread_rwlock_rdlock(&mut_cm_id);
	cm_listen_id = map_cm_id[id];
	pthread_rwlock_unlock(&mut_cm_id);
	if (cm_listen_id != NULL){
		ret = rdma_resolve_addr(cm_listen_id, NULL, (struct sockaddr*) d_addr, 2000);
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
  (JNIEnv *env, jobject obj, jlong id, jint to){
	struct rdma_cm_id *cm_listen_id = NULL;
	jint ret = -1;
	
	pthread_rwlock_rdlock(&mut_cm_id);
	cm_listen_id = map_cm_id[id];
	pthread_rwlock_unlock(&mut_cm_id);
	if (cm_listen_id != NULL){
		ret = rdma_resolve_route(cm_listen_id, 2000);
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
	int _timeout = (int) timeout;
	jint event = -1;

	pthread_rwlock_rdlock(&mut_cm_channel);
	cm_channel = map_cm_channel[channel];
	pthread_rwlock_unlock(&mut_cm_channel);
	if (cm_channel != NULL){
		struct pollfd pollfdcm;
		pollfdcm.fd = cm_channel->fd;
		pollfdcm.events  = POLLIN;
		pollfdcm.revents = 0;
		int ret = poll(&pollfdcm, 1, timeout);
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
					if (cm_event->event == RDMA_CM_EVENT_CONNECT_REQUEST){
						unsigned long long obj_id = (unsigned long long) cm_event->id;
						pthread_rwlock_wrlock(&mut_cm_id);
						map_cm_id[obj_id] = cm_event->id;
						pthread_rwlock_unlock(&mut_cm_id);
					}
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
  (JNIEnv *env, jobject obj, jlong id, jint retry, jint rnr_retry){
	struct rdma_cm_id *cm_listen_id = NULL;
	struct rdma_conn_param conn_param;
	jint ret = -1;

	pthread_rwlock_rdlock(&mut_cm_id);
	cm_listen_id = map_cm_id[id];
	pthread_rwlock_unlock(&mut_cm_id);
	struct ibv_device_attr dev_attr;
	ibv_query_device(cm_listen_id->verbs, &dev_attr);

	if (cm_listen_id != NULL){
		memset(&conn_param, 0, sizeof(conn_param));
		conn_param.initiator_depth = dev_attr.max_qp_rd_atom;
		conn_param.responder_resources = dev_attr.max_qp_rd_atom;
		conn_param.retry_count = (unsigned char) retry;
		conn_param.rnr_retry_count = (unsigned char) rnr_retry;
		ret = rdma_connect(cm_listen_id, &conn_param);
		if (ret == 0){
			log("j2c::connect: ret %i, guid %llu\n", ret, ibv_get_device_guid(cm_listen_id->verbs->device));
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

	pthread_rwlock_rdlock(&mut_cm_id);
	cm_listen_id = map_cm_id[id];
	pthread_rwlock_unlock(&mut_cm_id);
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

	pthread_rwlock_rdlock(&mut_cm_id);
	cm_listen_id = map_cm_id[id];
	pthread_rwlock_unlock(&mut_cm_id);

	if (cm_listen_id != NULL){
		ret = rdma_disconnect(cm_listen_id);
		if (ret == 0){
			pthread_rwlock_wrlock(&mut_cm_id);
			map_cm_id.erase(id);
			pthread_rwlock_unlock(&mut_cm_id);
			log("j2c::disconnect: ret %i\n", ret);
		} else {
			log("j2c::disconnect: rdma_disconnect failed\n");
		}
	} else {
		log("j2c:disconnect: cm_listen_id null\n");
	}

	return ret;
}

JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1destroyEventChannel
  (JNIEnv *env, jobject obj, jlong channel){
	struct rdma_event_channel *cm_channel = NULL;
	jint ret = -1;

	pthread_rwlock_rdlock(&mut_cm_channel);
	cm_channel = map_cm_channel[channel];
	pthread_rwlock_unlock(&mut_cm_channel);

	if (cm_channel != NULL){
		rdma_destroy_event_channel(cm_channel);
		pthread_rwlock_wrlock(&mut_cm_channel);
		map_cm_channel.erase(channel);
		pthread_rwlock_unlock(&mut_cm_channel);
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

	pthread_rwlock_rdlock(&mut_cm_id);
	cm_listen_id = map_cm_id[id];
	pthread_rwlock_unlock(&mut_cm_id);

	if (cm_listen_id != NULL){
		ret = rdma_destroy_id(cm_listen_id);
		pthread_rwlock_wrlock(&mut_cm_id);
		map_cm_id.erase(id);
		pthread_rwlock_unlock(&mut_cm_id);
		ret = 0;
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

	pthread_rwlock_rdlock(&mut_cm_id);
	cm_listen_id = map_cm_id[id];
	pthread_rwlock_unlock(&mut_cm_id);

	if (cm_listen_id != NULL){
		rdma_destroy_qp(cm_listen_id);
		pthread_rwlock_wrlock(&mut_cm_id);
		map_cm_id.erase(id);
		pthread_rwlock_unlock(&mut_cm_id);
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

	pthread_rwlock_rdlock(&mut_cm_id);
	cm_listen_id = map_cm_id[id];
	pthread_rwlock_unlock(&mut_cm_id);

	if (cm_listen_id != NULL){
		log("j2c::destroyEp: id %llu\n", id);
		rdma_destroy_ep(cm_listen_id);
		pthread_rwlock_wrlock(&mut_cm_id);
		map_cm_id.erase(id);
		pthread_rwlock_unlock(&mut_cm_id);
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

        pthread_rwlock_rdlock(&mut_cm_id);
        cm_listen_id = map_cm_id[id];
        pthread_rwlock_unlock(&mut_cm_id);

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

        pthread_rwlock_rdlock(&mut_cm_id);
        cm_listen_id = map_cm_id[id];
        pthread_rwlock_unlock(&mut_cm_id);

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

	pthread_rwlock_rdlock(&mut_cm_id);
	cm_listen_id = map_cm_id[id];
	pthread_rwlock_unlock(&mut_cm_id);

	if (cm_listen_id != NULL){
		struct ibv_context *context = cm_listen_id->verbs;
                if (context != NULL){
			obj_id = createObjectId(context);
                        pthread_rwlock_wrlock(&mut_context);
                        map_context[obj_id] = context;
                        pthread_rwlock_unlock(&mut_context);
                        log("j2c::getContext: obj_id %llu\n", obj_id);
                } else {
                        log("j2c::getContext: context_list empty %llu\n", cm_listen_id);
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

	pthread_rwlock_rdlock(&mut_context);
	context = map_context[ctx];
	pthread_rwlock_unlock(&mut_context);

	if (context != NULL){
		struct ibv_pd *pd = ibv_alloc_pd(context);
		if (pd != NULL){
			obj_id = createObjectId(pd);
			pthread_rwlock_wrlock(&mut_pd);
			map_pd[obj_id] = pd;
			pthread_rwlock_unlock(&mut_pd);
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

	pthread_rwlock_rdlock(&mut_context);
	context = map_context[ctx];
	pthread_rwlock_unlock(&mut_context);
	if (context != NULL){
		struct ibv_comp_channel *comp_channel = ibv_create_comp_channel(context);
		if (comp_channel != NULL){
			int flags = fcntl(comp_channel->fd, F_GETFL);
			int rc = fcntl(comp_channel->fd, F_SETFL, flags | O_NONBLOCK);
			obj_id = createObjectId(comp_channel);
			pthread_rwlock_wrlock(&mut_comp_channel);
			map_comp_channel[obj_id] = comp_channel;
			pthread_rwlock_unlock(&mut_comp_channel);
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
	
	pthread_rwlock_rdlock(&mut_context);
	context = map_context[ctx];
	pthread_rwlock_unlock(&mut_context);
	pthread_rwlock_rdlock(&mut_comp_channel);
	comp_channel = map_comp_channel[channel];
	pthread_rwlock_unlock(&mut_comp_channel);

	if (context != NULL && comp_channel != NULL){
		struct ibv_cq *cq = ibv_create_cq(context, _ncqe, NULL, comp_channel, 0);
		if (cq != NULL){
			obj_id = createObjectId(cq);
			pthread_rwlock_wrlock(&mut_cq);
			map_cq[obj_id] = cq;
			pthread_rwlock_unlock(&mut_cq);
			log("j2c::createCQ: obj_id %llu, cq %llu, cqnum %u, size %u\n", obj_id, cq, cq->handle, _ncqe);
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

	pthread_rwlock_rdlock(&mut_qp);
	queuepair = map_qp[qp];
	pthread_rwlock_unlock(&mut_qp);
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
 * Method:    _regMr
 * Signature: (IIJIJJJ)V
 */
JNIEXPORT jlong JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1regMr
  (JNIEnv *env, jobject obj, jlong pd, jlong address, jint len, jlong lkey, jlong rkey, jlong handle){
	struct ibv_pd *protection = NULL;
	int access = IBV_ACCESS_LOCAL_WRITE | IBV_ACCESS_REMOTE_WRITE | IBV_ACCESS_REMOTE_READ;
	void *addr = (void *) address;
	//jint ret = -1;
	unsigned long long obj_id = -1;
	
	pthread_rwlock_rdlock(&mut_pd);
	protection = map_pd[pd];
	pthread_rwlock_unlock(&mut_pd);
	if (protection != NULL){
		struct ibv_mr *mr = ibv_reg_mr(protection, addr, len, access);
		if (mr != NULL){
			obj_id = createObjectId(mr);
			pthread_rwlock_wrlock(&mut_mr);
			map_mr[obj_id] = mr;
			pthread_rwlock_unlock(&mut_mr);

			int *_lkey = (int *) lkey;
			int *_rkey = (int *) rkey;
			int *_handle = (int *) handle;

			*_lkey = mr->lkey;
			*_rkey = mr->rkey;
			*_handle = mr->handle;

			log("j2c::regMr: obj_id %llu, mr %llu\n", obj_id, mr);
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

	pthread_rwlock_rdlock(&mut_mr);
	mr = map_mr[handle];
	pthread_rwlock_unlock(&mut_mr);

	if (mr != NULL){
		pthread_rwlock_wrlock(&mut_mr);
		map_mr.erase(handle);
		pthread_rwlock_unlock(&mut_mr);
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

	pthread_rwlock_rdlock(&mut_qp);
	queuepair = map_qp[qp];
	pthread_rwlock_unlock(&mut_qp);
	if (queuepair != NULL){
		ret = ibv_post_send(queuepair, wr, &bad_wr);
		if (ret == 0){
			//log("j2c::post_send: ret %i\n", ret);
		} else {
			log("j2c::post_send: ibv_post_send failed %s\n", strerror(errno));
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

	pthread_rwlock_rdlock(&mut_qp);
	queuepair = map_qp[qp];
	pthread_rwlock_unlock(&mut_qp);
	if (queuepair != NULL){
		//log("j2c::post_recv: sizeof wr %zu, wrid %llu\n", sizeof *wr, wr->wr_id);
		ret = ibv_post_recv(queuepair, wr, &bad_wr);
		if (ret == 0){
			//log("j2c::post_recv: ret %i\n", ret);
		} else {
			log("j2c::post_recv: ibv_post_recv failed %s\n", strerror(errno));
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

	pthread_rwlock_rdlock(&mut_comp_channel);
	comp_channel = map_comp_channel[channel];
	pthread_rwlock_unlock(&mut_comp_channel);

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

	pthread_rwlock_rdlock(&mut_cq);
	completionqueue = map_cq[cq];
	pthread_rwlock_unlock(&mut_cq);
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

	pthread_rwlock_rdlock(&mut_cq);
	completionqueue = map_cq[cq];
	pthread_rwlock_unlock(&mut_cq);

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
	
	pthread_rwlock_rdlock(&mut_cq);
	completionqueue = map_cq[cq];
	pthread_rwlock_unlock(&mut_cq);

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

	pthread_rwlock_rdlock(&mut_comp_channel);
	comp_channel = map_comp_channel[channel];
	pthread_rwlock_unlock(&mut_comp_channel);
	if (comp_channel != NULL){
		ret = ibv_destroy_comp_channel(comp_channel);
		pthread_rwlock_wrlock(&mut_comp_channel);
		map_comp_channel.erase(channel);
		pthread_rwlock_unlock(&mut_comp_channel);
		ret = 0;
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

	pthread_rwlock_rdlock(&mut_pd);
	protection = map_pd[pd];
	pthread_rwlock_unlock(&mut_pd);
	if (protection != NULL){
		ret = ibv_dealloc_pd(protection);
		pthread_rwlock_wrlock(&mut_pd);
		map_pd.erase(pd);
		pthread_rwlock_unlock(&mut_pd);
		ret = 0;
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

	pthread_rwlock_rdlock(&mut_cq);
	completionqueue = map_cq[cq];
	pthread_rwlock_unlock(&mut_cq);
	if (completionqueue != NULL){
		ret = ibv_destroy_cq(completionqueue);
		pthread_rwlock_wrlock(&mut_cq);
		map_cq.erase(cq);
		pthread_rwlock_unlock(&mut_cq);
		ret = 0;
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

	pthread_rwlock_rdlock(&mut_qp);
	struct ibv_qp *qp = map_qp[obj_id];
	pthread_rwlock_unlock(&mut_qp);
	if (qp != NULL){
		qpnum = qp->qp_num;
		log("j2c::getQpNum: obj_id %llu, qpnum %i\n", obj_id, qpnum);
	} else {
		log("j2c::getQpNum: failed, obj_id %llu\n", obj_id);
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

        pthread_rwlock_rdlock(&mut_context);
        struct ibv_context *context = map_context[obj_id];
        pthread_rwlock_unlock(&mut_context);
        if (context != NULL){
                cmd_fd = context->cmd_fd;
                log("j2c::getContextFd: obj_id %llu, fd %i\n", obj_id, cmd_fd);
        } else {
                log("j2c::getContext: failed, obj_id %llu\n", obj_id);
        }

        return cmd_fd;
}

/*
 * Class:     com_ibm_zac_jverbs_impl_nat_NativeDispatcher
 * Method:    _getPdHandle
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_disni_rdma_verbs_impl_NativeDispatcher__1getPdHandle
  (JNIEnv *env, jobject obj, jlong pd){
	jint handle = -1;

	pthread_rwlock_rdlock(&mut_pd);
	struct ibv_pd *protection = map_pd[pd];
	pthread_rwlock_unlock(&mut_pd);
	if (protection != NULL){
		handle = protection->handle;
		log("j2c::getPdHandle: obj_id %llu, handle %i\n", pd, handle);
	} else {
		log("j2c::getPdHandle: failed, obj_id %llu\n", pd);
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

