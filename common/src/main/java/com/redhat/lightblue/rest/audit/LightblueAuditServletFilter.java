package com.redhat.lightblue.rest.audit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

/**
 * Filter all the request which must have data or metadata as their context path
 *
 * Created by lcestari on 4/1/15.
 */
@WebFilter(urlPatterns = {"/*"})
// Handle any request
public class LightblueAuditServletFilter implements Filter {

    static final Logger LOGGER = LoggerFactory.getLogger(LightblueAuditServletFilter.class);

    private static final ExecutorService jobExecutor = Executors.newCachedThreadPool();

    @Override
    public void doFilter(final ServletRequest req, ServletResponse res,
            final FilterChain fChain) throws IOException, ServletException {
        /*
         NOTE: do not log:
           - query parameters
           - request body
         */
        if (!(req instanceof HttpServletRequest)) {
            LOGGER.info("Unable to audit request of type: " + req.getClass());
            fChain.doFilter(req, res);
            return;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("LightblueAuditServletFilter.doFilter invoked - begin");
        }

        HttpServletRequest hReq = (HttpServletRequest) req;
        Principal p = hReq.getUserPrincipal();
        LogEntryBuilder logEntryBuilder = new LogEntryBuilder();

        boolean isMetadata = LightblueResource.METADATA.getPattern().matcher(hReq.getContextPath()).matches();
        boolean isCrud = LightblueResource.CRUD.getPattern().matcher(hReq.getContextPath()).matches();

        boolean auditReqFlag = true;
        if (!(isMetadata || isCrud)) {
            LOGGER.debug("Unable to perform audits on requests that are not data or metadata requests");
            auditReqFlag = false;
        }
        else if (p == null) {
            LOGGER.debug("Unable to perform audits on requests without a principle");
            auditReqFlag = false;
        }

        // audit authenticated requests

        Stopwatch stopwatch = null;
        if (auditReqFlag) {
            try {
                logEntryBuilder.setPrincipal(p);
                logEntryBuilder.setRequestSize(hReq.getContentLength());
                logEntryBuilder.setResource(hReq.getContextPath());

                if (res instanceof HttpServletResponse) {
                    HttpServletResponse httpServletResponseWrapperBuffered =
                            new HttpServletResponseWrapperBuffered((HttpServletResponse) res, new ByteArrayPrintWriter());
                    res = httpServletResponseWrapperBuffered;
                }

                stopwatch = Stopwatch.createStarted();
            } catch (Exception e) {
                //If audit fails, there is no reason to prevent the crud operation from completing.
                auditReqFlag = false;
                LOGGER.warn("Unexpected exception while attempting to start audit log", e);
            }
        }

        Date beforeTimeStamp = new Date();
        fChain.doFilter(hReq, res);

        if (auditReqFlag) {
            try {
                if (stopwatch != null) {
                    stopwatch.stop();
                    logEntryBuilder.setTimeElapsedInNs(stopwatch.elapsed(TimeUnit.NANOSECONDS));
                }

                //Log Async. No reason to hold up the response for auditing purposes.
                jobExecutor.execute(new LightblueAuditLogWritter(logEntryBuilder, hReq, res, isMetadata, beforeTimeStamp));

            } catch (RejectedExecutionException e) {
                LOGGER.warn("Audit thread rejected from executor", e);
            } catch (Exception e) {
                LOGGER.warn("Unexpected exception while attempting to finish audit log", e);
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("LightblueAuditServletFilter.doFilter invoked - end");
        }
    }

    @Override
    public void destroy() {
        jobExecutor.shutdown();
        LOGGER.debug("Destroying LightblueAuditServletFilter");
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        LOGGER.debug("Initializing LightblueAuditServletFilter");
    }

    static class HttpServletResponseWrapperBuffered extends HttpServletResponseWrapper {
        final ByteArrayPrintWriter byteArrayPrintWriter;

        public HttpServletResponseWrapperBuffered(HttpServletResponse httpServletResponse, ByteArrayPrintWriter byteArrayPrintWriter) {
            super(httpServletResponse);
            this.byteArrayPrintWriter = byteArrayPrintWriter;
        }

        @Override
        public PrintWriter getWriter() {
            return byteArrayPrintWriter.getWriter();
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return byteArrayPrintWriter.getStream();
        }

    }

    static class ByteArrayPrintWriter {
        private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintWriter printWriter = new PrintWriter(byteArrayOutputStream);
        private final ServletOutputStream byteArrayServletStream = new ByteArrayServletStream(byteArrayOutputStream);

        public PrintWriter getWriter() {
            return printWriter;
        }

        public ServletOutputStream getStream() {
            return byteArrayServletStream;
        }

        byte[] toByteArray() {
            return byteArrayOutputStream.toByteArray();
        }
    }

    private static class ByteArrayServletStream extends ServletOutputStream {
        private final ByteArrayOutputStream byteArrayOutputStream;

        ByteArrayServletStream(ByteArrayOutputStream byteArrayOutputStream) {
            this.byteArrayOutputStream = byteArrayOutputStream;
        }

        @Override
        public void write(int param) throws IOException {
            byteArrayOutputStream.write(param);
        }
    }
}
