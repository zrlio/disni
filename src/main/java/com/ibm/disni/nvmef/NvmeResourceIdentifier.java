package com.ibm.disni.nvmef;

import com.ibm.disni.nvmef.spdk.NvmeTransportId;
import com.ibm.disni.nvmef.spdk.NvmfAddressFamily;

import java.io.IOException;
import java.net.URI;
import java.util.StringTokenizer;

class NvmeResourceIdentifier {
	private String address;
	private String port;
	private int controller;
	private int namespace;
	private String subsystem;
	private String pci;
	
	public NvmeResourceIdentifier(String address, String port, int controller, int namespace, String subsystem, String pci){
		this.address = address;
		this.port = port;
		this.controller = controller;
		this.namespace = namespace;
		this.subsystem = subsystem;
		this.pci = pci;
	}
	
	public static NvmeResourceIdentifier parse(URI uri) throws IOException {
		if (!uri.getScheme().equalsIgnoreCase("nvmef")){
			throw new IOException("URL has wrong protocol " + uri.getScheme());
		}
		if (uri.getHost().isEmpty()){
			throw new IOException("URL has wrong protocol " + uri.getScheme());
		}		
		
		String address = uri.getHost();
		String port = Integer.toString(uri.getPort());
		int controller = 0;
		int namespace =  1;
		String subsystem = null;
		String pci = null;
		
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
		
		return new NvmeResourceIdentifier(address, port, controller, namespace, subsystem, pci);
	}

	public NvmeTransportId toTransportId() {
		if (getSubsystem() == null && getPci() != null) {
			return NvmeTransportId.pcie(getPci());
		} else {
			return NvmeTransportId.rdma(NvmfAddressFamily.IPV4, getAddress(), getPort(), getSubsystem());
		}
	}

	public String getAddress() {
		return address;
	}

	public String getPort() {
		return port;
	}

	public int getController() {
		return controller;
	}

	public int getNamespace() {
		return namespace;
	}

	public String getSubsystem() {
		return subsystem;
	}

	public String getPci() {
		return pci;
	}

	@Override
	public String toString() {
		return "address " + address + ", port " + port + ", subsystem " + subsystem + ", pci " + pci + ", controller " + controller;
	}
	
}
