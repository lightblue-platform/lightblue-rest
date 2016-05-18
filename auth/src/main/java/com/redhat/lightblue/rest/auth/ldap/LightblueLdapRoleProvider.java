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
import java.util.List;

import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.lightblue.rest.auth.LightblueRoleProvider;
import com.redhat.lightblue.rest.authz.RolesCache;

/**
 * Fetches user roles from ldap. Results are cached (see {@link RolesCache}).
 *
 *
 * @author mpatercz
 *
 */
public class LightblueLdapRoleProvider implements LightblueRoleProvider {
    private final Logger LOGGER = LoggerFactory.getLogger(LightblueLdapRoleProvider.class);

    InitialLdapContextProvider ldapContextProvider;
    String ldapSearchBase;

    public LightblueLdapRoleProvider(String server, String searchBase, String bindDn, String bindDNPwd) throws NamingException {
        LOGGER.debug("Creating LightblueLdapRoleProvider");
        ldapSearchBase = searchBase;
        ldapContextProvider = new InitialLdapContextProvider(server, bindDn, bindDNPwd);
    }

    @Override
    public List<String> getUserRoles(String userName) {
        LOGGER.debug("Invoking LightblueLdapRoleProvider#getUserRoles");
        List<String> userRoles = new ArrayList<>();
        try {

            List<String> roles = new CachedLdapFindUserRolesByUidCommand(ldapSearchBase, userName, ldapContextProvider).execute();
            if (roles != null) {
                userRoles.addAll(roles);
            }

        } catch (Exception ce) {
            // Some exception
            LOGGER.error("Naming error " + userName, ce);
            throw new RuntimeException(ce);
        }

        return userRoles;
    }

}
