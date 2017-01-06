package com.ibm.disni.nvmef.spdk;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by jpf on 06.01.17.
 */

public class Nvme {

    private NativeDispatcher nativeDispatcher;

    public Nvme() {
        nativeDispatcher = new NativeDispatcher();
    }

    public void probe(NvmeTransportId id, ArrayList<NvmeController> controller) throws IOException {
        int probeId = nativeDispatcher._nvme_probe(id);
        if (probeId < 0) {
            throw new IOException("spdk_nvme_probe failed with " + probeId);
        }
        long controllerId;
        do {
            controllerId = nativeDispatcher._nvme_probe_next(probeId);
            controller.add(new NvmeController(controllerId));
        } while (controllerId != 0);
    }
}
