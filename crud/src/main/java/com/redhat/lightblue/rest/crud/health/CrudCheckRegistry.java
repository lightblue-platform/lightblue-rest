package com.redhat.lightblue.rest.crud.health;

import com.codahale.metrics.health.HealthCheckRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrudCheckRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrudCheckRegistry.class);

    private static HealthCheckRegistry healthCheckRegistry;
    private static HealthCheckRegistry diagnosticCheckRegistry;

    private CrudCheckRegistry() {

    }

    public static HealthCheckRegistry getDiagnosticCheckRegistry() {
        if(diagnosticCheckRegistry == null) {
            diagnosticCheckRegistry = CrudCheckConfiguration.createDiagnosticCheckRegistry();
        }
        return diagnosticCheckRegistry;
    }


    public static HealthCheckRegistry getHealthCheckRegistry() {
        if(healthCheckRegistry == null) {
            healthCheckRegistry = CrudCheckConfiguration.createHealthCheckRegistry();
        }
        return healthCheckRegistry;
    }

}
