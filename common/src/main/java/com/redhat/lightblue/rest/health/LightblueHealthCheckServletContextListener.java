package com.redhat.lightblue.rest.health;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.crud.CRUDController;
import com.redhat.lightblue.rest.RestConfiguration;

/**
 * @author ssaurabh
 * 
 * Listener to register Health Check classes to Metrics Health check registery
 *
 */
public class LightblueHealthCheckServletContextListener extends HealthCheckServlet.ContextListener {

	public static HealthCheckRegistry HEALTH_CHECK_REGISTRY = new HealthCheckRegistry();

	static {

		LightblueFactory factory = RestConfiguration.getFactory();
		CRUDController[] crudControllers = null;
		try {
			crudControllers = factory.getFactory().getCRUDControllers();
		} catch (Exception e) {
		}

		for (CRUDController crudController : crudControllers) {
			HEALTH_CHECK_REGISTRY.register(crudController.toString(), new ControllerHealthCheck(crudController));
		}
	}

	@Override
	protected HealthCheckRegistry getHealthCheckRegistry() {
		return HEALTH_CHECK_REGISTRY;
	}

}
