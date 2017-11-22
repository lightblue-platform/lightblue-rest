/**
 *
 */
/*
 Copyright 2013 Red Hat, Inc. and/or its affiliates.

 This file is part of lightblue.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.redhat.lightblue.rest.auth.jboss;

import com.redhat.lightblue.rest.auth.CachedRolesProvider;
import com.redhat.lightblue.rest.auth.RolesCache;
import com.redhat.lightblue.rest.auth.RolesProvider;
import com.redhat.lightblue.rest.auth.ldap.LdapConfiguration;
import com.redhat.lightblue.rest.auth.ldap.LdapRolesProvider;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.jboss.security.SimpleGroup;
import org.jboss.security.auth.spi.BaseCertLoginModule;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * @author dhaynes
 *
 * Jboss loginModules authenticator that does authorization against LDAP
 * (authentication provided by CertRolesLoginModule 7/11/2014
 *
 */
public class CertLdapLoginModule extends BaseCertLoginModule {
    private final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CertLdapLoginModule.class);
    private static final Logger ACCESS_LOGGER = Logger.getLogger(CertLdapLoginModule.class, "access");


    public static final String AUTH_ROLE_NAME = "authRoleName";
    public static final String SERVER = "ldapServer";
    public static final String PORT = "port";
    public static final String SEARCH_BASE = "searchBase";
    public static final String BIND_DN = "bindDn";
    public static final String BIND_PWD = "bindPassword";
    public static final String USE_SSL = "useSSL";
    public static final String TRUST_STORE = "trustStore";
    public static final String TRUST_STORE_PASSWORD = "trustStorePassword";
    public static final String POOL_SIZE = "poolSize";
    public static final String POOL_MAX_CONNECTION_AGE_MS = "poolMaxConnectionAgeMS";
    public static final String CONNECTION_TIMEOUT_MS = "connectionTimeoutMS";
    public static final String RESPONSE_TIMEOUT_MS = "responseTimeoutMS";
    public static final String DEBUG = "debug";
    public static final String KEEP_ALIVE = "keepAlive";
    public static final String ROLES_CACHE_EXPIRY_MS = "rolesCacheExpiryMS";
    public static final String ENVIRONMENT = "environment";
    public static final String ALL_ACCESS_OU = "allAccessOu";

    private static final String[] ALL_VALID_OPTIONS = {
            AUTH_ROLE_NAME, SERVER, PORT, SEARCH_BASE, BIND_DN, BIND_PWD, USE_SSL,
            TRUST_STORE, TRUST_STORE_PASSWORD, POOL_SIZE, POOL_MAX_CONNECTION_AGE_MS,
            CONNECTION_TIMEOUT_MS,RESPONSE_TIMEOUT_MS,DEBUG,KEEP_ALIVE,
            ROLES_CACHE_EXPIRY_MS, ENVIRONMENT, ALL_ACCESS_OU};

    public static final String UID = "uid";
    public static final String CN = "cn";
    public static final String LOCATION = "l";
    public static final String OU = "ou";

    private static String environment;
    private static String allAccessOu;

    private static volatile RolesProvider rolesProvider = null;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        LOGGER.debug("CertLdapLoginModule#initialize was invoked");
        addValidOptions(ALL_VALID_OPTIONS);
        super.initialize(subject, callbackHandler, sharedState, options);
    }

    public void initializeRolesProvider() throws Exception {
        LOGGER.debug("CertLdapLoginModule#initializeLdap was invoked");
        if (rolesProvider == null) {
            synchronized(LdapRolesProvider.class) {
                if (rolesProvider == null) {
                    environment = (String) options.get(ENVIRONMENT);
                    allAccessOu = (String) options.get(ALL_ACCESS_OU);

                    LdapConfiguration ldapConf = new LdapConfiguration();
                    ldapConf.server((String) options.get(SERVER));
                    ldapConf.port(Integer.parseInt((String) options.get(PORT)));
                    String searchBase = (String) options.get(SEARCH_BASE);
                    ldapConf.bindDn((String) options.get(BIND_DN));
                    ldapConf.bindDNPwd((String) options.get(BIND_PWD));
                    ldapConf.useSSL(Boolean.parseBoolean( (String) options.get(USE_SSL)));
                    ldapConf.trustStore((String) options.get(TRUST_STORE));
                    ldapConf.trustStorePassword((String) options.get(TRUST_STORE_PASSWORD));
                    ldapConf.poolSize(Integer.parseInt((String) options.get(POOL_SIZE)));

                    // optional configurations
                    if (options.containsKey(CONNECTION_TIMEOUT_MS)) {
                        ldapConf.connectionTimeoutMS(Integer.parseInt((String)options.get(CONNECTION_TIMEOUT_MS)));
                    }
                    if (options.containsKey(RESPONSE_TIMEOUT_MS)) {
                        ldapConf.responseTimeoutMS(Integer.parseInt((String)options.get(RESPONSE_TIMEOUT_MS)));
                    }
                    if (options.containsKey(DEBUG)) {
                        ldapConf.debug(Boolean.parseBoolean((String)options.get(DEBUG)));
                    }
                    if (options.containsKey(KEEP_ALIVE)) {
                        ldapConf.keepAlive(Boolean.parseBoolean((String)options.get(KEEP_ALIVE)));
                    }
                    if (options.containsKey(POOL_MAX_CONNECTION_AGE_MS)) {
                        ldapConf.poolMaxConnectionAgeMS(Integer.parseInt((String)options.get(POOL_MAX_CONNECTION_AGE_MS)));
                    }

                    int rolesCacheExpiry = 5*60*1000; // default 5 minutes
                    if (options.containsKey(ROLES_CACHE_EXPIRY_MS)) {
                        rolesCacheExpiry = Integer.parseInt((String)options.get(ROLES_CACHE_EXPIRY_MS));
                    }

                    rolesProvider = new CachedRolesProvider(new LdapRolesProvider(searchBase, ldapConf), new RolesCache(rolesCacheExpiry));
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.security.auth.spi.AbstractServerLoginModule#getRoleSets()
     */
    @Override
    protected Group[] getRoleSets() throws LoginException {
        LOGGER.debug("staticRoleLoginModule getRoleSets()");

        String roleName = (String) options.get(AUTH_ROLE_NAME);

        SimpleGroup userRoles = new SimpleGroup("Roles");

        Principal p = null;

        String certPrincipal = getUsername();

        try {
            initializeRolesProvider();

            LOGGER.debug("Certificate principal:" + certPrincipal);

            //first try getting search name from uid in certificate principle (new certificates)
            String searchName = getLDAPAttribute(certPrincipal, UID);
            if(StringUtils.isNotBlank(searchName)) {
                //only try to validate environment if it is a certificate that contains uid
                validateEnvironment(certPrincipal);
            } else {
                // fallback to getting search name from cn in certificate principle (legacy certificates)
                searchName = getLDAPAttribute(certPrincipal, CN);
            }

            Collection<String> groupNames = rolesProvider.getUserRoles(searchName);

            p = super.createIdentity(roleName);

            userRoles.addMember(p);
            for (String groupName : groupNames) {
                Principal role = super.createIdentity(groupName);
                LOGGER.debug("Found role: " + groupName);
                userRoles.addMember(role);
            }

            if (ACCESS_LOGGER.isDebugEnabled()) {
                ACCESS_LOGGER.debug("Certificate principal: " + certPrincipal + ", roles: " + Arrays.toString(groupNames.toArray()));
            }

            LOGGER.debug("Assign principal [" + p.getName() + "] to role [" + roleName + "]");
        } catch (Exception e) {
            String principalName = p == null ? certPrincipal : p.getName();
            LOGGER.error("Failed to assign principal [" + principalName + "] to role [" + roleName + "]", e);
        }
        Group[] roleSets = {userRoles};
        return roleSets;
    }

    private void validateEnvironment(String certificatePrincipal) throws NamingException {

        String ou = getLDAPAttribute(certificatePrincipal, OU);
        LOGGER.debug("OU from certificate: ", ou);
        String location = getLDAPAttribute(certificatePrincipal, LOCATION);
        LOGGER.debug("Location from certificate: ", location);

        if(StringUtils.isBlank(ou)) {
            throw new NoSuchAttributeException("No ou in dn, you may need to update your certificate: " + certificatePrincipal);
        } else {
            if(allAccessOu.equalsIgnoreCase(StringUtils.replace(ou, " ", ""))){
                LOGGER.debug("Skipping environment validation, user ou matches {} ", allAccessOu);
            } else {
                //if dn not from allAccessOu, verify the location (l) field
                //in the cert matches the configured environment
                if(StringUtils.isBlank(location)) {
                    throw new NoSuchAttributeException("No location in dn, you may need to update your certificate: " + certificatePrincipal);
                } else if(!environment.equalsIgnoreCase(location)){
                    throw new NoSuchAttributeException("Invalid location from dn, expected " + environment + " but found l=" + location);
                }
            }
        }
    }

    private String getLDAPAttribute(String certificatePrincipal, String searchAttribute) throws NamingException {
        String searchName = new String();
        LdapName name = new LdapName(certificatePrincipal);
        for (Rdn rdn : name.getRdns()) {
            if (rdn.getType().equalsIgnoreCase(searchAttribute)) {
                searchName = (String) rdn.getValue();
                break;
            }
        }
        return searchName;
    }
}
