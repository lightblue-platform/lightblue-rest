/*
 Copyright 2015 Red Hat, Inc. and/or its affiliates.

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
package com.redhat.lightblue.rest.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

/**
 * Created by lcestari on 4/1/15.
 */
public class LogEntry {
    private final String timestampText;
    private final Principal principal;
    private final String resource;
    private final String operation;
    private final String entityName;
    private final String entityVersion;
    private final String entityStatus; // only for the AbstractMetadataResource @Path("/{entity}/{version}/{status}") updateSchemaStatus and AbstractMetadataResource @Path("/s={statuses}") getEntityNames
    private final int requestSize;
    private final int responseSize;
    private final long timeElapsedInNs;

    public LogEntry(String timestampText, Principal principal, String resource, String operation, String entityName, String entityVersion,String entityStatus, int requestSize, int responseSize, long timeElapsedInNs) {
        this.timestampText = timestampText;
        this.principal = principal;
        this.resource = resource;
        this.operation = operation;
        this.entityName = entityName;
        this.entityVersion = entityVersion;
        this.entityStatus = entityStatus;
        this.requestSize = requestSize;
        this.responseSize = responseSize;
        this.timeElapsedInNs = timeElapsedInNs;
    }

    public String getTimestampText() {
        return timestampText;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public String getResource() {
        return resource;
    }

    public String getOperation() {
        return operation;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getEntityVersion() {
        return entityVersion;
    }

    public int getRequestSize() {
        return requestSize;
    }

    public int getResponseSize() {
        return responseSize;
    }

    public long getTimeElapsedInNs() {
        return timeElapsedInNs;
    }

    public String getEntityStatus() {
        return entityStatus;
    }
}
