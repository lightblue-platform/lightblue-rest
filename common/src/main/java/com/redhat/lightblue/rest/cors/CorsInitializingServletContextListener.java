package com.redhat.lightblue.rest.cors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;
import java.io.InputStream;

public class CorsInitializingServletContextListener implements ServletContextListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CorsInitializingServletContextListener.class);
    private static final String CORS_CONFIGURATION_RESOURCE_PARAM = "cors.configuration.resource";

    private final CorsFilterRegistration corsFilter;

    public CorsInitializingServletContextListener() {
        this(new EbayCorsFilterRegistration());
    }

    public CorsInitializingServletContextListener(CorsFilterRegistration corsFilter) {
        this.corsFilter = corsFilter;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            ServletContext context = sce.getServletContext();

            if (!isCorsConfigurationResourceDefined(context)) {
                LOGGER.info("No CORS json configuration resource location provided. CORS will not "
                        + "be enabled.");
                return;
            }

            String configJsonResource = getConfigJsonResource(context);

            if (!isResourceFound(configJsonResource)) {
                LOGGER.info("CORS json configuration not found at " + configJsonResource + ". CORS "
                        + "will not be enabled.");
                return;
            }

            corsFilter.register(context, getConfig(configJsonResource));
        } catch (IOException e) {
            LOGGER.error("Error reading CORS configuration file. CORS will not be enabled.", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }

    protected boolean isCorsConfigurationResourceDefined(ServletContext context) {
        return getConfigJsonResource(context) == null;
    }

    protected String getConfigJsonResource(ServletContext context) {
        return context.getInitParameter(CORS_CONFIGURATION_RESOURCE_PARAM);
    }

    protected boolean isResourceFound(String resourcePath) {
        return getConfigStream(resourcePath) != null;
    }

    protected CorsConfiguration getConfig(String resourcePath) throws IOException {
        return new CorsConfiguration.Builder()
                .fromJson(getConfigStream(resourcePath))
                .build();
    }

    protected InputStream getConfigStream(String resourcePath) {
        return Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath);
    }
}
