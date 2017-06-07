package com.redhat.lightblue.rest.health;

import com.codahale.metrics.health.HealthCheck;
import com.redhat.lightblue.crud.CRUDController;
import com.redhat.lightblue.crud.LightblueHealth;

/**
 * @author ssaurabh
 * Metrics Health check class for CRUD Controller health
 *
 */
public class ControllerHealthCheck extends HealthCheck {
	private final CRUDController controller;

	public ControllerHealthCheck(CRUDController controller) {
		this.controller = controller;
	}

	@Override
	protected Result check() throws Exception {
		LightblueHealth health = controller.checkHealth();
		if (health.isHealthy()) {
			return Result.healthy(health.details());
		} else {
			return Result.unhealthy(health.details());
		}
	}
}