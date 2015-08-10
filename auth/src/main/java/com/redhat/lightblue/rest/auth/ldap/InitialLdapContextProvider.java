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

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds configurations needed for ldap lookup and does the lookup on demand.
 *
 * @author mpatercz
 *
 */
public class InitialLdapContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialLdapContextProvider.class);

    private Hashtable<String, Object> env = new Hashtable<>();

    public InitialLdapContextProvider(String server, String bindDn, String bindDNPwd) {
        LOGGER.debug("init()");

        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        if (bindDn != null) {
            env.put(Context.SECURITY_PRINCIPAL, bindDn);
        }
        if (bindDNPwd != null) {
            env.put(Context.SECURITY_CREDENTIALS, bindDNPwd);
        }
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, server);
    }

    /**
     * This call is expensive. InitialLdapContext should be reused, but then we have to deal with
     * ldap closing the connections. A real pooling with connection validation is needed.
     *
     * @throws NamingException
     */
    public InitialLdapContext lookupLdap() throws NamingException {
        LOGGER.debug("createInitialContext()");
        return new InitialLdapContext(env, null);
    }

}
