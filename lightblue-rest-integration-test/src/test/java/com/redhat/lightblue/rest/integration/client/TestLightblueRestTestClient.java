package com.redhat.lightblue.rest.integration.client;

import static com.redhat.lightblue.util.test.AbstractJsonNodeTest.loadJsonNode;

import javax.ws.rs.NotAuthorizedException;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.lightblue.rest.integration.FakeIdentityManager;

public class TestLightblueRestTestClient extends LightblueRestTestClient {

    public TestLightblueRestTestClient() throws Exception {
        super(new FakeIdentityManager().add("fakeuser", "secret"));
    }

    @Override
    protected JsonNode[] getMetadataJsonNodes() throws Exception {
        return new JsonNode[]{
                loadJsonNode("./metadata/test.json")
        };
    }

    @Test(expected = NotAuthorizedException.class)
    public void testAuthenticationFails_NoCredsProvided() throws Exception {
        createRequest((ResteasyClient client) -> {
            return client
                    .target(getDataUrl())
                    .path("/find/test")
                    .queryParam("Q", "_id:abc");
            }).get(String.class);
    }

    @Test(expected = NotAuthorizedException.class)
    public void testAuthenticationFails_InvalidCreds() throws Exception {
        createRequest((ResteasyClient client) -> {
            return client
                    .target(getDataUrl())
                    .path("/find/test")
                    .queryParam("Q", "_id:abc")
                    .register(new BasicAuthentication("fakeuser", "wrong"));
            }).get(String.class);
    }

    @Test
    public void testAuthenticationPasses() throws Exception {
        createRequest((ResteasyClient client) -> {
            return client
                    .target(getDataUrl())
                    .path("/find/test")
                    .queryParam("Q", "_id:abc")
                    .register(new BasicAuthentication("fakeuser", "secret"));
            }).get(String.class);
    }

}
