package com.redhat.lightblue.rest.audit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

import com.mifmif.common.regex.Generex;

/**
 * Created by lcestari on 4/2/15.
 */
public class LightblueAuditServletFilterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LightblueAuditServletFilterTest.class);

    static TeeInMemoryStringPrintStream out;
    static TeeInMemoryStringPrintStream err;
    static Field simpleLoggerOutField;
    static PrintStream simpleLoggerPrintStream;

    LightblueAuditServletFilter cut;

    FakeHttpServletRequest req;
    FakeHttpServletResponse res;
    FilterChain fChain = new FilterChain() {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            return;
        }
    };

    @BeforeClass
    public static void beforeAllTest() throws Exception {
        System.out.flush();
        out = TeeInMemoryStringPrintStream.teeTo(System.out);
        System.setOut(out);

        System.err.flush();
        err = TeeInMemoryStringPrintStream.teeTo(System.err);
        System.setErr(err);

        simpleLoggerOutField = SimpleLogger.class.getDeclaredField("TARGET_STREAM");
        simpleLoggerOutField.setAccessible(true);
        simpleLoggerPrintStream = (PrintStream) simpleLoggerOutField.get(null);
        simpleLoggerPrintStream.flush();
        simpleLoggerOutField.set(null, err); // Using the default configuration, SimpleLogger will use the err. Either way, we are going to reset to the original value after this set of tests
    }

    @AfterClass
    static public void afterAllTest() throws Exception {
        System.out.flush();
        System.setOut(out.originalPrintStream);

        System.err.flush();
        System.setErr(err.originalPrintStream);

        simpleLoggerOutField.set(null, simpleLoggerPrintStream);
    }

    @Before
    public void setUp() throws Exception {
        cut = new LightblueAuditServletFilter();
        req = new FakeHttpServletRequest();
        res = new FakeHttpServletResponse();
        out.resetInMemoryConsole();
        err.resetInMemoryConsole();
    }

    // Context path is not data or metadata, so it should not audit
    @Test
    public void testDoFilterNoAudit() throws Exception {
        final String wrong = "X";
        req.contextPath = "/data" + wrong;
        req.principal = new FakePrincipal("UserName");
        cut.doFilter(req, res, fChain);
        basicCheckAndReset();

        req.contextPath = "/data";
        req.principal = null;
        cut.doFilter(req, res, fChain);
        basicCheckAndReset();

        req.contextPath = "/metadata" + wrong;
        req.principal = new FakePrincipal("UserName");
        cut.doFilter(req, res, fChain);
        basicCheckAndReset();

        req.contextPath = "/metadata";
        req.principal = null;
        cut.doFilter(req, res, fChain);
        basicCheckAndReset();
    }

    // Context path is data or metadata, but it doesnt match any rest resource that we have
    @Test
    public void testDoFilterNoyValid() throws Exception {
        req.contextPath = "/metadata";
        req.principal = new FakePrincipal("UserName");

        req.method = "DELETE";// Not valid

        req.servletPath = "/MISTAKE/entity/version1.0-1:2/";// Not valid
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("The URL doesn't map to one of the rest services. Request URI"));
        err.resetInMemoryConsole();

        req.method = "CHECKOUT";
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("Invalid HTTP method:"));
    }

    // Auditable cases for emtadata
    @Test
    public void testDoFilterMetadata_GET() throws Exception {
        req.contextPath = "/metadata";
        req.principal = new FakePrincipal("UserName");

        req.method = "GET";

        req.servletPath = "/entity/version1.0-1:2/dependencies"; //LightblueMetadataOperationChecker.getDepGraphVersionRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"GET /{entity}/{version}/dependencies\""));
        basicCheckAndReset();

        req.servletPath = "/entity/dependencies"; //LightblueMetadataOperationChecker.getDepGraphEntityRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"GET /{entity}/dependencies\""));
        basicCheckAndReset();

        req.servletPath = "/dependencies"; //LightblueMetadataOperationChecker.getDepGraphRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"GET /dependencies\""));
        basicCheckAndReset();

        req.servletPath = "/entity/version1.0-1:2/roles"; //LightblueMetadataOperationChecker.getEntityRolesVersionRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"GET /{entity}/{version}/roles\""));
        basicCheckAndReset();

        req.servletPath = "/entity/roles"; //LightblueMetadataOperationChecker.getEntityRolesEntityRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"GET /{entity}/roles\""));
        basicCheckAndReset();

        req.servletPath = "/roles"; //LightblueMetadataOperationChecker.getEntityRolesRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"GET /roles\""));
        basicCheckAndReset();

        req.servletPath = "/"; //LightblueMetadataOperationChecker.getEntityNamesRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"GET /\""));
        basicCheckAndReset();

        req.servletPath = "/s=asdass"; //LightblueMetadataOperationChecker.getEntityNamesStatusRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"GET /s={statuses}\""));
        basicCheckAndReset();

        req.servletPath = "/newEntity"; //LightblueMetadataOperationChecker.getEntityVersionsRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"GET /{entity}\""));
        basicCheckAndReset();

        req.servletPath = "/newEntity/15.9q:b"; //LightblueMetadataOperationChecker.getMetadataRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"GET /{entity}/{version}\""));
        basicCheckAndReset();
    }

    @Test
    public void testDoFilterMetadata_POST() throws Exception {
        req.contextPath = "/metadata";
        req.principal = new FakePrincipal("UserName");

        req.method = "POST";

        req.servletPath = "/newEntity/15.9q:b/default"; //LightblueMetadataOperationChecker.createSchemaRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"POST /{entity}/{version}/default\""));
        basicCheckAndReset();
    }

    @Test
    public void testDoFilterMetadata_PUT() throws Exception {
        req.contextPath = "/metadata";
        req.principal = new FakePrincipal("UserName");

        req.method = "PUT";

        req.servletPath = "/newEntity/15.9q:b"; //LightblueMetadataOperationChecker.createMetadataRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"PUT /{entity}/{version}\""));
        basicCheckAndReset();

        req.servletPath = "/newEntity/schema=15.9q:b"; //LightblueMetadataOperationChecker.createSchemaRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"PUT /{entity}/schema={version}\""));
        basicCheckAndReset();

        req.servletPath = "/newEntity"; //LightblueMetadataOperationChecker.updateEntityInfoRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"PUT /{entity}\""));
        basicCheckAndReset();

        req.servletPath = "/newEntity/15.9q:b/test"; //LightblueMetadataOperationChecker.updateSchemaStatusRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"PUT /{entity}/{version}/{status}\""));
        basicCheckAndReset();
    }

    @Test
    public void testDoFilterMetadata_DELETE() throws Exception {
        req.contextPath = "/metadata";
        req.principal = new FakePrincipal("UserName");

        req.method = "DELETE";

        req.servletPath = "/newEntity"; //LightblueMetadataOperationChecker.updateEntityInfoRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"DELETE /{entity}\""));
        basicCheckAndReset();

        req.servletPath = "/newEntity/default"; //LightblueMetadataOperationChecker.updateEntityInfoRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/metadata\" , \"operation\":\"DELETE /{entity}/default\""));
        basicCheckAndReset();
    }

    // Auditable cases for emtadata
    @Test
    public void testDoFilterData_GET() throws Exception {
        req.contextPath = "/data";
        req.principal = new FakePrincipal("UserName");

        req.method = "GET";

        req.servletPath = "/find/nEntity"; //LightblueCrudOperationChecker.simpleFindVersionRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/data\" , \"operation\":\"GET /find/{entity}\""));
        basicCheckAndReset();

        req.servletPath = "/find/nEntity/15.9q:b"; //LightblueCrudOperationChecker.simpleFindRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/data\" , \"operation\":\"GET /find/{entity}/{version}\""));
        basicCheckAndReset();
    }

    @Test
    public void testDoFilterData_POST() throws Exception {
        req.contextPath = "/data";
        req.principal = new FakePrincipal("UserName");

        req.method = "POST";

        req.servletPath = "/save/newEntity"; //LightblueCrudOperationChecker.saveRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/data\" , \"operation\":\"POST /save/{entity}\""));
        basicCheckAndReset();

        req.servletPath = "/save/newEntity/15.9q:b"; //LightblueCrudOperationChecker.saveVersionRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/data\" , \"operation\":\"POST /save/{entity}/{version}\""));
        basicCheckAndReset();

        req.servletPath = "/save/newEntity"; //LightblueCrudOperationChecker.saveRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/data\" , \"operation\":\"POST /save/{entity}\""));
        basicCheckAndReset();

        req.servletPath = "/save/newEntity/15.9q:b"; //LightblueCrudOperationChecker.saveVersionRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/data\" , \"operation\":\"POST /save/{entity}/{version}\""));
        basicCheckAndReset();

        req.servletPath = "/update/newEntity"; //LightblueCrudOperationChecker.updateRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/data\" , \"operation\":\"POST /update/{entity}\""));
        basicCheckAndReset();

        req.servletPath = "/update/newEntity/15.9q:b"; //LightblueCrudOperationChecker.updateVersionRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/data\" , \"operation\":\"POST /update/{entity}/{version}\""));
        basicCheckAndReset();

        req.servletPath = "/delete/newEntity"; //LightblueCrudOperationChecker.deleteRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/data\" , \"operation\":\"POST /delete/{entity}\""));
        basicCheckAndReset();

        req.servletPath = "/delete/newEntity/15.9q:b"; //LightblueCrudOperationChecker.deleteVersionRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/data\" , \"operation\":\"POST /delete/{entity}/{version}\""));
        basicCheckAndReset();

        req.servletPath = "/find/newEntity"; //LightblueCrudOperationChecker.findRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/data\" , \"operation\":\"POST /find/{entity}\""));
        basicCheckAndReset();

        req.servletPath = "/find/newEntity/15.9q:b"; //LightblueCrudOperationChecker.findVersionRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/data\" , \"operation\":\"POST /find/{entity}/{version}\""));
        basicCheckAndReset();

    }

    @Test
    public void testDoFilterData_PUT() throws Exception {
        req.contextPath = "/data";
        req.principal = new FakePrincipal("UserName");

        req.method = "PUT";

        req.servletPath = "/insert/newEntity"; //LightblueCrudOperationChecker.insertRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/data\" , \"operation\":\"PUT /insert/{entity}\""));
        basicCheckAndReset();

        req.servletPath = "/insert/newEntity/15.9q:b"; //LightblueCrudOperationChecker.insertVersionRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/data\" , \"operation\":\"PUT /insert/{entity}/{version}\""));
        basicCheckAndReset();

        req.servletPath = "/newEntity"; //LightblueCrudOperationChecker.insertAltRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/data\" , \"operation\":\"PUT /{entity}\""));
        basicCheckAndReset();

        req.servletPath = "/newEntity/15.9q:b"; //LightblueCrudOperationChecker.insertAltVersionRegex
        cut.doFilter(req, res, fChain);
        Thread.sleep(250);
        assertTrue(err.inMemoryConsole.toString().contains("\"principal\":\"UserName\" , \"resource\":\"/data\" , \"operation\":\"PUT /{entity}/{version}\""));
        basicCheckAndReset();

    }

    private String generateStringFromPattern(String pattern) {
        Generex generex = new Generex(pattern);
        return generex.random().replace(" ", "");// it seems there is a bug on the dependency of the dependency with \\S as it generate whitespace some times
    }

    private void basicCheckAndReset() {
        assertTrue(err.inMemoryConsole.toString().contains("LightblueAuditServletFilter.doFilter invoked - begin"));
        assertTrue(err.inMemoryConsole.toString().contains("LightblueAuditServletFilter.doFilter invoked - end"));
        assertFalse(err.inMemoryConsole.toString().contains("The URL doesn't map to one of the rest services. Request URI"));
        assertFalse(err.inMemoryConsole.toString().contains("Invalid HTTP method:"));
        err.resetInMemoryConsole();
    }

    /*
        @Test
        public void testSetOperationEnittyVersionStatus() throws Exception {

        }
     */
    static class TeeInMemoryStringPrintStream extends PrintStream {
        public StringBuilder inMemoryConsole = new StringBuilder();
        public PrintStream originalPrintStream;

        TeeInMemoryStringPrintStream(OutputStream os, PrintStream originalPrintStream) {
            super(os);
            this.originalPrintStream = originalPrintStream;
        }

        public void resetInMemoryConsole() {
            inMemoryConsole = new StringBuilder();
        }

        static class TeeFilterOutputStream extends FilterOutputStream {
            TeeInMemoryStringPrintStream teeInMemoryStringPrintStream;

            public TeeFilterOutputStream(OutputStream out) {
                super(out);
            }

            public void setTeeInMemoryStringPrintStream(TeeInMemoryStringPrintStream teeInMemoryStringPrintStream) {
                this.teeInMemoryStringPrintStream = teeInMemoryStringPrintStream;
            }

            @Override
            public void write(int b) throws IOException {
                super.write(b);
                teeInMemoryStringPrintStream.inMemoryConsole.append((char) b);
            }

        }

        public static TeeInMemoryStringPrintStream teeTo(PrintStream originalPrintStream) {
            try {
                Field outField = FilterOutputStream.class.getDeclaredField("out");
                outField.setAccessible(true);
                OutputStream outputStream = (OutputStream) outField.get(originalPrintStream);

                TeeFilterOutputStream teeFilterOutputStream = new TeeFilterOutputStream(outputStream);
                TeeInMemoryStringPrintStream teeInMemoryStringPrintStream = new TeeInMemoryStringPrintStream(teeFilterOutputStream, originalPrintStream);
                teeFilterOutputStream.setTeeInMemoryStringPrintStream(teeInMemoryStringPrintStream);

                return teeInMemoryStringPrintStream;
            } catch (NoSuchFieldException nsfe) {
                throw new IllegalStateException(nsfe);
            } catch (IllegalArgumentException iae) {
                throw new IllegalStateException(iae);
            } catch (IllegalAccessException ie) {
                throw new IllegalStateException(ie);
            }
        }
    }
}
