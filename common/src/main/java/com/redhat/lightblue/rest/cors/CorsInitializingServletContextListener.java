package com.redhat.lightblue.rest.cors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;
import java.io.InputStream;

public class CorsInitializingServletContextListener implements ServletContextListener {
    static final String CORS_CONFIGURATION_RESOURCE_PARAM = "cors.configuration.resource";

    private static final Logger LOGGER = LoggerFactory.getLogger(CorsInitializingServletContextListener.class);

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
            String configJsonResource = getConfigJsonResource(context);

            if (configJsonResource == null) {
                LOGGER.info("No CORS json configuration resource location provided. CORS will not "
                        + "be enabled.");
                return;
            }

            try (InputStream configJsonStream = getConfigStream(configJsonResource)) {
                if (configJsonStream == null) {
                    LOGGER.info("CORS json configuration not found at " + configJsonResource + ". CORS "
                            + "will not be enabled.");
                    return;
                }

                CorsConfiguration config = getConfig(configJsonStream);
                corsFilter.register(context, config);

                LOGGER.info("Enabled CORS with configuration found at '" + configJsonResource + "': "
                        + config);
            }
        } catch (IOException e) {
            LOGGER.error("Error reading CORS configuration file. CORS will not be enabled.", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }

    protected String getConfigJsonResource(ServletContext context) {
        return context.getInitParameter(CORS_CONFIGURATION_RESOURCE_PARAM);
    }

    protected CorsConfiguration getConfig(InputStream configJsonStream) throws IOException {
        return new CorsConfiguration.Builder()
                .fromJson(configJsonStream)
                .build();
    }

    protected InputStream getConfigStream(String resourcePath) {
        return Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath);
    }
}
