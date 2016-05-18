package com.redhat.lightblue.rest.authz;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * RolesCache contains 2 caches: rolesCache and fallbackRolesCache. Roles put in
 * the former are evicted after few minutes, so that role membership changes in
 * ldap are reflected reasonably quickly. Roles put in the latter are evicted
 * after few hours. FallbackRolesCache is read only when ldap failure is
 * identified (circuit breaker pattern).
 *
 * @author mpatercz
 *
 */
public class RolesCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(RolesCache.class);
    private static Cache<String, List<String>> rolesCache; // non-persisted cache
    private static Cache<String, List<String>> fallbackRolesCache; // non-persisted cache

    static {
        rolesCache = CacheBuilder.newBuilder()
                .concurrencyLevel(10) // handle 10 concurrent request without a problem
                .maximumSize(500) // Hold 500 sessions before remove them
                .expireAfterWrite(5, TimeUnit.MINUTES) // If the session is inactive for more than 5 minutes, remove it
                .removalListener(
                        new RemovalListener<String, List<String>>() {
                    {
                        LOGGER.debug("Removal Listener created");
                    }

                    @Override
                    public void onRemoval(@ParametersAreNonnullByDefault RemovalNotification notification) {
                        LOGGER.debug("This data from " + notification.getKey() + " evacuated due:" + notification.getCause());
                    }
                }
                ).build();
        LOGGER.debug("RolesCache: rolesCache was created on a static block");

        fallbackRolesCache = CacheBuilder.newBuilder()
                .concurrencyLevel(10) // handle 10 concurrent request without a problem
                .maximumSize(500) // Hold 500 sessions before remove them
                .build();
        LOGGER.debug("RolesCache: fallbackRolesCache was created on a static block");
    }

    public static void put(String login, List<String> roles) {
        LOGGER.debug("RolesCache#put was invoked");
        rolesCache.put(login, roles);
        fallbackRolesCache.put(login, roles);
    }

    public static List<String> get(String login) {
        LOGGER.debug("RolesCache#get was invoked");
        return rolesCache.getIfPresent(login);
    }

    public static List<String> getFromFallback(String login) {
        LOGGER.debug("RolesCache#getFromFallback was invoked");
        return fallbackRolesCache.getIfPresent(login);
    }

    public static void invalidate(String login) {
        LOGGER.debug("RolesCache#invalidate was invoked");
        rolesCache.invalidate(login);
        fallbackRolesCache.invalidate(login);
    }

    public static Cache<String, List<String>> getRolesCache() {
        return rolesCache;
    }

    public static Cache<String, List<String>> getFallbackRolesCache() {
        return fallbackRolesCache;
    }

    public static void invalidateAll() {
        rolesCache.invalidateAll();
        fallbackRolesCache.invalidateAll();
    }

    public static void setRolesCache(Cache<String, List<String>> rolesCache) {
        RolesCache.rolesCache = rolesCache;
    }

    public static void setFallbackRolesCache(Cache<String, List<String>> fallbackRolesCache) {
        RolesCache.fallbackRolesCache = fallbackRolesCache;
    }
}
