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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.lightblue.ClientIdentification;
import com.redhat.lightblue.Response;
import com.redhat.lightblue.crud.FindRequest;
import com.redhat.lightblue.metadata.types.DefaultTypes;
import com.redhat.lightblue.query.Projection;
import com.redhat.lightblue.query.Sort;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.rest.crud.RestCrudConstants;
import com.redhat.lightblue.savedsearch.FindRequestBuilder;
import com.redhat.lightblue.util.Error;

public class RunSavedSearchCommand extends AbstractRestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunSavedSearchCommand.class);

    private final String entity;
    private final String version;
    private final String searchName;
    private final Projection projection;
    private final Sort sort;
    private final Integer from,to;
    private final Map<String,String> params;
	
    public RunSavedSearchCommand(String searchName,
                                 String entity,
                                 String version,
                                 Projection projection,
                                 Sort sort,
                                 Integer from,
                                 Integer to,
                                 Map<String,String> properties) {
        this.searchName=searchName;
        this.entity=entity;
        this.version=version;
        this.projection=projection;
        this.sort=sort;
        this.from=from;
        this.to=to;
        this.params=properties;
        this.metricNamespace=getMetricsNamespace("savedsearch", entity, version);
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
            ClientIdentification callerId=getCallerId();
            JsonNode searchDoc=RestConfiguration.getSavedSearchCache().getSavedSearch(getMediator(),getCallerId(),searchName,entity,version);
            if(searchDoc==null)
                throw Error.get(RestCrudConstants.ERR_REST_SAVED_SEARCH,searchName+":"+entity+":"+version);
            Map<String,String> parameters=FindRequestBuilder.fillDefaults(searchDoc,params,new DefaultTypes());
            LOGGER.debug("Parameters:{}",parameters);
            FindRequest req=FindRequestBuilder.buildRequest(searchDoc,entity,version,callerId,parameters);
            if(projection!=null) {
                req.setProjection(Projection.add(req.getProjection(),projection));
            }
            if(sort!=null) {
                req.setSort(sort);
            }
            if(from!=null) {
                req.setFrom(from.longValue());
            }
            if(to!=null) {
                req.setTo(to.longValue());
            }
            LOGGER.debug("Request:{}",req);
            System.out.println("request:"+req);
            Response r = getMediator().find(req);
            return new CallStatus(r);
        } catch (Error e) {
            metricsRegistry.meter(getErrorNamespace(metricNamespace, e)).mark();
            LOGGER.error("saved_search failure: {}", e);
            return new CallStatus(e);
        } catch (Exception e) {
            metricsRegistry.meter(getErrorNamespace(metricNamespace, e)).mark();
            LOGGER.error("saved_search failure: {}", e);
            return new CallStatus(Error.get(RestCrudConstants.ERR_REST_FIND, e.toString()));
        } finally {
            timer.stop();
            activeRequests.dec();
        }
    }
}
