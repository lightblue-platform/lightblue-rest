/*
 Copyright 2015 Red Hat, Inc. and/or its affiliates.

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
        String allowCredentials = Boolean.toString(config.areCredentialsAllowed());
        String enableLogging = Boolean.toString(config.isLoggingEnabled());

        filter.setInitParameter("cors.allowed.origins", allowedOrigins);
        filter.setInitParameter("cors.allowed.methods", allowedMethods);
        filter.setInitParameter("cors.allowed.headers", allowedHeaders);
        filter.setInitParameter("cors.exposed.headers", exposedHeaders);
        filter.setInitParameter("cors.preflight.maxage", preflightMaxAge);
        filter.setInitParameter("cors.support.credentials", allowCredentials);
        filter.setInitParameter("cors.logging.enabled", enableLogging);

        // Not necessary for Lightblue services.
        filter.setInitParameter("cors.request.decorate", "false");
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
