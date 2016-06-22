/**
 * *********************************************************************
 * Copyright (c) 2016: Istituto Nazionale di Fisica Nucleare (INFN) -
 * INDIGO-DataCloud
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

package com.liferay.portal.security.sso.iam.model;

import java.util.List;

import com.liferay.portal.kernel.json.JSON;

import aQute.bnd.annotation.ProviderType;


/**
 * @author Marco Fargetta
 */
@ProviderType
public class TokenInfo {

	
	/**
	 * @return the error
	 */
	public final String getError() {
		return error;
	}
	/**
	 * @param error the error to set
	 */
	public final void setError(String error) {
		this.error = error;
	}
	/**
	 * @return the subject
	 */
	public final String getSubject() {
		return subject;
	}
	/**
	 * @param subject the subject to set
	 */
	public final void setSubject(String subject) {
		this.subject = subject;
	}
	/**
	 * @return the groups
	 */
	@JSON
	public final List<String> getGroups() {
		return groups;
	}
	/**
	 * @param groups the groups to set
	 */
	public final void setGroups(List<String> groups) {
		this.groups = groups;
	}


	private String error;
	private String subject;
	private List<String> groups;
}
