package com.ibm.disni.examples;

import com.ibm.disni.nvmef.spdk.*;

import java.util.ArrayList;

/**
 * Created by jpf on 06.01.17.
 */
public class SpdkProbe {
    public static void main(String[] args) throws Exception {
        Nvme nvme = new Nvme();
        NvmeTransportId transportId = new NvmeTransportId(NvmeTransportType.RDMA, NvmfAddressFamily.IPV4, "40.0.0.17", "4420", "");
        ArrayList<NvmeController> controllers = new ArrayList<NvmeController>();
        nvme.probe(transportId, controllers);
        for (NvmeController controller : controllers) {
            System.out.println("Controller: " + controller.getObjId());
        }
    }
}