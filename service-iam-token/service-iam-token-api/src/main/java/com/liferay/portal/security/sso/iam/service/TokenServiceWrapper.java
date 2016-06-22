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

import com.liferay.portal.kernel.service.ServiceWrapper;

/**
 * Provides a wrapper for {@link TokenService}.
 *
 * @author Brian Wing Shun Chan
 * @see TokenService
 * @generated
 */
@ProviderType
public class TokenServiceWrapper implements TokenService,
	ServiceWrapper<TokenService> {
	public TokenServiceWrapper(TokenService tokenService) {
		_tokenService = tokenService;
	}

	@Override
	public com.liferay.portal.security.sso.iam.model.TokenInfo getTokenInfo(
		java.lang.String token,
		com.liferay.portal.kernel.service.ServiceContext serviceContext)
		throws com.liferay.portal.kernel.exception.PortalException {
		return _tokenService.getTokenInfo(token, serviceContext);
	}

	/**
	* Returns the OSGi service identifier.
	*
	* @return the OSGi service identifier
	*/
	@Override
	public java.lang.String getOSGiServiceIdentifier() {
		return _tokenService.getOSGiServiceIdentifier();
	}

	@Override
	public java.lang.String getToken(
		com.liferay.portal.kernel.service.ServiceContext serviceContext)
		throws com.liferay.portal.kernel.exception.PortalException {
		return _tokenService.getToken(serviceContext);
	}

	@Override
	public java.lang.String getToken(long userId,
		com.liferay.portal.kernel.service.ServiceContext serviceContext)
		throws com.liferay.portal.kernel.exception.PortalException {
		return _tokenService.getToken(userId, serviceContext);
	}

	@Override
	public TokenService getWrappedService() {
		return _tokenService;
	}

	@Override
	public void setWrappedService(TokenService tokenService) {
		_tokenService = tokenService;
	}

	private TokenService _tokenService;
}