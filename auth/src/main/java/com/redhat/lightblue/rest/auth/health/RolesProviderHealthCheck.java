package com.redhat.lightblue.rest.auth.health;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.annotation.Async;
import com.redhat.lightblue.rest.auth.RolesProvider;

import java.util.Map;
import java.util.concurrent.TimeUnit;

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

        ResultBuilder resultBuilder;

        if (health.isHealthy()) {
            resultBuilder = Result.builder().healthy();
        } else {
            resultBuilder = Result.builder().unhealthy();
        }

        for (Map.Entry<String, Object> entry : health.details().entrySet()) {
            resultBuilder.withDetail(entry.getKey(), entry.getValue());
        }

        return resultBuilder.build();
    }
}
