package com.redhat.lightblue.rest.audit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.impl.SimpleLogger;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * Created by lcestari on 4/2/15.
 */
public class LightblueAuditServletFilterTest {
    static TeeInMemoryStringPrintStream out;
    static TeeInMemoryStringPrintStream err;
    static Field simpleLoggerOutField;
    static PrintStream simpleLoggerPrintStream;

    LightblueAuditServletFilter cut;

    MyHttpServletRequest req;
    MyHttpServletResponse res;
    FilterChain fChain = new FilterChain() {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            return;
        }
    };

    @BeforeClass
    public static void beforeAllTest() throws Exception {
        out = TeeInMemoryStringPrintStream.teeTo(System.out);
        System.setOut(out);

        err = TeeInMemoryStringPrintStream.teeTo(System.err);
        System.setErr(err);

        simpleLoggerOutField = SimpleLogger.class.getDeclaredField("TARGET_STREAM");
        simpleLoggerOutField.setAccessible(true);
        simpleLoggerPrintStream = (PrintStream) simpleLoggerOutField.get(null);
        simpleLoggerOutField.set(null, err); // Using the default configuration, SimpleLogger will use the err. Either way, we are going to reset to the original value after this set of tests
    }

    @AfterClass
    static public void afterAllTest() throws Exception {
        System.setOut(out.originalPrintStream);
        System.setErr(err.originalPrintStream);
        simpleLoggerOutField.set(null, simpleLoggerPrintStream);
    }

    @Before
    public void setUp() throws Exception {
        cut = new LightblueAuditServletFilter();
        req = new MyHttpServletRequest();
        res = new MyHttpServletResponse();
        out.resetInMemoryConsole();
        err.resetInMemoryConsole();
    }

    @Test
    public void testTeeToInMemoryStringBuilderIsWorking() throws Exception {
        cut.init(null);
        assertTrue(err.inMemoryConsole.toString().contains("Initializing LightblueAuditServletFilter"));
        assertFalse(err.inMemoryConsole.toString().contains("Destroying LightblueAuditServletFilter"));
        cut.destroy();
        assertTrue(err.inMemoryConsole.toString().contains("Destroying LightblueAuditServletFilter"));
    }

    // Context path is not data or metadata, so it should not audit
    @Test
    public void testDoFilterNoAudit() throws Exception {
        final String wrong = "X";
        req.contextPath = "/data" + wrong;
        req.principal = new MyPrincipal("UserName");
        cut.doFilter(req, res, fChain);
        basicCheckAndReset();

        req.contextPath = "/data";
        req.principal = null;
        cut.doFilter(req, res, fChain);
        basicCheckAndReset();


        req.contextPath = "/metadata"+ wrong;
        req.principal = new MyPrincipal("UserName");
        cut.doFilter(req, res, fChain);
        basicCheckAndReset();

        req.contextPath = "/metadata";
        req.principal = null;
        cut.doFilter(req, res, fChain);
        basicCheckAndReset();
    }

    // Auditable cases for emtadata
    @Test
    public void testDoFilterMetadata() throws Exception {
        req.contextPath = "/metadata";
        req.principal = new MyPrincipal("UserName");
        req.method = "GET";
        req.servletPath="/entity/version/dependencies";
        cut.doFilter(req, res, fChain);
        basicCheckAndReset();
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

        public void resetInMemoryConsole(){
            inMemoryConsole = new StringBuilder();
        }

        static class TeeFilterOutputStream extends FilterOutputStream{
            TeeInMemoryStringPrintStream teeInMemoryStringPrintStream;

            public TeeFilterOutputStream(OutputStream out) {
                super(out);
            }

            public void setTeeInMemoryStringPrintStream(TeeInMemoryStringPrintStream teeInMemoryStringPrintStream) {
                this.teeInMemoryStringPrintStream = teeInMemoryStringPrintStream;
            }

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
                TeeInMemoryStringPrintStream teeInMemoryStringPrintStream = new TeeInMemoryStringPrintStream( teeFilterOutputStream, originalPrintStream);
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