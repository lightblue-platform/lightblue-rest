package com.redhat.lightblue.rest.auth.ldap;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.naming.directory.SearchResult;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class hold the references to all local caches and have helper method to deal with them
 * <p/>
 * Created by lcestari on 2/23/15.
 */
public class LDAPCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(LDAPCache.class);
    private static final Cache<LDAPCacheKey, SearchResult> ldapCacheSession; // non-persisted cache
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

                            @Override
                            public void onRemoval(@ParametersAreNonnullByDefault RemovalNotification notification) {
                                LOGGER.debug("This data from " + notification.getKey() + " evacuated due:" + notification.getCause());
                            }
                        }
                ).build();
        LOGGER.debug("LDAPCache: ldapCacheSession was created on a static block");

        userRolesCacheSession = CacheBuilder.newBuilder()
                .concurrencyLevel(10) // handle 10 concurrent request without a problem
                .maximumSize(500) // Hold 500 sessions before remove them
                .expireAfterWrite(7, TimeUnit.DAYS) // If the session is inactive for more than 7 days, remove it
                .removalListener(
                        new RemovalListener<LDAPCacheKey, List<String>>() {
                            {
                                LOGGER.info("Removal Listener created");
                            }

                            @Override
                            public void onRemoval(@ParametersAreNonnullByDefault RemovalNotification notification) {
                                LOGGER.debug("This data from " + notification.getKey() + " evacuated due:" + notification.getCause());
                            }
                        }
                ).build();
        LOGGER.debug("LDAPCache: userRolesCacheSession was created on a static block");
    }

    public static Cache<LDAPCacheKey, SearchResult> getLDAPCacheSession() {
        LOGGER.debug("LDAPCache#getLDAPCacheSession");
        return ldapCacheSession;
    }

    public static Cache<LDAPCacheKey, List<String>> getUserRolesCacheSession() {
        LOGGER.debug("LDAPCache#getUserRolesCacheSession");
        return userRolesCacheSession;
    }

    public static void invalidateKey(LDAPCacheKey key) {
        LOGGER.debug("LDAPCache#invalidateKey was invoked");
        ldapCacheSession.invalidate(key);
        userRolesCacheSession.invalidate(key);
    }
}
