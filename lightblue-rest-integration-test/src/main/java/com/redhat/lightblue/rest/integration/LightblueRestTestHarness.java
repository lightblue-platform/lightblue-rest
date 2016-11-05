package com.redhat.lightblue.rest.integration;

import java.io.IOException;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;

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
import io.undertow.security.idm.IdentityManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;

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

    private final static String DEFAULT_HOST = "localhost";
    private final static int DEFAULT_PORT = 8000;

    private static UndertowJaxrsServer httpServer;

    private final int httpPort;
    private final String dataUrl;
    private final String metadataUrl;
    private IdentityManager identityManager;

    @AfterClass
    public static void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop();
        }
        httpServer = null;
    }

    public String getHttpHost() {
        return DEFAULT_HOST;
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

    public String getDataContextPath() {
        return DEFAULT_CONTEXT_PATH_DATA;
    }

    public String getMetadataContextPath() {
        return DEFAULT_CONTEXT_PATH_METADATA;
    }

    /**
     * {@link IdentityManager} used for authentication. By default the value is <code>null</code> which
     * is the same as no authentication required.
     * @return {@link IdentityManager}.
     */
    public IdentityManager getIdentityManager() {
        return identityManager;
    }

    /**
     * <p>{@link IdentityManager} type can be changed, but a change will not take effect until the application
     * server is restarted.</p>
     * <p>A <code>null</code> value is the same as no authentication.
     * <p>To restart application server, call {@link #stopHttpServer()} and then {@link #ensureHttpServerIsRunning()}.</p>
     * @param identityManager - implementation of {@link IdentityManager}.
     */
    public void setIdentityManager(IdentityManager identityManager) {
        this.identityManager = identityManager;
    }

    public LightblueRestTestHarness() throws Exception {
        this(DEFAULT_PORT);
    }

    public LightblueRestTestHarness(IdentityManager identityManager) throws Exception {
        this(DEFAULT_PORT, identityManager);
    }

    /**
     * Setup lightblue backend with provided schemas and rest endpoints.
     *
     * @param httpServerPort port used for http (rest endpoints)
     * @throws Exception
     */
    public LightblueRestTestHarness(int httpServerPort) throws Exception {
        this(httpServerPort, null);
    }

    public LightblueRestTestHarness(int httpServerPort, IdentityManager identityManager) throws Exception {
        super();
        httpPort = httpServerPort;
        this.identityManager = identityManager;

        dataUrl = new StringBuffer("http://")
                .append(getHttpHost())
                .append(':')
                .append(getHttpPort())
                .append(getDataContextPath())
                .toString();

        metadataUrl = new StringBuffer("http://")
                .append(getHttpHost())
                .append(':')
                .append(getHttpPort())
                .append(getMetadataContextPath())
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
                    .addHttpListener(getHttpPort(), getHttpHost());

            httpServer = new UndertowJaxrsServer();
            httpServer.start(builder);

            DeploymentInfo dataDeploymentInfo = httpServer.undertowDeployment(dataDeployment);
            configureDeployment(dataDeploymentInfo, "data", getDataContextPath());

            DeploymentInfo metadataDeploymentInfo = httpServer.undertowDeployment(metadataDeployment);
            configureDeployment(metadataDeploymentInfo, "metadata", getMetadataContextPath());

            //Optionally require authentication, by default none is used.
            if (getIdentityManager() != null) {
                configureDeploymentSecurity(dataDeploymentInfo);
                configureDeploymentSecurity(metadataDeploymentInfo);
            }

            httpServer.deploy(dataDeploymentInfo);
            httpServer.deploy(metadataDeploymentInfo);
        }
    }

    private void configureDeployment(DeploymentInfo deploymentInfo, String name, String contextPath) {
        deploymentInfo.setClassLoader(getClass().getClassLoader());
        deploymentInfo.setDeploymentName(name);
        deploymentInfo.setContextPath(contextPath);
        deploymentInfo.addFilter(Servlets.filter(name + "LoggingFilter", LoggingFilter.class));
        deploymentInfo.addFilterUrlMapping(name + "LoggingFilter", "/*", DispatcherType.REQUEST);
    }

    private void configureDeploymentSecurity(DeploymentInfo deploymentInfo) {
        deploymentInfo.setIdentityManager(identityManager);
        deploymentInfo.setLoginConfig(new LoginConfig(HttpServletRequest.BASIC_AUTH, "lightblueRealm"));
    }

}
