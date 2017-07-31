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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import static com.codahale.metrics.MetricRegistry.name;

import java.io.IOException;

import javax.ws.rs.core.StreamingOutput;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.Response;
import com.redhat.lightblue.crud.FindRequest;
import com.redhat.lightblue.crud.DocCtx;
import com.redhat.lightblue.mediator.Mediator;
import com.redhat.lightblue.mediator.StreamingResponse;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.crud.RestCrudConstants;
import com.redhat.lightblue.rest.crud.metrics.MetricsInstrumentator;
import com.redhat.lightblue.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author nmalik
 */
public class FindCommand extends AbstractRestCommand implements MetricsInstrumentator{
    private static final Logger LOGGER = LoggerFactory.getLogger(FindCommand.class);

    private final String entity;
    private final String version;
    private final String request;
    private final boolean stream;

    private StreamingResponse streamResponse;
    
    private String metricNamespace;
	private Counter activeRequests;
	private Timer requestTimer;

    public FindCommand(String entity, String version, String request) {
        this(null,entity,version,request,false);
    }
        
    public FindCommand(String entity, String version, String request, boolean stream) {
        this(null, entity, version, request,stream);
    }

    public FindCommand(Mediator mediator, String entity, String version, String request) {
        this(mediator,entity,version,request,false);
    }
    
    public FindCommand(Mediator mediator, String entity, String version, String request, boolean stream) {
        super(mediator);
        this.entity = entity;
        this.version = version;
        this.request = request;
        this.stream=stream;
        this.metricNamespace=getSuccessMetricsNamespace("find", entity, version);
        initializeMetrics(metricNamespace);
    }
    
    @Override
	public void initializeMetrics(String merticNamespace) {
		this.activeRequests = metricsRegistry.counter(name(merticNamespace, "activeRequests"));
		this.requestTimer = metricsRegistry.timer(name(merticNamespace, "requests"));
	}   

    /**
     * The streaming protocol:
     * <pre>
     *   {
     *     ... ; result, without document data, or metadata
     *   }
     *   {
     *      processed: { document },
     *      resultMetadata: { metadata }
     *   }
     *    ...
     *  {
     *      last: true
     *      processed: { document },
     *      resultMetadata: { metadata }
     *   }
     * </pre>
     */
    public StreamingOutput getResponseStream() {
        return new StreamingOutput() {            
            @Override
            public void write(OutputStream os) throws IOException {
                // Send the header
                Writer writer=new OutputStreamWriter(os);
                writer.write(streamResponse.toJson().toString());
                writer.flush();

                try {
                    // Send the docs
                    while(streamResponse.documentStream.hasNext()) {
                        DocCtx doc=streamResponse.documentStream.next();
                        ObjectNode chunkNode=JsonNodeFactory.instance.objectNode();
                        if(!streamResponse.documentStream.hasNext()) {
                            chunkNode.set("last",JsonNodeFactory.instance.booleanNode(true));
                        }
                        chunkNode.set("processed",doc.getRoot());
                        if(doc.getResultMetadata()!=null)
                            chunkNode.set("resultMetadata",doc.getResultMetadata().toJson());
                        writer.write(chunkNode.toString());
                    }
                    writer.flush();
                } finally {
                    streamResponse.documentStream.close();
                }
            }
        };
    }
    

    
    @Override
    public CallStatus run() {
    	activeRequests.inc();
    	final Timer.Context context = requestTimer.time();
        LOGGER.debug("run: entity={}, version={}", entity, version);
        Error.reset();
        Error.push("rest");
        Error.push(getClass().getSimpleName());
        Error.push(entity);
        try {
            FindRequest ireq;
            try {
                ireq = getJsonTranslator().parse(FindRequest.class, JsonUtils.json(request));
            } catch (Exception e) {
                metricsRegistry.meter(getErrorMetricsNamespace(metricNamespace, e)).mark();
                LOGGER.error("find:parse failure: {}", e);
                return new CallStatus(Error.get(RestCrudConstants.ERR_REST_FIND, "Error during the parse of the request"));
            }
            LOGGER.debug("Find request:{}", ireq);
            try {
                validateReq(ireq, entity, version);
            } catch (Exception e) {
                metricsRegistry.meter(getErrorMetricsNamespace(metricNamespace, e)).mark();
                LOGGER.error("find:validate failure: {}", e);
                return new CallStatus(Error.get(RestCrudConstants.ERR_REST_FIND, "Request is not valid"));
            }
            addCallerId(ireq);
            // Until streaming is supported in mediator, we'll get the
            // results and stream them
            if(stream) {
                streamResponse=getMediator().findAndStream(ireq);
                return new CallStatus(new Response());
            } else {
                return new CallStatus(getMediator().find(ireq));
            }
        } catch (Error e) {
            metricsRegistry.meter(getErrorMetricsNamespace(metricNamespace, e)).mark();
            LOGGER.error("find:generic_error failure: {}", e);
            return new CallStatus(e);
        } catch (Exception e) {
            metricsRegistry.meter(getErrorMetricsNamespace(metricNamespace, e)).mark();
            LOGGER.error("find:generic_exception failure: {}", e);
            return new CallStatus(Error.get(RestCrudConstants.ERR_REST_FIND, e.toString()));
		} finally {
			context.stop();
			activeRequests.dec();
		}
    }

	@Override
	public String getSuccessMetricsNamespace(String operationName, String entityName, String entityVersion) {
		 return operationName + "." + entityName + "." + entityVersion;
	}

	@Override
	public String getErrorMetricsNamespace(String metricNamespace, Throwable exception) {
		Class<? extends Throwable> actualExceptionClass = unravelReflectionExceptions(exception);
	    return metricNamespace + ".exception." + actualExceptionClass.getName();
	}
}
