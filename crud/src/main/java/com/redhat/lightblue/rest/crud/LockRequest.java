package com.redhat.lightblue.rest.crud;

public class LockRequest {
    public static final String OPERATION_ACQUIRE = "acquire";
    public static final String OPERATION_RELEASE = "release";
    public static final String OPERATION_COUNT = "count";
    public static final String OPERATION_PING = "ping";

    String operation;
    String domain;
    String callerId;
    String resourceId;
    Long ttl;

    public LockRequest() {

    }

    public LockRequest(String operation, String domain, String callerId, String resourceId) {
        this.operation = operation;
        this.domain = domain;
        this.callerId = callerId;
        this.resourceId = resourceId;
    }

    public LockRequest(String operation, String domain, String callerId, String resourceId, Long ttl) {
        this.operation = operation;
        this.domain = domain;
        this.callerId = callerId;
        this.resourceId = resourceId;
        this.ttl = ttl;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getCallerId() {
        return callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}
