package com.redhat.lightblue.rest.crud;

import static com.redhat.lightblue.util.test.AbstractJsonNodeTest.loadJsonNode;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.lightblue.rest.integration.client.LightblueRestTestClient;

public class TestCrudResource extends LightblueRestTestClient {

    public TestCrudResource() throws Exception {
        super();
    }

    @Override
    protected JsonNode[] getMetadataJsonNodes() throws Exception {
        return new JsonNode[]{
                loadJsonNode("metadata/test.json")
        };
    }

    @Before
    public void before() throws Exception {
        cleanupMongoCollections("test");
    }

    protected String insertTestEntry(String id, String value) throws Exception {
        Map<String, Object> entityMap = new HashMap<>();
        entityMap.put("objectType", "test");

        Map<String, Object> dataMap = new HashMap<>();
        if (id != null) {
            dataMap.put("_id", id);
        }
        if (value != null) {
            dataMap.put("value", value);
        }
        entityMap.put("data", dataMap);

        Map<String, Object> projectionMap = new HashMap<>();
        projectionMap.put("field", "*");
        projectionMap.put("include", true);
        projectionMap.put("recursive", true);
        entityMap.put("projection", projectionMap);

        String response = createRequest((ResteasyClient client) -> {
            return client
                    .target(getDataUrl())
                    .path("/insert/test");
        }).put(Entity.entity(
                    new ObjectMapper().writeValueAsString(entityMap),
                    MediaType.APPLICATION_JSON),
                String.class);

        assertNotNull(response);
        JSONAssert.assertEquals(
                "{\"status\":\"COMPLETE\",\"modifiedCount\":1,\"matchCount\":0}",
                response, false);
        return response;
    }

    @Test
    public void testFindSimple() throws Exception {
        insertTestEntry("abc", "sample");

        String response = createRequest((ResteasyClient client) -> {
            return client
                    .target(getDataUrl())
                    .path("/find/test")
                    .queryParam("Q", "_id:abc");
        }).get(String.class);

        assertNotNull(response);
        JSONAssert.assertEquals(
                "{\"status\":\"COMPLETE\",\"modifiedCount\":0,\"matchCount\":1}",
                response, false);
    }

}
