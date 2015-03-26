/*
 Copyright 2015 Red Hat, Inc. and/or its affiliates.

 This file is part of lightblue.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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

            InputStream configJsonStream = getConfigStream(configJsonResource);

            if (configJsonStream == null) {
                LOGGER.info("CORS json configuration not found at " + configJsonResource + ". CORS "
                        + "will not be enabled.");
                return;
            }

            corsFilter.register(context, getConfig(configJsonStream));
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
