package com.redhat.lightblue.rest.auth.ldap;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.redhat.lightblue.util.ServoGraphiteSetup;
import org.jboss.logging.Logger;

import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

/**
 * Created by nmalik on 11/11/14.
 */
public class LdapFindUserByUidCommand extends HystrixCommand<SearchResult> {
    private final Logger LOGGER = Logger.getLogger(LightblueLdapRoleProvider.class);

    public static final String GROUPKEY = "ldap";

    static {
        ServoGraphiteSetup.initialize();
    }

    private final String uid;
    private final LdapContext ldapContext;
    private final String ldapSearchBase;


    public LdapFindUserByUidCommand(LdapContext ldapContext, String ldapSearchBase, String uid) {
        super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(GROUPKEY)).
                andCommandKey(HystrixCommandKey.Factory.asKey(GROUPKEY + ":" + LdapFindUserByUidCommand.class.getSimpleName())));

        this.ldapContext = ldapContext;
        this.ldapSearchBase = ldapSearchBase;
        this.uid = uid;
    }

    @Override
    protected SearchResult run() throws Exception {
        String searchFilter = "(uid=" + uid + ")";

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration<SearchResult> results = ldapContext.search(ldapSearchBase, searchFilter, searchControls);

        SearchResult searchResult = null;
        if (results.hasMoreElements()) {
            searchResult = results.nextElement();

            //make sure there is not another item available, there should be only 1 match
            if (results.hasMoreElements()) {
                LOGGER.error("Matched multiple users for the accountName: " + uid);
                return null;
            }
        }

        return searchResult;
    }
}
