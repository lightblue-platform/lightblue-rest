# How to configure authz on JBoss

In standalone.xml:

```
<subsystem xmlns="urn:jboss:domain:security:1.2">
 <security-domain name="lightblue-cert">
	<authentication>
		<login-module name="CertLdapLoginModule" code="com.redhat.lightblue.rest.auth.jboss.CertLdapLoginModule" flag="required">
			<module-option name="password-stacking" value="useFirstPass"/>
			<module-option name="securityDomain" value="lightblue-cert"/>
			<module-option name="verifier" value="org.jboss.security.auth.certs.AnyCertVerifier"/>
			<module-option name="authRoleName" value="authenticated"/>
			<module-option name="ldapServer" value="<ldap hostname>"/>
			<module-option name="port" value="636"/>
			<module-option name="searchBase" value="ou=lightblue,dc=redhat,dc=com"/>
			<module-option name="bindDn" value="uid=lightblueapp,ou=serviceusers,ou=lightblue,dc=redhat,dc=com"/>
			<module-option name="bindPassword" value="<password>"/>
			<module-option name="useSSL" value="true"/>
			<module-option name="poolSize" value="5"/>
			<module-option name="trustStore" value="${jboss.server.config.dir}/eap6trust.keystore"/>
			<module-option name="trustStorePassword" value="<password>"/>
		</login-module>
	</authentication>
	<jsse keystore-password="<password>" keystore-url="file://${jboss.server.config.dir}/eap6.keystore" truststore-password="<password>" truststore-url="file://${jboss.server.config.dir}/eap6trust.keystore" client-auth="true"/>
  </security-domain>
</subsystem>
```
