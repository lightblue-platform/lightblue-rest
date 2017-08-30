/*
 Copyright 2013 Red Hat, Inc. and/or its affiliates.

 This file is part of lightblue.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.redhat.lightblue.rest.crud.cmd;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import javax.ws.rs.WebApplicationException;

import org.eclipse.jetty.io.WriterOutputStream;
import org.junit.Assert;
import org.junit.Test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.lightblue.crud.DocCtx;
import com.redhat.lightblue.crud.ListDocumentStream;
import com.redhat.lightblue.mediator.StreamingResponse;
import com.redhat.lightblue.util.JsonDoc;
import com.redhat.lightblue.util.metrics.DropwizardRequestMetrics;
import com.redhat.lightblue.util.metrics.NoopRequestMetrics;
import com.redhat.lightblue.util.metrics.RequestMetrics;

/**
 * @author nmalik
 */
public class FindCommandTest extends AbstractRestCommandTest {
	
    MetricRegistry metricsRegistry = new MetricRegistry();
    RequestMetrics requestMetrics = new DropwizardRequestMetrics(metricsRegistry);

    @Test
    public void runFindWithReturn() {
        FindCommand command = new FindCommand(mediator, "name", "version", "{\"request\":\"data\"}", new NoopRequestMetrics());

        String output = command.run().toString();

        Assert.assertNotNull(output);

        Assert.assertEquals("find", mediator.methodCalled);
    }

    @Test
    public void runFindWithParseProblem() {
        FindCommand command = new FindCommand(mediator, "name", "version", "{\"request\":\"invalid}", new NoopRequestMetrics());

        String output = command.run().toString();

        Assert.assertNotNull(output);

        Assert.assertTrue(output.contains("Error during the parse of the request"));
    }

    @Test
    public void runFindWithInvalid() {
        FindCommand command = new FindCommand(mediator, null, "version", "{\"request\":\"invalid\"}", new NoopRequestMetrics());

        String output = command.run().toString();

        Assert.assertNotNull(output);

        Assert.assertTrue(output.contains("Request is not valid"));
    }

    @Test
    public void runFindWithStream() throws WebApplicationException, IOException {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode rootDoc = mapper.createObjectNode().put("foo", "bar");
        ObjectNode projectedDoc = mapper.createObjectNode();

        DocCtx doc = new DocCtx(new JsonDoc(rootDoc));
        doc.setOutputDocument(new JsonDoc(projectedDoc));

        Assert.assertNotNull(doc.getRoot());
        Assert.assertNotNull(doc.getOutputDocument().getRoot());
        Assert.assertNotNull(doc.getRoot().get("foo"));
        Assert.assertNull(doc.getOutputDocument().getRoot().get("foo"));

        StreamingResponse sr = new StreamingResponse();
        sr.documentStream = new ListDocumentStream<>(Arrays.asList(new DocCtx[] { doc }));
        mediator.streamingResponse = sr;

        FindCommand command = new FindCommand(mediator, "name", "version", "{\"request\":\"data\"}", true, new NoopRequestMetrics());

        command.run();

        Assert.assertEquals("findAndStream", mediator.methodCalled);

        StringWriter sw = new StringWriter();
        command.getResponseStream().write(new WriterOutputStream(sw));

        Assert.assertTrue("Should return projected doc: {}, but the response is "+sw.toString(), sw.toString().endsWith("\"processed\":{}}"));
    }

    @Test
    public void runFindWithReturnAndMetrics() {
        FindCommand command = new FindCommand(mediator, "name", "version", "{\"request\":\"data\"}", requestMetrics);

        command.run();
        
        Counter activeRequestCounter = metricsRegistry.counter("api.find.name.version.requests.active");
        Timer completedRequestTimer = metricsRegistry.timer("api.find.name.version.requests.latency");
        
        Assert.assertEquals("find", mediator.methodCalled);
        Assert.assertEquals(0, activeRequestCounter.getCount());
        Assert.assertEquals(1, completedRequestTimer.getCount());
    }
    
    @Test
    public void runFindWithParseProblemAndMetrics() {
        FindCommand command = new FindCommand(mediator, "name", "version", "{\"request\":\"invalid}", requestMetrics);

        String output = command.run().toString();

        Counter activeRequestCounter = metricsRegistry.counter("api.find.name.version.requests.active");
        Timer completedRequestTimer = metricsRegistry.timer("api.find.name.version.requests.latency");
        Meter restExceptionMeter = metricsRegistry.meter("api.find.name.version.requests.exception.Error.rest-crud-RestFindError");
        
        Assert.assertTrue(output.contains("Error during the parse of the request"));
        Assert.assertEquals(0, activeRequestCounter.getCount());
        Assert.assertEquals(1, completedRequestTimer.getCount());
        Assert.assertEquals(1, restExceptionMeter.getCount());
    }

    @Test
    public void runFindWithInvalidAndMetrics() {
        FindCommand command = new FindCommand(mediator, null, "version", "{\"request\":\"invalid\"}", requestMetrics);

        String output = command.run().toString();

        Counter activeRequestCounter = metricsRegistry.counter("api.find.version.requests.active");
        Timer completedRequestTimer = metricsRegistry.timer("api.find.version.requests.latency");
        Meter restExceptionMeter = metricsRegistry.meter("api.find.version.requests.exception.Error.rest-crud-RestFindError");
        
        Assert.assertTrue(output.contains("Request is not valid"));
        Assert.assertEquals(0, activeRequestCounter.getCount());
        Assert.assertEquals(1, completedRequestTimer.getCount());
        Assert.assertEquals(1, restExceptionMeter.getCount());
    }
    
    @Test
    public void runStreamFindWithParseProblemAndMetrics() {
        FindCommand command = new FindCommand(mediator, "name", "version", "{\"request\":\"invalid}", true, requestMetrics);

        String output = command.run().toString();

        Counter activeRequestCounter = metricsRegistry.counter("api.stream.find.name.version.requests.active");
        Timer completedRequestTimer = metricsRegistry.timer("api.stream.find.name.version.requests.latency");
        Meter restExceptionMeter = metricsRegistry.meter("api.stream.find.name.version.requests.exception.Error.rest-crud-RestFindError");
        
        Assert.assertTrue(output.contains("Error during the parse of the request"));
        Assert.assertEquals(0, activeRequestCounter.getCount());
        Assert.assertEquals(1, completedRequestTimer.getCount());
        Assert.assertEquals(1, restExceptionMeter.getCount());
    }

    @Test
    public void runStreamFindWithInvalidAndMetrics() {
        FindCommand command = new FindCommand(mediator, null, "version", "{\"request\":\"invalid\"}", true, requestMetrics);

        String output = command.run().toString();

        Counter activeRequestCounter = metricsRegistry.counter("api.stream.find.version.requests.active");
        Timer completedRequestTimer = metricsRegistry.timer("api.stream.find.version.requests.latency");
        Meter restExceptionMeter = metricsRegistry.meter("api.stream.find.version.requests.exception.Error.rest-crud-RestFindError");
        
        Assert.assertTrue(output.contains("Request is not valid"));
        Assert.assertEquals(0, activeRequestCounter.getCount());
        Assert.assertEquals(1, completedRequestTimer.getCount());
        Assert.assertEquals(1, restExceptionMeter.getCount());
    }
    
    @Test
    public void runStreamFindWithMetrics() throws WebApplicationException, IOException {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode rootDoc = mapper.createObjectNode().put("foo", "bar");
        ObjectNode projectedDoc = mapper.createObjectNode();

        DocCtx doc = new DocCtx(new JsonDoc(rootDoc));
        doc.setOutputDocument(new JsonDoc(projectedDoc));

        StreamingResponse sr = new StreamingResponse();
        sr.documentStream = new ListDocumentStream<>(Arrays.asList(new DocCtx[] { doc }));
        mediator.streamingResponse = sr;

        FindCommand command = new FindCommand(mediator, "name", "version", "{\"request\":\"data\"}", true, requestMetrics);

        Counter activeRequestCounter = metricsRegistry.counter("api.stream.find.name.version.requests.active");
        Timer completedRequestTimer = metricsRegistry.timer("api.stream.find.name.version.requests.latency");
        
        command.run();
        
        Assert.assertEquals(1, activeRequestCounter.getCount());
        Assert.assertEquals(0, completedRequestTimer.getCount());
        Assert.assertEquals("findAndStream", mediator.methodCalled);

        StringWriter sw = new StringWriter();
        command.getResponseStream().write(new WriterOutputStream(sw));

        Assert.assertEquals(0, activeRequestCounter.getCount());
        Assert.assertEquals(1, completedRequestTimer.getCount());
    }    
}
