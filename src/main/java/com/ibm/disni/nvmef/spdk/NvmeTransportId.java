package com.ibm.disni.nvmef.spdk;

/**
 * Created by jpf on 06.01.17.
 */
public class NvmeTransportId {
    private NvmeTransportType type;
    private NvmfAddressFamily addressFamily;
    private String address;
    private String serviceId;
    private String subsystemNQN;

    public NvmeTransportId(NvmeTransportType type, NvmfAddressFamily addressFamily, String address, String serviceId, String subsystemNQN) {
        this.type = type;
        this.addressFamily = addressFamily;
        this.address = address;
        this.serviceId = serviceId;
        this.subsystemNQN = subsystemNQN;
    }

    public NvmeTransportType getType() {
        return type;
    }

    public void setType(NvmeTransportType type) {
        this.type = type;
    }

    public NvmfAddressFamily getAddressFamily() {
        return addressFamily;
    }

    public void setAddressFamily(NvmfAddressFamily addressFamily) {
        this.addressFamily = addressFamily;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getSubsystemNQN() {
        return subsystemNQN;
    }

    public void setSubsystemNQN(String subsystemNQN) {
        this.subsystemNQN = subsystemNQN;
    }
}
