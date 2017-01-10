/*
 * jVerbs: RDMA verbs support for the Java Virtual Machine
 *
 * Author: Jonas Pfefferle <jpf@zurich.ibm.com>
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

package com.ibm.disni.nvmef.spdk;

import sun.nio.ch.DirectBuffer;

import java.io.IOException;
import java.lang.annotation.Native;
import java.nio.ByteBuffer;

public class NvmeController extends NatObject {
    private NvmeNamespace namespaces[];
    private int numberOfNamespaces;

    private NativeDispatcher nativeDispatcher;

    private NvmeControllerData data;

    NvmeController(long objId, NativeDispatcher nativeDispatcher) {
        super(objId);
        this.nativeDispatcher = nativeDispatcher;
        ByteBuffer buffer = ByteBuffer.allocateDirect(NvmeControllerData.CSIZE);
        nativeDispatcher._nvme_ctrlr_get_data(getObjId(), ((DirectBuffer)buffer).address());
        data = new NvmeControllerData();
        data.update(buffer);
    }

    public short getPCIVendorID() {
        return data.getPciVendorID();
    }

    public short getPCISubsystemVendorID() {
        return data.getPciSubsystemVendorID();
    }

    public String getSerialNumber() {
        return new String(data.getSerialNumber());
    }

    public String getModelNumber() {
        return new String(data.getModelNumber());
    }

    public String getFirmwareRevision() {
        return new String(data.getFirmwareRevision());
    }

    public NvmeNamespace getNamespace(int id) throws IOException {
        int idx = id - 1;
        if (namespaces[idx] == null) {
            long namespace = nativeDispatcher._nvme_ctrlr_get_ns(getObjId(), id);
            if (namespace == 0) {
                throw new IOException("nvme_ctrlr_get_ns failed");
            }
            namespaces[idx] = new NvmeNamespace(namespace, nativeDispatcher);
        }
        return namespaces[idx];
    }

    public int getNumberOfNamespaces() throws IOException {
        if (namespaces == null) {
            numberOfNamespaces = nativeDispatcher._nvme_ctrlr_get_num_ns(getObjId());
            if (numberOfNamespaces < 0) {
                throw new IOException("nvme_ctrlr_get_num_ns failed with " + numberOfNamespaces);
            }
            namespaces = new NvmeNamespace[numberOfNamespaces];
        }
        return numberOfNamespaces;
    }
}
