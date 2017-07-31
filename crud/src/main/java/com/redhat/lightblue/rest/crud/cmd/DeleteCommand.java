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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.Response;
import com.redhat.lightblue.crud.DeleteRequest;
import com.redhat.lightblue.mediator.Mediator;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.crud.RestCrudConstants;
import com.redhat.lightblue.rest.crud.metrics.MetricsInstrumentator;
import com.redhat.lightblue.util.JsonUtils;

import static com.codahale.metrics.MetricRegistry.name;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author nmalik
 */
public class DeleteCommand extends AbstractRestCommand implements MetricsInstrumentator{
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteCommand.class);

    private final String entity;
    private final String version;
    private final String request;

    private String metricNamespace;
	private Counter activeRequests;
	private Timer requestTimer;
	
    public DeleteCommand(String entity, String version, String request) {
        this(null, entity, version, request);
    }

    public DeleteCommand(Mediator mediator, String entity, String version, String request) {
        super(mediator);
        this.entity = entity;
        this.version = version;
        this.request = request;
        this.metricNamespace=getSuccessMetricsNamespace("delete", entity, version);
        initializeMetrics(metricNamespace);
    }

    @Override
	public void initializeMetrics(String merticNamespace) {
		this.activeRequests = metricsRegistry.counter(name(merticNamespace, "activeRequests"));
		this.requestTimer = metricsRegistry.timer(name(merticNamespace, "requests"));
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
            DeleteRequest ireq = getJsonTranslator().parse(DeleteRequest.class, JsonUtils.json(request));
            validateReq(ireq, entity, version);
            addCallerId(ireq);
            Response r = getMediator().delete(ireq);
            return new CallStatus(r);
        } catch (Error e) {
            metricsRegistry.meter(getErrorMetricsNamespace(metricNamespace, e)).mark();
            LOGGER.error("delete failure: {}", e);
            return new CallStatus(e);
        } catch (Exception e) {
            metricsRegistry.meter(getErrorMetricsNamespace(metricNamespace, e)).mark();
            LOGGER.error("delete failure: {}", e);
            return new CallStatus(Error.get(RestCrudConstants.ERR_REST_DELETE, e.toString()));
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
