package com.redhat.lightblue.rest.integration;

import com.redhat.lightblue.mongo.test.LightblueMongoTestHarness;
import com.redhat.lightblue.rest.RestConfiguration;
import com.restcompress.provider.LZFDecodingInterceptor;
import com.restcompress.provider.LZFEncodingInterceptor;
import com.sun.net.httpserver.HttpServer;
import org.jboss.resteasy.plugins.server.sun.http.HttpContextBuilder;
import org.junit.AfterClass;

import java.io.IOException;
import java.net.InetSocketAddress;

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
        httpPort = httpServerPort;
        dataUrl = "http://localhost:" + httpPort + "/rest/data";
        metadataUrl = "http://localhost:" + httpPort + "/rest/metadata";

        ensureHttpServerIsRunning();
    }

    public void ensureHttpServerIsRunning() throws IOException {
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
