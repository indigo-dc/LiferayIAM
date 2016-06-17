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

package com.liferay.portal.security.sso.iam.internal.util;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.module.configuration.ConfigurationException;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.security.sso.iam.configuration.IAMConfiguration;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;

/**
 * @author Marco Fargetta
 */
public class IAMEndPoints {
	public IAMEndPoints(IAMConfiguration iamConf) throws ConfigurationException {

		try {
			oidcMeta = getMetadata(iamConf);
		} catch (Exception ex) {
			_log.error("IAM Configuration URL '" + iamConf.configurationURL()
				+ "' is not reachable");	
		}
		if (oidcMeta != null) {
			authURI = oidcMeta.getAuthorizationEndpointURI();
			tokenURI = oidcMeta.getTokenEndpointURI();
			jwkURI = oidcMeta.getJWKSetURI();
			userInfo = oidcMeta.getUserInfoEndpointURI();
			issuer = oidcMeta.getIssuer();
			try {
			validator = new IDTokenValidator(
					oidcMeta.getIssuer(),
					new ClientID(iamConf.appId()),
					JWSAlgorithm.RS256,
					oidcMeta.getJWKSetURI().toURL());
			} catch (MalformedURLException mue) {
				throw new ConfigurationException("JWK URI publication errror");
			}
		} else {
			if (Validator.isNull(iamConf.oauthAuthURL()) || Validator.isNull(iamConf.oauthTokenURL()) || Validator.isNull(iamConf.openidJwkURL())) {
				throw new ConfigurationException("IAM Authentication is not properly configured. Authentication cannot proceed");
			}
			issuer = new Issuer(iamConf.openidIssuer());
			try {
				authURI = new URI(iamConf.oauthAuthURL());
				tokenURI = new URI(iamConf.oauthTokenURL());
				jwkURI = new URI(iamConf.openidJwkURL());
				userInfo = new URI(iamConf.openidUserinfoURL());
				validator = new IDTokenValidator(issuer,
						new ClientID(iamConf.appId()),
						JWSAlgorithm.RS256,
						jwkURI.toURL());
			} catch(URISyntaxException use) {
				throw new ConfigurationException("IAM Authentication is not properly configured. Authentication cannot proceed");
			} catch(MalformedURLException mue) {
				throw new ConfigurationException("JWK URL is not properly configured");
			}
			
		}		
		clientID = new ClientID(iamConf.appId());
		secret = new Secret(iamConf.appSecret());
	}

	private OIDCProviderMetadata getMetadata(IAMConfiguration iamConf) throws Exception {
		if (Validator.isNull(iamConf.configurationURL())) {
			return null;
		}
		URL configurationURL = new URL(iamConf.configurationURL());
		InputStream stream = configurationURL.openStream();
		String info = null;
		try (java.util.Scanner s = new java.util.Scanner(stream)) {
			info = s.useDelimiter("\\A").hasNext() ? s.next() : "";
		}
		return OIDCProviderMetadata.parse(info);
	}

	
	
	/**
	 * @return the oidcMeta
	 */
	public final OIDCProviderMetadata getOidcMeta() {
		return oidcMeta;
	}

	/**
	 * @return the tokenURI
	 */
	public final URI getTokenURI() {
		return tokenURI;
	}

	/**
	 * @return the jwkURI
	 */
	public final URI getJwkURI() {
		return jwkURI;
	}

	/**
	 * @return the userInfo
	 */
	public final URI getUserInfo() {
		return userInfo;
	}

	/**
	 * @return the issuer
	 */
	public final Issuer getIssuer() {
		return issuer;
	}

	/**
	 * @return the clientID
	 */
	public final ClientID getClientID() {
		return clientID;
	}

	/**
	 * @return the secret
	 */
	public final Secret getSecret() {
		return secret;
	}

	/**
	 * @return the validator
	 */
	public final IDTokenValidator getValidator() {
		return validator;
	}

	/**
	 * @return the authURI
	 */
	public final URI getAuthURI() {
		return authURI;
	}

	private OIDCProviderMetadata oidcMeta = null;
	private URI authURI = null;
	private URI tokenURI = null;
	private URI jwkURI = null;
	private URI userInfo = null;
	private Issuer issuer = null;
	private ClientID clientID = null;
	private Secret secret = null;
	private IDTokenValidator validator = null;
	private static final Log _log = LogFactoryUtil.getLog(
			IAMEndPoints.class);
}
