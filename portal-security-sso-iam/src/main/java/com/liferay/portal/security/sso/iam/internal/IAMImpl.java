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

package com.liferay.portal.security.sso.iam.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.expando.kernel.model.ExpandoTableConstants;
import com.liferay.expando.kernel.model.ExpandoValue;
import com.liferay.expando.kernel.service.ExpandoValueLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Contact;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.UserGroupRole;
import com.liferay.portal.kernel.module.configuration.ConfigurationException;
import com.liferay.portal.kernel.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.security.auth.AuthException;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.settings.CompanyServiceSettingsLocator;
import com.liferay.portal.kernel.util.CalendarFactoryUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.security.sso.iam.IAM;
import com.liferay.portal.security.sso.iam.configuration.IAMConfiguration;
import com.liferay.portal.security.sso.iam.constants.IAMConstants;
import com.liferay.portal.security.sso.iam.constants.IAMWebKeys;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;

/**
 * @author Marco Fargetta
 */
@Component(
		configurationPid = "com.liferay.portal.security.sso.iam.configuration.IAMConfiguration",
		immediate = true,
		service = IAM.class
)
public class IAMImpl implements IAM {

	@Override
	public User addOrUpdateUser(HttpSession session, long companyId, String authorizationCode, String returnRequestUri,
			List<String> scopes) throws Exception {
		_log.debug("Add or update a new user");
		IAMConfiguration iamConf = getIAMConfiguration(companyId);
		OIDCProviderMetadata oidcMeta = null;
		URI tokenURI = null;
		URI jwkURI = null;
		URI userInfo = null;
		Issuer issuer = null;
		IDTokenValidator validator = null;
		AuthorizationCode authCode = new AuthorizationCode(authorizationCode);

		try {
			oidcMeta = getMetadata(companyId);
		} catch (Exception ex) {
			_log.error("IAM Configuration URL '" + iamConf.configurationURL()
				+ "' is not reachable");	
		}
		if (oidcMeta != null) {
			tokenURI = oidcMeta.getTokenEndpointURI();
			jwkURI = oidcMeta.getJWKSetURI();
			userInfo = oidcMeta.getUserInfoEndpointURI();
			issuer = oidcMeta.getIssuer();
			validator = new IDTokenValidator(
					oidcMeta.getIssuer(),
					new ClientID(iamConf.appId()),
					JWSAlgorithm.RS256,
					oidcMeta.getJWKSetURI().toURL());
					
		} else {
			if (Validator.isNull(iamConf.oauthTokenURL()) || Validator.isNull(iamConf.openidJwkURL())) {
				throw new ConfigurationException("IAM Authentication is not properly configured. Authentication cannot proceed");
			}
			issuer = new Issuer(iamConf.openidIssuer());
			tokenURI = new URI(iamConf.oauthTokenURL());
			jwkURI = new URI(iamConf.openidJwkURL());
			userInfo = new URI(iamConf.openidUserinfoURL());
			validator = new IDTokenValidator(issuer,
					new ClientID(iamConf.appId()),
					JWSAlgorithm.RS256,
					jwkURI.toURL());
		}		
		TokenRequest tokenReq = new TokenRequest(
				tokenURI,
				new ClientSecretBasic(new ClientID(iamConf.appId()), new Secret(iamConf.appSecret())),
				new AuthorizationCodeGrant(authCode, new URI(returnRequestUri)));

		HTTPResponse tokenHTTPResp = null;
        try {
            _log.debug("Token request header content: "+tokenReq.toHTTPRequest().getHeader("Content-Type"));
            _log.debug("Token request header authorisation: "+tokenReq.toHTTPRequest().getHeader("Authorization"));
            _log.debug("Token request query: "+tokenReq.toHTTPRequest().getQuery());
            tokenHTTPResp = tokenReq.toHTTPRequest().send();
        } catch (SerializeException ex) {
            _log.error(ex);
        } catch (IOException ex) {
            _log.error(ex);
        }

        TokenResponse tokenResp = null;
        try {
            _log.debug("Token response: "+tokenHTTPResp.getContent());
            tokenResp = OIDCTokenResponseParser.parse(tokenHTTPResp);
        } catch (ParseException ex) {
            _log.error(ex);
        }

        if(tokenResp == null || tokenResp instanceof TokenErrorResponse){
            throw new AuthException("OpenId Connect server does not authenticate");
        }

        OIDCTokenResponse oidcTokenResponse = (OIDCTokenResponse) tokenResp;
        
        try {
        	_log.debug("Token response: " + oidcTokenResponse.toJSONObject().toString());
        	IDTokenClaimsSet idtcs = validator.validate(oidcTokenResponse.getOIDCTokens().getIDToken(), null);
        	_log.debug("IDToken claims" + idtcs.toJSONObject().toJSONString());
        } catch(BadJOSEException bjse) {
        	_log.error("Invalid token detected");
        	return null;
        }
        UserInfoRequest userInfoReq = new UserInfoRequest(
        		userInfo, oidcTokenResponse.getOIDCTokens().getBearerAccessToken());
  
        HTTPResponse userInfoHTTPResp = null;
        try {
            userInfoHTTPResp = userInfoReq.toHTTPRequest().send();
        } catch (SerializeException ex) {
            _log.error(ex);
        } catch (IOException ex) {
            _log.error(ex);
        }
        UserInfoResponse userInfoResp = null;
        try {
            userInfoResp = UserInfoResponse.parse(userInfoHTTPResp);
        } catch (ParseException ex) {
            _log.error(ex);
        }

        if(userInfoResp==null || userInfoResp instanceof UserInfoErrorResponse)
            throw new AuthException("OpenId Connect server does not authenticate");

        UserInfoSuccessResponse successUserResponse = (UserInfoSuccessResponse) userInfoResp;
		_log.debug("User Info Details: " + successUserResponse.getUserInfo().toJSONObject().toJSONString());
		return doAddOrUpdateUser(session, companyId,
				successUserResponse.getUserInfo(),
				oidcTokenResponse.getOIDCTokens().getBearerAccessToken(),
				oidcTokenResponse.getOIDCTokens().getRefreshToken());
	}


	@Override
	public String getLoginRedirect(long companyId, String returnRequestUri, List<String> scopes) throws Exception {
		IAMConfiguration iamConf = getIAMConfiguration(companyId);
		URI authURI = null;
		
		try {
			OIDCProviderMetadata oidcMeta = getMetadata(companyId);
			if (oidcMeta != null) {
				authURI = oidcMeta.getAuthorizationEndpointURI();
			}
		} catch (Exception ex) {
			_log.error("IAM Configuration URL '" + iamConf.configurationURL()
				+ "' is not reachable");
		}
		
		if (authURI == null) {
			if (Validator.isNull(iamConf.oauthAuthURL())) {
				throw new ConfigurationException("IAM Authentication is not properly configured. Authentication cannot proceed");
			}
			authURI = new URI(iamConf.oauthAuthURL());
		}
		AuthenticationRequest aReq = new AuthenticationRequest(
				authURI,
				new ResponseType(ResponseType.Value.CODE),
				Scope.parse(StringUtil.merge(scopes, " ")),
				new ClientID(iamConf.appId()),
				new URI(returnRequestUri),
				new State(),
				new Nonce());
		_log.debug("Generated IAM authentication request at " + aReq.toURI().toString());
		return aReq.toURI().toString();
	}

	@Override
	public boolean isEnabled(long companyId) {
		IAMConfiguration iamConfiguration = getIAMConfiguration(companyId);
		if (Validator.isNull(iamConfiguration.appId()) ||
				Validator.isNull(iamConfiguration.appSecret())) {
			return false;
		}

		return iamConfiguration.enabled();
	}
	
	protected IAMConfiguration getIAMConfiguration(long companyId) {
		try {
			return configurationProvider.getConfiguration(
					IAMConfiguration.class,
					new CompanyServiceSettingsLocator(
							companyId, IAMConstants.SERVICE_NAME));
		}
		catch (ConfigurationException ce) {
			throw new SystemException(ce);
		}
	}

	private OIDCProviderMetadata getMetadata(long companyId) throws Exception{
		IAMConfiguration iamConf = getIAMConfiguration(companyId);
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

	private User doAddOrUpdateUser(HttpSession session, long companyId,
			UserInfo userInfo, BearerAccessToken bearerAccessToken,
			RefreshToken refreshToken) throws Exception {
		
		session.setAttribute(IAMWebKeys.IAM_USER_ID, userInfo.getSubject().getValue());
		long classNameId = ClassNameLocalServiceUtil.getClassNameId(User.class);
		List<ExpandoValue> values = 
				ExpandoValueLocalServiceUtil.getColumnValues(
						companyId, classNameId,
						ExpandoTableConstants.DEFAULT_TABLE_NAME,
						"iamUserID", userInfo.getSubject().getValue(), -1, -1);
		if (values.size() > 1) {
			throw new AuthException("Multiple user with the same IAM identifier");
		}
		if (values.size() == 1) {
			return updateUser(companyId, userLocalService.getUser(values.get(0).getClassPK()),
					userInfo, bearerAccessToken, refreshToken);
		}

		try {
			User user = userLocalService.getUserByEmailAddress(companyId, userInfo.getEmail().toString());
			session.setAttribute(IAMWebKeys.IAM_USER_EMAIL_ADDRESS, userInfo.getEmail().toString());
			return updateUser(companyId, user, userInfo, bearerAccessToken, refreshToken);
		} catch (PortalException pe) {
			_log.debug("No user found. It will be added");
		}
		return addUser(companyId, userInfo, bearerAccessToken, refreshToken);
	}
	
	private User addUser(long companyId, UserInfo userInfo,
			BearerAccessToken bearerAccessToken, RefreshToken refreshToken)
					throws Exception {
		long creatorUserId = 0;
		boolean autoPassword = true;
		String password1 = StringPool.BLANK;
		String password2 = StringPool.BLANK;
		boolean autoScreenName = true;
		String screenName = StringPool.BLANK;
		String emailAddress = userInfo.getEmail().toString();
		String openId = StringPool.BLANK;
		Locale locale = LocaleUtil.getDefault();
		long prefixId = 0;
		long suffixId = 0;
		boolean male = Validator.equals(userInfo.getGender(), "male");
		int birthdayMonth = Calendar.JANUARY;
		int birthdayDay = 1;
		int birthdayYear = 1970;
		String jobTitle = StringPool.BLANK;
		long[] groupIds = null;
		long[] organizationIds = null;
		long[] roleIds = null;
		long[] userGroupIds = null;
		boolean sendEmail = true;

		
		String firstName = userInfo.getGivenName();
		if (Validator.isNull(firstName)) {
			if (Validator.isNotNull(userInfo.getName())) {
				String fullName[] = userInfo.getName().split(" "); 
				switch (fullName.length) {
					case 1:
					case 2:
						firstName = fullName[0];
						break;
					default:
						firstName = fullName[1];
				}
			} else {
				firstName = userInfo.getSubject().getValue().substring(0, 8) + "...";
			}
		}
		String middleName = StringPool.BLANK;
		String lastName = userInfo.getFamilyName();
		if (Validator.isNull(lastName)) {
			if (Validator.isNotNull(userInfo.getName())) {
				String fullName[] = userInfo.getName().split(" "); 
				switch (fullName.length) {
					case 1:
						firstName = fullName[0];
						break;
					case 2:
						firstName = fullName[1];
						break;
					default:
						firstName = fullName[fullName.length - 1];
				}
			} else {
				lastName = "IAM User";
			}
		}

		if (Validator.isNotNull(userInfo.getPreferredUsername())) {
			try {
				userLocalService.getUserByScreenName(companyId, userInfo.getPreferredUsername());
			} catch (PortalException pe) {
				autoScreenName = false;
				screenName = userInfo.getPreferredUsername();				
			}
		}
		

		ServiceContext serviceContext = new ServiceContext();
		_log.debug("New user with mail " + emailAddress + " to be added");
		User user = userLocalService.addUser(
			creatorUserId, companyId, autoPassword, password1, password2,
			autoScreenName, screenName, emailAddress, 0, openId, locale,
			firstName, middleName, lastName, prefixId, suffixId, male,
			birthdayMonth, birthdayDay, birthdayYear, jobTitle, groupIds,
			organizationIds, roleIds, userGroupIds, sendEmail, serviceContext);
		
		user = userLocalService.updatePasswordReset(user.getUserId(), false);

		user = userLocalService.updateEmailAddressVerified(
			user.getUserId(), true);
		
		user = userLocalService.updateAgreedToTermsOfUse(user.getUserId(), true);
		
		user = userLocalService.updateReminderQuery(user.getUserId(), "IAM Authentication", "NOT REQUESTED");
		
		ExpandoValueLocalServiceUtil.addValue(
				companyId, User.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME,
				"iamUserID", user.getUserId(), userInfo.getSubject().getValue());
		ExpandoValueLocalServiceUtil.addValue(
				companyId, User.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME,
				"iamAccessToken", user.getUserId(), bearerAccessToken.getValue());
		if (Validator.isNotNull(refreshToken)) {
			ExpandoValueLocalServiceUtil.addValue(
					companyId, User.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME,
					"iamRefreshToken", user.getUserId(), refreshToken.getValue());
		}
		_log.info("New user with id '" + user.getUserId() + "' added");
		return user;
	}

	private User updateUser(long companyId, User user, UserInfo userInfo,
			BearerAccessToken bearerAccessToken, RefreshToken refreshToken)
					throws Exception {
		
		String emailAddress = userInfo.getEmail().toString();
		String firstName = userInfo.getName();
		String lastName = userInfo.getFamilyName();
		boolean male = Validator.equals(userInfo.getGender(), "male");

		ExpandoValueLocalServiceUtil.addValue(
				companyId, User.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME,
				"iamUserID", user.getUserId(), userInfo.getSubject().getValue());
		ExpandoValueLocalServiceUtil.addValue(
				companyId, User.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME,
				"iamAccessToken", user.getUserId(), bearerAccessToken.getValue());
		if (Validator.isNotNull(refreshToken)) {
			ExpandoValueLocalServiceUtil.addValue(
					companyId, User.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME,
					"iamRefreshToken", user.getUserId(), refreshToken.getValue());
		}
		if (emailAddress.equals(user.getEmailAddress()) &&
			firstName.equals(user.getFirstName()) &&
			lastName.equals(user.getLastName())) {

			return user;
		}

		Contact contact = user.getContact();

		Calendar birthdayCal = CalendarFactoryUtil.getCalendar();

		birthdayCal.setTime(contact.getBirthday());

		int birthdayMonth = birthdayCal.get(Calendar.MONTH);
		int birthdayDay = birthdayCal.get(Calendar.DAY_OF_MONTH);
		int birthdayYear = birthdayCal.get(Calendar.YEAR);

		long[] groupIds = null;
		long[] organizationIds = null;
		long[] roleIds = null;
		List<UserGroupRole> userGroupRoles = null;
		long[] userGroupIds = null;

		ServiceContext serviceContext = new ServiceContext();


		if (!StringUtil.equalsIgnoreCase(
				emailAddress, user.getEmailAddress())) {

			userLocalService.updateEmailAddress(
				user.getUserId(), StringPool.BLANK, emailAddress, emailAddress);
		}

		userLocalService.updateEmailAddressVerified(user.getUserId(), true);

		return userLocalService.updateUser(
			user.getUserId(), StringPool.BLANK, StringPool.BLANK,
			StringPool.BLANK, false, user.getReminderQueryQuestion(),
			user.getReminderQueryAnswer(), user.getScreenName(), emailAddress,
			0, user.getOpenId(), true, null, user.getLanguageId(),
			user.getTimeZoneId(), user.getGreeting(), user.getComments(),
			firstName, user.getMiddleName(), lastName, contact.getPrefixId(),
			contact.getSuffixId(), male, birthdayMonth, birthdayDay,
			birthdayYear, contact.getSmsSn(), contact.getFacebookSn(),
			contact.getJabberSn(), contact.getSkypeSn(), contact.getTwitterSn(),
			contact.getJobTitle(), groupIds, organizationIds, roleIds,
			userGroupRoles, userGroupIds, serviceContext);	}

	@Reference
	private ConfigurationProvider configurationProvider;

	@Reference
	private UserLocalService userLocalService;

	private static final Log _log = LogFactoryUtil.getLog(
			IAMImpl.class);
}
