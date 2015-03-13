package com.redhat.lightblue.rest.auth.ldap;

import com.google.common.base.Strings;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.redhat.lightblue.hystrix.ServoGraphiteSetup;
import org.jboss.logging.Logger;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

/**
 * LDAP Hystrix command that can provide metrics for this service and fall back in case the server was unreachable as well.
 * <p/>
 * Created by nmalik  and lcestari
 */
public class LdapFindUserByUidCommand extends HystrixCommand<SearchResult> {
    public static final String GROUPKEY = "ldap";
    private static final String INVALID_PARAM = "%s is null or empty";
    private static final Logger LOGGER = Logger.getLogger(LightblueLdapRoleProvider.class);

    static {
        ServoGraphiteSetup.initialize();
    }

    private final LDAPCacheKey cacheKey;

    public LdapFindUserByUidCommand(LdapContext ldapContext, String ldapSearchBase, String uid) {
        super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(GROUPKEY)).
                andCommandKey(HystrixCommandKey.Factory.asKey(GROUPKEY + ":" + LdapFindUserByUidCommand.class.getSimpleName())));
        //check if the informed parameters are valid
        if (Strings.isNullOrEmpty(uid)) {
            throw new IllegalArgumentException(String.format(INVALID_PARAM, "uid"));
        } else if (Strings.isNullOrEmpty(ldapSearchBase)) {
            throw new IllegalArgumentException(String.format(INVALID_PARAM, "ldapSearchBase"));
        } else if (ldapContext == null) {
            throw new IllegalArgumentException(String.format(INVALID_PARAM, "ldapContext"));
        }

        this.cacheKey = new LDAPCacheKey(uid, ldapContext, ldapSearchBase, "(uid=" + uid + ")", SearchControls.SUBTREE_SCOPE);
    }

    @Override
    protected SearchResult run() throws Exception {
        SearchResult searchResult = null;

        try {
            searchResult = LDAPSearcher.searchLDAPServer(cacheKey);
        } catch (NamingException e) {
            LOGGER.error("Naming problem with LDAP for user: " + cacheKey.uid, e);
            //propagate the exception
            throw e;
        } catch (LDAPUserNotFoundException | LDAPMultipleUserFoundException e) {
            // Return null in case the User not found or multiple Users were found (which is inconsistent)

            if (e instanceof LDAPUserNotFoundException)
                LOGGER.info("No result found roles for user: " + cacheKey.uid, e);
            else {
                LOGGER.error("Multiples users found and only one was expected for user: " + cacheKey.uid, e);
            }

            searchResult = LDAPCache.getLDAPCacheSession().getIfPresent(cacheKey);
            if (searchResult != null) {
                // if (not found on the server OR server state is inconsistent ) and cache hold the old value, evict the entry
                LDAPCache.invalidateKey(cacheKey);
            }
        }
        LDAPCache.getLDAPCacheSession().put(cacheKey, searchResult);

        return searchResult;
    }

    /*
        This methods is executed for all types of failure such as run() failure, timeout, thread pool or semaphore rejection, and circuit-breaker short-circuiting
     */
    @Override
    protected SearchResult getFallback() {
        LOGGER.info("Error during the execution of the command. Falling back to the cache");
        return new FallbackViaLDAPServerProblemCommand(cacheKey, getFailedExecutionException()).execute();

    }


    /*
        Use the cache in case tje LDAP Server was not available and also to we have metrics around the fallback
     */
    private static class FallbackViaLDAPServerProblemCommand extends HystrixCommand<SearchResult> {
        private static final Logger LOGGER = Logger.getLogger(FallbackViaLDAPServerProblemCommand.class);

        private final LDAPCacheKey cacheKey;
        private final Throwable failedExecutionThrowable;

        public FallbackViaLDAPServerProblemCommand(LDAPCacheKey cacheKey, Throwable failedExecutionThrowable) {
            super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(GROUPKEY)).
                    andCommandKey(HystrixCommandKey.Factory.asKey(GROUPKEY + ":" + FallbackViaLDAPServerProblemCommand.class.getSimpleName())));
            this.cacheKey = cacheKey;
            this.failedExecutionThrowable = failedExecutionThrowable;
        }

        @Override
        protected SearchResult run() throws Exception {
            SearchResult searchResult = LDAPCache.getLDAPCacheSession().getIfPresent(cacheKey);
            if (searchResult == null) {
                CachedLDAPUserNotFoundException e = new CachedLDAPUserNotFoundException();
                LOGGER.error("Failed to connect to the server and no result found roles for user: " + cacheKey.uid, e);
                throw e;
            }
            // was able to use the cache or use the LDAP server on the second retry
            return searchResult;
        }

    }

}
