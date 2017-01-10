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

package com.ibm.disni.examples;

import com.ibm.disni.nvmef.spdk.*;

import java.util.ArrayList;

public class SpdkProbe {
    public static void main(String[] args) throws Exception {
        Nvme nvme = new Nvme();
        NvmeTransportId transportId = new NvmeTransportId(NvmeTransportType.RDMA, NvmfAddressFamily.IPV4, "10.40.0.17", "4420", "");
        ArrayList<NvmeController> controllers = new ArrayList<NvmeController>();
        nvme.probe(transportId, controllers);
        for (NvmeController controller : controllers) {
            System.out.println("Controller: " + controller.getObjId());
        }
    }
}