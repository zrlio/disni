/*
 * jVerbs: RDMA verbs support for the Java Virtual Machine
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

package com.ibm.disni.examples.benchmarks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.LinkedList;

import org.slf4j.Logger;

import com.ibm.disni.util.DiSNILogger;

public class BufferUtils { 
	private static final Logger logger = DiSNILogger.getLogger();

	public static void tagBuffer(ByteBuffer[] fragments, int round) {
		if (fragments.length >= 1) {
			ByteBuffer first = fragments[0];
			ByteBuffer last = fragments[fragments.length - 1];

			first.putInt(0, round);
			last.putInt(0, round);
		}
	} 

	public static boolean checkTag(ByteBuffer[] fragments, int round) {
		if (fragments.length >= 1) {
			ByteBuffer first = fragments[0];
			ByteBuffer last = fragments[fragments.length - 1];

			int firsttag = first.getInt(0);
			int lasttag = last.getInt(0);
			if (lasttag == round && firsttag == round) {
				return true;
			} 
		}
		return false;
	}

	public static ByteBuffer[] getBufferFragments(String filename, int size,
			int fragmentsize, boolean memoryDirect)
			throws Exception {
		int remainingsize = size;
		LinkedList<Integer> _fragments = new LinkedList<Integer>();
		while (remainingsize > 0) {
			int currentbuffersize = fragmentsize;
			if (currentbuffersize > remainingsize) {
				currentbuffersize = remainingsize;
			}
			_fragments.add(Integer.valueOf(currentbuffersize));
			remainingsize -= currentbuffersize;
		}

		ByteBuffer[] fragments = new ByteBuffer[_fragments.size()];
		int i = 0;
		int offset = 0;
		for (Iterator<Integer> iter = _fragments.iterator(); iter.hasNext(); i++) {
			Integer _size = iter.next();
			ByteBuffer buffer = getBuffer(filename, offset, _size.intValue(), memoryDirect);
			buffer.clear();
			fragments[i] = buffer;
			offset += _size.intValue();
		}

		return fragments;
	}

	public static ByteBuffer getBuffer(String filename, int offset, int size,
			boolean memoryDirect) throws FileNotFoundException,
			IOException {
		ByteBuffer buffer;
		if (filename.isEmpty()) {
			buffer = _getMemoryBuffer(size, memoryDirect);
		} else {
			File file = new File(filename);
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			FileChannel channel = raf.getChannel();
			buffer = _getMappedBuffer(channel, offset, size);
			raf.close();
		}

		return buffer;
	}

	public static ByteBuffer _getMemoryBuffer(int size, boolean memoryDirect) {
		ByteBuffer buffer;
		if (memoryDirect) {
			buffer = ByteBuffer.allocateDirect(size);
		} else {
			logger.info("getting memory buffer, on-heap");
			buffer = ByteBuffer.allocate(size);
		}
		return buffer;
	}

	public static ByteBuffer _getMappedBuffer(FileChannel channel, int offset,
			int size) throws IOException {
		logger.info("getting mapped buffer");
		ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, offset,
				size);
		return buffer;
	}
	
	public static void setString(ByteBuffer buffer, int index){
		byte[] data = new byte[buffer.capacity()];
		for (int i = 0; i < data.length; i++){
			data[i] = 'x';
		}
		String _index = "" + index;
		byte[] __index = _index.getBytes();
		for (int i = 0; i < __index.length; i++){
			data[i] = __index[i];
		}
		buffer.clear();
		buffer.put(data);
		buffer.clear();
	}	
	
	public static void clearString(ByteBuffer buffer){
		byte[] data = new byte[buffer.capacity()];
		for (int i = 0; i < data.length; i++){
			data[i] = '0';
		}
		buffer.clear();
		buffer.put(data);
		buffer.clear();
	}	
	
	public static void dumpBuffer(ByteBuffer buffer) {
		String outputbuf = "";
		for (int i = 0; i < buffer.capacity(); i++) {
			byte tmp1 = buffer.get(i);
			int tmp2 = tmp1 & 0xff;
			outputbuf += tmp2 + " ";
		}
		logger.info(outputbuf);
		buffer.clear();
	}

	public static void dumpString(ByteBuffer buffer) {
		buffer.clear();
		byte[] data = new byte[buffer.capacity()];
		buffer.get(data);
		String tmp = new String(data);
		buffer.clear();
		logger.info("buffer string: " + tmp);
	}

}
