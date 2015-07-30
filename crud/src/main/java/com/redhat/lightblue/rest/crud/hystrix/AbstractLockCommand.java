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
package com.redhat.lightblue.rest.crud.hystrix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.crud.RestCrudConstants;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.util.JsonUtils;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.SimpleJsonObject;
import com.redhat.lightblue.extensions.synch.Locking;


public abstract class AbstractLockCommand extends AbstractRestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLockCommand.class);

    protected final String domain;
    protected final String resource;
    protected final String caller;

    public AbstractLockCommand(Class commandClass,String clientKey, String domain,String caller,String resource) {
        super(commandClass,clientKey,null);
        this.domain=domain;
        this.resource=resource;
        this.caller=caller;
    }

    @Override
    protected CallStatus run() {
        LOGGER.debug("run: domain={}, resource={}, caller={}",domain,resource,caller);
        Error.reset();
        Error.push("rest");
        Error.push(getClass().getSimpleName());
        Error.push(resource);
        try {
            Locking locking=RestConfiguration.getFactory().getLocking(domain);
            JsonNode result=runLockCommand(locking);
            ObjectNode o=NODE_FACTORY.objectNode();
            o.set("result",result);
            return new CallStatus(new SimpleJsonObject(o));
        } catch (Error e) {
            LOGGER.error("failure: {}", e);
            return new CallStatus(e);
        } catch (Exception e) {
            LOGGER.error("failure: {}", e);
            return new CallStatus(Error.get(RestCrudConstants.ERR_REST_ERROR, e.toString()));
        }
    }

    protected abstract JsonNode runLockCommand(Locking locking);
}
