package com.redhat.lightblue.rest.integration;

import javax.ws.rs.core.UriBuilder;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;

public abstract class LightblueRestTestClient extends LightblueRestTestHarness {

    public LightblueRestTestClient() throws Exception {
        super();
        setupSystemProperties();
    }

    public LightblueRestTestClient(int httpServerPort) throws Exception {
        super(httpServerPort);
        setupSystemProperties();
    }

    private void setupSystemProperties() {
        System.setProperty("client.data.url", getDataUrl());
        System.setProperty("client.metadata.url", getMetadataUrl());
    }

    protected ClientRequest createRequest(String baseUrl, String relativePath) {
        return new ClientRequestFactory(UriBuilder.fromUri(baseUrl).build())
                .createRelativeRequest(relativePath);
    }

    /**
     * Creates a {@link ClientRequest} that speaks to the data api.
     * @param relativePath - eg. /find/myEntity?Q=_id:1"
     * @return {@link ClientRequest}
     */
    public ClientRequest createDataRequest(String relativePath) {
        return createRequest(getDataUrl(), relativePath);
    }

    /**
     * Creates a {@link ClientRequest} that speaks to the metadata api.
     * @param relativePath - eg. /myEntity/entityVersion"
     * @return {@link ClientRequest}
     */
    public ClientRequest createMetadataRequest(String relativePath) {
        return createRequest(getMetadataUrl(), relativePath);
    }

}
