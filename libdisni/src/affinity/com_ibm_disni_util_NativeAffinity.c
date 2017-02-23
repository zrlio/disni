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

#define _GNU_SOURCE
#include <jni.h>
#include <sched.h>
#include "com_ibm_disni_util_NativeAffinity.h"
#include <stdio.h>

JNIEXPORT jlong JNICALL Java_com_ibm_disni_util_NativeAffinity__1getAffinity(JNIEnv *env, jclass c){
	cpu_set_t mask;
	int ret = sched_getaffinity(0, sizeof(mask), &mask);
	if (ret < 0) return ~0LL;
	long long mask2 = 0, i;
	for(i=0;i<sizeof(mask2)*8;i++){
		if (CPU_ISSET(i, &mask)){
			mask2 |= 1L << i;
		}
	}
	return (jlong) mask2;
}

JNIEXPORT void JNICALL Java_com_ibm_disni_util_NativeAffinity__1setAffinity(JNIEnv *env, jclass c, jlong affinity){
	int i;
	cpu_set_t mask;
	CPU_ZERO(&mask);
	//printf("incoming mask %llu\n", affinity);
	for(i=0;i<sizeof(affinity)*8;i++){
		if ((affinity >> i) & 1){
			//printf("setting bit index %i\n", i);
			CPU_SET(i, &mask);
		}
	}
	sched_setaffinity(0, sizeof(mask), &mask);
}

