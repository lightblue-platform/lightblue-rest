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
package com.redhat.lightblue.rest.crud.metrics;

import static com.codahale.metrics.MetricRegistry.name;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class RequestMetrics {

    private final String API = "api";

    private String metricNamespace;
    private Counter activeRequests;
    private Timer requestTimer;
    private Timer.Context timer;

    private final MetricRegistry metricsRegistry = MetricRegistryFactory.getMetricRegistry();

    /**
     * Creates namespace for metric reporting and initializes metric meters, 
     * these meters will be used to report metrics of each command object
     * 
     */
    private void initializeMetrics(String operation, String entity, String version) {
        this.metricNamespace = name(API, operation, entity, version);
        this.activeRequests = metricsRegistry.counter(name(metricNamespace, "requests", "active"));
        this.requestTimer = metricsRegistry.timer(name(metricNamespace, "requests", "completed"));
    }

    /**
     * Create exception namespace for metric reporting based on exception name
     * 
     */
    private String errorNamespace(String metricNamespace, Throwable exception) {
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
    public void startRequestMonitoring(String operation, String entity, String version) {
        initializeMetrics(operation, entity, version);
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
        metricsRegistry.meter(errorNamespace(metricNamespace, e)).mark();
    }
}
