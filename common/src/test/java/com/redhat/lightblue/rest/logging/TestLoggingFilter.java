package com.redhat.lightblue.rest.logging;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestLoggingFilter {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @Test
    public void testRequest_SetAttribute_RequestUUID() throws Exception {
        ArgumentCaptor<String> captureUUID = ArgumentCaptor.forClass(String.class);

        new LoggingFilter().doFilter(request, response, chain);

        verify(request, atLeastOnce()).setAttribute(eq(LoggingFilter.HEADER_REQUEST_UUID), captureUUID.capture());
        assertNotNull(captureUUID.getValue());
    }
    
    @Test
    public void testResponse_SetHeader_RequestUUID() throws Exception {
        ArgumentCaptor<String> captureUUID = ArgumentCaptor.forClass(String.class);

        new LoggingFilter().doFilter(request, response, chain);

        verify(response, atLeastOnce()).setHeader(eq(LoggingFilter.HEADER_REQUEST_UUID), captureUUID.capture());
        assertNotNull(captureUUID.getValue());
    }

}
