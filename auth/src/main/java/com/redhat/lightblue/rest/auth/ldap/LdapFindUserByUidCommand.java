package com.redhat.lightblue.rest.auth.ldap;

import com.google.common.base.Strings;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.redhat.lightblue.util.ServoGraphiteSetup;
import org.jboss.logging.Logger;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

/**
 * LDAP Hystrix command that can provide metrics for this service and fall back in case the server was unreachable as well.
 *
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
        if(Strings.isNullOrEmpty(uid)) {
            throw new IllegalArgumentException(String.format(INVALID_PARAM, "uid"));
        } else if(Strings.isNullOrEmpty(ldapSearchBase)) {
            throw new IllegalArgumentException(String.format(INVALID_PARAM, "ldapSearchBase"));
        } else if(ldapContext == null) {
            throw new IllegalArgumentException(String.format(INVALID_PARAM, "ldapContext"));
        }

        this.cacheKey = new LDAPCacheKey(uid, ldapContext, ldapSearchBase,  "(uid=" + uid + ")", SearchControls.SUBTREE_SCOPE);
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
        } catch (LDAPUserNotFoundException | LDAPMutipleUserFoundException e) {
            // Return null in case the User not found or multiple Users were found (which is inconsistent)

            if(e instanceof LDAPUserNotFoundException)
                LOGGER.info("No result found roles for user: " + cacheKey.uid, e);
            else{
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
            //iff it was a problem with the server, try the following:
            if(failedExecutionThrowable instanceof NamingException) {
                SearchResult searchResult = LDAPCache.getLDAPCacheSession().getIfPresent(cacheKey);
                if (searchResult == null) {
                    CachedLDAPUserNotFoundException e = new CachedLDAPUserNotFoundException();
                    LOGGER.error("Failed to connect to the server and no result found roles for user: " + cacheKey.uid, e);
                    throw e;
                }
                // was able to use the cache or use the LDAP server on the second retry
                return searchResult;

            } else if(failedExecutionThrowable instanceof Exception) {
                // if it is an exception, propagate it until someone handle it properly
                LOGGER.error("An internal problem occurred during the query for roles for the  user: " + cacheKey.uid, failedExecutionThrowable);
                throw ((Exception) failedExecutionThrowable);
            } else {
                // probably java.lang.Error or a custom Throwable exception. In this case we can't handle the problem
                // and if we wrap it we can change the JVM to inconsistent state, so we have to exit with error status
                // code
                LOGGER.fatal("a Throwable instance (which is not instance of Exception) was thrown and it can't be handled.", failedExecutionThrowable);
                System.exit(-1);
                return null;
            }
        }

    }

}
