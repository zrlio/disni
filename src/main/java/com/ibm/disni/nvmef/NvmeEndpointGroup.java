package com.ibm.disni.nvmef;

import java.io.IOException;
import java.util.ArrayList;

import com.ibm.disni.nvmef.spdk.Nvme;
import com.ibm.disni.nvmef.spdk.NvmeController;
import com.ibm.disni.nvmef.spdk.NvmeTransportId;
import com.ibm.disni.nvmef.spdk.NvmeTransportType;
import com.ibm.disni.nvmef.spdk.NvmfAddressFamily;

public class NvmeEndpointGroup {
	private Nvme nvme;
	
	public NvmeEndpointGroup(){
		this.nvme = new Nvme();
	}
	
	//most likely not used
	public NvmeServerEndpoint createServerEndpoint(){
		return new NvmeServerEndpoint(this);
	}
	
	public NvmeEndpoint createEndpoint(){
		return new NvmeEndpoint(this);
	}
	
	//--------------- internal ------------------

	NvmeController probe(String host, int i) throws IOException {
		ArrayList<NvmeController> controllers = new ArrayList<NvmeController>();
		NvmeTransportId transportId = new NvmeTransportId(NvmeTransportType.RDMA, NvmfAddressFamily.IPV4, host, "4420", "nqn.2014-08.org.nvmexpress.discovery");
		nvme.probe(transportId, controllers);
		return controllers.get(i);
	}
}
