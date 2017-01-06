package com.ibm.disni.nvmef.spdk;

public enum NvmfAddressFamily {
	IPV4(0x1), IPV6(0x2), IB(0x3), FC(0x4), INTRA_HOST(0xfe);

	private int numVal;

	NvmfAddressFamily(int numVal) {
		this.numVal = numVal;
	}

	public int getNumVal() {
		return numVal;
	}
}