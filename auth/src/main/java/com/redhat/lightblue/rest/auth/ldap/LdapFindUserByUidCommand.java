package com.redhat.lightblue.rest.auth.ldap;

import com.google.common.base.Strings;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.redhat.lightblue.hystrix.ServoGraphiteSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

/**
 * LDAP Hystrix command that can provide metrics for this service and fall back
 * in case the server was unreachable as well.
 * <p/>
 * Created by nmalik and lcestari
 */
public class LdapFindUserByUidCommand extends HystrixCommand<SearchResult> {
    public static final String GROUPKEY = "ldap";
    private static final String INVALID_PARAM = "%s is null or empty";
    private static final Logger LOGGER = LoggerFactory.getLogger(LightblueLdapRoleProvider.class);

    static {
        LOGGER.debug("Invoking ServoGraphiteSetup#initialize on a static block");
        ServoGraphiteSetup.initialize();
    }

    private final LDAPCacheKey cacheKey;

    public LdapFindUserByUidCommand(LdapContext ldapContext, String ldapSearchBase, String uid) {
        super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(GROUPKEY)).
                andCommandKey(HystrixCommandKey.Factory.asKey(GROUPKEY + ":" + LdapFindUserByUidCommand.class.getSimpleName())));
        LOGGER.debug("Creating LdapFindUserByUidCommand");
        //check if the informed parameters are valid
        if (Strings.isNullOrEmpty(uid)) {
            LOGGER.error("uid informed in LdapFindUserByUidCommand constructor is empty or null");
            throw new IllegalArgumentException(String.format(INVALID_PARAM, "uid"));
        } else if (Strings.isNullOrEmpty(ldapSearchBase)) {
            LOGGER.error("ldapSearchBase informed in LdapFindUserByUidCommand constructor is empty or null");
            throw new IllegalArgumentException(String.format(INVALID_PARAM, "ldapSearchBase"));
        } else if (ldapContext == null) {
            LOGGER.error("ldapContext informed in LdapFindUserByUidCommand constructor is null");
            throw new IllegalArgumentException(String.format(INVALID_PARAM, "ldapContext"));
        }
        this.cacheKey = new LDAPCacheKey(uid, ldapContext, ldapSearchBase, "(uid=" + uid + ")", SearchControls.SUBTREE_SCOPE);
    }

    @Override
    protected SearchResult run() throws Exception {
        LOGGER.debug("LdapFindUserByUidCommand#run was invoked");

        SearchResult searchResult = null;
        try {
            searchResult = LDAPSearcher.searchLDAPServer(cacheKey);
        } catch (NamingException e) {
            LOGGER.error("Naming problem with LDAP for user: " + cacheKey.uid, e);
            //propagate the exception
            throw e;
        } catch (LDAPUserNotFoundException | LDAPMultipleUserFoundException e) {
            // Return null in case the User not found or multiple Users were found (which is inconsistent)

            if (e instanceof LDAPUserNotFoundException) {
                LOGGER.error("No result found roles for user: " + cacheKey.uid, e);
            } else {
                LOGGER.error("Multiples users found and only one was expected for user: " + cacheKey.uid, e);
            }

        // Disabling due to issues with threading, maybe related to https://github.com/google/guava/issues/1715
//            searchResult = LDAPCache.getLDAPCacheSession().getIfPresent(cacheKey);
//            if (searchResult != null) {
            // if (not found on the server OR server state is inconsistent ) and cache hold the old value, evict the entry
//                LDAPCache.invalidateKey(cacheKey);
//            }
        }
        // Disabling due to issues with threading, maybe related to https://github.com/google/guava/issues/1715
//        LOGGER.debug("LdapFindUserByUidCommand#run : user found! Adding it to the cache");
//        LDAPCache.getLDAPCacheSession().put(cacheKey, searchResult);

        return searchResult;
    }

    /**
     * This methods is executed for all types of failure such as run() failure,
     * timeout, thread pool or semaphore rejection, and circuit-breaker
     * short-circuiting.
     * 
     * Disabling due to issues with threading, maybe related to https://github.com/google/guava/issues/1715
     */
//    @Override
//    protected SearchResult getFallback() {
//        LOGGER.warn("Error during the execution of the command. Falling back to the cache");
//        return new FallbackViaLDAPServerProblemCommand(cacheKey, getFailedExecutionException()).execute();
//    }

    /**
     * Use the cache in case the LDAP Server was not available and also to we
     * have metrics around the fallback
     */
    private static class FallbackViaLDAPServerProblemCommand extends HystrixCommand<SearchResult> {
        private static final Logger LOGGER = LoggerFactory.getLogger(FallbackViaLDAPServerProblemCommand.class);

        private final LDAPCacheKey cacheKey;
        private final Throwable failedExecutionThrowable;

        public FallbackViaLDAPServerProblemCommand(LDAPCacheKey cacheKey, Throwable failedExecutionThrowable) {
            super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(GROUPKEY)).
                    andCommandKey(HystrixCommandKey.Factory.asKey(GROUPKEY + ":" + FallbackViaLDAPServerProblemCommand.class.getSimpleName())));
            LOGGER.debug("FallbackViaLDAPServerProblemCommand constructor");
            this.cacheKey = cacheKey;
            this.failedExecutionThrowable = failedExecutionThrowable;
        }

        @Override
        protected SearchResult run() throws Exception {
            LOGGER.debug("FallbackViaLDAPServerProblemCommand#run was invoked and the following Exception caused the fallback", failedExecutionThrowable);
            SearchResult searchResult = LDAPCache.getLDAPCacheSession().getIfPresent(cacheKey);
            if (searchResult == null) {
                CachedLDAPUserNotFoundException e = new CachedLDAPUserNotFoundException();
                LOGGER.error("Failed to connect to the server and no result found roles for user: " + cacheKey.uid, e);
                throw e;
            }
            // was able to use the cache or use the LDAP server on the second retry
            LOGGER.debug("FallbackViaLDAPServerProblemCommand#run : user found!");
            return searchResult;
        }

    }

}
