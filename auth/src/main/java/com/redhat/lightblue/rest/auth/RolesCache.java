package com.redhat.lightblue.rest.auth;

import java.util.Set;
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
 * ldap are reflected reasonably quickly. Roles put in the latter are never evicted.
 * FallbackRolesCache is read only when ldap failure is identified.
 *
 * @author mpatercz
 *
 */
public class RolesCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(RolesCache.class);
    private Cache<String, Set<String>> rolesCache; // non-persisted cache
    private Cache<String, Set<String>> fallbackRolesCache; // non-persisted cache

    public static final int maximumSize = 500;
    public static final int concurrencyLevel = 10;

    public RolesCache(int expiryMS) {
        rolesCache = CacheBuilder.newBuilder()
                .concurrencyLevel(concurrencyLevel)
                .maximumSize(maximumSize)
                .expireAfterWrite(expiryMS, TimeUnit.MILLISECONDS)
                .removalListener(
                        new RemovalListener<String, Set<String>>() {
                    {
                        LOGGER.debug("Removal Listener created");
                    }

                    @Override
                    public void onRemoval(@ParametersAreNonnullByDefault RemovalNotification<String, Set<String>> notification) {
                        LOGGER.debug("This data from " + notification.getKey() + " evacuated due:" + notification.getCause());
                    }
                }
                ).build();

        fallbackRolesCache = CacheBuilder.newBuilder()
                .concurrencyLevel(concurrencyLevel) // handle 10 concurrent request without a problem
                .maximumSize(maximumSize) // Hold 500 sessions before remove them
                .build();

        LOGGER.info("RolesCache initialized with expiry={}", expiryMS);
    }

    public void put(String login, Set<String> roles) {
        LOGGER.debug("RolesCache#put was invoked");
        rolesCache.put(login, roles);
        fallbackRolesCache.put(login, roles);
    }

    public Set<String> get(String login) {
        LOGGER.debug("RolesCache#get was invoked");
        return rolesCache.getIfPresent(login);
    }

    public Set<String> getFromFallback(String login) {
        LOGGER.debug("RolesCache#getFromFallback was invoked");
        return fallbackRolesCache.getIfPresent(login);
    }

    public void invalidate(String login) {
        LOGGER.debug("RolesCache#invalidate was invoked");
        rolesCache.invalidate(login);
        fallbackRolesCache.invalidate(login);
    }

    public Cache<String, Set<String>> getRolesCache() {
        return rolesCache;
    }

    public Cache<String, Set<String>> getFallbackRolesCache() {
        return fallbackRolesCache;
    }

    public void invalidateAll() {
        rolesCache.invalidateAll();
        fallbackRolesCache.invalidateAll();
    }

    public void setRolesCache(Cache<String, Set<String>> rolesCache) {
        this.rolesCache = rolesCache;
    }

    public void setFallbackRolesCache(Cache<String, Set<String>> fallbackRolesCache) {
        this.fallbackRolesCache = fallbackRolesCache;
    }
}
