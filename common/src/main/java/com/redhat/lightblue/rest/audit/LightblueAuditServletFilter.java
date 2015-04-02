package com.redhat.lightblue.rest.audit;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lcestari on 4/1/15.
 */
@WebFilter(urlPatterns = { "/*" }) // Handle any request
public class LightblueAuditServletFilter implements Filter {
    public static final String YYYY_MM_DD_T_HH_MM_SS_SSSZ = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    //AbstractCrudResource @Path("/find/{entity}") or @Path("/find/{entity}/{version}") w/ querystring simpleFind
    public static final Pattern simpleFindVersionRegex = Pattern.compile("/+find/+(\\w)/+([-._:A-Za-z0-9]+)/*");
    public static final Pattern simpleFindRegex = Pattern.compile("/+find/+(\\w)/*");
    //AbstractMetadataResource @Path("/dependencies") or @Path("/{entity}/dependencies") or @Path("/{entity}/{version}/dependencies")
    public static final Pattern getDepGraphVersionRegex = Pattern.compile("/+(\\w)/+([-._:A-Za-z0-9]+)/+dependencies/*");
    public static final Pattern getDepGraphEntityRegex = Pattern.compile("/+(\\w)/+dependencies/*");
    public static final Pattern getDepGraphRegex = Pattern.compile("/+dependencies/*");
    //AbstractMetadataResource @Path("/roles") or @Path("/{entity}/roles") or @Path("/{entity}/{version}/roles") getEntityRoles
    public static final Pattern getEntityRolesVersionRegex = Pattern.compile("/+(\\w)/+([-._:A-Za-z0-9]+)/+roles/*");
    public static final Pattern getEntityRolesEntityRegex = Pattern.compile("/+(\\w)/+roles/*");
    public static final Pattern getEntityRolesRegex = Pattern.compile("/+roles/*");
    //AbstractMetadataResource @Path("/") or @Path("/s={statuses}") getEntityNames
    public static final Pattern getEntityNamesRegex = Pattern.compile("/*");
    public static final Pattern getEntityNamesStatusRegex = Pattern.compile("/+s=(\\w)/*");
    //AbstractMetadataResource @Path("/{entity}") getEntityVersions
    public static final Pattern getEntityVersionsRegex = Pattern.compile("/+(\\w)/*");
    //AbstractMetadataResource @Path("/{entity}/{version}") getMetadata
    public static final Pattern getMetadataRegex = Pattern.compile("/+(\\w)/+([-._:A-Za-z0-9]+)/*");

    //AbstractCrudResource @Path("/save/{entity}") or @Path("/save/{entity}/{version}") save
    public static final Pattern saveRegex = Pattern.compile("/+save/+(\\w)/+/*");
    public static final Pattern saveVersionRegex = Pattern.compile("/+save/+(\\w)/+([-._:A-Za-z0-9]+)/*");
    //AbstractCrudResource @Path("/update/{entity}") or @Path("/update/{entity}/{version}") update
    public static final Pattern updateRegex = Pattern.compile("/+update/+(\\w)/+/*");
    public static final Pattern updateVersionRegex = Pattern.compile("/+update/+(\\w)/+([-._:A-Za-z0-9]+)/*");
    //AbstractCrudResource @Path("/delete/{entity}") or @Path("/delete/{entity}/{version}") delete
    public static final Pattern deleteRegex = Pattern.compile("/+delete/+(\\w)/+/*");
    public static final Pattern deleteVersionRegex = Pattern.compile("/+delete/+(\\w)/+([-._:A-Za-z0-9]+)/*");
    //AbstractCrudResource @Path("/find/{entity}") or @Path("/find/{entity}/{version}") find
    public static final Pattern findRegex = Pattern.compile("/+find/+(\\w)/+/*");
    public static final Pattern findVersionRegex = Pattern.compile("/+find/+(\\w)/+([-._:A-Za-z0-9]+)/*");
    //AbstractMetadataResource @Path("/{entity}/{version}/default") setDefaultVersion
    public static final Pattern setDefaultVersionRegex = Pattern.compile("/+(\\w)/+([-._:A-Za-z0-9]+)/+default/*");

    //AbstractCrudResource @Path("/insert/{entity}") or @Path("/insert/{entity}/{version}")  insert
    public static final Pattern insertRegex = Pattern.compile("/+insert/+(\\w)/*");
    public static final Pattern insertVersionRegex = Pattern.compile("/+insert/+(\\w)/+([-._:A-Za-z0-9]+)/*");
    //AbstractCrudResource @Path("/{entity}") or @Path("/{entity}/{version}") is insertAlt
    public static final Pattern insertAltRegex = Pattern.compile("/+(\\w)/*");
    public static final Pattern insertAltVersionRegex = Pattern.compile("/+(\\w)/+([-._:A-Za-z0-9]+)/*");
    //AbstractMetadataResource @Path("/{entity}/{version}") createMetadata
    public static final Pattern createMetadataRegex = Pattern.compile("/+(\\w)/+([-._:A-Za-z0-9]+)/*");
    //AbstractMetadataResource @Path("/{entity}/schema={version}") createSchema
    public static final Pattern createSchemaRegex = Pattern.compile("/+(\\w)/+schema=([-._:A-Za-z0-9]+)/*");
    //AbstractMetadataResource @Path("/{entity}") updateEntityInfo
    public static final Pattern updateEntityInfoRegex = Pattern.compile("/+(\\w)/*");
    //AbstractMetadataResource @Path("/{entity}/{version}/{status}") updateSchemaStatus
    public static final Pattern updateSchemaStatusRegex = Pattern.compile("/+(\\w)/+([-._:A-Za-z0-9]+)/+(\\w)/*");

    //AbstractMetadataResource @Path("/{entity}") removeEntity
    public static final Pattern removeEntityRegex = Pattern.compile("/+(\\w)/*");
    //AbstractMetadataResource @Path("/{entity}/default") clearDefaultVersion
    public static final Pattern clearDefaultVersionRegex = Pattern.compile("/+(\\w)/+default/*");


    public static final Pattern metatadataRegex = Pattern.compile("/+metadata/*");
    public static final Pattern crudRegex = Pattern.compile("/+data/*");

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

        boolean isMetadata = metatadataRegex.matcher(hReq.getContextPath()).matches();
        boolean isCrud = crudRegex.matcher(hReq.getContextPath()).matches();

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
        }

        if(auditReqFlag) {
            stopwatch = Stopwatch.createStarted();
        }

        fChain.doFilter(hReq, res);

        if(auditReqFlag) {
            stopwatch.stop();
            long elapsedTime = stopwatch.elapsed(TimeUnit.NANOSECONDS); // elapsedTime in ns
            logEntryBuilder.setTimeElapsedInNs(elapsedTime);
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
        String operation = null;
        String entity = null;
        String version = null;
        String status = null;
        // List of methods in http://www.w3.org/Protocols/HTTP/Methods.html
        String method = hReq.getMethod().toUpperCase();
        boolean processed = false;
        switch (method){
            case "GET":
                if(!processed) {
                    processed = true;
                    if (!isMetadata) {
                        Matcher mSimpleFindVersion = simpleFindVersionRegex.matcher(hReq.getServletPath());
                        if (mSimpleFindVersion.matches()) {
                            operation = "GET /find/{entity}/{version}";
                            entity = mSimpleFindVersion.group(1);
                            version = mSimpleFindVersion.group(2);
                            break;
                        } else {
                            Matcher mSimpleFind = simpleFindRegex.matcher(hReq.getServletPath());
                            if (mSimpleFind.matches()) {
                                operation = "GET /find/{entity}";
                                entity = mSimpleFind.group(1);
                                break;
                            }
                        }
                    } else {
                        Matcher mDepGraphVersion = getDepGraphVersionRegex.matcher(hReq.getServletPath());
                        if (mDepGraphVersion.matches()) {
                            operation = "GET /{entity}/{version}/dependencies";
                            entity = mDepGraphVersion.group(1);
                            version = mDepGraphVersion.group(2);
                            break;
                        } else {
                            Matcher mDepGraphEntity = getDepGraphEntityRegex.matcher(hReq.getServletPath());
                            if (mDepGraphEntity.matches()) {
                                operation = "GET /{entity}/dependencies";
                                entity = mDepGraphEntity.group(1);
                                break;
                            } else {
                                Matcher mDepGraph = getDepGraphRegex.matcher(hReq.getServletPath());
                                if (mDepGraph.matches()) {
                                    operation = "GET /dependencies";
                                    break;
                                } else {
                                    Matcher mEntityRolesVersion = getEntityRolesVersionRegex.matcher(hReq.getServletPath());
                                    if (mEntityRolesVersion.matches()) {
                                        operation = "GET /{entity}/{version}/roles";
                                        entity = mEntityRolesVersion.group(1);
                                        version = mEntityRolesVersion.group(2);
                                        break;
                                    } else {
                                        Matcher mEntityRolesEntity = getEntityRolesEntityRegex.matcher(hReq.getServletPath());
                                        if (mEntityRolesEntity.matches()) {
                                            operation = "GET /{entity}/roles";
                                            entity = mEntityRolesEntity.group(1);
                                            break;
                                        } else {
                                            Matcher mEntityRoles = getEntityRolesRegex.matcher(hReq.getServletPath());
                                            if (mEntityRoles.matches()) {
                                                operation = "GET /roles";
                                                break;
                                            } else {
                                                Matcher mGetEntityNames = getEntityNamesRegex.matcher(hReq.getServletPath());
                                                if (mGetEntityNames.matches()) {
                                                    operation = "GET /";
                                                    break;
                                                } else {
                                                    Matcher mGetEntityNamesStatus = getEntityNamesStatusRegex.matcher(hReq.getServletPath());
                                                    if (mGetEntityNamesStatus.matches()) {
                                                        operation = "GET /s={statuses}";
                                                        status = mGetEntityNamesStatus.group(1);
                                                        break;
                                                    } else {
                                                        Matcher mGetEntityVersions = getEntityVersionsRegex.matcher(hReq.getServletPath());
                                                        if (mGetEntityVersions.matches()) {
                                                            operation = "GET /{entity}";
                                                            entity = mGetEntityVersions.group(1);
                                                            break;
                                                        } else {
                                                            Matcher mGetMetadata = getMetadataRegex.matcher(hReq.getServletPath());
                                                            if (mGetMetadata.matches()) {
                                                                operation = "GET /{entity}/{version}";
                                                                entity = mGetMetadata.group(1);
                                                                version = mGetMetadata.group(2);
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            case "POST":
                if(!processed) {
                    processed = true;
                    if (!isMetadata) {
                        Matcher mFind = findRegex.matcher(hReq.getServletPath());
                        if (mFind.matches()) {
                            operation = "POST /find/{entity}";
                            entity = mFind.group(1);
                            break;
                        } else {
                            Matcher mFindVersion = findVersionRegex.matcher(hReq.getServletPath());
                            if (mFindVersion.matches()) {
                                operation = "POST /find/{entity}/{version}";
                                entity = mFindVersion.group(1);
                                version = mFindVersion.group(2);
                                break;
                            } else {
                                Matcher mDel = deleteRegex.matcher(hReq.getServletPath());
                                if (mDel.matches()) {
                                    operation = "POST /delete/{entity}";
                                    entity = mDel.group(1);
                                    break;
                                } else {
                                    Matcher mDelVersion = deleteVersionRegex.matcher(hReq.getServletPath());
                                    if (mDelVersion.matches()) {
                                        operation = "POST /delete/{entity}/{version}";
                                        entity = mDelVersion.group(1);
                                        version = mDelVersion.group(2);
                                        break;
                                    } else {
                                        Matcher mUpdate = updateRegex.matcher(hReq.getServletPath());
                                        if (mUpdate.matches()) {
                                            operation = "POST /update/{entity}";
                                            entity = mUpdate.group(1);
                                            break;
                                        } else {
                                            Matcher mUpdateVersion = updateVersionRegex.matcher(hReq.getServletPath());
                                            if (mUpdateVersion.matches()) {
                                                operation = "POST /update/{entity}/{version}";
                                                entity = mUpdateVersion.group(1);
                                                version = mUpdateVersion.group(2);
                                                break;
                                            } else {
                                                Matcher mSave = saveRegex.matcher(hReq.getServletPath());
                                                if (mSave.matches()) {
                                                    operation = "POST /save/{entity}";
                                                    entity = mSave.group(1);
                                                    break;
                                                } else {
                                                    Matcher mSaveVersion = saveVersionRegex.matcher(hReq.getServletPath());
                                                    if (mSaveVersion.matches()) {
                                                        operation = "POST /save/{entity}/{version}";
                                                        entity = mSaveVersion.group(1);
                                                        version = mSaveVersion.group(2);
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Matcher mSetDef = setDefaultVersionRegex.matcher(hReq.getServletPath());
                        if (mSetDef.matches()) {
                            operation = "POST /{entity}/{version}/default";
                            entity = mSetDef.group(1);
                            break;
                        }
                    }
                }
            case "PUT":
                if(!processed) {
                    processed = true;
                    if (!isMetadata) {
                        Matcher mInsert = insertRegex.matcher(hReq.getServletPath());
                        if (mInsert.matches()) {
                            operation = "PUT /insert/{entity}";
                            entity = mInsert.group(1);
                            break;
                        } else {
                            Matcher mInsertV = insertVersionRegex.matcher(hReq.getServletPath());
                            if (mInsertV.matches()) {
                                operation = "PUT /insert/{entity}/{version}";
                                entity = mInsertV.group(1);
                                version = mInsertV.group(2);
                                break;
                            } else {
                                Matcher mInsertAlt = insertAltRegex.matcher(hReq.getServletPath());
                                if (mInsertAlt.matches()) {
                                    operation = "PUT /{entity}";
                                    entity = mInsertAlt.group(1);
                                    break;
                                } else {
                                    Matcher mInsertAltV = insertAltVersionRegex.matcher(hReq.getServletPath());
                                    if (mInsertAltV.matches()) {
                                        operation = "PUT /{entity}/{version}t";
                                        entity = mInsertAltV.group(1);
                                        version = mInsertAltV.group(2);
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        Matcher mCreateS = createSchemaRegex.matcher(hReq.getServletPath());
                        if (mCreateS.matches()) {
                            operation = "PUT /{entity}/schema={version}";
                            entity = mCreateS.group(1);
                            version = mCreateS.group(2);
                            break;
                        } else {
                            Matcher mUpdateS = updateSchemaStatusRegex.matcher(hReq.getServletPath());
                            if (mUpdateS.matches()) {
                                operation = "PUT /{entity}/{version}/{status}";
                                entity = mUpdateS.group(1);
                                version = mUpdateS.group(2);
                                status = mUpdateS.group(3);
                                break;
                            } else {
                                Matcher mUpdateE = updateEntityInfoRegex.matcher(hReq.getServletPath());
                                if (mUpdateE.matches()) {
                                    operation = "PUT /{entity}";
                                    entity = mUpdateE.group(1);
                                    break;
                                } else {
                                    Matcher mCreateM = createMetadataRegex.matcher(hReq.getServletPath());
                                    if (mCreateM.matches()) {
                                        operation = "PUT /{entity}/{version}";
                                        entity = mCreateM.group(1);
                                        version = mCreateM.group(2);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            case "DELETE":
                if(!processed) {
                    processed = true;
                    Matcher mClear = clearDefaultVersionRegex.matcher(hReq.getServletPath());
                    if (mClear.matches()) {
                        operation = "DELETE /{entity}/default";
                        entity = mClear.group(1);
                        version = "default";
                        break;
                    } else {
                        Matcher mRemove = removeEntityRegex.matcher(hReq.getServletPath());
                        if (mRemove.matches()) {
                            operation = "DELETE /{entity}";
                            entity = mRemove.group(1);
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
        logEntryBuilder.setOperation(operation);
        logEntryBuilder.setEntityName(entity);
        logEntryBuilder.setEntityVersion(version);
        logEntryBuilder.setEntityStatus(status);
    }

    @Override
    public void destroy() {
        LOGGER.debug("Destroying LightblueAuditServletFilter");
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        LOGGER.debug("Initializing LightblueAuditServletFilter");
    }

}