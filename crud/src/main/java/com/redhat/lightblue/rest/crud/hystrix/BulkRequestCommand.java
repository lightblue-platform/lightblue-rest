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
import com.redhat.lightblue.Request;
import com.redhat.lightblue.crud.BulkResponse;
import com.redhat.lightblue.crud.BulkRequest;
import com.redhat.lightblue.mediator.Mediator;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.crud.RestCrudConstants;
import com.redhat.lightblue.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulkRequestCommand extends AbstractRestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(BulkRequestCommand.class);

    private final String request;

    public BulkRequestCommand(String clientKey, String request) {
        super(BulkRequestCommand.class,clientKey, null);
        this.request=request;
    }

    @Override
    protected CallStatus run() {
        LOGGER.debug("bulk request");
        Error.reset();
        Error.push("rest");
        Error.push(getClass().getSimpleName());
        try {
            BulkRequest req;
            try {
                req=getJsonTranslator().parse(BulkRequest.class,JsonUtils.json(request));
            } catch (Exception e) {
                LOGGER.error("bulk:parse failure: {}",e);
                return new CallStatus(Error.get(RestCrudConstants.ERR_REST_ERROR, "Error parsing request"));
            }
            try{
                for(Request r:req.getEntries()) {
                    validateReq(r, r.getEntityVersion().getEntity(), r.getEntityVersion().getVersion());
                    addCallerId(r);
                }
            } catch (Exception e) {
                LOGGER.error("bulk:validate failure: {}", e);
                return new CallStatus(Error.get(RestCrudConstants.ERR_REST_ERROR, "Request is not valid"));
            }
            BulkResponse r = getMediator().bulkRequest(req);
            return new CallStatus(r);
        } catch (Error e) {
            LOGGER.error("bulk:generic_error failure: {}", e);
            return new CallStatus(e);
        } catch (Exception e) {
            LOGGER.error("bulk:generic_exception failure: {}", e);
            return new CallStatus(Error.get(RestCrudConstants.ERR_REST_ERROR, e.toString()));
        }
    }
}
