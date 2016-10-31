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

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;

import com.ibm.disni.rdma.verbs.IbvMr;


public class ReadyToReceive {
	private int dataSize;
	private int fragmentsize;
	private int loop;
	private LinkedList<IbvMr> mrList;

	public ReadyToReceive(int dataSize, int fragmentsize, int loop,
			LinkedList<IbvMr> mrList) {
		this.dataSize = dataSize;
		this.fragmentsize = fragmentsize;
		this.loop = loop;
		this.mrList = new LinkedList<IbvMr>();
		this.mrList.addAll(mrList);
	}

	public ReadyToReceive(int dataSize, int fragmentsize, int loop) {
		this.dataSize = dataSize;
		this.fragmentsize = fragmentsize;
		this.loop = loop;
		this.mrList = null;
	}

	public void writeBack(ByteBuffer buffer) {
		buffer.putInt(dataSize);
		buffer.putInt(fragmentsize);
		buffer.putInt(loop);
		if (mrList != null) {
			int mrsize = mrList.size();
			buffer.putInt(mrsize);
			for (Iterator<IbvMr> iter = mrList.iterator(); iter.hasNext();) {
				IbvMr mr = iter.next();
				buffer.putLong(mr.getAddr());
				buffer.putInt(mr.getLkey());
			}
		} else {
			int mrsize = 0;
			buffer.putInt(mrsize);
		}
	}

	public void update(ByteBuffer buffer) throws Exception {
		this.dataSize = buffer.getInt();
		this.fragmentsize = buffer.getInt();
		this.loop = buffer.getInt();
		int mrsize = buffer.getInt();
		this.mrList = new LinkedList<IbvMr>();
		for (int i = 0; i < mrsize; i++) {
			long addr = buffer.getLong();
			int lkey = buffer.getInt();
			IbvMr mr = new IbvMr(null, addr, 0, lkey, 0, 0);
			mrList.add(mr);
		}
	}

	public int size() {
		int mrsize = 0;
		if (mrList != null) {
			mrsize = mrList.size();
		}
		return 16 + mrsize * 4 + mrsize * 8;
	}

	public int getDataSize() {
		return dataSize;
	}

	public void setDataSize(int dataSize) {
		this.dataSize = dataSize;
	}

	public int getLoop() {
		return loop;
	}

	public void setLoop(int loop) {
		this.loop = loop;
	}

	public LinkedList<IbvMr> getStagList() {
		return mrList;
	}
}
