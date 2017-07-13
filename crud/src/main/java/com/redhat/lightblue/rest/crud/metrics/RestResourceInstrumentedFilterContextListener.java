package com.redhat.lightblue.rest.crud.metrics;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.redhat.lightblue.rest.metrics.InstrumentedFilterContextListener;

/**
 * Register REST endpoint's and JVM metrics and report them via JMX
 */
public class RestResourceInstrumentedFilterContextListener extends InstrumentedFilterContextListener {

	public static final MetricRegistry METRIC_REGISTRY = new MetricRegistry();

	static {
		METRIC_REGISTRY.register("garbage-collection", new GarbageCollectorMetricSet());
		METRIC_REGISTRY.register("memory", new MemoryUsageGaugeSet());
		METRIC_REGISTRY.register("threads", new ThreadStatesGaugeSet());

		final JmxReporter jmxReporter = JmxReporter.forRegistry(METRIC_REGISTRY)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).build();
		jmxReporter.start();
	}

	@Override
	protected MetricRegistry getMetricRegistry() {
		return METRIC_REGISTRY;
	}
}