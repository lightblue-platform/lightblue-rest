package com.redhat.lightblue.rest.logging;

import static com.redhat.lightblue.util.test.AbstractJsonNodeTest.loadJsonNode;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.lightblue.rest.integration.LightblueRestTestClient;

public class TestLoggingFilter extends LightblueRestTestClient {

    public TestLoggingFilter() throws Exception {
        super();
    }

    @Override
    protected JsonNode[] getMetadataJsonNodes() throws Exception {
        return new JsonNode[]{
                loadJsonNode("metadata/test.json")
        };
    }

    @Test
    public void testResponseForRequestUuidHeader() throws Exception {
        ClientRequest request = createDataRequest("/find/test?Q=_id:abc");
        ClientResponse<String> response = request.get(String.class);
        assertNotNull(response);
        String entity = response.getEntity(String.class);
        assertNotNull(entity);
        assertTrue(response.getHeaders().containsKey(LoggingFilter.HEADER_REQUEST_UUID));
        assertNotNull(response.getHeaders().get(LoggingFilter.HEADER_REQUEST_UUID));
    }

}
