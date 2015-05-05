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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.Response;
import com.redhat.lightblue.crud.FindRequest;
import com.redhat.lightblue.mediator.Mediator;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.crud.RestCrudConstants;
import com.redhat.lightblue.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author nmalik
 */
public class FindCommand extends AbstractRestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(FindCommand.class);

    private final String entity;
    private final String version;
    private final String request;

    public FindCommand(String clientKey, String entity, String version, String request) {
        this(clientKey, null, entity, version, request);
    }

    public FindCommand(String clientKey, Mediator mediator, String entity, String version, String request) {
        super(FindCommand.class, clientKey, mediator);
        this.entity = entity;
        this.version = version;
        this.request = request;
    }

    @Override
    protected CallStatus run() {
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
                LOGGER.error("find:parse failure: {}", e);
                return new CallStatus(Error.get(RestCrudConstants.ERR_REST_FIND, "Error during the parse of the request"));
            }
            LOGGER.debug("Find request:{}", ireq);
            try{
                validateReq(ireq, entity, version);
            } catch (Exception e) {
                LOGGER.error("find:validate failure: {}", e);
                return new CallStatus(Error.get(RestCrudConstants.ERR_REST_FIND, "Request is not valid"));
            }
            addCallerId(ireq);
            Response r = getMediator().find(ireq);
            return new CallStatus(r);
        } catch (Error e) {
            LOGGER.error("find:generic_error failure: {}", e);
            return new CallStatus(e);
        } catch (Exception e) {
            LOGGER.error("find:generic_exception failure: {}", e);
            return new CallStatus(Error.get(RestCrudConstants.ERR_REST_FIND, e.toString()));
        }
    }
}
