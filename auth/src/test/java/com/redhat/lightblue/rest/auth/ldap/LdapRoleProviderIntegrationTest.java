/*
 Copyright 2014 Red Hat, Inc. and/or its affiliates.

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.redhat.lightblue.ldap.test.LdapServerExternalResource;
import com.redhat.lightblue.ldap.test.LdapServerExternalResource.InMemoryLdapServer;
import com.redhat.lightblue.rest.auth.health.RolesProviderHealth;
import com.unboundid.ldap.sdk.Attribute;



@InMemoryLdapServer
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LdapRoleProviderIntegrationTest {

    @ClassRule
    public static LdapServerExternalResource ldapServer = LdapServerExternalResource.createDefaultInstance();

    private static final String BASEDB_USERS = "ou=Users,dc=example,dc=com";
    private static final String USER_WITH_ROLES = "uid=derek63,ou=Users,dc=example,dc=com";
    private static final String USER_WITH_NO_ROLES = "uid=lcestari,ou=Users,dc=example,dc=com";
    private static final String BASEDB_GROUPS = "ou=Departments,dc=example,dc=com";

    private static LdapRolesProvider provider;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ldapServer.add(BASEDB_USERS, new Attribute[]{new Attribute("objectClass", "top"), new Attribute("objectClass", "organizationalUnit"),
            new Attribute("ou", "Users")});
        ldapServer.add(BASEDB_GROUPS, new Attribute[]{new Attribute("objectClass", "top"), new Attribute("objectClass", "organizationalUnit"),
            new Attribute("ou", "Groups")});
        ldapServer.add(USER_WITH_NO_ROLES, new Attribute[]{new Attribute("cn", "lcestari"), new Attribute("uid", "lcestari"),
            new Attribute("objectClass", "person")});
        ldapServer.add(USER_WITH_ROLES, new Attribute[]{new Attribute("cn", "derek63"), new Attribute("uid", "derek63"),
            new Attribute("memberOf", "cn=lightblue-contributors,ou=Groups,dc=example,dc=com"),
            new Attribute("memberOf", "cn=lightblue-developers,ou=Groups,dc=example,dc=com"), new Attribute("objectClass", "person")});

        System.setProperty("ldap.host", "localhost");
        System.setProperty("ldap.port", String.valueOf(LdapServerExternalResource.DEFAULT_PORT));
        System.setProperty("ldap.database", "test");
        System.setProperty("ldap.person.basedn", BASEDB_USERS);
        System.setProperty("ldap.department.basedn", BASEDB_GROUPS);

        LdapConfiguration ldapConfig = new LdapConfiguration()
                .bindDn(LdapServerExternalResource.DEFAULT_BINDABLE_DN)
                .bindDNPwd(LdapServerExternalResource.DEFAULT_PASSWORD)
                .server("localhost")
                .port(LdapServerExternalResource.DEFAULT_PORT);

        provider = new LdapRolesProvider(
                LdapServerExternalResource.DEFAULT_BASE_DN,
                ldapConfig);

    }

    @Test
    public void testUserWithRoles() throws Exception {
        Set<String> expectedUserRoles = new HashSet<>();
        expectedUserRoles.add("lightblue-contributors");
        expectedUserRoles.add("lightblue-developers");

        Set<String> userRoles = provider.getUserRoles("derek63");
        assertNotNull(userRoles);
        assertEquals(expectedUserRoles, userRoles);
    }

    @Test
    public void testUserWithNoRoles() throws Exception {
        assertTrue(provider.getUserRoles("lcestari").isEmpty());
    }

    @Test
    public void testUserWhoDoesNotExist() throws Exception {
        assertTrue(provider.getUserRoles("idontexist").isEmpty());
    }

    @Test
    public void testHealthCheck() throws Exception {
        
        RolesProviderHealth health = provider.checkHealth();
        System.out.println(health.details());
        assertTrue(health.isHealthy());
    }
}
