package com.redhat.lightblue.rest.crud.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.crud.CRUDController;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.rest.health.ControllerHealthCheck;

/**
 * Listener to register Health Check classes to Metrics Health check registry
 */
public class CrudHealthCheckServletContextListener extends HealthCheckServlet.ContextListener {

    private final Logger LOGGER = LoggerFactory.getLogger(CrudHealthCheckServletContextListener.class);
    
    @Override
    protected HealthCheckRegistry getHealthCheckRegistry() {
        
        HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
        
        LightblueFactory factory = RestConfiguration.getFactory();
        CRUDController[] crudControllers = null;
        try {
            crudControllers = factory.getFactory().getCRUDControllers();
        } catch (Exception e) {
            LOGGER.error("Could not fetch CRUD Controllers from Lightblue Factory. Reason: " + e.getMessage());
        }

        if (crudControllers != null) {
            for (CRUDController crudController : crudControllers) {
                healthCheckRegistry.register(crudController.getClass().getSimpleName(),
                        new ControllerHealthCheck(crudController));
            }
        }
        
        return healthCheckRegistry;
    }
}
