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

package com.liferay.portal.security.sso.iam.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.UserGroup;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.security.sso.iam.IAM;
import com.liferay.portal.security.sso.iam.constants.IAMConfigurationKeys;
import com.liferay.portal.security.sso.iam.exception.NoAccessTokenAvailable;
import com.liferay.portal.security.sso.iam.exception.NoSuchTokenException;
import com.liferay.portal.security.sso.iam.model.TokenInfo;
import com.liferay.portal.security.sso.iam.service.base.TokenServiceBaseImpl;
import com.liferay.portal.security.sso.iam.service.permission.
    TokenAccessPermissionChecker;
import com.liferay.portal.spring.extender.service.ServiceReference;

import aQute.bnd.annotation.ProviderType;

/**
 * The implementation of the token remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are
 * added, rerun ServiceBuilder to copy their definitions into the
 * {@link com.liferay.portal.security.sso.iam.service.TokenService} interface.
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have
 * security checks based on the propagated JAAS credentials because this service
 * can be accessed remotely.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @author Marco Fargetta
 * @see TokenServiceBaseImpl
 * @see com.liferay.portal.security.sso.iam.service.TokenServiceUtil
 */
@ProviderType
public class TokenServiceImpl extends TokenServiceBaseImpl {
    /*
     * NOTE FOR DEVELOPERS:
     *
     * Never reference this class directly. Always use {@link
     * com.liferay.portal.security.sso.iam.service.TokenServiceUtil} to access
     * the token remote service.
     */

    /**
     * Retrieves the token for the user.
     *
     * @param userId The user identifier
     * @param serviceContext The service context of the call
     * @return The token
     * @throws PortalException If there are problem to collect the information
     */
    public final String getToken(
            final long userId, final ServiceContext serviceContext)
            throws PortalException {
        TokenAccessPermissionChecker.check(getPermissionChecker(), userId,
                serviceContext.getScopeGroupId(), "VIEW");
        try {
            return iam.getUserToken(userId);
        } catch (Exception e) {
            throw new NoAccessTokenAvailable("User " + userId
                    + " has not access token");
        }
    }

    /**
     * Retrieves the token for the calling user.
     *
     * @param serviceContext The service context of the call
     * @return The token
     * @throws PortalException If there are problem to collect the information
     */
    public final String getToken(final ServiceContext serviceContext)
            throws PortalException {
        TokenAccessPermissionChecker.check(getPermissionChecker(), getUserId(),
                serviceContext.getScopeGroupId(), "VIEW");
        try {
            return iam.getUserToken(getUserId());
        } catch (Exception e) {
            throw new NoAccessTokenAvailable("User " + getUserId()
                    + " has not access token");
        }
    }

    /**
     * Retrieves the information associated with a token.
     * If the token is not valid an error message is included in the token
     * information and not other values are provided
     *
     * @param token The token to analyse
     * @param serviceContext The service context of the call
     * @return The token information
     * @throws PortalException If there are problem to collect the information
     */
    public final TokenInfo getTokenInfo(
            final String token, final ServiceContext serviceContext)
            throws PortalException {
        TokenAccessPermissionChecker.check(getPermissionChecker(), getUserId(),
                serviceContext.getScopeGroupId(), "TOKEN_INFO");
        TokenInfo ti = new TokenInfo();
        try {
            User user = iam.getTokenUser(serviceContext.getCompanyId(), token);
            if (user != null) {
                ti.setSubject(iam.getTokenSubject(serviceContext
                        .getCompanyId(), user.getUserId()));
                List<String> ugList = new ArrayList<>();

                for (UserGroup ug : user.getUserGroups()) {
                    if (ug.getName().startsWith(
                            IAMConfigurationKeys.GROUP_PREFIX)) {
                        ugList.add(ug.getName().substring(
                                IAMConfigurationKeys.GROUP_PREFIX.length()));
                    }
                }
                ti.setGroups(ugList);
                return ti;
            }
            ti.setError("No user associated with the token");
            return ti;
        } catch (Exception e) {
            throw new NoSuchTokenException("Token '" + token
                    + "' is not valid");
        }
    }

    /**
     * Reference to the OSGi IAM service.
     */
    @ServiceReference(type = com.liferay.portal.security.sso.iam.IAM.class)
    private IAM iam;
}
