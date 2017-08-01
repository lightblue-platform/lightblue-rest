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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.lightblue.extensions.synch.Locking;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.rest.crud.RestCrudConstants;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.SimpleJsonObject;

public abstract class AbstractLockCommand extends AbstractRestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLockCommand.class);

    public static final String OPERATION_ACQUIRE = "acquire";
    public static final String OPERATION_RELEASE = "release";
    public static final String OPERATION_COUNT = "count";
    public static final String OPERATION_PING = "ping";

    protected final String domain;
    protected final String resource;
    protected final String caller;

    public AbstractLockCommand(String domain, String caller, String resource) {
        this.domain = domain;
        this.resource = resource;
        this.caller = caller;
        this.metricNamespace=getMetricsNamespace("lock", domain, resource+"."+caller);
        initializeMetrics(metricNamespace);
    }

    public static AbstractLockCommand getLockCommand(String request) {
        AbstractLockCommand command = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readValue(request, JsonNode.class);
            String operation = rootNode.get("operation").asText();
            String domain = rootNode.get("domain").asText();
            String callerId = rootNode.get("callerId").asText();
            String resourceId = rootNode.get("resourceId").asText();

            switch(operation) {
                case OPERATION_ACQUIRE :
                    Long ttl = null;
                    if(null != rootNode.get("ttl")) {
                        ttl = rootNode.get("ttl").asLong();
                    }
                    command = new AcquireCommand(domain, callerId, resourceId, ttl);
                    break;
                case OPERATION_RELEASE :
                    command = new ReleaseCommand(domain, callerId, resourceId);
                    break;
                case OPERATION_COUNT :
                    command = new GetLockCountCommand(domain, callerId, resourceId);
                    break;
                case OPERATION_PING :
                    command = new LockPingCommand(domain, callerId, resourceId);
                    break;
                default :
                    Error.push("Error parsing lock request");
            }
        } catch (Exception e) {
            Error.push("Error parsing lock request");
        }
        return command;
    }

    @Override
    public CallStatus run() {
        activeRequests.inc();
        final Timer.Context timer = requestTimer.time();    	
        LOGGER.debug("run: domain={}, resource={}, caller={}", domain, resource, caller);
        Error.reset();
        Error.push("rest");
        Error.push(getClass().getSimpleName());
        Error.push(resource);
        try {
            Locking locking = RestConfiguration.getFactory().getLocking(domain);
            JsonNode result = runLockCommand(locking);
            ObjectNode o = NODE_FACTORY.objectNode();
            o.set("result", result);
            return new CallStatus(new SimpleJsonObject(o));
        } catch (Error e) {
            metricsRegistry.meter(getErrorNamespace(metricNamespace, e)).mark();
            LOGGER.error("failure: {}", e);
            return new CallStatus(e);
        } catch (Exception e) {
            metricsRegistry.meter(getErrorNamespace(metricNamespace, e)).mark();
            LOGGER.error("failure: {}", e);
            return new CallStatus(Error.get(RestCrudConstants.ERR_REST_ERROR, e.toString()));
        } finally {
            timer.stop();
            activeRequests.dec();
        }
    }

    protected abstract JsonNode runLockCommand(Locking locking); 
}
