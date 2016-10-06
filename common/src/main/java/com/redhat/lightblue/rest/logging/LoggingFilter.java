package com.redhat.lightblue.rest.logging;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@WebFilter(urlPatterns = {"/*"})
public class LoggingFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingFilter.class);

    public static final String HEADER_REQUEST_ID = "RequestID";
    public static final String HEADER_REQUEST_PRINCIPAL = "RequestPrincipal";

    @Override
    public void destroy() {
        //Do Nothing!
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        try {
            String requestID = String.valueOf(UUID.randomUUID().getMostSignificantBits());
            LOGGER.debug(HEADER_REQUEST_ID + ": " + requestID);

            MDC.put(HEADER_REQUEST_ID, requestID);

            if (req instanceof HttpServletRequest) {
                HttpServletRequest httpReq = ((HttpServletRequest) req);
                httpReq.setAttribute(HEADER_REQUEST_ID, requestID);

                //Add principal to MDC
                if (httpReq.getUserPrincipal() != null) {
                    String principal = httpReq.getUserPrincipal().getName();
                    if (principal != null) {
                        MDC.put(HEADER_REQUEST_PRINCIPAL, principal);
                    }
                }
            }
            else {
                LOGGER.info("ServletRequest of type: " + req.getClass());
            }

            if (resp instanceof HttpServletResponse) {
                ((HttpServletResponse) resp).setHeader(HEADER_REQUEST_ID, requestID);
            }
            else {
                LOGGER.info("ServletResponse of type: " + resp.getClass());
            }

            LOGGER.debug("log for testing");

            chain.doFilter(req, resp);
        } finally {
            MDC.remove(HEADER_REQUEST_ID);
            MDC.remove(HEADER_REQUEST_PRINCIPAL);
        }
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        //Do Nothing!
    }

}
