---
description: This section describe how to deploy and configure the plugins to allow the authentication using IAM.
---

# Administration Guide


To deploy the LiferayIAM modules and make available INDIGO-AAI in Liferay the portal administrator has to upload into the portal
the five jar modules included in the release. The module upload page is available in `Liferay > Control Panel > Apps > App Manager`.

After the modules are deployed the new IAM authentication configuration should be available in the control panel going
to `Liferay > Control Panel > Configuration > Instance Settings`, under the section *authentication*. The following picture
shows the configuration made with the IAM-Test instance:

![IAM configuration](img/iamConf.png)

To work with the test configuration it is enough to provide the application identifiers (id and secret) and
the URL of the well known OpenId Connect configuration. For the IAM test the URL is:
[https://iam-test.indigo-datacloud.eu/.well-known/openid-configuration](https://iam-test.indigo-datacloud.eu/.well-known/openid-configuration)

Administrator can require some extra scopes for the token. The default configuration requires only the scopes for the user
identification as requested by Liferay (name, mail and other attributes) and the refresh token.

Checking the enable check will make the authentication method available to the user.

Some application could require to access to reserved remote methods provided by the modules (to retrieve user token and/or
validate them). The access to these methods is possible only from authorised accounts.
The best way to provide the authorisation is to create a special role and assign this rule to the user or a group of users
associated with the applications requiring the permissions. The role has to enable the access
to this method. This can be configured going to the page `Liferay > Control Panel > Users > Roles`. In the page the administrator
can create a new role and then open the `Define Permissions` dialog which will show the authorisation for the iam related method under the *Users and Organisations* group, as shown in the following picture:

![IAM permissions](img/permissions.png)

Enabling the permissions for the `model.resource.com.liferay.portal.security.sso.iam`, among the *Resource Permissions*,
everyone with this role can access the remote methods.
