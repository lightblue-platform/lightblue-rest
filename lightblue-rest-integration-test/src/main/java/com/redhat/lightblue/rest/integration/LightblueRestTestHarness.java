package com.redhat.lightblue.rest.integration;

import java.io.IOException;

import javax.servlet.DispatcherType;

import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.junit.AfterClass;

import com.redhat.lightblue.mongo.test.LightblueMongoTestHarness;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.rest.crud.CrudResource;
import com.redhat.lightblue.rest.logging.LoggingFilter;
import com.redhat.lightblue.rest.metadata.MetadataResource;
import com.restcompress.provider.LZFDecodingInterceptor;
import com.restcompress.provider.LZFEncodingInterceptor;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;

/**
 * <p>
 * Utility class for adding rest layer on top of mongo CRUD Controller. Extend
 * this class when writing tests.</p>
 *
 * <p>
 * <b>NOTE:</b> {@link com.redhat.lightblue.rest.PluginConfiguration} cannot be
 * used with the {@link LightblueRestTestHarness} as the expectation is you
 * should never be writing test cases for libraries not in a test scope.</p>
 *
 * @author mpatercz
 *
 */
public abstract class LightblueRestTestHarness extends LightblueMongoTestHarness {

    private static final String DEFAULT_CONTEXT_PATH_METADATA = "/rest/metadata";
    private static final String DEFAULT_CONTEXT_PATH_DATA = "/rest/data";

    private final static int DEFAULT_PORT = 8000;

    private static UndertowJaxrsServer httpServer;

    private final String httpHost;
    private final int httpPort;
    private final String dataUrl;
    private final String metadataUrl;

    @AfterClass
    public static void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop();
        }
        httpServer = null;
    }

    public String getHttpHost() {
        return httpHost;
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

    public LightblueRestTestHarness() throws Exception {
        this(DEFAULT_PORT);
    }

    /**
     * Setup lightblue backend with provided schemas and rest endpoints.
     *
     * @param httpServerPort port used for http (rest endpoints)
     * @throws Exception
     */
    public LightblueRestTestHarness(int httpServerPort) throws Exception {
        super();
        httpHost = "localhost";
        httpPort = httpServerPort;

        dataUrl = new StringBuffer("http://")
                .append(httpHost)
                .append(':')
                .append(httpPort)
                .append(DEFAULT_CONTEXT_PATH_DATA)
                .toString();

        metadataUrl = new StringBuffer("http://")
                .append(httpHost)
                .append(':')
                .append(httpPort)
                .append(DEFAULT_CONTEXT_PATH_METADATA)
                .toString();

        ensureHttpServerIsRunning();
    }

    public void ensureHttpServerIsRunning() throws IOException {
        if (httpServer == null) {
            RestConfiguration.setFactory(getLightblueFactory());

            ResteasyDeployment dataDeployment = new ResteasyDeployment();
            dataDeployment.getActualResourceClasses().add(CrudResource.class);
            dataDeployment.getActualProviderClasses().add(LZFEncodingInterceptor.class);
            dataDeployment.getActualProviderClasses().add(LZFDecodingInterceptor.class);

            ResteasyDeployment metadataDeployment = new ResteasyDeployment();
            metadataDeployment.getActualResourceClasses().add(MetadataResource.class);
            metadataDeployment.getActualProviderClasses().add(LZFEncodingInterceptor.class);
            metadataDeployment.getActualProviderClasses().add(LZFDecodingInterceptor.class);

            Undertow.Builder builder = Undertow.builder()
                    .addHttpListener(httpPort, httpHost);

            httpServer = new UndertowJaxrsServer();
            httpServer.start(builder);

            DeploymentInfo dataDeploymentInfo = httpServer.undertowDeployment(dataDeployment);
            dataDeploymentInfo.setClassLoader(getClass().getClassLoader());
            dataDeploymentInfo.setDeploymentName("data");
            dataDeploymentInfo.setContextPath(DEFAULT_CONTEXT_PATH_DATA);
            dataDeploymentInfo.addFilter(Servlets.filter("DataLoggingFilter", LoggingFilter.class));
            dataDeploymentInfo.addFilterUrlMapping("DataLoggingFilter", "/*", DispatcherType.REQUEST);

            DeploymentInfo metadataDeploymentInfo = httpServer.undertowDeployment(metadataDeployment);
            metadataDeploymentInfo.setClassLoader(getClass().getClassLoader());
            metadataDeploymentInfo.setDeploymentName("metadata");
            metadataDeploymentInfo.setContextPath(DEFAULT_CONTEXT_PATH_METADATA);
            metadataDeploymentInfo.addFilter(Servlets.filter("MetadataLoggingFilter", LoggingFilter.class));
            metadataDeploymentInfo.addFilterUrlMapping("MetadataLoggingFilter", "/*", DispatcherType.REQUEST);

            httpServer.deploy(dataDeploymentInfo);
            httpServer.deploy(metadataDeploymentInfo);
        }
    }

}
