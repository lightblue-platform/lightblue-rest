/*
 Copyright 2015 Red Hat, Inc. and/or its affiliates.

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

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.redhat.lightblue.hystrix.ServoGraphiteSetup;
import com.redhat.lightblue.rest.authz.RolesCache;

/**
 * LDAP Hystrix command that can provide metrics for this service and fall back
 * in case the server was unreachable as well.
 * <p/>
 * Created by nmalik and lcestari and mpatercz
 */
public class CachedLdapFindUserRolesByUidCommand extends HystrixCommand<List<String>> {
    public static final String GROUPKEY = "ldap";
    private static final String INVALID_PARAM = "%s is null or empty";
    private static final Logger LOGGER = LoggerFactory.getLogger(LightblueLdapRoleProvider.class);

    static {
        LOGGER.debug("Invoking ServoGraphiteSetup#initialize on a static block");
        ServoGraphiteSetup.initialize();
    }

    private final LDAPQuery ldapQuery;
    private final InitialLdapContextProvider ldapContextProvider;

    public CachedLdapFindUserRolesByUidCommand(String ldapSearchBase, String uid, InitialLdapContextProvider contextProvider) {
        super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(GROUPKEY)).
                andCommandKey(HystrixCommandKey.Factory.asKey(GROUPKEY + ":" + CachedLdapFindUserRolesByUidCommand.class.getSimpleName())));
        LOGGER.debug("Creating CachedLdapFindUserRolesByUidCommand");
        //check if the informed parameters are valid
        if (Strings.isNullOrEmpty(uid)) {
            LOGGER.error("uid informed in LdapFindUserByUidCommand constructor is empty or null");
            throw new IllegalArgumentException(String.format(INVALID_PARAM, "uid"));
        } else if (Strings.isNullOrEmpty(ldapSearchBase)) {
            LOGGER.error("ldapSearchBase informed in LdapFindUserByUidCommand constructor is empty or null");
            throw new IllegalArgumentException(String.format(INVALID_PARAM, "ldapSearchBase"));
        } else if (contextProvider == null) {
            LOGGER.error("contextProvider cannot be null");
            throw new IllegalArgumentException(String.format(INVALID_PARAM, "contextProvider"));
        }
        this.ldapQuery = new LDAPQuery(uid, ldapSearchBase, "(uid=" + uid + ")", SearchControls.SUBTREE_SCOPE);
        this.ldapContextProvider = contextProvider;
    }

    @Override
    protected List<String> run() throws Exception {
        LOGGER.debug("CachedLdapFindUserRolesByUidCommand#run was invoked");

        List<String> roles = RolesCache.get(ldapQuery.uid);

        if (roles != null) {
            LOGGER.debug("Found roles in cache for uid="+ldapQuery.uid);
            return roles;
        }

        try {

            LOGGER.debug("Cache missed for uid="+ldapQuery.uid+". Calling ldap.");
            // LDAPSearcher does the lookup and the call only when cache is missed
            SearchResult searchResult = LDAPSearcher.getInstance().searchLDAPServer(ldapQuery, ldapContextProvider);

            roles = getUserRolesFromLdap(searchResult);

            if (roles != null) {
                RolesCache.put(ldapQuery.uid, roles);
            }

            return roles;

        } catch (NamingException e) {
            LOGGER.error("Naming problem with LDAP for user: " + ldapQuery.uid, e);
            //propagate the exception
            throw e;
        } catch (LDAPUserNotFoundException | LDAPMultipleUserFoundException e) {
            // Return null in case the User not found or multiple Users were found (which is inconsistent)

            if (e instanceof LDAPUserNotFoundException) {
                LOGGER.error("No result found roles for user: " + ldapQuery.uid, e);
            } else {
                LOGGER.error("Multiples users found and only one was expected for user: " + ldapQuery.uid, e);
            }

            return null;
        }

    }

    private List<String> getUserRolesFromLdap(SearchResult ldapUser) throws NamingException {
        LOGGER.debug("Invoking LightblueLdapRoleProvider#getUserRolesFromLdap");
        List<String> groups = new ArrayList<>();

        //if no user found it should return an empty list (I think)
        if (ldapUser == null || ldapUser.getAttributes() == null || ldapUser.getAttributes().get("memberOf") == null) {
            return groups;
        }

        NamingEnumeration<?> groupAttributes = ldapUser.getAttributes().get("memberOf").getAll();

        while (groupAttributes.hasMore()) {
            LdapName name = new LdapName((String) groupAttributes.next());

            for (Rdn rdn : name.getRdns()) {
                if (rdn.getType().equalsIgnoreCase("cn")) {
                    groups.add((String) rdn.getValue());
                    break;
                }
            }
        }

        return groups;
    }

    /**
     * This methods is executed for all types of failure such as run() failure,
     * timeout, thread pool or semaphore rejection, and circuit-breaker
     * short-circuiting.
     *
     */
    @Override
    protected List<String> getFallback() {
        LOGGER.warn("Error during the execution of the command. Falling back to the cache");
        return new FallbackViaLDAPServerProblemCommand(ldapQuery, getFailedExecutionException()).execute();
    }

    /**
     * Use the cache in case the LDAP Server was not available and also to we
     * have metrics around the fallback
     */
    private static class FallbackViaLDAPServerProblemCommand extends HystrixCommand<List<String>> {
        private static final Logger LOGGER = LoggerFactory.getLogger(FallbackViaLDAPServerProblemCommand.class);

        private final LDAPQuery cacheKey;
        private final Throwable failedExecutionThrowable;

        public FallbackViaLDAPServerProblemCommand(LDAPQuery cacheKey, Throwable failedExecutionThrowable) {
            super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(GROUPKEY)).
                    andCommandKey(HystrixCommandKey.Factory.asKey(GROUPKEY + ":" + FallbackViaLDAPServerProblemCommand.class.getSimpleName())));
            LOGGER.debug("FallbackViaLDAPServerProblemCommand constructor");
            this.cacheKey = cacheKey;
            this.failedExecutionThrowable = failedExecutionThrowable;
        }

        @Override
        protected List<String> run() throws Exception {
            LOGGER.debug("FallbackViaLDAPServerProblemCommand#run was invoked and the following Exception caused the fallback", failedExecutionThrowable);
            List<String> roles = RolesCache.getFromFallback(cacheKey.uid);
            if (roles == null) {
                CachedLDAPUserNotFoundException e = new CachedLDAPUserNotFoundException();
                LOGGER.error("Failed to connect to the server and no result found roles for user: " + cacheKey.uid, e);
                throw e;
            }
            // was able to use the cache or use the LDAP server on the second retry
            LOGGER.debug("FallbackViaLDAPServerProblemCommand#run : user found!");
            return roles;
        }

    }

}
