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

package com.liferay.portal.security.sso.iam.service.permission;

import org.osgi.service.component.annotations.Component;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.BaseResourcePermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.ResourcePermissionChecker;

/**
 * @author Marco Fargetta
 */
@Component(
	immediate = true,
	property = {"resource.name=" + TokenAccessPermissionChecker.RESOURCE_NAME},
	service = ResourcePermissionChecker.class
)
public class TokenAccessPermissionChecker extends BaseResourcePermissionChecker {

	
	public static void check(
			PermissionChecker permissionChecker, User user, long groupId, String actionId)
					throws PortalException {
		check(permissionChecker, user.getUserId(), groupId, actionId);
	}



	public static void check(
			PermissionChecker permissionChecker, long userId, long groupId, String actionId)
					throws PortalException {
		if (!hasPermission(permissionChecker, userId, groupId, actionId)) {
			throw new PrincipalException.MustHavePermission(userId, actionId);
		}
	}
	
	public static boolean hasPermission(PermissionChecker permissionChecker, long groupId, String actionId) {
		return hasPermission(permissionChecker, -1, groupId, actionId);
	}

	public static boolean hasPermission(PermissionChecker permissionChecker, long primaryKey, long groupId, String actionId) {
		if (permissionChecker.isCompanyAdmin() || permissionChecker.isOmniadmin()) {
			return true;
		}
		if (permissionChecker.getUserId() == primaryKey){
			return true;
		}
		return contains(permissionChecker, RESOURCE_NAME, groupId, actionId);		
	}
	
	@Override
	public Boolean checkResource(PermissionChecker permissionChecker, long classPK, String actionId) {
		return hasPermission(permissionChecker, classPK, actionId);
	}

	public static final String RESOURCE_NAME = "com.liferay.portal.security.sso.iam";
}
