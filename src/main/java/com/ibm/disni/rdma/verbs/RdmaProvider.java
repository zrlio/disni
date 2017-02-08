/*
 * DiSNI: Direct Storage and Networking Interface
 *
 * Author: Patrick Stuedi <stu@zurich.ibm.com>
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

package com.ibm.disni.rdma.verbs;

import java.io.IOException;

//import com.ibm.jverbs.impl.RdmaProviderImpl;



import com.ibm.disni.rdma.verbs.impl.RdmaProviderNat;


// TODO: Auto-generated Javadoc
/**
 * Factory class to enable multiple implementations of the jVerbs API. 
 * 
 * Applications when opening the connection management interface (RdmaCm) or the verbs interface (RdmaVerbs) will use the RdmaProvider class to determine the actual implementation of the CM and the Verbs interface. 
 * 
 * A concrete implementation of the jVerbs API will have to implement the abstract methods of this class. 
 * 
 */
public abstract class RdmaProvider {  
	private static RdmaProvider provider = null;
	
	protected RdmaProvider(){
		
	}
	
	/**
	 *
	 * Create an instance of a RDMA provider. 
	 * 
	 * @return The RDMA provider. Which RDMA provider is instantiated is determined by the 'com.ibm.jverbs.provider' system property.
	 * @throws IOException TODO
	 */
	public static synchronized RdmaProvider provider() throws IOException {
		if (provider == null){
			provider = new RdmaProviderNat();
		}
		return provider;
	}
	
	/**
	 * Open the connection management interface.
	 *
	 * @return the RdmaCm interface.
	 * @throws Exception if no implementation available.
	 */
	public abstract RdmaCm openCm() throws IOException;
	
	/**
	 * Open the verbs interface.
	 *
	 * @return the RdmaVerbs interface.
	 * @throws Exception if no implementation available.
	 */
	public abstract RdmaVerbs openVerbs() throws IOException;
	
	/**
	 * Returns the version of jVerbs
	 *
	 * @return version
	 * @throws Exception if no implementation available.
	 */	
	public abstract int getVersion();
}
