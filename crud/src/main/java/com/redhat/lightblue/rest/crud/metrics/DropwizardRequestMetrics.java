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
import java.util.Objects;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class DropwizardRequestMetrics implements RequestMetrics {

    private static final String API = "api";

    private final MetricRegistry metricsRegistry;

    public DropwizardRequestMetrics(MetricRegistry metricRegistry) {
        metricsRegistry = metricRegistry;
    }

    /**
     * Create exception namespace for metric reporting based on exception name
     * 
     */
    private static String errorNamespace(String metricNamespace, Throwable exception) {
        Class<? extends Throwable> actualExceptionClass = unravelReflectionExceptions(exception);
        return name(metricNamespace, "requests", "exception", actualExceptionClass.getSimpleName());
    }

    /**
     * Get to the cause we actually care about in case the bubbled up exception is a
     * higher level framework exception that encapsulates the stuff we really care
     * about.
     * 
     */
    private static Class<? extends Throwable> unravelReflectionExceptions(Throwable e) {
        while (e.getCause() != null
                && (e instanceof UndeclaredThrowableException || e instanceof InvocationTargetException)) {
            e = e.getCause();
        }
        return e.getClass();
    }

    @Override
    public Context startEntityRequest(String operation, String entity, String version) {
        return new DropwizardContext(name(API, operation, entity, version));
    }

    @Override
    public Context startLock(String lockOperation, String domain) {
        return new DropwizardContext(name(API, "lock", domain, lockOperation));
    }

    // TODO: I didn't use this but just a demonstration of another request where the parameters are
    // different
    @Override
    public Context startGenerate(String entity, String version, String path) {
        return new DropwizardContext(name(API, "generate", entity, version, path));
    }

    @Override
    public Context startBulkRequest() {
        // Not very useful :(... can consider alternatives, but should probably refactor more, see
        // comments in BulkRequestCommand
        return new DropwizardContext(name(API, "bulk"));
    }

    public class DropwizardContext implements Context {
        private final String metricNamespace;
        private final Timer.Context context;
        private final Counter activeRequests;
        private boolean ended = false;

        public DropwizardContext(String metricNamespace) {
            this.metricNamespace = Objects.requireNonNull(metricNamespace, "metricNamespace");
            this.context = metricsRegistry.timer(name(metricNamespace, "latency")).time();
            this.activeRequests = metricsRegistry.counter(name(metricNamespace, "requests", "active"));

            activeRequests.inc();
        }
        @Override
        public void endRequestMonitoring() {
            // Added this error handling to catch bugs. Might want to synchronize this, or consider
            // only logging a warning instead. Important point is that we don't decrement counter
            // again if request already ended.
            if (ended) {
                throw new IllegalStateException("Request already ended.");
            }

            ended = true;
            activeRequests.dec();
            context.stop();
        }

        @Override
        public void markRequestException(Exception e) {
            metricsRegistry.meter(errorNamespace(metricNamespace, e)).mark();
        }
    }
}
