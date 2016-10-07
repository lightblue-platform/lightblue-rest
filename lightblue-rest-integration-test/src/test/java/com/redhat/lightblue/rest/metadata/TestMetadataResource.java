package com.redhat.lightblue.rest.metadata;

import static com.redhat.lightblue.util.test.AbstractJsonNodeTest.loadResource;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.lightblue.rest.integration.client.LightblueRestTestClient;

public class TestMetadataResource extends LightblueRestTestClient {

    public TestMetadataResource() throws Exception {
        super();
    }

    @Override
    protected JsonNode[] getMetadataJsonNodes() throws Exception {
        return new JsonNode[]{};
    }

    @Test
    public void testCreateMetadata() throws Exception {
        String entity = StrSubstitutor.replaceSystemProperties(loadResource("./metadata/test.json"));
        String response = createRequest((ResteasyClient client) -> {
            return client
                    .target(getMetadataUrl())
                    .path("/test/1.0.0");
        }).put(Entity.entity(entity, MediaType.APPLICATION_JSON), String.class);

        assertNotNull(response);
        JSONAssert.assertEquals(
                "{\"entityInfo\":{\"name\":\"test\",\"defaultVersion\":\"1.0.0\",\"indexes\":[{\"name\":null,\"unique\":true,\"fields\":[{\"field\":\"_id\",\"dir\":\"$asc\",\"caseInsensitive\":false}]}],\"datastore\":{\"datasource\":\"mongo\",\"collection\":\"test\",\"backend\":\"mongo\"},\"_id\":\"test|\"},\"schema\":{\"name\":\"test\",\"version\":{\"value\":\"1.0.0\",\"changelog\":\"test metadata\"},\"status\":{\"value\":\"active\"},\"access\":{\"insert\":[\"anyone\"],\"update\":[\"anyone\"],\"find\":[\"anyone\"],\"delete\":[\"anyone\"]},\"fields\":{\"_id\":{\"type\":\"string\",\"description\":null,\"valueGenerator\":{\"type\":\"UUID\"}},\"value\":{\"type\":\"string\",\"description\":null},\"objectType\":{\"type\":\"string\",\"description\":null,\"access\":{\"find\":[\"anyone\"],\"update\":[\"noone\"]},\"constraints\":{\"required\":true,\"minLength\":1}}},\"_id\":\"test|1.0.0\"}}",
                response,
                true);
    }

}
