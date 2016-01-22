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

import com.redhat.lightblue.config.DataSourcesConfiguration;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.util.JsonUtils;
import java.io.InputStream;

/**
 * Moving initialization logic out of RestApplication.
 *
 * @author nmalik
 */
public final class RestConfiguration {

    public static final String DATASOURCE_FILENAME = "datasources.json";

    private static DataSourcesConfiguration datasources;
    private static LightblueFactory factory;

    private RestConfiguration() {}

    public static DataSourcesConfiguration getDatasources() {
        return datasources;
    }

    public synchronized static LightblueFactory createFactory(final DataSourcesConfiguration ds) {
        datasources = ds;
        factory = new LightblueFactory(ds);
        return factory;
    }

    public static LightblueFactory getFactory() {
        if (factory == null) {
            return createFactory(loadDefaultDatasources());
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

}
