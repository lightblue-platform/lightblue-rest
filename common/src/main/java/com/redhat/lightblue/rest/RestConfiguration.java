/*
 Copyright 2013 Red Hat, Inc. and/or its affiliates.

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
package com.redhat.lightblue.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.lightblue.config.DataSourcesConfiguration;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.util.JsonUtils;

/**
 * <p>Initialization logic for RestApplication.</p>
 *
 * <p><b>NOTE:</b> In order to guarentee consistent behavior, if it is desirable to specify both the
 * {@link ExternalResourceConfiguration} and {@link DataSourcesConfiguration},
 * then call {@link #appendToThreadClassLoader(ExternalResourceConfiguration)} prior to
 * instantiating an instance of {@link DataSourcesConfiguration} and passing it into
 * {@link #getFactory(DataSourcesConfiguration)}.</p>
 *
 * @author nmalik
 */
public final class RestConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestConfiguration.class);

    public static final String DATASOURCE_FILENAME = "datasources.json";
    public static final String EXTERNAL_RESOURCE_CONFIGURATION = "lightblue-external-resources.json";

    private static DataSourcesConfiguration datasources;
    private static LightblueFactory factory;

    private RestConfiguration() {}

    public static DataSourcesConfiguration getDatasources() {
        return datasources;
    }

    private synchronized static LightblueFactory createFactory(final DataSourcesConfiguration ds) {
        datasources = ds;
        factory = new LightblueFactory(ds);
        return factory;
    }

    public static LightblueFactory getFactory() {
        return getFactory(loadDefaultExternalResources());
    }

    public static LightblueFactory getFactory(
            final ExternalResourceConfiguration externalResources) {
        appendToThreadClassLoader(externalResources);

        return getFactory(loadDefaultDatasources());
    }

    public static LightblueFactory getFactory(
            final DataSourcesConfiguration ds) {
        if (factory == null) {
            return createFactory(ds);
        }
        return factory;
    }

    public static void setFactory(LightblueFactory f) {
        factory = f;
    }

    private static DataSourcesConfiguration loadDefaultDatasources() {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(DATASOURCE_FILENAME)) {
            return new DataSourcesConfiguration(JsonUtils.json(is,true));
        } catch (Exception e) {
            throw new RuntimeException("Cannot initialize datasources.", e);
        }
    }

    private static ExternalResourceConfiguration loadDefaultExternalResources() {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(EXTERNAL_RESOURCE_CONFIGURATION)) {
            if (is == null) {
                //There are no external resources, this is ok.
                return new ExternalResourceConfiguration();
            }
            return new ExternalResourceConfiguration(JsonUtils.json(is));
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize external resources.", e);
        }
    }

    public static void appendToThreadClassLoader(ExternalResourceConfiguration externalResources) {
        Set<URL> externalUrls = externalResources.getExternalUrls();

        if (externalResources == null || externalUrls.isEmpty()) {
            //No external resources provided, this is ok.
            return;
        }

        //TODO Check that urls are not already on class path?

        ClassLoader currentThreadLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader cl = new URLClassLoader(externalUrls.toArray(new URL[0]), currentThreadLoader);

        Thread.currentThread().setContextClassLoader(cl);
    }

}
