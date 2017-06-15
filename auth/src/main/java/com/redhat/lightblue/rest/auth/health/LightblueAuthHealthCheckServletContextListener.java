package com.redhat.lightblue.rest.auth.health;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.redhat.lightblue.rest.auth.jboss.CertLdapLoginModule;

/**
 * Listener to register Health Check classes to Metrics Health check registry
 */
public class LightblueAuthHealthCheckServletContextListener extends HealthCheckServlet.ContextListener {

    @Override
    protected HealthCheckRegistry getHealthCheckRegistry() {

        HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
        healthCheckRegistry.register("ldap-auth-healthcheck",
                new LdapAuthHealthCheck(CertLdapLoginModule.getRolesProvider()));

        return healthCheckRegistry;
    }
}