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

package com.liferay.portal.security.sso.iam.configuration;

import aQute.bnd.annotation.metatype.Meta;
import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

/**
 * @author Marco Fargetta
 */
@ExtendedObjectClassDefinition(category = "foundation")
@Meta.OCD(
		id = "com.liferay.portal.security.sso.iam.configuration.IAMConfiguration",
		localization = "content/Language",
		name = "iam.configuration.name"
)
public interface IAMConfiguration {

	@Meta.AD(deflt = "false", required = false)
	public boolean enabled();

	@Meta.AD(
		deflt = "false", name ="%require-verified-account", required = false
	)
	public boolean verifiedAccountRequired();

	@Meta.AD(name ="%application-id", required = false)
	public String appId();

	@Meta.AD(name ="%application-secret", required = false)
	public String appSecret();

	@Meta.AD(deflt = "https://iam.indigo-datacloud.eu/.well-known/openid-configuration",
			name ="%configuration-url", required = false)
	public String configurationURL();

	@Meta.AD(
		deflt = "https://iam.indigo-datacloud.eu/oauth2-as/oauth2-authz",
		name ="%oauth-authentication-url", required = false
	)
	public String oauthAuthURL();

	@Meta.AD(
		deflt = "https://iam.indigo-datacloud.eu/oauth2/token",
		name ="%oauth-token-url", required = false
	)
	public String oauthTokenURL();

	@Meta.AD(
		deflt = "https://iam.indigo-datacloud.eu/oauth2/userinfo",
		name ="%openid-userinfo-url", required = false
	)
	public String openidUserinfoURL();

	@Meta.AD(
		deflt = "https://iam.indigo-datacloud.eu/oauth2/jwk",
		name ="%openid-jwk-url", required = false
	)
	public String openidJwkURL();

	@Meta.AD(name ="%openid-issuer", required = false)
	public String openidIssuer();
}
