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
    private final String resource; // /crud or /metadata
    private final String operation; //operation (insert, update, find, etc)
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
