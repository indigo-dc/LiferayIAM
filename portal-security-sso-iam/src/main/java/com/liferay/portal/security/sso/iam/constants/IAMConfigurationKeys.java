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

package com.liferay.portal.security.sso.iam.constants;

/**
 * @author Marco Fargetta
 */
public class IAMConfigurationKeys {

	public static final String APP_ID = "appId";

	public static final String APP_SECRET = "appSecret";

	public static final String AUTH_ENABLED = "enabled";

	public static final String CONFIGURATION_URL = "configurationURL";

	public static final String OAUTH_AUTH_URL = "oauthAuthURL";

	public static final String OAUTH_TOKEN_URL = "oauthTokenURL";

	public static final String OAUTH_EXTRA_SCOPES = "oauthExtraScopes";

	public static final String OPENID_USERINFO_URL = "openidUserinfoURL";

	public static final String OPENID_JWK_URL = "openidJwkURL";

	public static final String OPENID_ISSUER = "openidIssuer";

	public static final String VERIFIED_ACCOUNT_REQUIRED =
		"verifiedAccountRequired";
}
