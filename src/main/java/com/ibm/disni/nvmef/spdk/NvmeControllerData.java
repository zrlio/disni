package com.ibm.disni.nvmef.spdk;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by jpf on 10.01.17.
 */
class NvmeControllerData {
    public static int CSIZE = 512;
    private short pciVendorID;
    private short pciSubsystemVendorID;
    private byte serialNumber[];
    private byte modelNumber[];
    private byte firmwareRevision[];

    NvmeControllerData() {
        serialNumber = new byte[20];
        modelNumber = new byte[40];
        firmwareRevision = new byte[8];
    }

    void update(ByteBuffer buffer) {
        buffer.order(ByteOrder.nativeOrder());
        pciVendorID = buffer.getShort();
        pciSubsystemVendorID = buffer.getShort();
        buffer.get(serialNumber);
        buffer.get(modelNumber);
        buffer.get(firmwareRevision);
        // TODO: missing ctrlr_data fields
    }

    public short getPciVendorID() {
        return pciVendorID;
    }

    public short getPciSubsystemVendorID() {
        return pciSubsystemVendorID;
    }

    public byte[] getSerialNumber() {
        return serialNumber;
    }

    public byte[] getModelNumber() {
        return modelNumber;
    }

    public byte[] getFirmwareRevision() {
        return firmwareRevision;
    }
}
