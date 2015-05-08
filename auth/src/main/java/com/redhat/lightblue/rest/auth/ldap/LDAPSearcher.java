package com.redhat.lightblue.rest.auth.ldap;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class si responsible to handle how to query data from LDAP server and also handle the exceptions different flows
 * Created by lcestari on 2/23/15.
 */
public class LDAPSearcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(LDAPSearcher.class);

    private static LDAPSearcher instance = null;

    public static LDAPSearcher getInstance() {
        if (instance == null) {
            instance = new LDAPSearcher();
        }

        return instance;
    }

    public SearchResult searchLDAPServer(LDAPQuery ldapQuery, InitialLdapContextProvider contextProvider) throws NamingException, LDAPUserNotFoundException, LDAPMultipleUserFoundException {
        LOGGER.debug("LDAPSearcher#searchLDAPServer was invoked and it will call the remote LDAP server");

        // Extension a: returns an exception as the LDAP server is down (eg.: this can be meaningful to use the cache )
        NamingEnumeration<SearchResult> results = contextProvider.lookupLdap().search(ldapQuery.ldapSearchBase, ldapQuery.searchFilter, ldapQuery.searchControls);
        if (results.hasMoreElements()) {
            SearchResult searchResult = results.nextElement();

            //make sure there is not another item available, there should be only 1 match
            if (results.hasMoreElements()) {
                String message = "Matched multiple users for the accountName: " + ldapQuery.uid;
                LOGGER.error(message);
                // Extension b: returns an exception to warn about the bad inconsistent state
                throw new LDAPMultipleUserFoundException(message);
            }

            // Basic flow: returns the unique entry from LDAP server
            LOGGER.debug("LDAPSearcher#searchLDAPServer could retrieve the values from the remote LDAP Server");
            return searchResult;
        } else {
            // Extension c: returns an exception to notify that the user was not found (eg.: this can be meaningful to evict the key )
            LOGGER.debug("LDAPSearcher#searchLDAPServer could NOT retrieve the user from the remote LDAP Server");
            throw new LDAPUserNotFoundException();
        }
    }

    public static void setInstance(LDAPSearcher instance) {
        LDAPSearcher.instance = instance;
    }
}
