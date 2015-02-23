package com.redhat.lightblue.rest.auth.ldap;

import com.google.common.cache.*;
import org.jboss.logging.Logger;

import javax.naming.NamingException;
import javax.naming.directory.SearchResult;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class hold the references to all local caches and have helper method to deal with them
 *
 * Created by lcestari on 2/23/15.
 */
public class LDAPCache {
    private static final Logger LOGGER = Logger.getLogger(LDAPCache.class);
    private static final LoadingCache<LDAPCacheKey, SearchResult> ldapCacheSession; // non-persisted cache
    private static final Cache<LDAPCacheKey, List<String>> userRolesCacheSession; // non-persisted cache

    // The values should be loaded from somewhere and also we should define the default values/behavior as well
    static {
        ldapCacheSession = CacheBuilder.newBuilder()
                .concurrencyLevel(10) // handle 10 concurrent request without a problem
                .maximumSize(500) // Hold 500 sessions before remove them
                .expireAfterWrite(7, TimeUnit.DAYS) // If the session is inactive for more than 7 days, remove it
                .removalListener(
                        new RemovalListener<LDAPCacheKey, SearchResult>() {
                            {
                                LOGGER.info("Removal Listener created");
                            }

                            public void onRemoval(RemovalNotification notification) {
                                LOGGER.debug("This data from " + notification.getKey() + " evacuated due:" + notification.getCause());
                            }
                        }
                ).build(
                        new CacheLoader<LDAPCacheKey, SearchResult>() {
                            public SearchResult load(LDAPCacheKey key) throws NamingException, LDAPUserNotFoundException, LDAPMutipleUserFoundException {
                                return LDAPSearcher.searchLDAPServer(key);
                            }
                        }
                );

        userRolesCacheSession = CacheBuilder.newBuilder()
                .concurrencyLevel(10) // handle 10 concurrent request without a problem
                .maximumSize(500) // Hold 500 sessions before remove them
                .expireAfterWrite(7, TimeUnit.DAYS) // If the session is inactive for more than 7 days, remove it
                .removalListener(
                        new RemovalListener<LDAPCacheKey, List<String>>() {
                            {
                                LOGGER.info("Removal Listener created");
                            }

                            public void onRemoval(RemovalNotification notification) {
                                LOGGER.debug("This data from " + notification.getKey() + " evacuated due:" + notification.getCause());
                            }
                        }
                ).build();
    }

    public static LoadingCache<LDAPCacheKey, SearchResult> getLDAPCacheSession() {
        return ldapCacheSession;
    }

    public static Cache<LDAPCacheKey, List<String>> getUserRolesCacheSession() {
        return userRolesCacheSession;
    }

    public static void invalidateKey(LDAPCacheKey key){
        ldapCacheSession.invalidate(key);
        userRolesCacheSession.invalidate(key);
    }
}
