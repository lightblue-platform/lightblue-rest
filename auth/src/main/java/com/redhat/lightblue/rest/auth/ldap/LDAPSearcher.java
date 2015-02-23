package com.redhat.lightblue.rest.auth.ldap;

import org.jboss.logging.Logger;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchResult;

/**
 * This class si responsible to handle how to query data from LDAP server and also handle the exceptions different flows
 * Created by lcestari on 2/23/15.
 */
public class LDAPSearcher {
    private static final Logger LOGGER = Logger.getLogger(LDAPSearcher.class);

    public static SearchResult searchLDAPServer(LDAPCacheKey ldapCacheKey) throws NamingException, LDAPUserNotFoundException, LDAPMutipleUserFoundException {
        // Extension a: returns an exception as the LDAP server is down (eg.: this can be meaningful to use the cache )
        NamingEnumeration<SearchResult> results = ldapCacheKey.ldapContext.search(ldapCacheKey.ldapSearchBase, ldapCacheKey.searchFilter, ldapCacheKey.searchControls);
        SearchResult searchResult = null;
        if (results.hasMoreElements()) {
            searchResult = results.nextElement();

            //make sure there is not another item available, there should be only 1 match
            if (results.hasMoreElements()) {
                String message = new StringBuilder().append("Matched multiple users for the accountName: ").append(ldapCacheKey.uid).toString();
                LOGGER.error(message);
                // Extension b: returns an exception to warn about the bad inconsistent state
                throw new LDAPMutipleUserFoundException(message);
            }

            // Basic flow: returns the unique entry from LDAP server
            return searchResult;
        } else {
            // Extension c: returns an exception to notify that the user was not found (eg.: this can be meaningful to evict the key )
            throw new LDAPUserNotFoundException();
        }
    }
}
