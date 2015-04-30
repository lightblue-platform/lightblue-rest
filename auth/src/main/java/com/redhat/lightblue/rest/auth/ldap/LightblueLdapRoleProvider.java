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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.redhat.lightblue.rest.auth.LightblueRoleProvider;
import com.redhat.lightblue.rest.authz.RolesCache;

/**
 * Fetches user roles from ldap. Results are cached (see {@link RolesCache}).
 *
 * Initialization of this class is quite expensive due to the cost of ldap jndi lookup (@link {@link InitialLdapContext} init).
 * Use it as a singleton.
 *
 * @author mpatercz
 *
 */
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

            List<String> roles = new CachedLdapFindUserRolesByUidCommand(ldapContext, ldapSearchBase, userName).execute();
            userRoles.addAll(roles);

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




}
