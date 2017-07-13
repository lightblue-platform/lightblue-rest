package com.redhat.lightblue.rest.auth.health;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.annotation.Async;
import com.redhat.lightblue.rest.auth.RolesProvider;

/**
 * Metrics Health check class for Roles Provider health
 */
@Async(period = 10, unit = TimeUnit.SECONDS)
public class RolesProviderHealthCheck extends HealthCheck {

    private final RolesProvider rolesProvider;

    public RolesProviderHealthCheck(RolesProvider rolesProvider) {
        this.rolesProvider = rolesProvider;
    }

    @Override
    protected Result check() throws Exception {

        RolesProviderHealth health = rolesProvider.checkHealth();

        if (health.isHealthy()) {
            return Result.healthy(health.details());
        } else {
            return Result.unhealthy(health.details());
        }
    }
}
