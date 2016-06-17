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
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.ArrayUtils;
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
import com.liferay.portal.kernel.model.UserGroup;
import com.liferay.portal.kernel.model.UserGroupRole;
import com.liferay.portal.kernel.module.configuration.ConfigurationException;
import com.liferay.portal.kernel.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.security.auth.AuthException;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserGroupLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.settings.CompanyServiceSettingsLocator;
import com.liferay.portal.kernel.util.CalendarFactoryUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.security.sso.iam.IAM;
import com.liferay.portal.security.sso.iam.configuration.IAMConfiguration;
import com.liferay.portal.security.sso.iam.constants.IAMConfigurationKeys;
import com.liferay.portal.security.sso.iam.constants.IAMConstants;
import com.liferay.portal.security.sso.iam.constants.IAMWebKeys;
import com.liferay.portal.security.sso.iam.internal.util.IAMEndPoints;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
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

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

/**
 * @author Marco Fargetta
 */
@Component(
		configurationPid = "com.liferay.portal.security.sso.iam.configuration.IAMConfiguration",
		immediate = true,
		service = IAM.class
)
public class IAMImpl implements IAM {

	public IAMImpl() {
		_log.error("Invoked the IAM service");
	}
	@Override
	public User addOrUpdateUser(HttpSession session, long companyId, String authorizationCode, String returnRequestUri,
			List<String> scopes) throws Exception {
		_log.debug("Add or update a new user");
		IAMEndPoints iamEP = new IAMEndPoints(getIAMConfiguration(companyId));
		AuthorizationCode authCode = new AuthorizationCode(authorizationCode);

		TokenRequest tokenReq = new TokenRequest(
				iamEP.getTokenURI(),
				new ClientSecretBasic(iamEP.getClientID(), iamEP.getSecret()),
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
        	IDTokenClaimsSet idtcs = iamEP.getValidator().validate(oidcTokenResponse.getOIDCTokens().getIDToken(), null);
        	_log.debug("IDToken claims" + idtcs.toJSONObject().toJSONString());
        } catch(BadJOSEException bjse) {
        	_log.error("Invalid token detected: " + bjse.getMessage());
        	throw bjse;
        }
        UserInfoRequest userInfoReq = new UserInfoRequest(
        		iamEP.getUserInfo(), oidcTokenResponse.getOIDCTokens().getBearerAccessToken());
  
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
	public String getLoginRedirect(long companyId, String returnRequestUri, List<String> scopes, boolean isRefreshTokenRequested) throws Exception {
		IAMConfiguration iamConf = getIAMConfiguration(companyId);
		IAMEndPoints iamEP = new IAMEndPoints(iamConf);
		String fullScopes = StringUtil.merge(scopes, " ") + " " + iamConf.oauthExtraScopes();
		if (isRefreshTokenRequested) {
			fullScopes = fullScopes.concat(" offline_access");
		}
		AuthenticationRequest aReq = new AuthenticationRequest(
				iamEP.getAuthURI(),
				new ResponseType(ResponseType.Value.CODE),
				Scope.parse(fullScopes),
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
	
	@Override
	public String getUserToken(long userId) throws Exception {
		User user = userLocalService.getUser(userId);
		String token;
		ExpandoValue accessToken = ExpandoValueLocalServiceUtil.getValue(
				user.getCompanyId(), User.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME,
				"iamAccessToken", user.getUserId());
		token = accessToken.getData();
		if (!isValidToken(user.getCompanyId(), token)) {
			token = updateUserToken(user);
		}
		return token;
	}

	@Override
	public boolean hasRefreshToken(User user) {
		ExpandoValue refreshToken = ExpandoValueLocalServiceUtil.getValue(
				user.getCompanyId(), User.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME,
				"iamRefreshToken", user.getUserId());
		_log.debug("User has the refresh token: " + refreshToken);
		return (Validator.isNotNull(refreshToken) && Validator.isNotNull(refreshToken.getData()));
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
		long[] userGroupIds = getUserGroupIds(companyId, userInfo.getClaim("groups"));
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

	private long[] getUserGroupIds(long companyId, Object listGroups) {
		if (Validator.isNull(listGroups) ||
				!(listGroups instanceof JSONArray)) {
			return null;
		}
		JSONArray groups = (JSONArray) listGroups;
		List<Long> groupIds = new LinkedList<>();
		for (int i=0; i< groups.size(); i++) {
			JSONObject gr = (JSONObject) groups.get(i);
			String groupName = IAMConfigurationKeys.GROUP_PREFIX + gr.get("name").toString();
			
			UserGroup lifeUG = null;
			ServiceContext serviceContext = new ServiceContext();
			try {
				lifeUG = userGroupLocalService.getUserGroup(companyId, groupName);
			} catch (PortalException pe) {
				try {
					lifeUG = userGroupLocalService.addUserGroup(
							userLocalService.getDefaultUserId(companyId),
							companyId, groupName, "IAM Provided group",
							serviceContext);
					_log.info("Created the group: " + groupName);
				} catch (Exception pe2) {
					_log.error("Impossible update group: " + groupName);
					continue;
				}
			}
			groupIds.add(lifeUG.getUserGroupId());
		}
		if (groupIds.isEmpty()) {
			return null;
		}
		long realGroups[] = ArrayUtils.toPrimitive(groupIds.toArray(new Long[groupIds.size()])); 
		return realGroups;
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
		long[] userGroupIds = getUserGroupIds(companyId, userInfo.getClaim("groups"));
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
			userGroupRoles, userGroupIds, serviceContext);
	}

	private String updateUserToken(User user) {
		ExpandoValue refreshTokenEV = ExpandoValueLocalServiceUtil.getValue(
				user.getCompanyId(), User.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME,
				"iamRefreshToken", user.getUserId());
		RefreshToken refreshToken = new RefreshToken(refreshTokenEV.getData());
		AuthorizationGrant refreshTokenGrant = new RefreshTokenGrant(refreshToken);
		IAMEndPoints iamEP;
		try {
			iamEP = new IAMEndPoints(getIAMConfiguration(user.getCompanyId()));
		} catch (ConfigurationException e) {
			_log.error(e);
			return null;
		}
		ClientAuthentication clientAuth = new ClientSecretBasic(iamEP.getClientID(), iamEP.getSecret());
		TokenRequest request = new TokenRequest(iamEP.getTokenURI(), clientAuth, refreshTokenGrant);
		TokenResponse response;
		try {
			response = TokenResponse.parse(request.toHTTPRequest().send());
		} catch (ParseException | IOException e) {
			_log.error("Impossible to parse the token endpoint response to update the token");
			return null;
		}
		if (! response.indicatesSuccess()) {
		    TokenErrorResponse errorResponse = (TokenErrorResponse) response;
		    _log.error("Refresh token error: \n" + errorResponse.toJSONObject().toString());
		    return null;
		}
		AccessTokenResponse successResponse = (AccessTokenResponse) response;
		AccessToken accessToken = successResponse.getTokens().getAccessToken();
		try {
			ExpandoValueLocalServiceUtil.addValue(
					user.getCompanyId(), User.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME,
					"iamAccessToken", user.getUserId(), accessToken.getValue());
		} catch (PortalException e) {
			_log.error("Impossible to store the updated access token for the user '" + user.getScreenName() + "'");
		}
		
		return accessToken.getValue();
	}


	private boolean isValidToken(long companyId, String token) {
		IAMEndPoints iamEP = null;
		ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<SecurityContext>();
		JWKSource<SecurityContext> keySource;
		try {
			iamEP = new IAMEndPoints(getIAMConfiguration(companyId));
			keySource = new RemoteJWKSet<SecurityContext>(iamEP.getJwkURI().toURL());
		} catch(MalformedURLException | ConfigurationException mue) {
			_log.error("Impossible to access the jwk key");
			return false;
		}
		jwtProcessor.setJWSKeySelector(new JWSVerificationKeySelector<SecurityContext>(JWSAlgorithm.RS256, keySource));
		JWTClaimsSet claimsSet;
		try {
			claimsSet = jwtProcessor.process(token, null);
		} catch (java.text.ParseException | BadJOSEException | JOSEException e) {
			_log.error("The following token cannot be parsed: " + token);
			return false;
		}
		return claimsSet.getExpirationTime().after(new Date(System.currentTimeMillis() + (3 * 60 * 1000)));
	}

	@Reference
	private ConfigurationProvider configurationProvider;

	@Reference
	private UserLocalService userLocalService;

	@Reference
	private UserGroupLocalService userGroupLocalService;

	private static final Log _log = LogFactoryUtil.getLog(
			IAMImpl.class);
}
