package com.redhat.lightblue.rest.health;

import com.redhat.lightblue.crud.CRUDController;
import com.redhat.lightblue.crud.CRUDHealth;

import com.codahale.metrics.health.HealthCheck;

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
