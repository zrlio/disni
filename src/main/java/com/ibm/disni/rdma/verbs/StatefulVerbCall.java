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

/**
 * The Interface StatefulVerbCall.
 * 
 * StatefulVerbCall represents a verb call for a given set of parameter values, a so called stateful verb call (SVC). An application uses 
 * execute() method to execute the SVC object. The execute() returns a call specific type T which can be queried for the result of the operation. 
 * 
 * One key advantage of SVCs is that they can be cached and re-executed many times. Semantically, each execution of an SVC object is identical to a jVerbs call evaluated against the 
 * current parameter state of the SVC object. Any serialization state that is necessary while executing the SVC object, however, will only have to be created 
 * when executing the object for the first time. Subsequent calls use the already established serialization state, and will 
 * therefore execute much faster. Once the SVC object no longer needed, resources can be freed using the free() API call.
 *
 * @param <T> the type of the result of this SVC.
 */
public interface StatefulVerbCall<T extends StatefulVerbCall<?>> {
	
	/**
	 * Execute the state of this verbs call.
	 *
	 * @return the call-specific SVC object which can be used to query the result.
	 * @throws Exception on failure.
	 */
	public T execute() throws IOException;

	/**
	 * Checks if this SVC object is still valid and can be executed.
	 *
	 * @return true, if is valid
	 */
	public boolean isValid();
	
	/**
	 * Free any resources associated with this SVC.
	 *
	 * @return the SVC object itself.
	 */
	public T free();
	
	
}
