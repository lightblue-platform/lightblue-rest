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

import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.redhat.lightblue.rest.auth.LightblueRoleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.util.*;

public class LightblueLdapRoleProvider implements LightblueRoleProvider {
    private final Logger LOGGER = LoggerFactory.getLogger(LightblueLdapRoleProvider.class);

    LdapContext ldapContext;
    String ldapSearchBase;

    public LightblueLdapRoleProvider(String server, String searchBase, String bindDn, String bindDNPwd) throws NamingException {
        LOGGER.debug("Creating LightblueLdapRoleProvider");
        Hashtable<String, Object> env = new Hashtable<>();
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        if (bindDn != null) {
            env.put(Context.SECURITY_PRINCIPAL, bindDn);
        }
        if (bindDNPwd != null) {
            env.put(Context.SECURITY_CREDENTIALS, bindDNPwd);
        }
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, server);
        ldapSearchBase = searchBase;
        LOGGER.debug("Creating InitialLdapContext ");
        ldapContext = new InitialLdapContext(env, null);
    }

    @Override
    public List<String> getUserRoles(String userName) {
        LOGGER.debug("Invoking LightblueLdapRoleProvider#getUserRoles");
        List<String> userRoles = new ArrayList<>();
        try {
            List<String> userRolesFromCache = getUserRolesFromCache(userName);
            if( userRolesFromCache != null && !userRolesFromCache.isEmpty() ) {
                userRoles.addAll(userRolesFromCache);
            }

            // Not found on cache due it expired or it wasn't search for this user yet (assuming the user exist)
            if (userRoles.isEmpty()) {
                SearchResult searchResult = new LdapFindUserByUidCommand(ldapContext, ldapSearchBase, userName).execute();
                userRoles.addAll(getUserRolesFromLdap(searchResult));
            }

        } catch (NamingException ne) {
            // caught this exception because getUserRolesFromLdap() method which access the remote server
            LOGGER.error("Naming problem with LDAP for user: " + userName, ne);
        } catch (HystrixRuntimeException ce) {
            // Not found in cache, returns an empty list
            LOGGER.error("Not found in cache, returns an empty list " + userName, ce);
        }

        return userRoles;
    }

    @Override
    public Collection<String> getUsersInGroup(String groupName) {
        LOGGER.error("Invoking LightblueLdapRoleProvider#getUsersInGroup (not supported))");
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void flushRoleCache(String roleName) {
        LOGGER.error("Invoking LightblueLdapRoleProvider#flushRoleCache (not supported))");
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void flushUserCache(String userName) {
        LOGGER.error("Invoking LightblueLdapRoleProvider#flushUserCache (not supported))");
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private List<String> getUserRolesFromCache(String userName) {
        LOGGER.debug("Invoking LightblueLdapRoleProvider#getUserRolesFromCache");
        LDAPCacheKey cacheKey = new LDAPCacheKey(userName, ldapContext, ldapSearchBase, "(uid=" + userName + ")", SearchControls.SUBTREE_SCOPE);
        List<String> ifPresent = LDAPCache.getUserRolesCacheSession().getIfPresent(cacheKey);
        return ifPresent;
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
}
