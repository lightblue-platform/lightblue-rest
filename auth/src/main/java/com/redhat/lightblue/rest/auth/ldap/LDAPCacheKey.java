package com.redhat.lightblue.rest.auth.ldap;

import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;

/**
 * This class holds the values to represent an unique cache key. It is also used to query data as well
 *
 * Created by lcestari on 2/23/15.
*/
public class LDAPCacheKey {
    final String uid;
    final LdapContext ldapContext;
    final String ldapSearchBase;
    final String searchFilter;
    final SearchControls searchControls = new SearchControls();

    public LDAPCacheKey(String uid, LdapContext ldapContext, String ldapSearchBase, String searchFilter, int searchControlScope) {
        this.uid = uid;
        this.ldapContext = ldapContext;
        this.ldapSearchBase = ldapSearchBase;
        this.searchFilter = searchFilter;
        this.searchControls.setSearchScope(searchControlScope);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LDAPCacheKey)) return false;

        LDAPCacheKey that = (LDAPCacheKey) o;

        if (!ldapContext.equals(that.ldapContext)) return false;
        if (!ldapSearchBase.equals(that.ldapSearchBase)) return false;
        if (!uid.equals(that.uid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = uid.hashCode();
        result = 31 * result + ldapContext.hashCode();
        result = 31 * result + ldapSearchBase.hashCode();
        return result;
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
