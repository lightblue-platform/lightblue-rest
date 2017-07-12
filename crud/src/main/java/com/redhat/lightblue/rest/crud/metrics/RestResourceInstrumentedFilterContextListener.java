package com.redhat.lightblue.rest.crud.metrics;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlet.InstrumentedFilterContextListener;

/**
 * Register REST endpoint's metrics and report them via JMX
 */
public class RestResourceInstrumentedFilterContextListener extends InstrumentedFilterContextListener {

	public static final MetricRegistry METRIC_REGISTRY = new MetricRegistry();

	static {
		final JmxReporter jmxReporter = JmxReporter.forRegistry(METRIC_REGISTRY)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS)
				.build();
		jmxReporter.start();
	}

	@Override
	protected MetricRegistry getMetricRegistry() {
		return METRIC_REGISTRY;
	}
}