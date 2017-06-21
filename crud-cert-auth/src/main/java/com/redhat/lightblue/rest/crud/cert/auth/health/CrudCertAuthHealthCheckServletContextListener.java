package com.redhat.lightblue.rest.crud.cert.auth.health;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.crud.CRUDController;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.rest.auth.health.RolesProviderHealthCheck;
import com.redhat.lightblue.rest.auth.jboss.CertLdapLoginModule;
import com.redhat.lightblue.rest.health.ControllerHealthCheck;

/**
 * Listener to register Health Check classes to Metrics Health check registry
 */
public class CrudCertAuthHealthCheckServletContextListener extends HealthCheckServlet.ContextListener {

    @Override
    protected HealthCheckRegistry getHealthCheckRegistry() {

        HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();

        LightblueFactory factory = RestConfiguration.getFactory();
        CRUDController[] crudControllers = null;
        try {
            crudControllers = factory.getFactory().getCRUDControllers();
        } catch (Exception e) {
        }

        if (crudControllers != null) {
            for (CRUDController crudController : crudControllers) {
                healthCheckRegistry.register(crudController.getClass().getName(),
                        new ControllerHealthCheck(crudController));
            }
        }
        
        healthCheckRegistry.register("ldap-auth-healthcheck",
                new RolesProviderHealthCheck(CertLdapLoginModule.getRolesProvider()));

        return healthCheckRegistry;
    }
}