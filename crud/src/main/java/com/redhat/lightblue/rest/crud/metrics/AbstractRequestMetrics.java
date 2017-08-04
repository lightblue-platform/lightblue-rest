package com.redhat.lightblue.rest.crud.metrics;

import static com.codahale.metrics.MetricRegistry.name;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public abstract class AbstractRequestMetrics {

    private final String API = "api";

    private String metricNamespace;
    private Counter activeRequests;
    private Timer requestTimer;
    private Timer.Context timer;

    private final MetricRegistry metricsRegistry = MetricRegistryFactory.getMetricRegistry();

    /**
     * Create namespace for reporting metrics via jmx
     * 
     */
    private String getMetricsNamespace(String operationName, String entityName, String entityVersion) {
        return name(API, operationName, entityName, entityVersion);
    }

    /**
     * Initialize metric meters, these meters will be used to report metrics of each
     * command object
     * 
     */
    public void initializeMetrics(String operation, String entity, String version) {
        this.metricNamespace = getMetricsNamespace(operation, entity, version);
        this.activeRequests = metricsRegistry.counter(name(metricNamespace, "requests", "active"));
        this.requestTimer = metricsRegistry.timer(name(metricNamespace, "requests", "completed"));
    }

    /**
     * Create exception namespace for jmx metrics reporting based on exception name
     * 
     */
    private String getErrorNamespace(String metricNamespace, Throwable exception) {
        Class<? extends Throwable> actualExceptionClass = unravelReflectionExceptions(exception);
        return name(metricNamespace, "requests", "exception", actualExceptionClass.getSimpleName());
    }

    /**
     * Get to the cause we actually care about in case the bubbled up exception is a
     * higher level framework exception that encapsulates the stuff we really care
     * about.
     * 
     */
    private Class<? extends Throwable> unravelReflectionExceptions(Throwable e) {
        while (e.getCause() != null
                && (e instanceof UndeclaredThrowableException || e instanceof InvocationTargetException)) {
            e = e.getCause();
        }
        return e.getClass();
    }

    /**
     * Start request monitoring
     * 
     */
    public void startRequestMonitoring() {
        activeRequests.inc();
        timer = requestTimer.time();
    }

    /**
     * Stop request monitoring
     * 
     */
    public void endRequestMonitoring() {
        activeRequests.dec();
        timer.stop();
    }

    /**
     * Mark request as exception
     * 
     */
    public void markRequestException(Exception e) {
        metricsRegistry.meter(getErrorNamespace(metricNamespace, e)).mark();
    }
}