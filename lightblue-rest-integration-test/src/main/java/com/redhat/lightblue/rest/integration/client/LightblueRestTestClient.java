package com.redhat.lightblue.rest.integration.client;

import javax.ws.rs.client.Invocation;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import com.redhat.lightblue.rest.integration.LightblueRestTestHarness;

import io.undertow.security.idm.IdentityManager;

public abstract class LightblueRestTestClient extends LightblueRestTestHarness {

    public LightblueRestTestClient() throws Exception {
        super();
        setupSystemProperties();
    }

    public LightblueRestTestClient(int httpServerPort) throws Exception {
        super(httpServerPort);
        setupSystemProperties();
    }

    public LightblueRestTestClient(IdentityManager identityManager) throws Exception {
        super(identityManager);
    }

    private void setupSystemProperties() {
        System.setProperty("client.data.url", getDataUrl());
        System.setProperty("client.metadata.url", getMetadataUrl());
    }

    protected ResteasyClient createClient() {
        return new ResteasyClientBuilder()
                .defaultProxy(getHttpHost(), getHttpPort())
                .build();
    }

    public Invocation.Builder createRequest(ResteasyWebTargetBuilder builder) {
        return builder.build(createClient()).request();
    }

    public interface ResteasyWebTargetBuilder {
        ResteasyWebTarget build(ResteasyClient client);
    }

}
