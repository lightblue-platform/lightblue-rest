package com.redhat.lightblue.rest.crud;

import static com.redhat.lightblue.util.test.AbstractJsonNodeTest.loadJsonNode;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.lightblue.rest.integration.LightblueRestTestClient;

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

    protected <T> ClientResponse<T> insertTestEntry(String id, String value, Class<T> type) throws Exception {
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

        ClientRequest request = createDataRequest("/insert/test");
        request.body(MediaType.APPLICATION_JSON, new ObjectMapper().writeValueAsString(entityMap));
        ClientResponse<T> response = request.put(type);
        assertNotNull(response);
        assertNotNull(response.getEntity());
        return response;
    }

    @Test
    public void testFindSimple() throws Exception {
        insertTestEntry("abc", "sample", String.class);

        ClientRequest request = createDataRequest("/find/test?Q=_id:abc");
        ClientResponse<String> response = request.get(String.class);
        assertNotNull(response);
        String entity = response.getEntity(String.class);
        assertNotNull(entity);
    }

}
