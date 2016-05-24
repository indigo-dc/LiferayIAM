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

package com.liferay.portal.security.sso.iam.internal.verify;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.settings.SettingsFactory;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.security.sso.iam.constants.IAMConfigurationKeys;
import com.liferay.portal.security.sso.iam.constants.IAMConstants;
import com.liferay.portal.verify.BaseCompanySettingsVerifyProcess;
import com.liferay.portal.verify.VerifyProcess;

/**
 * @author Marco Fargetta
 */
@Component(
	immediate = true,
	property = {"verify.process.name=com.liferay.portal.security.sso.iam"},
	service = VerifyProcess.class
)
public class IAMCompanySettingsVerifyProcess
	extends BaseCompanySettingsVerifyProcess {

	@Override
	protected CompanyLocalService getCompanyLocalService() {
		return _companyLocalService;
	}

	@Override
	protected Set<String> getLegacyPropertyKeys() {
		return new HashSet<String>();
	}

	@Override
	protected Dictionary<String, String> getPropertyValues(long companyId) {
		Dictionary<String, String> dictionary = new HashMapDictionary<>();

		dictionary.put(
			IAMConfigurationKeys.AUTH_ENABLED, StringPool.FALSE);
		dictionary.put(
			IAMConfigurationKeys.APP_ID, StringPool.BLANK);
		dictionary.put(
			IAMConfigurationKeys.APP_SECRET, StringPool.BLANK);
		dictionary.put(
			IAMConfigurationKeys.CONFIGURATION_URL, StringPool.BLANK);
		dictionary.put(
			IAMConfigurationKeys.OAUTH_AUTH_URL, StringPool.BLANK);
		dictionary.put(
			IAMConfigurationKeys.OAUTH_TOKEN_URL, StringPool.BLANK);
		dictionary.put(
			IAMConfigurationKeys.OPENID_USERINFO_URL, StringPool.BLANK);
		dictionary.put(
			IAMConfigurationKeys.OPENID_JWK_URL, StringPool.BLANK);
		dictionary.put(
			IAMConfigurationKeys.OPENID_ISSUER, StringPool.BLANK);
		dictionary.put(
			IAMConfigurationKeys.VERIFIED_ACCOUNT_REQUIRED, StringPool.FALSE);
		return dictionary;
	}

	@Override
	protected SettingsFactory getSettingsFactory() {
		return _settingsFactory;
	}

	@Override
	protected String getSettingsId() {
		return IAMConstants.SERVICE_NAME;
	}

	@Reference(unbind = "-")
	protected void setCompanyLocalService(
		CompanyLocalService companyLocalService) {
		_companyLocalService = companyLocalService;
	}

	@Reference(unbind = "-")
	protected void setSettingsFactory(SettingsFactory settingsFactory) {
		_settingsFactory = settingsFactory;
	}
	private CompanyLocalService _companyLocalService;
	private SettingsFactory _settingsFactory;
}
