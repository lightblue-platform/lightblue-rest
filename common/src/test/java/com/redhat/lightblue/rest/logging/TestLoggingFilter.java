package com.redhat.lightblue.rest.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

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
    public void testRequest_SetAttribute_RequestID() throws Exception {
        ArgumentCaptor<String> captureID = ArgumentCaptor.forClass(String.class);

        new LoggingFilter().doFilter(request, response, chain);
        new LoggingFilter().doFilter(request, response, chain);

        verify(request, times(2)).setAttribute(eq(LoggingFilter.HEADER_REQUEST_ID), captureID.capture());

        List<String> values = captureID.getAllValues();
        assertNotNull(values);
        assertEquals(2, values.size());
        assertNotNull(values.get(0));
        assertNotNull(values.get(1));

        //Ensure that two subsequent runs produce different values
        assertNotEquals(values.get(0), values.get(1));
    }

    @Test
    public void testResponse_SetHeader_RequestID() throws Exception {
        ArgumentCaptor<String> captureID = ArgumentCaptor.forClass(String.class);

        new LoggingFilter().doFilter(request, response, chain);
        new LoggingFilter().doFilter(request, response, chain);

        verify(response, times(2)).setHeader(eq(LoggingFilter.HEADER_REQUEST_ID), captureID.capture());

        List<String> values = captureID.getAllValues();
        assertNotNull(values);
        assertEquals(2, values.size());
        assertNotNull(values.get(0));
        assertNotNull(values.get(1));

        //Ensure that two subsequent runs produce different values
        assertNotEquals(values.get(0), values.get(1));
    }

}
