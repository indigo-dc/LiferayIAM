/**
 * *********************************************************************
 * Copyright (c) 2016: Istituto Nazionale di Fisica Nucleare (INFN), Italy
 * Consorzio COMETA (COMETA), Italy
 *
 * See http://www.infn.it and and http://www.consorzio-cometa.it for details on
 * the copyright holders.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 **********************************************************************
 */
package com.liferay.portal.security.sso.iam.exception;

import com.liferay.portal.kernel.exception.PortalException;

import aQute.bnd.annotation.ProviderType;

/**
 * @author Marco Fargetta
 */
@ProviderType
public class NoAccessTokenAvailable extends PortalException {

	public NoAccessTokenAvailable() {
		super();
		// TODO Auto-generated constructor stub
	}

	public NoAccessTokenAvailable(String msg, Throwable cause) {
		super(msg, cause);
		// TODO Auto-generated constructor stub
	}

	public NoAccessTokenAvailable(String msg) {
		super(msg);
		// TODO Auto-generated constructor stub
	}

	public NoAccessTokenAvailable(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

}
