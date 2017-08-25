package com.redhat.lightblue.rest.crud.health;

import com.codahale.metrics.health.HealthCheckRegistry;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class CrudCheckRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrudCheckRegistry.class);

    private static HealthCheckRegistry healthCheckRegistry;
    private static HealthCheckRegistry diagnosticCheckRegistry;

    private CrudCheckRegistry() {

    }

    public static HealthCheckRegistry getDiagnosticCheckRegistry() {
        if(diagnosticCheckRegistry == null) {
            diagnosticCheckRegistry = createDiagnosticCheckRegistry();
        }
        return diagnosticCheckRegistry;
    }

    public static HealthCheckRegistry getHealthCheckRegistry() {
        if(healthCheckRegistry == null) {
            healthCheckRegistry = createHealthCheckRegistry();
        }
        return healthCheckRegistry;
    }

    private static HealthCheckRegistry createHealthCheckRegistry() {
        LOGGER.debug("Initializing healthCheckRegistry");
        HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
        healthCheckRegistry.register("readHealthCheck", new ReadHealthCheck());
        return healthCheckRegistry;
    }

    private static HealthCheckRegistry createDiagnosticCheckRegistry() {
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

        if(ldapConfigurationExists()) {
            RolesProvider rolesProvider = null;
            try {
                rolesProvider = createRolesProvider();
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("Could not create Roles Provider. Reason: " + e.getMessage());
            }

            if (rolesProvider != null) {
                diagnosticCheckRegistry.register("ldap-auth-healthcheck", new RolesProviderHealthCheck(rolesProvider));
            }
        }

        return diagnosticCheckRegistry;
    }

    private static RolesProvider createRolesProvider() throws Exception {
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

    private static JsonNode readLdapConfiguration() throws IOException {
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

    private static boolean ldapConfigurationExists() {
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(LdapConfiguration.FILENAME)) {
            if (null == is) {
                return false;
            } else {
                return true;
            }
        } catch (IOException e) {
            return false;
        }
    }


}
