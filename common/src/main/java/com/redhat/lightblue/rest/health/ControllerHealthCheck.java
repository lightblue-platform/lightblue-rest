package com.redhat.lightblue.rest.health;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.annotation.Async;
import com.redhat.lightblue.crud.CRUDController;
import com.redhat.lightblue.crud.CRUDHealth;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Metrics Health check class for CRUD Controller health
 */
@Async(period = 10, unit = TimeUnit.SECONDS)
public class ControllerHealthCheck extends HealthCheck {
    private final CRUDController controller;

    public ControllerHealthCheck(CRUDController controller) {
        this.controller = controller;
    }

    @Override
    protected Result check() throws Exception {
        CRUDHealth health = controller.checkHealth();

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