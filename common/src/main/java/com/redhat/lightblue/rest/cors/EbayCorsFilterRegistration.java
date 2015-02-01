package com.redhat.lightblue.rest.cors;

import org.ebaysf.web.cors.CORSFilter;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import java.util.Iterator;
import java.util.List;

public class EbayCorsFilterRegistration implements CorsFilterRegistration {
    @Override
    public void register(ServletContext context, CorsConfiguration config) {
        FilterRegistration.Dynamic filter = context.addFilter("cors", CORSFilter.class);

        addUrlPatterns(filter, config);
        addInitParameters(filter, config);
    }

    private void addUrlPatterns(FilterRegistration.Dynamic filter, CorsConfiguration config) {
        List<String> urlPatternsList = config.getUrlPatterns();
        String[] urlPatterns = urlPatternsList.toArray(new String[urlPatternsList.size()]);

        filter.addMappingForUrlPatterns(null, false, urlPatterns);
    }

    private void addInitParameters(FilterRegistration.Dynamic filter, CorsConfiguration config) {
        String allowedOrigins = commaDelimited(config.getAllowedOrigins());
        String allowedMethods = commaDelimited(config.getAllowedMethods());
        String allowedHeaders = commaDelimited(config.getAllowedHeaders());
        String exposedHeaders = commaDelimited(config.getExposedHeaders());
        String preflightMaxAge = Integer.toString(config.getPreflightMaxAge());
        String supportCredentials = Boolean.toString(config.areCredentialsSupported());
        String enableLogging = Boolean.toString(config.isLoggingEnabled());
        String decorateRequest = Boolean.toString(config.shouldDecorateRequests());

        filter.setInitParameter("cors.allowed.origins", allowedOrigins);
        filter.setInitParameter("cors.allowed.methods", allowedMethods);
        filter.setInitParameter("cors.allowed.headers", allowedHeaders);
        filter.setInitParameter("cors.exposed.headers", exposedHeaders);
        filter.setInitParameter("cors.preflight.maxage", preflightMaxAge);
        filter.setInitParameter("cors.support.credentials", supportCredentials);
        filter.setInitParameter("cors.logging.enabled", enableLogging);
        filter.setInitParameter("cors.request.decorate", decorateRequest);
    }

    private String commaDelimited(List<String> list) {
        Iterator<String> elements = list.iterator();
        String joined;

        if (elements.hasNext()) {
            joined = elements.next();
        } else {
            joined = "";
        }

        while (elements.hasNext()) {
            joined = joined + "," + elements.next();
        }

        return joined;
    }
}
