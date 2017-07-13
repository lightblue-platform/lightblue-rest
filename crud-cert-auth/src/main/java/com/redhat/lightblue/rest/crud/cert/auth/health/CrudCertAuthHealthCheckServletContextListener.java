package com.redhat.lightblue.rest.crud.cert.auth.health;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.crud.CRUDController;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.rest.auth.CachedRolesProvider;
import com.redhat.lightblue.rest.auth.RolesCache;
import com.redhat.lightblue.rest.auth.RolesProvider;
import com.redhat.lightblue.rest.auth.health.RolesProviderHealthCheck;
import com.redhat.lightblue.rest.auth.ldap.LdapConfiguration;
import com.redhat.lightblue.rest.auth.ldap.LdapRolesProvider;
import com.redhat.lightblue.rest.health.ControllerHealthCheck;
import com.redhat.lightblue.util.JsonUtils;

/**
 * Listener to register Health Check classes to Metrics Health check registry
 */
public class CrudCertAuthHealthCheckServletContextListener extends HealthCheckServlet.ContextListener {

    private final Logger LOGGER = LoggerFactory.getLogger(CrudCertAuthHealthCheckServletContextListener.class);
    
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
        
        RolesProvider rolesProvider = null;
        try {
            rolesProvider = createRolesProvider();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Could not create Roles Provider. Reason: " + e.getMessage());
        }

        if (rolesProvider != null) {
            healthCheckRegistry.register("ldap-auth-healthcheck", new RolesProviderHealthCheck(rolesProvider));
        }
        
        return healthCheckRegistry;
    }
    
    private RolesProvider createRolesProvider() throws Exception {

        JsonNode root = readLdapConfiguration();
        String searchBase = null;
        int rolesCacheExpiry = 5 * 60 * 1000; // default 5 minutes

        if (root != null) {
            JsonNode node = root.get("ldap-config");

            if (node != null) {
                JsonNode x = node.get("search_base");
                if (x != null) {
                    searchBase = x.asText();
                }
                x = node.get("rolesCacheExpiryMS");
                if (x != null) {
                    rolesCacheExpiry = x.asInt();
                }
            }
        }

        LdapConfiguration ldapConfiguration = new LdapConfiguration();
        ldapConfiguration.initializeFromJson(root);
        
        return new CachedRolesProvider(new LdapRolesProvider(searchBase, ldapConfiguration),
                new RolesCache(rolesCacheExpiry));
    }

    private JsonNode readLdapConfiguration() throws IOException {
        JsonNode root = null;
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(LdapConfiguration.FILENAME)) {
            if (null == is) {
                throw new FileNotFoundException(LdapConfiguration.FILENAME);
            }
            root = JsonUtils.json(is, true);
        }

        return root;
    }
}
