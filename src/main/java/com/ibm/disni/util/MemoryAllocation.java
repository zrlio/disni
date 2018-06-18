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

package com.ibm.disni.util;

import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;


public class MemoryAllocation {
	private static final int MIN_BLOCK_SIZE = 64; // 64B
	private int MAX_CACHE_SIZE = 4 * 1024 * 1024; // 4MB
	private final ConcurrentHashMap<Integer, AllocatorStack> allocStackMap =
		new ConcurrentHashMap<>();
	private static final Logger logger = DiSNILogger.getLogger();
	private static MemoryAllocation instance = null;

	public synchronized static MemoryAllocation getInstance() {
		if (instance == null) {
			instance = new MemoryAllocation();
		}
		return instance;
	}

	private class AllocatorStack {
		private final ConcurrentLinkedDeque<MemBuf> stack = new ConcurrentLinkedDeque<>();
		private final AtomicLong idleBuffersSize = new AtomicLong(0);
		private final int size;
		private long lastAccess;

		private AllocatorStack(int size) {
			this.size = size;
		}

		private MemBuf get() {
			lastAccess = System.nanoTime();
			MemBuf buffer = stack.pollFirst();
			if (buffer == null) {
				logger.debug("Allocating buffer of size {}" , size);
				ByteBuffer buff = ByteBuffer.allocateDirect(size);
				buff.order(ByteOrder.nativeOrder());
				return new MemBuf(buff);
			} else {
				logger.debug("Reusing buffer of size {}", size);
				return buffer;
			}
		}

		private void put(MemBuf buffer) {
			lastAccess = System.nanoTime();
			buffer.getBuffer().clear();
			stack.addLast(buffer);
			idleBuffersSize.addAndGet(size);
		}

		public void preallocate(int numBuffers){
			logger.debug("Pre allocating {} buffer of size {} B", numBuffers, size);
			for (int i = 0; i < numBuffers; i++) {
				ByteBuffer buff = ByteBuffer.allocateDirect(size);
				buff.order(ByteOrder.nativeOrder());
				put(new MemBuf(buff));
			}
		}
	}

	private int roundUpSize(int size){
		// Round up length to the nearest power of two, or the minimum block size
		if (size < MIN_BLOCK_SIZE) {
			size = MIN_BLOCK_SIZE;
		} else {
			size--;
			size |= size >> 1;
			size |= size >> 2;
			size |= size >> 4;
			size |= size >> 8;
			size |= size >> 16;
			size++;
		}
		return size;
	}

	public MemBuf allocate(int size){
		return getOrCreateAllocatorStack(roundUpSize(size)).get();
	}

	private AllocatorStack getOrCreateAllocatorStack(int length) {
		AllocatorStack allocatorStack = allocStackMap.get(length);
		if (allocatorStack == null) {
			allocStackMap.putIfAbsent(length, new AllocatorStack(length));
			allocatorStack = allocStackMap.get(length);
		}

		return allocatorStack;
	}

	public void put(MemBuf buf){
		AllocatorStack allocatorStack = allocStackMap.get(buf.size());
		if (allocatorStack != null) {
			allocatorStack.put(buf);
			// Check the size of current idling buffers
			long idleBuffersSize = allocStackMap
				.reduceValuesToLong(100L,
					allocStack -> allocStack.idleBuffersSize.get(), 0L, Long::sum);
			// If it reached out 90% of idle buffer capacity, clean old stacks
			if (idleBuffersSize > MAX_CACHE_SIZE * 0.90) {
				cleanLRUSBuffers(idleBuffersSize);
			}
		}
	}

	private void cleanLRUSBuffers(long idleBuffersSize){
		logger.debug("Current idling buffers size {}KB exceed 90% of maxCacheSize {}KB." +
			" Cleaning LRU idling buffers", idleBuffersSize / 1024, MAX_CACHE_SIZE / 1024);
		// Find least recently used buffer stack - it has lowest lastAccess value
		AllocatorStack lruStack = allocStackMap.values().stream()
			.sorted(Comparator.comparingLong(s -> s.lastAccess)).iterator().next();
		long totalCleaned = 0;
		// Will clean up to 65% of capacity
		long needToClean = idleBuffersSize - (long) (MAX_CACHE_SIZE * 0.65);
		while (!lruStack.stack.isEmpty() && totalCleaned < needToClean) {
			MemBuf buffer = lruStack.stack.pollFirst();
			if (buffer != null) {
				totalCleaned += lruStack.size;
				lruStack.idleBuffersSize.addAndGet(-lruStack.size);
			}
		}
		logger.debug("Cleaned {} KB of idle stacks of size {} KB",
			totalCleaned / 1024, lruStack.size / 1024);
	}

	void setCacheSize(int maxCacheSize){
		this.MAX_CACHE_SIZE = maxCacheSize;
	}

	public void preAllocate(int size, int nBuffers){
		getOrCreateAllocatorStack(roundUpSize(size)).preallocate(nBuffers);
	}
}
