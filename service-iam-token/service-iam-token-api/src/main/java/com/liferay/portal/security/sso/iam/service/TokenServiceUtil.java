/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.security.sso.iam.service;

import aQute.bnd.annotation.ProviderType;

import com.liferay.osgi.util.ServiceTrackerFactory;

import org.osgi.util.tracker.ServiceTracker;

/**
 * Provides the remote service utility for Token. This utility wraps
 * {@link com.liferay.portal.security.sso.iam.service.impl.TokenServiceImpl} and is the
 * primary access point for service operations in application layer code running
 * on a remote server. Methods of this service are expected to have security
 * checks based on the propagated JAAS credentials because this service can be
 * accessed remotely.
 *
 * @author Brian Wing Shun Chan
 * @see TokenService
 * @see com.liferay.portal.security.sso.iam.service.base.TokenServiceBaseImpl
 * @see com.liferay.portal.security.sso.iam.service.impl.TokenServiceImpl
 * @generated
 */
@ProviderType
public class TokenServiceUtil {
	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify this class directly. Add custom service methods to {@link com.liferay.portal.security.sso.iam.service.impl.TokenServiceImpl} and rerun ServiceBuilder to regenerate this class.
	 */

	/**
	* Returns the OSGi service identifier.
	*
	* @return the OSGi service identifier
	*/
	public static java.lang.String getOSGiServiceIdentifier() {
		return getService().getOSGiServiceIdentifier();
	}

	public static java.lang.String getToken()
		throws com.liferay.portal.kernel.exception.PortalException {
		return getService().getToken();
	}

	public static java.lang.String getToken(long userId,
		com.liferay.portal.kernel.service.ServiceContext serviceContext)
		throws com.liferay.portal.kernel.exception.PortalException {
		return getService().getToken(userId, serviceContext);
	}

	public static java.lang.String getTokenInfo(java.lang.String token,
		com.liferay.portal.kernel.service.ServiceContext serviceContext)
		throws com.liferay.portal.kernel.exception.PortalException {
		return getService().getTokenInfo(token, serviceContext);
	}

	public static TokenService getService() {
		return _serviceTracker.getService();
	}

	private static ServiceTracker<TokenService, TokenService> _serviceTracker = ServiceTrackerFactory.open(TokenService.class);
}