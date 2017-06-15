package com.redhat.lightblue.rest.auth.health;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.annotation.Async;
import com.redhat.lightblue.rest.auth.RolesProvider;

/**
 * Metrics Health check class for Ldap Auth health
 *
 */
@Async(period = 15, unit = TimeUnit.MINUTES)
public class LdapAuthHealthCheck extends HealthCheck {

    private final RolesProvider rolesProvider;

    public LdapAuthHealthCheck(RolesProvider rolesProvider) {
        this.rolesProvider = rolesProvider;
    }

    @Override
    protected Result check() throws Exception {

        LdapAuthHealth health = rolesProvider.checkHealth();

        if (health.isHealthy()) {
            return Result.healthy(health.details());
        } else {
            return Result.unhealthy(health.details());
        }
    }
}
