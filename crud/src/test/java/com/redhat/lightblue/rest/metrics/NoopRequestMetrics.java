package com.redhat.lightblue.rest.metrics;

import com.redhat.lightblue.rest.crud.metrics.RequestMetrics;

public class NoopRequestMetrics implements RequestMetrics {

    private static final NoopContext NOOP_CONTEXT = new NoopContext();

    @Override
    public Context startEntityRequest(String operation, String entity, String version) {
        return NOOP_CONTEXT;
    }

    @Override
    public Context startLock(String lockOperation, String domain) {
        return NOOP_CONTEXT;
    }

    @Override
    public Context startGenerate(String entity, String version, String path) {
        return NOOP_CONTEXT;
    }

    @Override
    public Context startBulkRequest() {
        return NOOP_CONTEXT;
    }

    private static class NoopContext implements Context {
        @Override
        public void endRequestMonitoring() {

        }

        @Override
        public void markRequestException(Exception e) {

        }
    }
}
