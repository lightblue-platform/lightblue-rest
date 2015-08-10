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

import javax.naming.directory.SearchControls;

/**
 *
 * Created by lcestari on 2/23/15.
 */
public class LDAPQuery {
    final String uid;
    final String ldapSearchBase;
    final String searchFilter;
    final SearchControls searchControls = new SearchControls();

    public LDAPQuery(String uid, String ldapSearchBase, String searchFilter, int searchControlScope) {
        this.uid = uid;
        this.ldapSearchBase = ldapSearchBase;
        this.searchFilter = searchFilter;
        this.searchControls.setSearchScope(searchControlScope);
    }

    @Override
    public String toString() {
        return "LDAPCacheKey{" +
                "uid='" + uid + '\'' +
                ", ldapSearchBase='" + ldapSearchBase + '\'' +
                ", searchFilter='" + searchFilter + '\'' +
                ", searchControls=" + searchControls +
                '}';
    }
}
