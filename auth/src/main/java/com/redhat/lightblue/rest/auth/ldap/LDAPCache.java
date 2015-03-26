/*
 Copyright 2015 Red Hat, Inc. and/or its affiliates.

 This file is part of lightblue.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
