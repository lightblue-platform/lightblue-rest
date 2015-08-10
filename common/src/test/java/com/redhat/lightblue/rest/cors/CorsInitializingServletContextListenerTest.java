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

import static com.redhat.lightblue.rest.cors.CorsInitializingServletContextListener.CORS_CONFIGURATION_RESOURCE_PARAM;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;

@RunWith(JUnit4.class)
public class CorsInitializingServletContextListenerTest {
    private ServletContextEvent mockContextEvent = mock(ServletContextEvent.class);
    private ServletContext mockContext = mock(ServletContext.class);
    private CorsFilterRegistration mockFilterRegistration = mock(CorsFilterRegistration.class);
    private ServletContextListener listener = new CorsInitializingServletContextListener(mockFilterRegistration);

    @Before
    public void stubOutMocks() {
        when(mockContextEvent.getServletContext()).thenReturn(mockContext);
    }

    @Test
    public void shouldNotEnableCorsIfConfigurationFileIsNotFound() {
        when(mockContext.getInitParameter(CORS_CONFIGURATION_RESOURCE_PARAM))
                .thenReturn("this/does/not/exist.json");

        listener.contextInitialized(mockContextEvent);

        verify(mockFilterRegistration, never()).register(any(ServletContext.class),
                any(CorsConfiguration.class));
        verify(mockContext, never()).addFilter(anyString(), anyString());
        verify(mockContext, never()).addFilter(anyString(), any(Class.class));
        verify(mockContext, never()).addFilter(anyString(), any(Filter.class));
    }

    @Test
    public void shouldNotEnableCorsIfConfigurationFilePathIsNotDefined() {
        listener.contextInitialized(mockContextEvent);

        verify(mockFilterRegistration, never()).register(any(ServletContext.class),
                any(CorsConfiguration.class));
        verify(mockContext, never()).addFilter(anyString(), anyString());
        verify(mockContext, never()).addFilter(anyString(), any(Class.class));
        verify(mockContext, never()).addFilter(anyString(), any(Filter.class));
    }

    @Test
    public void shouldRegisterCorsFilterWithJsonConfigurationIfFound() throws IOException {
        when(mockContext.getInitParameter(CORS_CONFIGURATION_RESOURCE_PARAM))
                .thenReturn("emptyCorsConfig.json");

        listener.contextInitialized(mockContextEvent);

        verify(mockFilterRegistration).register(mockContext, new CorsConfiguration.Builder()
                .fromJsonResource("emptyCorsConfig.json")
                .build());

        // Make sure these aren't called; the filter registration and only the filter registration
        // should do this.
        verify(mockContext, never()).addFilter(anyString(), anyString());
        verify(mockContext, never()).addFilter(anyString(), any(Class.class));
        verify(mockContext, never()).addFilter(anyString(), any(Filter.class));
    }
}
