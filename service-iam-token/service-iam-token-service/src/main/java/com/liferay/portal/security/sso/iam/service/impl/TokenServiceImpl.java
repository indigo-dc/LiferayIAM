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

package com.liferay.portal.security.sso.iam.service.impl;

import org.osgi.service.component.annotations.Reference;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.security.sso.iam.IAM;
import com.liferay.portal.security.sso.iam.service.base.TokenServiceBaseImpl;
import com.liferay.portal.security.sso.iam.service.permission.TokenAccessPermissionChecker;

import aQute.bnd.annotation.ProviderType;

/**
 * The implementation of the token remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.liferay.portal.security.sso.iam.service.TokenService} interface.
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see TokenServiceBaseImpl
 * @see com.liferay.portal.security.sso.iam.service.TokenServiceUtil
 */
@ProviderType
public class TokenServiceImpl extends TokenServiceBaseImpl {
	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never reference this class directly. Always use {@link com.liferay.portal.security.sso.iam.service.TokenServiceUtil} to access the token remote service.
	 */
	public String getToken(long userId, ServiceContext serviceContext) throws PortalException {
		TokenAccessPermissionChecker.check(getPermissionChecker(), userId, serviceContext.getScopeGroupId(), "VIEW");
	    return "Accesso Token is: Ciccino";
	}
	
	public String getToken() throws PortalException {
	    return "Accesso Token is: Ciccino";
	}

	public String getTokenInfo(String token, ServiceContext serviceContext)  throws PortalException {
		return null;
	}

	@Reference(unbind = "-")
	protected void setIam(IAM iam) {
		this.iam = iam;
	}

	private IAM iam;
}