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