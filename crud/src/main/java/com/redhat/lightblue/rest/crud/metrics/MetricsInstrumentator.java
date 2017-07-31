package com.redhat.lightblue.rest.crud.metrics;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import com.codahale.metrics.MetricRegistry;

public interface MetricsInstrumentator {
	
    final MetricRegistry metricsRegistry = MetricRegistryFactory.getMetricRegistry();
	
	String getSuccessMetricsNamespace(String operationName, String entityName, String entityVersion);
	
	String getErrorMetricsNamespace(String metricNamespace, Throwable exception);
	
	void initializeMetrics(String metricNamespace);
	
    /**
     * Get to the cause we actually care about in case the bubbled up exception is a higher level
     * framework exception that encapsulates the stuff we really care about.
     */
    default Class<? extends Throwable> unravelReflectionExceptions(Throwable e) {
      while (e.getCause() != null &&
          (e instanceof UndeclaredThrowableException || e instanceof InvocationTargetException)) {
        e = e.getCause();
      }

      return e.getClass();
    }
   
}
