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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.redhat.lightblue.Response;
import com.redhat.lightblue.crud.FindRequest;
import com.redhat.lightblue.mediator.Mediator;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.crud.RestCrudConstants;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.JsonUtils;

public class ExplainCommand extends AbstractRestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExplainCommand.class);

    private final String entity;
    private final String version;
    private final String request;
	
    public ExplainCommand(String entity, String version, String request) {
        this(null, entity, version, request);
    }

    public ExplainCommand(Mediator mediator, String entity, String version, String request) {
        super(mediator);
        this.entity = entity;
        this.version = version;
        this.request = request;
        this.metricNamespace=getMetricsNamespace("explain", entity, version);
        initializeMetrics(metricNamespace);
    }

    @Override
    public CallStatus run() {
        activeRequests.inc();
        final Timer.Context timer = requestTimer.time();
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
                metricsRegistry.meter(getErrorNamespace(metricNamespace, e)).mark();
                LOGGER.error("explain:parse failure: {}", e);
                return new CallStatus(Error.get(RestCrudConstants.ERR_REST_FIND, "Error during the parse of the request"));
            }
            LOGGER.debug("Find request:{}", ireq);
            try {
                validateReq(ireq, entity, version);
            } catch (Exception e) {
                metricsRegistry.meter(getErrorNamespace(metricNamespace, e)).mark();
                LOGGER.error("explain:validate failure: {}", e);
                return new CallStatus(Error.get(RestCrudConstants.ERR_REST_FIND, "Request is not valid"));
            }
            addCallerId(ireq);
            Response r = getMediator().explain(ireq);
            return new CallStatus(r);
        } catch (Error e) {
            metricsRegistry.meter(getErrorNamespace(metricNamespace, e)).mark();
            LOGGER.error("explain:generic_error failure: {}", e);
            return new CallStatus(e);
        } catch (Exception e) {
            metricsRegistry.meter(getErrorNamespace(metricNamespace, e)).mark();
            LOGGER.error("explain:generic_exception failure: {}", e);
            return new CallStatus(Error.get(RestCrudConstants.ERR_REST_FIND, e.toString()));
        } finally {
            timer.stop();
            activeRequests.dec();
        }
    }
}
