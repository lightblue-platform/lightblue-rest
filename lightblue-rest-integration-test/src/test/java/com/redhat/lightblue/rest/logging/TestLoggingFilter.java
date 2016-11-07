package com.redhat.lightblue.rest.logging;

import static com.redhat.lightblue.util.test.AbstractJsonNodeTest.loadJsonNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;

import org.apache.log4j.spi.LoggingEvent;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.lightblue.rest.integration.FakeIdentityManager;
import com.redhat.lightblue.rest.integration.client.LightblueRestTestClient;
import com.redhat.lightblue.rest.integration.util.CapturableLogAppender;

public class TestLoggingFilter extends LightblueRestTestClient {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private static CapturableLogAppender logAppender;

    @BeforeClass
    public static void beforeClass() {
        logAppender = CapturableLogAppender.createInstanceFor(LoggingFilter.class);
    }

    @Before
    public void before() {
        logAppender.clear();
    }

    @AfterClass
    public static void afterClass() {
        CapturableLogAppender.removeAppenderFor(logAppender, LoggingFilter.class);
    }

    public TestLoggingFilter() throws Exception {
        super(new FakeIdentityManager().add("fakeuser", "secret"));
    }

    @Override
    protected JsonNode[] getMetadataJsonNodes() throws Exception {
        return new JsonNode[]{
                loadJsonNode("metadata/test.json")
        };
    }

    @Test
    public void testResponseForRequestIDHeader() throws Exception {
        Response response = createRequest((ResteasyClient client) -> {
            return client
                    .target(getDataUrl())
                    .path("/find/test")
                    .queryParam("Q", "_id:abc")
                    .register(new BasicAuthentication("fakeuser", "secret"));
        }).get();

        assertNotNull(response);
        assertTrue(response.getHeaders().containsKey(LoggingFilter.HEADER_REQUEST_ID));
        assertNotNull(response.getHeaders().get(LoggingFilter.HEADER_REQUEST_ID));
    }

    @Test
    public void testLogsForRequestID() throws Exception {
        exception.expect(InternalServerErrorException.class);

        try{
            createRequest((ResteasyClient client) -> {
                return client
                        .target(getDataUrl())
                        .path("/find/fake")
                        .queryParam("Q", "_id:abc")
                        .register(new BasicAuthentication("fakeuser", "secret"));
            }).get(String.class);
        }
        catch(Exception e) {
            assertFalse(logAppender.getEvents().isEmpty());
            boolean atLeastOne = false;
            for (LoggingEvent le : logAppender.getEvents()) {
                if (le.getRenderedMessage().contains("log for testing")) {
                    assertNotNull(le.getRenderedMessage(), le.getMDC(LoggingFilter.HEADER_REQUEST_ID));
                    atLeastOne = true;
                }
            }
            assertTrue(atLeastOne);
            throw e;
        }
    }

    @Test
    public void testLogsForPrincipal() throws Exception {
        exception.expect(InternalServerErrorException.class);

        try{
            createRequest((ResteasyClient client) -> {
                return client
                        .target(getDataUrl())
                        .path("/find/fake")
                        .queryParam("Q", "_id:abc")
                        .register(new BasicAuthentication("fakeuser", "secret"));
            }).get(String.class);
        }
        catch(Exception e) {
            assertFalse(logAppender.getEvents().isEmpty());
            boolean atLeastOne = false;
            for (LoggingEvent le : logAppender.getEvents()) {
                if (le.getRenderedMessage().contains("log for testing")) {
                    assertEquals(le.getRenderedMessage(), "fakeuser", le.getMDC(LoggingFilter.HEADER_REQUEST_PRINCIPAL));
                    atLeastOne = true;
                }
            }
            assertTrue(atLeastOne);
            throw e;
        }
    }

}
