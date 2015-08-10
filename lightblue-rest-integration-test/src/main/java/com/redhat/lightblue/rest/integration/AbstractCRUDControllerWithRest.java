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
package com.redhat.lightblue.rest.integration;

import java.net.InetSocketAddress;

import org.jboss.resteasy.plugins.server.sun.http.HttpContextBuilder;
import org.junit.AfterClass;

import com.redhat.lightblue.mongo.test.AbstractMongoCRUDTestController;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.rest.crud.CrudResource;
import com.redhat.lightblue.rest.metadata.MetadataResource;
import com.restcompress.provider.LZFDecodingInterceptor;
import com.restcompress.provider.LZFEncodingInterceptor;
import com.sun.net.httpserver.HttpServer;

/**
 * Utility class for adding rest layer on top of mongo CRUD Controller. Extend this class when writing tests.
 *
 * @author mpatercz
 *
 */
public abstract class AbstractCRUDControllerWithRest extends AbstractMongoCRUDTestController {

    private final static int DEFAULT_PORT = 8000;

    private static HttpServer httpServer;

    private final int httpPort;
    private final String dataUrl;
    private final String metadataUrl;

    @AfterClass
    public static void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        httpServer = null;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public String getDataUrl() {
        return dataUrl;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public AbstractCRUDControllerWithRest() throws Exception {
        this(DEFAULT_PORT);
    }

    /**
     * Setup lightblue backend with provided schemas and rest endpoints.
     *
     * @param httpServerPort
     *            port used for http (rest endpoints)
     * @throws Exception
     */
    public AbstractCRUDControllerWithRest(int httpServerPort) throws Exception {
        super();
        httpPort = httpServerPort;
        dataUrl = "http://localhost:" + httpPort + "/rest/data";
        metadataUrl = "http://localhost:" + httpPort + "/rest/metadata";

        if (httpServer == null) {
            RestConfiguration.setFactory(getLightblueFactory());

            httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);

            HttpContextBuilder dataContext = new HttpContextBuilder();
            dataContext.getDeployment().getActualResourceClasses().add(CrudResource.class);
            dataContext.getDeployment().getActualProviderClasses().add(LZFEncodingInterceptor.class);
            dataContext.getDeployment().getActualProviderClasses().add(LZFDecodingInterceptor.class);
            dataContext.setPath("/rest/data");
            dataContext.bind(httpServer);

            HttpContextBuilder metadataContext = new HttpContextBuilder();
            metadataContext.getDeployment().getActualResourceClasses().add(MetadataResource.class);
            metadataContext.getDeployment().getActualProviderClasses().add(LZFEncodingInterceptor.class);
            metadataContext.getDeployment().getActualProviderClasses().add(LZFDecodingInterceptor.class);
            metadataContext.setPath("/rest/metadata");
            metadataContext.bind(httpServer);

            httpServer.start();
        }
    }

}
