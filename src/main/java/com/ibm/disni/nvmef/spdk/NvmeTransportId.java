/*
 * DiSNI: Direct Storage and Networking Interface
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

package com.ibm.disni.nvmef.spdk;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.StringTokenizer;

public class NvmeTransportId {
	private NvmeTransportType type;
	private NvmfAddressFamily addressFamily;
	private String address;
	private String serviceId;
	private String subsystemNQN;

	NvmeTransportId(NvmeTransportType type, NvmfAddressFamily addressFamily, String address, String serviceId, String subsystemNQN) {
		this.type = type;
		this.addressFamily = addressFamily;
		this.address = address;
		this.serviceId = serviceId;
		this.subsystemNQN = subsystemNQN;
	}

	public static NvmeTransportId rdma(NvmfAddressFamily addressFamily, String address, String serviceId, String subsystemNQN) {
		return new NvmeTransportId(NvmeTransportType.RDMA, addressFamily, address, serviceId, subsystemNQN);
	}

	public static NvmeTransportId pcie(String pciAddress) {
		// for transport type PCIE the rest of the parameters are ignored by spdk (at least for now)
		return new NvmeTransportId(NvmeTransportType.PCIE, NvmfAddressFamily.INTRA_HOST, pciAddress, "", "");
	}
	
	public static NvmeTransportId parse(URI uri) throws IOException {
		String address = uri.getHost();
		String port = Integer.toString(uri.getPort());
		int controller = 0;
		int namespace =  1;
		String subsystem = "";
		String pci = "";
		
		String path = uri.getPath();
		if (path != null){
			StringTokenizer pathTokenizer = new StringTokenizer(path, "/");
			if (pathTokenizer.countTokens() > 2){
				throw new IOException("URL format error, too many elements in path");
			}
			for (int i = 0; pathTokenizer.hasMoreTokens(); i++){
				switch(i) {
				case 0:
					controller = Integer.parseInt(pathTokenizer.nextToken());
					break;
				case 1:
					namespace = Integer.parseInt(pathTokenizer.nextToken());
					break;
				}
			}
		}
		
		String query = uri.getQuery();
		if (query != null){
			StringTokenizer queryTokenizer = new StringTokenizer(query, "&");
			while (queryTokenizer.hasMoreTokens()){
				String param = queryTokenizer.nextToken();
				if (param.startsWith("subsystem")){
					subsystem = param.substring(10);
				}
				if (param.startsWith("pci")){
					pci = param.substring(4);
				}	
			}			
		}
		
		if (pci.isEmpty()){
			return new NvmeTransportId(NvmeTransportType.RDMA, NvmfAddressFamily.IPV4, address, port, subsystem);
		} else {
			return new NvmeTransportId(NvmeTransportType.PCIE, NvmfAddressFamily.INTRA_HOST, pci, "", "");
		}
	}	
	
	public URI toURI() throws URISyntaxException {
		if (type == NvmeTransportType.RDMA){
			return new URI("nvmef://" + address + ":" + serviceId + "/0/1?subsystem=" + this.subsystemNQN);
		} else {
			return new URI("nvmef://127.0.0.1:7777?pci=" + address);
		}
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

	@Override
	public String toString() {
		return "Transport type = " + type.name() +
				", Adress Family = " + addressFamily.name() +
				", address = " + address +
				", serviceId = " + serviceId +
				", subsystem NQN = " + subsystemNQN;
	}
}
