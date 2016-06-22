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

package com.liferay.login.authentication.iam.web.internal.servlet.taglib;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.taglib.BaseDynamicInclude;
import com.liferay.portal.kernel.servlet.taglib.DynamicInclude;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.security.sso.iam.IAM;

/**
 * @author Marco Fargetta
 */
@Component(immediate = true, service = DynamicInclude.class)
public class IAMNavigationPreDynamicInclude extends BaseDynamicInclude {

	@Override
	public void include(HttpServletRequest request, HttpServletResponse response, String key)
		throws IOException {

		ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(
				WebKeys.THEME_DISPLAY);

			if (!iam.isEnabled(themeDisplay.getCompanyId())) {
				return;
			}

			RequestDispatcher requestDispatcher =
				servletContext.getRequestDispatcher(_JSP_PATH);

			try {
				requestDispatcher.include(request, response);
			}
			catch (ServletException se) {
				_log.error("Unable to include JSP " + _JSP_PATH, se);

				throw new IOException("Unable to include JSP " + _JSP_PATH, se);
			}		
	}

	@Override
	public void register(DynamicIncludeRegistry dynamicIncludeRegistry) {

		dynamicIncludeRegistry.register(
			"com.liferay.login.web#/navigation.jsp#pre");
	}

	@Reference(unbind = "-")
	protected void setIam(IAM iam) {
		this.iam = iam;
	}

	@Reference(
		target = "(osgi.web.symbolicname=com.liferay.login.authentication.iam.web)",
		unbind = "-"
	)
	protected void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}
	
	private static final String _JSP_PATH =
			"/html/portlet/login/navigation/iam.jsp";
	private static final Log _log = LogFactoryUtil.getLog(
			IAMNavigationPreDynamicInclude.class);

	private ServletContext servletContext;
	private IAM iam;
}
