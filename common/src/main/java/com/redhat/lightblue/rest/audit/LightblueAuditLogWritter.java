package com.redhat.lightblue.rest.audit;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.redhat.lightblue.rest.audit.LightblueAuditServletFilter.HttpServletResponseWrapperBuffered;
import com.redhat.lightblue.rest.audit.LightblueOperationChecker.Info;

/**
 * Allows expensive audit work to be performed asynchronously from the actual user request.
 *
 * @author dcrissman
 */
public class LightblueAuditLogWritter implements Runnable {

    private static final String YYYY_MM_DD_T_HH_MM_SS_SSSZ = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS_SSSZ);

    private final LogEntryBuilder logEntryBuilder;
    private final boolean isMetadata;
    private final HttpServletRequest req;
    private final ServletResponse res;
    private final Date beforeTimestamp;

    public LightblueAuditLogWritter(LogEntryBuilder logEntryBuilder, HttpServletRequest req, ServletResponse res, boolean isMetadata, Date beforeTimestamp) {
        this.logEntryBuilder = logEntryBuilder;
        this.isMetadata = isMetadata;
        this.req = req;
        this.res = res;
        this.beforeTimestamp = beforeTimestamp;
    }

    @Override
    public void run() {
        Info info = parseOperationEntityVersionStatus(req, isMetadata, logEntryBuilder);

        logEntryBuilder.setTimestampText(DATE_FORMAT.format(beforeTimestamp));

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
                LightblueAuditServletFilter.LOGGER.warn("Unable to write response bytes for audit", e);
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

        LightblueAuditServletFilter.LOGGER.info(logEntryString);
    }

    private LightblueOperationChecker.Info parseOperationEntityVersionStatus(HttpServletRequest hReq, boolean isMetadata, LogEntryBuilder logEntryBuilder) {
        // List of methods in http://www.w3.org/Protocols/HTTP/Methods.html
        String method = hReq.getMethod().toUpperCase();
        boolean processed = false;
        String servletPath = hReq.getServletPath();

        LightblueOperationChecker.Info info = null;
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
                    LightblueAuditServletFilter.LOGGER.warn("The URL doesn't map to one of the rest services. Request URI: " + hReq.getRequestURI()); // TODO Unique case where the whole URI is logged, when it is an exception
                } else {
                    // Called on of the not mapped HTTP methods
                    LightblueAuditServletFilter.LOGGER.info("Invalid HTTP method: " + method);
                }
        }
        return null;
    }

}
