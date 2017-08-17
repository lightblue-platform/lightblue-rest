package com.redhat.lightblue.rest.crud.health;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.crud.CRUDController;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.rest.health.ControllerHealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrudCheckConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrudCheckConfiguration.class);

    public static HealthCheckRegistry getDiagnosticCheckRegistry() {
        LOGGER.debug("Initializing diagnosticCheckRegistry");

        HealthCheckRegistry diagnosticCheckRegistry = new HealthCheckRegistry();

        LightblueFactory factory = RestConfiguration.getFactory();
        CRUDController[] crudControllers = null;
        try {
            crudControllers = factory.getFactory().getCRUDControllers();
        } catch (Exception e) {
            LOGGER.error("Could not fetch CRUD Controllers from Lightblue Factory. Reason: " + e.getMessage());
        }

        if (crudControllers != null) {
            for (CRUDController crudController : crudControllers) {
                diagnosticCheckRegistry.register(crudController.getClass().getSimpleName(),
                        new ControllerHealthCheck(crudController));
            }
        }
        return diagnosticCheckRegistry;
    }


    public static HealthCheckRegistry getHealthCheckRegistry() {
        LOGGER.debug("Initializing healthCheckRegistry");
        HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
        healthCheckRegistry.register("readHealthCheck", new ReadHealthCheck());
        return healthCheckRegistry;
    }
}
