package com.redhat.lightblue.rest.audit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

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
import com.redhat.lightblue.rest.audit.LightblueOperationChecker.Info;

/**
 * Filter all the request which must have data or metadata as their context path
 *
 * Created by lcestari on 4/1/15.
 */
@WebFilter(urlPatterns = {"/*"})
// Handle any request
public class LightblueAuditServletFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LightblueAuditServletFilter.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Override
    public void doFilter(final ServletRequest req, ServletResponse res,
            final FilterChain fChain) throws IOException, ServletException {
        /*
         NOTE: do not log:
           - query parameters
           - request body
         */
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("LightblueAuditServletFilter.doFilter invoked - begin");
        }

        if (!(req instanceof HttpServletRequest)) {
            LOGGER.info("Unable to audit request of type: " + req.getClass());
            fChain.doFilter(req, res);
            return;
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
                logEntryBuilder.setTimestampText(DATE_FORMAT.format(new Date()));
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

        fChain.doFilter(hReq, res);

        if (auditReqFlag) {
            try {
                if (stopwatch != null) {
                    stopwatch.stop();
                    logEntryBuilder.setTimeElapsedInNs(stopwatch.elapsed(TimeUnit.NANOSECONDS));
                }

                //Log Async. No reason to hold up the response for auditing purposes.
                new Thread(new AuditLogWritter(logEntryBuilder, hReq, res, isMetadata), "AuditServlet").start();

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
        LOGGER.debug("Destroying LightblueAuditServletFilter");
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        LOGGER.debug("Initializing LightblueAuditServletFilter");
    }

    private static class HttpServletResponseWrapperBuffered extends HttpServletResponseWrapper {
        private final ByteArrayPrintWriter byteArrayPrintWriter;

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

    private static class ByteArrayPrintWriter {
        private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        private final PrintWriter printWriter = new PrintWriter(byteArrayOutputStream);
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

    /**
     * Allows expensive audit work to be performed asynchronously from the actual user request.
     *
     * @author dcrissman
     */
    private static class AuditLogWritter implements Runnable {

        private final LogEntryBuilder logEntryBuilder;
        private final boolean isMetadata;
        private final HttpServletRequest req;
        private final ServletResponse res;

        public AuditLogWritter(LogEntryBuilder logEntryBuilder, HttpServletRequest req, ServletResponse res, boolean isMetadata) {
            this.logEntryBuilder = logEntryBuilder;
            this.isMetadata = isMetadata;
            this.req = req;
            this.res = res;
        }

        @Override
        public void run() {
            Info info = parseOperationEntityVersionStatus(req, isMetadata, logEntryBuilder);
            if (info != null) {
                logEntryBuilder.setOperation(info.operation);
                logEntryBuilder.setEntityName(info.entity);
                logEntryBuilder.setEntityVersion(info.version);
                logEntryBuilder.setEntityStatus(info.status);
            }

            if (res instanceof HttpServletResponseWrapperBuffered) {
                HttpServletResponseWrapperBuffered httpServletResponseWrapperBuffered = (HttpServletResponseWrapperBuffered) res;
                httpServletResponseWrapperBuffered.byteArrayPrintWriter.printWriter.flush();
                byte[] bytes = httpServletResponseWrapperBuffered.byteArrayPrintWriter.toByteArray();
                try {
                    res.getOutputStream().write(bytes);
                    logEntryBuilder.setResponseSize(bytes.length);
                } catch (IOException e) {
                    LOGGER.warn("Unable to write response bytes for audit", e);
                }
            }

            final LogEntry logEntry = logEntryBuilder.createLogEntry();
            String logEntryString = String.format(
                    "Audited lightblue rest request => " +
                            "{ " +
                            "\"initialTimestamp\":\"%s\", " +
                            "\"currentTimestamp\":\"%s\" , " +
                            "\"principal\":\"%s\" , " +
                            "\"resource\":\"%s\" , " +
                            "\"operation\":\"%s\" , " +
                            "\"entityName\":\"%s\" , " +
                            "\"entityVersion\":\"%s\" , " +
                            "\"entityStatus\":\"%s\" , " +
                            "\"requestSize\":\"%d\" , " +
                            "\"responseSize\":\"%d\" , " +
                            "\"timeElapsedInNs\":\"%d\"  " +
                            " }",
                    logEntry.getTimestampText(),
                    DATE_FORMAT.format(new Date()),
                    logEntry.getPrincipal().getName(),
                    logEntry.getResource(),
                    logEntry.getOperation(),
                    logEntry.getEntityName(),
                    logEntry.getEntityVersion(),
                    logEntry.getEntityStatus(),
                    logEntry.getRequestSize(),
                    logEntry.getResponseSize(),
                    logEntry.getTimeElapsedInNs()
                    );

            LOGGER.info(logEntryString);
        }

        private LightblueOperationChecker.Info parseOperationEntityVersionStatus(HttpServletRequest hReq, boolean isMetadata, LogEntryBuilder logEntryBuilder) {
            // List of methods in http://www.w3.org/Protocols/HTTP/Methods.html
            String method = hReq.getMethod().toUpperCase();
            boolean processed = false;
            String servletPath = hReq.getServletPath();

            LightblueOperationChecker.Info info = null;
            Matcher m = null;
            switch (method) {
                case "GET":
                    if (!processed) {
                        processed = true;
                        if (!isMetadata) {
                            if ((info = LightblueCrudOperationChecker.simpleFindVersionRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueCrudOperationChecker.simpleFindRegex.matches(servletPath)).found) {
                                return info;
                            }
                        } else {
                            if ((info = LightblueMetadataOperationChecker.getDepGraphVersionRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueMetadataOperationChecker.getDepGraphEntityRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueMetadataOperationChecker.getDepGraphRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueMetadataOperationChecker.getEntityRolesVersionRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueMetadataOperationChecker.getEntityRolesEntityRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueMetadataOperationChecker.getEntityRolesRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueMetadataOperationChecker.getEntityNamesRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueMetadataOperationChecker.getEntityNamesStatusRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueMetadataOperationChecker.getEntityVersionsRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueMetadataOperationChecker.getMetadataRegex.matches(servletPath)).found) {
                                return info;
                            }
                        }
                    }
                case "POST":
                    if (!processed) {
                        processed = true;
                        if (!isMetadata) {
                            if ((info = LightblueCrudOperationChecker.findRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueCrudOperationChecker.findVersionRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueCrudOperationChecker.deleteRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueCrudOperationChecker.deleteVersionRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueCrudOperationChecker.updateRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueCrudOperationChecker.updateVersionRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueCrudOperationChecker.saveRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueCrudOperationChecker.saveVersionRegex.matches(servletPath)).found) {
                                return info;
                            }

                        } else {
                            if ((info = LightblueMetadataOperationChecker.setDefaultVersionRegex.matches(servletPath)).found) {
                                return info;
                            }
                        }
                    }
                case "PUT":
                    if (!processed) {
                        processed = true;
                        if (!isMetadata) {
                            if ((info = LightblueCrudOperationChecker.insertRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueCrudOperationChecker.insertVersionRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueCrudOperationChecker.insertAltRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueCrudOperationChecker.insertAltVersionRegex.matches(servletPath)).found) {
                                return info;
                            }
                        } else {
                            if ((info = LightblueMetadataOperationChecker.createSchemaRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueMetadataOperationChecker.updateSchemaStatusRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueMetadataOperationChecker.updateEntityInfoRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueMetadataOperationChecker.createMetadataRegex.matches(servletPath)).found) {
                                return info;
                            }
                        }
                    }
                case "DELETE":
                    if (!processed) {
                        if (isMetadata) {
                            processed = true;
                            if ((info = LightblueMetadataOperationChecker.clearDefaultVersionRegex.matches(servletPath)).found) {
                                return info;
                            } else if ((info = LightblueMetadataOperationChecker.removeEntityRegex.matches(servletPath)).found) {
                                return info;
                            }
                        }
                    }
                case "HEAD":
                case "CONNECT":
                case "OPTIONS":
                case "TRACE":
                case "SEARCH":
                case "SPACEJUMP":
                case "CHECKIN":
                case "UNLINK":
                case "SHOWMETHOD":
                case "CHECKOUT":
                    // Valid HTTP method but not used yet
                default:
                    if (processed) {
                        // The URL missed all the patterns. Maybe there is a not mapped rest service or the URL is invalid
                        LOGGER.warn("The URL doesn't map to one of the rest services. Request URI: " + hReq.getRequestURI()); // TODO Unique case where the whole URI is logged, when it is an exception
                    } else {
                        // Called on of the not mapped HTTP methods
                        LOGGER.info("Invalid HTTP method: " + method);
                    }
            }
            return null;
        }

    }
}
