package com.redhat.lightblue.rest.crud.metrics;

public interface RequestMetrics {
    /**
     * Start timers and counters for the request. Use the returned context to complete the request
     * and optionally mark errors if they occur.
     *
     * <p>The returned context itself is not completely thread safe, it is expected to be used by
     * one and only one thread concurrently.
     */
    Context startEntityRequest(String operation, String entity, String version);

    Context startLock(String lockOperation, String domain);

    // TODO: I didn't use this but just a demonstration of another request where the parameters are
    // different
    Context startGenerate(String entity, String version, String path);

    Context startBulkRequest();

    interface Context {
        void endRequestMonitoring();

        void markRequestException(Exception e);
    }
}
