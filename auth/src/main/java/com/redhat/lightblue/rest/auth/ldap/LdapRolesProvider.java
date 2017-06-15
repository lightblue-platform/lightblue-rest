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
package com.redhat.lightblue.rest.auth.ldap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.lightblue.rest.auth.RolesProvider;
import com.redhat.lightblue.rest.auth.health.LdapAuthHealth;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.util.DebugType;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustStoreTrustManager;



/**
 * Fetches user roles from ldap. Pools ldap connections. See {@link LdapConfiguration} for
 * confiugration options.
 *
 * Ldap connection pool is created per @{link {@link LdapRolesProvider} instance. Most likely
 * you'll want to use this class as a singleton.
 *
 *
 * @author mpatercz
 *
 */
public class LdapRolesProvider implements RolesProvider {
    private final Logger LOGGER = LoggerFactory.getLogger(LdapRolesProvider.class);

    private String searchBase;

    private LdapConfiguration ldapConfiguration;

    // Connection pool needs to be a singleton
    private LDAPConnectionPool connectionPool;

    public LdapRolesProvider(String searchBase, LdapConfiguration ldapConfiguration) throws Exception {
        LOGGER.debug("Creating LightblueLdapRoleProvider");

        Objects.requireNonNull(searchBase);
        Objects.requireNonNull(ldapConfiguration);

        this.searchBase = searchBase;
        this.ldapConfiguration = ldapConfiguration;

        initialize();
    }

    private void initialize() throws Exception {

        if (ldapConfiguration.isDebug()) {
            // bridge java.util.Logger output to log4j
            System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

            // setting the ldap debug level...
            System.setProperty("com.unboundid.ldap.sdk.debug.enabled", "true");
            System.setProperty("com.unboundid.ldap.sdk.debug.level", "FINEST");
            System.setProperty("com.unboundid.ldap.sdk.debug.type", DebugType.getTypeNameList());
        }

        LDAPConnection ldapConnection;

        LDAPConnectionOptions options = new LDAPConnectionOptions();

        // A value which specifies the maximum length of time in milliseconds that an attempt to establish a connection should be allowed to block before failing. By default, a timeout of 60,000 milliseconds (1 minute) will be used.
        options.setConnectTimeoutMillis(ldapConfiguration.getConnectionTimeoutMS());
        // A value which specifies the default timeout in milliseconds that the SDK should wait for a response from the server before failing. By default, a timeout of 300,000 milliseconds (5 minutes) will be used.
        options.setResponseTimeoutMillis(ldapConfiguration.getResponseTimeoutMS());
        // A flag that indicates whether to use the SO_KEEPALIVE socket option to attempt to more quickly detect when idle TCP connections have been lost or to prevent them from being unexpectedly closed by intermediate network hardware. By default, the SO_KEEPALIVE socket option will be used.
        options.setUseKeepAlive(ldapConfiguration.isKeepAlive());


        if(ldapConfiguration.getUseSSL()) {
            TrustStoreTrustManager trustStoreTrustManager = new TrustStoreTrustManager(
                    ldapConfiguration.getTrustStore(),
                    ldapConfiguration.getTrustStorePassword().toCharArray(),
                    "JKS",
                    true);
            SSLSocketFactory socketFactory = new SSLUtil(trustStoreTrustManager).createSSLSocketFactory();

            ldapConnection = new LDAPConnection(
                    socketFactory,
                    options,
                    ldapConfiguration.getServer(),
                    ldapConfiguration.getPort(),
                    ldapConfiguration.getBindDn(),
                    ldapConfiguration.getBindDNPwd()
            );
        } else {
            LOGGER.warn("Not using SSL to connect to ldap. This is very insecure - do not use in prod environments!");

            ldapConnection = new LDAPConnection(
                    options,
                    ldapConfiguration.getServer(),
                    ldapConfiguration.getPort(),
                    ldapConfiguration.getBindDn(),
                    ldapConfiguration.getBindDNPwd()
            );
        }

        BindRequest bindRequest = new SimpleBindRequest(ldapConfiguration.getBindDn(), ldapConfiguration.getBindDNPwd());
        BindResult bindResult = ldapConnection.bind(bindRequest);

        if (bindResult.getResultCode() != ResultCode.SUCCESS) {
            LOGGER.error("Error binding to LDAP" + bindResult.getResultCode());
            throw new LDAPException(bindResult.getResultCode(), "Error binding to LDAP");
        }

        connectionPool = new LDAPConnectionPool(ldapConnection, ldapConfiguration.getPoolSize());
        connectionPool.setMaxConnectionAgeMillis(ldapConfiguration.getPoolMaxConnectionAgeMS());
        LOGGER.info("Initialized LDAPConnectionPool: poolSize={}, poolMaxAge={}, connectionTimeout={}, responseTimeout={}, debug={}, keepAlive={}.",
                ldapConfiguration.getPoolSize(), ldapConfiguration.getPoolMaxConnectionAgeMS(), ldapConfiguration.getConnectionTimeoutMS(), ldapConfiguration.getResponseTimeoutMS(),
                ldapConfiguration.isDebug(), ldapConfiguration.isKeepAlive());

    }

    @Override
    public Set<String> getUserRoles(String username) throws Exception {
        LOGGER.debug("getRoles("+username+")");

        Objects.requireNonNull(username);

        Set<String> roles = new HashSet<>();

        String filter = "(uid=" + username + ")";

        SearchRequest searchRequest = new SearchRequest(searchBase, SearchScope.SUB, filter);
        SearchResult searchResult = connectionPool.search(searchRequest);
        List<SearchResultEntry> searchResultEntries = searchResult.getSearchEntries();

        if(searchResultEntries.isEmpty()) {
            LOGGER.warn("No result found roles for user: " + username);
            return new HashSet<String>();
        } else if (searchResultEntries.size() > 1) {
            LOGGER.error("Multiples users found and only one was expected for user: " + username);
            return new HashSet<String>();
        } else {
            for(SearchResultEntry searchResultEntry : searchResultEntries) {
                String[] groups = searchResultEntry.getAttributeValues("memberOf");
                if(null != groups) {
                    for(String group : groups) {
                        for (RDN rdn : new DN(group).getRDNs()) {
                            if (rdn.hasAttribute("cn")) {
                                roles.addAll(Arrays.asList(rdn.getAttributeValues()));
                                break;
                            }
                        }
                    }
                }
            }
        }

        return roles;
    }

    @Override
    public LdapAuthHealth checkHealth() {

        boolean isHealthy = true;
        String details = null;
        try {
            LDAPConnection ldapConnection = connectionPool.getConnection();

            if (!ldapConnection.isConnected()) {
                isHealthy = false;
            }

        } catch (LDAPException e) {
            isHealthy = false;
        }

        details = new StringBuilder("LDAPConnection [DN: ").append(ldapConfiguration.getBindDn()).append(", Status: ")
                .append(isHealthy).append("]").toString();

        return new LdapAuthHealth(isHealthy, details);
    }
}
