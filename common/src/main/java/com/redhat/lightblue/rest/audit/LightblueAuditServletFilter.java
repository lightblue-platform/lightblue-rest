package com.redhat.lightblue.rest.audit;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter all the request which must have data or metadata as their context path
 *
 * Created by lcestari on 4/1/15.
 */
@WebFilter(urlPatterns = { "/*" }) // Handle any request
public class LightblueAuditServletFilter implements Filter {

    public static final String YYYY_MM_DD_T_HH_MM_SS_SSSZ = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static final Logger LOGGER = LoggerFactory.getLogger(LightblueAuditServletFilter.class);
    private static final ThreadLocal<SimpleDateFormat> threadDateFormat =  new ThreadLocal<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain fChain) throws IOException, ServletException {
        /*
         NOTE: do not log:
           - query parameters
           - request body
         */
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("LightblueAuditServletFilter.doFilter invoked - begin");
        }

        HttpServletRequest hReq = (HttpServletRequest) req;
        Principal p = hReq.getUserPrincipal();
        Stopwatch stopwatch = null;
        LogEntryBuilder logEntryBuilder = null;

        boolean isMetadata = LightblueResource.METADATA.getPattern().matcher(hReq.getContextPath()).matches();
        boolean isCrud = LightblueResource.CRUD.getPattern().matcher(hReq.getContextPath()).matches();

        // audit authenticated requests
        boolean auditReqFlag = p != null && (isMetadata || isCrud);

        if(auditReqFlag){
            if(threadDateFormat.get() == null){
                threadDateFormat.set(new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS_SSSZ));
            }
            logEntryBuilder = new LogEntryBuilder();
            logEntryBuilder.setPrincipal(p);
            logEntryBuilder.setRequestSize(hReq.getContentLength());
            logEntryBuilder.setTimestampText(threadDateFormat.get().format(new Date()));
            logEntryBuilder.setResource(hReq.getContextPath());

            setOperationEnittyVersionStatus(hReq, isMetadata, logEntryBuilder);


            HttpServletResponse httpServletResponse = (HttpServletResponse) res;
            HttpServletResponse httpServletResponseWrapperBuffered = new HttpServletResponseWrapperBuffered(httpServletResponse,new ByteArrayPrintWriter());
            res = httpServletResponseWrapperBuffered;
        }

        if(auditReqFlag) {
            stopwatch = Stopwatch.createStarted();
        }

        fChain.doFilter(hReq, res);

        if(auditReqFlag) {
            stopwatch.stop();
            long elapsedTime = stopwatch.elapsed(TimeUnit.NANOSECONDS); // elapsedTime in ns
            logEntryBuilder.setTimeElapsedInNs(elapsedTime);
            HttpServletResponseWrapperBuffered httpServletResponseWrapperBuffered = (HttpServletResponseWrapperBuffered) res;
            httpServletResponseWrapperBuffered.byteArrayPrintWriter.printWriter.flush();
            byte[] bytes = httpServletResponseWrapperBuffered.byteArrayPrintWriter.toByteArray();
            res.getOutputStream().write(bytes);
            logEntryBuilder.setResponseSize(bytes.length);
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
                    threadDateFormat.get().format(new Date()),
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

        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("LightblueAuditServletFilter.doFilter invoked - end");
        }
    }

    protected void setOperationEnittyVersionStatus(HttpServletRequest hReq, boolean isMetadata, LogEntryBuilder logEntryBuilder) {
        // List of methods in http://www.w3.org/Protocols/HTTP/Methods.html
        String method = hReq.getMethod().toUpperCase();
        boolean processed = false;
        String servletPath = hReq.getServletPath();

        LightblueOperationChecker.Info info = null;
        Matcher m = null;
        switch (method){
            case "GET":
                if(!processed) {
                    processed = true;
                    if (!isMetadata) {
                        if ((info = LightblueCrudOperationChecker.simpleFindVersionRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueCrudOperationChecker.simpleFindRegex.matches(servletPath)).found) {
                            break;
                        }
                    } else {
                        if ((info = LightblueMetadataOperationChecker.getDepGraphVersionRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueMetadataOperationChecker.getDepGraphEntityRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueMetadataOperationChecker.getDepGraphRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueMetadataOperationChecker.getEntityRolesVersionRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueMetadataOperationChecker.getEntityRolesEntityRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueMetadataOperationChecker.getEntityRolesRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueMetadataOperationChecker.getEntityNamesRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueMetadataOperationChecker.getEntityNamesStatusRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueMetadataOperationChecker.getEntityVersionsRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueMetadataOperationChecker.getMetadataRegex.matches(servletPath)).found) {
                            break;
                        }
                    }
                }
            case "POST":
                if(!processed) {
                    processed = true;
                    if (!isMetadata) {
                        if ((info = LightblueCrudOperationChecker.findRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueCrudOperationChecker.findVersionRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueCrudOperationChecker.deleteRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueCrudOperationChecker.deleteVersionRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueCrudOperationChecker.updateRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueCrudOperationChecker.updateVersionRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueCrudOperationChecker.saveRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueCrudOperationChecker.saveVersionRegex.matches(servletPath)).found) {
                            break;
                        }

                    } else {
                        if ((info = LightblueMetadataOperationChecker.setDefaultVersionRegex.matches(servletPath)).found) {
                            break;
                        }
                    }
                }
            case "PUT":
                if(!processed) {
                    processed = true;
                    if (!isMetadata) {
                        if ((info = LightblueCrudOperationChecker.insertRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueCrudOperationChecker.insertVersionRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueCrudOperationChecker.insertAltRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueCrudOperationChecker.insertAltVersionRegex.matches(servletPath)).found) {
                            break;
                        }
                    } else {
                        if ((info = LightblueMetadataOperationChecker.createSchemaRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueMetadataOperationChecker.updateSchemaStatusRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueMetadataOperationChecker.updateEntityInfoRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueMetadataOperationChecker.createMetadataRegex.matches(servletPath)).found) {
                            break;
                        }
                    }
                }
            case "DELETE":
                if(!processed) {
                    if (isMetadata) {
                        processed = true;
                        if ((info = LightblueMetadataOperationChecker.clearDefaultVersionRegex.matches(servletPath)).found) {
                            break;
                        } else if ((info = LightblueMetadataOperationChecker.removeEntityRegex.matches(servletPath)).found) {
                            break;
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
                if(processed){
                    // The URL missed all the patterns. Maybe there is a not mapped rest service or the URL is invalid
                    LOGGER.warn("The URL doesn't map to one of the rest services. Request URI: " + hReq.getRequestURI()); // TODO Unique case where the whole URI is logged, when it is an exception
                } else {
                    // Called on of the not mapped HTTP methods
                    LOGGER.info("Invalid HTTP method: " + method);
                }
        }
        if(info != null) {
            logEntryBuilder.setOperation(info.operation);
            logEntryBuilder.setEntityName(info.entity);
            logEntryBuilder.setEntityVersion(info.version);
            logEntryBuilder.setEntityStatus(info.status);
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
        private ByteArrayPrintWriter byteArrayPrintWriter;

        public HttpServletResponseWrapperBuffered(HttpServletResponse httpServletResponse, ByteArrayPrintWriter byteArrayPrintWriter) {
            super(httpServletResponse);
            this.byteArrayPrintWriter = byteArrayPrintWriter;
        }

        public PrintWriter getWriter() {
            return byteArrayPrintWriter.getWriter();
        }

        public ServletOutputStream getOutputStream() {
            return byteArrayPrintWriter.getStream();
        }

    }

    private static class ByteArrayPrintWriter {
        private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        private PrintWriter printWriter = new PrintWriter(byteArrayOutputStream);
        private ServletOutputStream byteArrayServletStream = new ByteArrayServletStream(byteArrayOutputStream);

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
        private ByteArrayOutputStream byteArrayOutputStream;

        ByteArrayServletStream(ByteArrayOutputStream byteArrayOutputStream) {
            this.byteArrayOutputStream = byteArrayOutputStream;
        }

        public void write(int param) throws IOException {
            byteArrayOutputStream.write(param);
        }
    }
}