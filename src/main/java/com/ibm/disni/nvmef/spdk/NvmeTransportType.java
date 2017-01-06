package com.ibm.disni.nvmef.spdk;

public enum NvmeTransportType {
	PCIE(256), RDMA(0x1);

	private int numVal;

	NvmeTransportType(int numVal) {
		this.numVal = numVal;
	}

	public int getNumVal() {
		return numVal;
	}
}