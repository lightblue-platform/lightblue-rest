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

import java.security.Principal;

public class LogEntryBuilder {
    private String timestampText;
    private Principal principal;
    private String resource;
    private String operation;
    private String entityName;
    private String entityVersion;
    private String entityStatus;
    private int requestSize;
    private int responseSize;
    private long timeElapsedInNs;

    public LogEntryBuilder setTimestampText(String timestampText) {
        this.timestampText = timestampText;
        return this;
    }

    public LogEntryBuilder setPrincipal(Principal principal) {
        this.principal = principal;
        return this;
    }

    public LogEntryBuilder setResource(String resource) {
        this.resource = resource;
        return this;
    }

    public LogEntryBuilder setOperation(String operation) {
        this.operation = operation;
        return this;
    }

    public LogEntryBuilder setEntityName(String entityName) {
        this.entityName = entityName;
        return this;
    }

    public LogEntryBuilder setEntityVersion(String entityVersion) {
        this.entityVersion = entityVersion;
        return this;
    }

    public LogEntryBuilder setEntityStatus(String entityStatus) {
        this.entityStatus = entityStatus;
        return this;
    }

    public LogEntryBuilder setRequestSize(int requestSize) {
        this.requestSize = requestSize;
        return this;
    }

    public LogEntryBuilder setResponseSize(int responseSize) {
        this.responseSize = responseSize;
        return this;
    }

    public LogEntryBuilder setTimeElapsedInNs(long timeElapsedInNs) {
        this.timeElapsedInNs = timeElapsedInNs;
        return this;
    }

    public LogEntry createLogEntry() {
        return new LogEntry(timestampText, principal, resource, operation, entityName, entityVersion, entityStatus, requestSize, responseSize, timeElapsedInNs);
    }
}