package com.redhat.lightblue.rest.crud.tck.simpleflat;

import com.redhat.lightblue.rest.crud.tck.BaseCompatibilityTest;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class RDBMSCompatibilityTest extends BaseCompatibilityTest {

    /*
        Each BaseCompatibilityTest would build its own LightblueInit to setup it enviroment, the rest of the
        execution must be the same across the controllers
     */

    @ClassRule
    public static ExternalResource resource= new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            try {
                System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.naming.java.javaURLContextFactory");
                System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");
                InitialContext ic = new InitialContext();

                ic.createSubcontext("java:");
                ic.createSubcontext("java:/comp");
                ic.createSubcontext("java:/comp/env");
                ic.createSubcontext("java:/comp/env/jdbc");

                JdbcConnectionPool ds = JdbcConnectionPool.create("jdbc:h2:file:/tmp/test.db;FILE_LOCK=NO;MVCC=TRUE;DB_CLOSE_ON_EXIT=TRUE", "sa", "sasasa");

                ic.bind("java:/mydatasource", ds);
            } catch (NamingException ex) {
                throw new IllegalStateException(ex);
            }
        };

        @Override
        protected void after() {
            try {
                Context initCtx = new InitialContext();
                DataSource ds = (DataSource) initCtx.lookup("java:/mydatasource");
                Connection conn = ds.getConnection();
                Statement stmt = conn.createStatement();
                stmt.execute("DROP ALL OBJECTS ");
                stmt.close();
            } catch (SQLException | NamingException ex) {
                throw new IllegalStateException(ex);
            }
        };
    };

}

