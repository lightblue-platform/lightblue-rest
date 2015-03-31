package com.redhat.lightblue.rest.cors;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.ebaysf.web.cors.CORSFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import java.io.IOException;

@RunWith(JUnit4.class)
public class EbayCorsFilterRegistrationTest {
    private ServletContext mockContext = mock(ServletContext.class);
    private FilterRegistration.Dynamic mockFilterReg = mock(FilterRegistration.Dynamic.class);
    private CorsFilterRegistration registration = new EbayCorsFilterRegistration();

    @Before
    public void stubOutMocks() {
        when(mockContext.addFilter(anyString(), any(Class.class))).thenReturn(mockFilterReg);
    }

    @Test
    public void shouldActuallyAddTheFilter() {
        CorsConfiguration config = new CorsConfiguration.Builder().build();

        registration.register(mockContext, config);

        verify(mockContext).addFilter("cors", CORSFilter.class);
    }

    @Test
    public void shouldSetCorrectInitParametersForConfiguration() {
        CorsConfiguration config = new CorsConfiguration.Builder()
                .allowedOrigins("origin1", "origin2")
                .allowedMethods("method1", "method2")
                .allowedHeaders("header1", "header2")
                .exposedHeaders("header3", "header4", "header5")
                .preflightMaxAge(100)
                .allowCredentials(false)
                .enableLogging(true)
                .build();

        registration.register(mockContext, config);

        verify(mockFilterReg).setInitParameter("cors.allowed.origins", "origin1,origin2");
        verify(mockFilterReg).setInitParameter("cors.allowed.methods", "method1,method2");
        verify(mockFilterReg).setInitParameter("cors.allowed.headers", "header1,header2");
        verify(mockFilterReg).setInitParameter("cors.exposed.headers", "header3,header4,header5");
        verify(mockFilterReg).setInitParameter("cors.preflight.maxage", "100");
        verify(mockFilterReg).setInitParameter("cors.support.credentials", "false");
        verify(mockFilterReg).setInitParameter("cors.logging.enabled", "true");

        // Not read from config; should just always be false
        verify(mockFilterReg).setInitParameter("cors.request.decorate", "false");
    }

    @Test
    public void shouldSetCorrectUrlPatternForConfiguration() {
        CorsConfiguration config = new CorsConfiguration.Builder()
                .urlPatterns("/test", "/data")
                .build();

        registration.register(mockContext, config);

        verify(mockFilterReg).addMappingForUrlPatterns(null, false, "/test", "/data");
    }
}
