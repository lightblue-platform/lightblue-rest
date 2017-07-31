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

import com.redhat.lightblue.util.Error;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.redhat.lightblue.Request;
import com.redhat.lightblue.crud.BulkResponse;
import com.redhat.lightblue.crud.BulkRequest;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.crud.RestCrudConstants;
import com.redhat.lightblue.rest.crud.metrics.MetricsInstrumentator;
import com.redhat.lightblue.util.JsonUtils;

import static com.codahale.metrics.MetricRegistry.name;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulkRequestCommand extends AbstractRestCommand implements MetricsInstrumentator{
    private static final Logger LOGGER = LoggerFactory.getLogger(BulkRequestCommand.class);

    private final String request;
    
    private String metricNamespace;
    private Counter activeRequests;
    private Timer requestTimer;

    public BulkRequestCommand(String request) {
        this.request = request;
        this.metricNamespace=getSuccessMetricsNamespace("bulkrequest", null, null);
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
        final Timer.Context timer = requestTimer.time();
        LOGGER.debug("bulk request");
        Error.reset();
        Error.push("rest");
        Error.push(getClass().getSimpleName());
        try {
            BulkRequest req;
            try {
                req = getJsonTranslator().parse(BulkRequest.class, JsonUtils.json(request));
            } catch (Exception e) {
                metricsRegistry.meter(getErrorMetricsNamespace(metricNamespace, e)).mark();
                LOGGER.error("bulk:parse failure: {}", e);
                return new CallStatus(Error.get(RestCrudConstants.ERR_REST_ERROR, "Error parsing request"));
            }
            try {
                for (Request r : req.getEntries()) {
                    validateReq(r, r.getEntityVersion().getEntity(), r.getEntityVersion().getVersion());
                    addCallerId(r);
                }
            } catch (Exception e) {
                metricsRegistry.meter(getErrorMetricsNamespace(metricNamespace, e)).mark();
                LOGGER.error("bulk:validate failure: {}", e);
                return new CallStatus(Error.get(RestCrudConstants.ERR_REST_ERROR, "Request is not valid"));
            }
            BulkResponse r = getMediator().bulkRequest(req);
            return new CallStatus(r);
        } catch (Error e) {
            metricsRegistry.meter(getErrorMetricsNamespace(metricNamespace, e)).mark();
            LOGGER.error("bulk:generic_error failure: {}", e);
            return new CallStatus(e);
        } catch (Exception e) {
            metricsRegistry.meter(getErrorMetricsNamespace(metricNamespace, e)).mark();
            LOGGER.error("bulk:generic_exception failure: {}", e);
            return new CallStatus(Error.get(RestCrudConstants.ERR_REST_ERROR, e.toString()));
        } finally {
            timer.stop();
            activeRequests.dec();
        }
    }
    
	@Override
	public String getSuccessMetricsNamespace(String operationName, String entityName, String entityVersion) {
		 return operationName;
	}

	@Override
	public String getErrorMetricsNamespace(String metricNamespace, Throwable exception) {
		Class<? extends Throwable> actualExceptionClass = unravelReflectionExceptions(exception);
	    return metricNamespace + ".exception." + actualExceptionClass.getName();
	}
}
