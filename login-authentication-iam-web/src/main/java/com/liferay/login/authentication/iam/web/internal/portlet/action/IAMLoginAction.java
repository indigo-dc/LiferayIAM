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

package com.liferay.login.authentication.iam.web.internal.portlet.action;

import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.struts.BaseStrutsAction;
import com.liferay.portal.kernel.struts.StrutsAction;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PortletKeys;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.security.sso.iam.IAM;

import java.util.Arrays;
import java.util.List;

import javax.portlet.PortletMode;
import javax.portlet.PortletRequest;
import javax.portlet.PortletURL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Marco Fargetta
 */
@Component(
	immediate = true,
	property = {
		"path=/portal/iam_openidconnect"
	},
	service = StrutsAction.class
)
public class IAMLoginAction extends BaseStrutsAction {

	@Override
	public String execute(
			HttpServletRequest request, HttpServletResponse response)
					throws Exception {
		ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(
				WebKeys.THEME_DISPLAY);

		if (!iam.isEnabled(themeDisplay.getCompanyId())) {
			throw new PrincipalException.MustBeEnabled(
					themeDisplay.getCompanyId(),
					IAM.class.getName());
		}
		String cmd = ParamUtil.getString(request, Constants.CMD);
		if (cmd.equals("login")) {
			String returnRequestUri = getReturnRequestUri(request);

			String loginRedirect = iam.getLoginRedirect(
				themeDisplay.getCompanyId(), returnRequestUri, _scopesLogin);

			response.sendRedirect(loginRedirect);
		}
		else if (cmd.equals("token")) {
			HttpSession session = request.getSession();

			String authorizationCode = ParamUtil.getString(request, "code");

			if (Validator.isNotNull(authorizationCode)) {
				String returnRequestUri = getReturnRequestUri(request);

				User user = iam.addOrUpdateUser(
					session, themeDisplay.getCompanyId(), authorizationCode,
					returnRequestUri, _scopesLogin);

				if ((user != null) &&
					(user.getStatus() == WorkflowConstants.STATUS_INCOMPLETE)) {

					sendUpdateAccountRedirect(request, response, user);

					return null;
				}

				sendLoginRedirect(request, response);

				return null;
			}

			String error = ParamUtil.getString(request, "error");

			if (error.equals("access_denied")) {
				sendLoginRedirect(request, response);

				return null;
			}
		}
		return super.execute(request, response);
	}

	protected void sendLoginRedirect(
			HttpServletRequest request, HttpServletResponse response)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(
			WebKeys.THEME_DISPLAY);

		PortletURL portletURL = PortletURLFactoryUtil.create(
			request, PortletKeys.LOGIN, themeDisplay.getPlid(),
			PortletRequest.RENDER_PHASE);

		portletURL.setParameter(
			"mvcRenderCommandName", "/login/login_redirect");
		portletURL.setWindowState(LiferayWindowState.POP_UP);

		response.sendRedirect(portletURL.toString());
	}

	protected void sendUpdateAccountRedirect(
			HttpServletRequest request, HttpServletResponse response, User user)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(
			WebKeys.THEME_DISPLAY);

		PortletURL portletURL = PortletURLFactoryUtil.create(
			request, PortletKeys.LOGIN, themeDisplay.getPlid(),
			PortletRequest.RENDER_PHASE);

		portletURL.setParameter("saveLastPath", Boolean.FALSE.toString());
		portletURL.setParameter(
			"mvcRenderCommandName", "/login/associate_iam_user");

		PortletURL redirectURL = PortletURLFactoryUtil.create(
			request, PortletKeys.LOGIN, themeDisplay.getPlid(),
			PortletRequest.RENDER_PHASE);

		redirectURL.setParameter(
			"mvcRenderCommandName", "/login/login_redirect");
		redirectURL.setParameter("emailAddress", user.getEmailAddress());
		redirectURL.setParameter("anonymousUser", Boolean.FALSE.toString());
		redirectURL.setPortletMode(PortletMode.VIEW);
		redirectURL.setWindowState(LiferayWindowState.POP_UP);

		portletURL.setParameter("redirect", redirectURL.toString());

		portletURL.setParameter("userId", String.valueOf(user.getUserId()));
		portletURL.setParameter("emailAddress", user.getEmailAddress());
		portletURL.setParameter("firstName", user.getFirstName());
		portletURL.setParameter("lastName", user.getLastName());
		portletURL.setPortletMode(PortletMode.VIEW);
		portletURL.setWindowState(LiferayWindowState.POP_UP);

		response.sendRedirect(portletURL.toString());
	}

	protected String getReturnRequestUri(HttpServletRequest request) {
		return PortalUtil.getPortalURL(request) + PortalUtil.getPathMain() +
			_REDIRECT_URI;
	}

	@Reference(unbind = "-")
	protected void setIam(IAM iam) {
		this.iam = iam;
	}

	private static final String _REDIRECT_URI =
			"/portal/iam_openidconnect?cmd=token";

	private static final List<String> _scopesLogin = Arrays.asList(
		"email", "profile");

	private IAM iam;
}