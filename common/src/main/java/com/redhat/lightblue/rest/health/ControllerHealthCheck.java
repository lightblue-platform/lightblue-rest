package com.redhat.lightblue.rest.health;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.annotation.Async;
import com.redhat.lightblue.crud.CRUDController;
import com.redhat.lightblue.crud.CRUDHealth;

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
        if (health.isHealthy()) {
            return Result.healthy(health.details());
        } else {
            return Result.unhealthy(health.details());
        }
    }
}