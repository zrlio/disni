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
        long controllerIds[] = new long[8];
        int i;
        int probeId;
        do {
            probeId = nativeDispatcher._nvme_probe(id.getType().getNumVal(), id.getAddressFamily().getNumVal(),
                    id.getAddress(), id.getServiceId(), id.getSubsystemNQN(), controllerIds);
            if (probeId < 0) {
                throw new IOException("spdk_nvme_probe failed with " + probeId);
            }

            for (i = 0; i < probeId; i++) {
                if (controllerIds[i] == 0) {
                    break;
                }
                controller.add(new NvmeController(controllerIds[i]));
            }
        } while (i == probeId);
    }
}
