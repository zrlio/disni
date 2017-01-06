package com.ibm.disni.nvmef.spdk;

import com.ibm.disni.util.DiSNILogger;
import org.slf4j.Logger;

import java.util.ArrayList;

/**
 * Created by jpf on 06.01.17.
 */
public class NativeDispatcher {
    private static final Logger logger = DiSNILogger.getLogger();

    static {
        System.loadLibrary("disni");
    }

    public native int _nvme_probe(NvmeTransportId id, long[] controllerIds);
}
