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

import java.io.FileNotFoundException;
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
 * {@link PluginConfiguration} and {@link DataSourcesConfiguration},
 * then call {@link #appendToThreadClassLoader(PluginConfiguration)} prior to
 * instantiating an instance of {@link DataSourcesConfiguration} and passing it into
 * {@link #getFactory(DataSourcesConfiguration)}.</p>
 *
 * @author nmalik
 */
public final class RestConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestConfiguration.class);

    public static final String DATASOURCE_FILENAME = "datasources.json";
    public static final String PLUGIN_CONFIGURATION = "lightblue-plugins.json";

    private static DataSourcesConfiguration datasources;
    private static volatile LightblueFactory factory;

    private RestConfiguration() {}

    public static DataSourcesConfiguration getDatasources() {
        return datasources;
    }

    public static LightblueFactory getFactory(final DataSourcesConfiguration ds) {
        LightblueFactory f = factory;
        if (f == null) {
            synchronized (RestConfiguration.class) {
                if (factory == null) {
                    datasources = ds;
                    f = new LightblueFactory(ds);
                    factory = f;
                }
            }
        }
        return f;
    }

    public static LightblueFactory getFactory() {
        LightblueFactory f = factory;
        if (f == null) {
            return getFactory(loadDefaultPlugins());
        }
        return f;
    }

    public static LightblueFactory getFactory(
            final PluginConfiguration pluginConfiguration) {
        LightblueFactory f = factory;
        if (f == null) {
            synchronized (RestConfiguration.class) {
                f = factory;
                if (f == null) {
                    appendToThreadClassLoader(pluginConfiguration);
                    return getFactory(loadDefaultDatasources());
                }
            }
        }
        return f;
    }

    public static void setFactory(LightblueFactory f) {
        factory = f;
    }

    private static DataSourcesConfiguration loadDefaultDatasources() {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(DATASOURCE_FILENAME)) {
            if (null == is) {
                throw new FileNotFoundException(DATASOURCE_FILENAME);
            }
            return new DataSourcesConfiguration(JsonUtils.json(is,true));
        } catch (Exception e) {
            throw new RuntimeException("Cannot initialize datasources.", e);
        }
    }

    private static PluginConfiguration loadDefaultPlugins() {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(PLUGIN_CONFIGURATION)) {
            if (is == null) {
                //There are no external plugins, this is ok.
                return new PluginConfiguration();
            }
            return new PluginConfiguration(JsonUtils.json(is));
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize lightblue plugins.", e);
        }
    }

    private static void appendToThreadClassLoader(PluginConfiguration pluginConfiguration) {
        Set<URL> externalUrls = pluginConfiguration.getPluginUrls();

        if (pluginConfiguration == null || externalUrls.isEmpty()) {
            //No external resources provided, this is ok.
            return;
        }

        //TODO Check that urls are not already on class path?

        LOGGER.info("Adding url to classpath: " + pluginConfiguration.toString());

        ClassLoader currentThreadLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader cl = new URLClassLoader(externalUrls.toArray(new URL[0]), currentThreadLoader);

        Thread.currentThread().setContextClassLoader(cl);
    }

}
