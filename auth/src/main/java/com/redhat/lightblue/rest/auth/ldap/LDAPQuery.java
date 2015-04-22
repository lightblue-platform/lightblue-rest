package com.redhat.lightblue.rest.auth.ldap;

import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;

/**
 *
 * Created by lcestari on 2/23/15.
 */
public class LDAPQuery {
    final String uid;
    final LdapContext ldapContext;
    final String ldapSearchBase;
    final String searchFilter;
    final SearchControls searchControls = new SearchControls();

    public LDAPQuery(String uid, LdapContext ldapContext, String ldapSearchBase, String searchFilter, int searchControlScope) {
        this.uid = uid;
        this.ldapContext = ldapContext;
        this.ldapSearchBase = ldapSearchBase;
        this.searchFilter = searchFilter;
        this.searchControls.setSearchScope(searchControlScope);
    }

    @Override
    public String toString() {
        return "LDAPCacheKey{" +
                "uid='" + uid + '\'' +
                ", ldapContext=" + ldapContext +
                ", ldapSearchBase='" + ldapSearchBase + '\'' +
                ", searchFilter='" + searchFilter + '\'' +
                ", searchControls=" + searchControls +
                '}';
    }
}
