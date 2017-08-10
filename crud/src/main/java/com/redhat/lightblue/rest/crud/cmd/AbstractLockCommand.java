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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.lightblue.extensions.synch.Locking;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.rest.crud.RestCrudConstants;
import com.redhat.lightblue.util.metrics.RequestMetrics;
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
    private final RequestMetrics metrics;

    public AbstractLockCommand(String domain, String caller, String resource, RequestMetrics metrics) {
        this.domain = domain;
        this.resource = resource;
        this.caller = caller;
        this.metrics = metrics;
    }

    public static AbstractLockCommand getLockCommand(String request, RequestMetrics metrics) {
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
                    command = new AcquireCommand(domain, callerId, resourceId, ttl, metrics);
                    break;
                case OPERATION_RELEASE :
                    command = new ReleaseCommand(domain, callerId, resourceId, metrics);
                    break;
                case OPERATION_COUNT :
                    command = new GetLockCountCommand(domain, callerId, resourceId, metrics);
                    break;
                case OPERATION_PING :
                    command = new LockPingCommand(domain, callerId, resourceId, metrics);
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
        // Omitting resource because might be too many different targets, number of resources is
        // unbounded.
        RequestMetrics.Context context = metrics.startLockRequest(getCommandName(), domain);
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
            context.markRequestException(e);
            LOGGER.error("failure: {}", e);
            return new CallStatus(e);
        } catch (Exception e) {
            context.markRequestException(e);
            LOGGER.error("failure: {}", e);
            return new CallStatus(Error.get(RestCrudConstants.ERR_REST_ERROR, e.toString()));
        } finally {
            context.endRequestMonitoring();
        }
    }

    protected abstract JsonNode runLockCommand(Locking locking);
}
