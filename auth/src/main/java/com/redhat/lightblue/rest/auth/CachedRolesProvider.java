package com.redhat.lightblue.rest.auth;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches results from {@link RolesProvider} and supports fallback to cache.
 * See {@link RolesCache} for more details.
 *
 * @author mpatercz
 *
 */
public class CachedRolesProvider implements RolesProvider {

    private final Logger LOGGER = LoggerFactory.getLogger(CachedRolesProvider.class);

    private RolesProvider rolesProvider;
    private RolesCache rolesCache;

    public CachedRolesProvider(RolesProvider rolesProvider) {
        super();
        this.rolesProvider = rolesProvider;
        this.rolesCache = new RolesCache(5*60*1000);
    }

    public CachedRolesProvider(RolesProvider rolesProvider, RolesCache rolesCache) {
        super();
        this.rolesProvider = rolesProvider;
        this.rolesCache = rolesCache;
    }

    @Override
    public Set<String> getUserRoles(String username) throws Exception {

        try {
            Set<String> roles = rolesCache.get(username);

            if (roles != null) {
                LOGGER.debug("Found roles in cache for uid={}", username);
                return roles;
            }


            LOGGER.debug("Cache missed for uid={}. Calling ldap.", username);

            roles = rolesProvider.getUserRoles(username);
            rolesCache.put(username, roles);

            return roles;
        } catch (Exception e) {
            Set<String> roles = rolesCache.getFromFallback(username);

            if (roles != null) {
                LOGGER.error("There was an error getting roles for "+username+", taking roles from fallback cache.", e);
                return roles;
            }

            // no fallback, nothing we can do
            throw e;
        }
    }

    protected Set<String> getFallback(String username) {
        Set<String> roles = rolesCache.getFromFallback(username);
        if (roles == null) {
            return null;
        }
        // was able to use the cache or use the LDAP server on the second retry
        LOGGER.debug("User "+username+" found in failoverCache");
        return roles;
    }

}
