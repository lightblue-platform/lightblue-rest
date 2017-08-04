package com.redhat.lightblue.rest.crud.metrics;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;

/**
 * Register REST endpoint's and JVM metrics and report them via JMX
 */
public class MetricRegistryFactory {

	private static final MetricRegistry METRIC_REGISTRY = new MetricRegistry();

	static {
	    METRIC_REGISTRY.register("garbage-collector", new GarbageCollectorMetricSet());
	    METRIC_REGISTRY.register("buffers", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
	    METRIC_REGISTRY.register("memory", new MemoryUsageGaugeSet());
	    METRIC_REGISTRY.register("threads", new ThreadStatesGaugeSet());
	    
	    final JmxReporter jmxReporter = JmxReporter.forRegistry(METRIC_REGISTRY)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS).build();
        jmxReporter.start();
	}

	public static MetricRegistry getMetricRegistry() {
	    return METRIC_REGISTRY;
	}
}
