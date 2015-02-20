/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * This file is part of lightblue.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.redhat.lightblue.rest.crud;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.redhat.lightblue.config.CrudConfiguration;
import com.redhat.lightblue.config.DataSourcesConfiguration;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.config.MetadataConfiguration;
import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.mongo.MongoMetadata;
import com.redhat.lightblue.mongo.config.MongoConfiguration;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.util.JsonUtils;
import com.redhat.lightblue.util.test.FileUtil;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.io.IStreamProcessor;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.runtime.Network;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.json.JSONException;
import org.junit.*;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author lcestari
 */
@Ignore
@RunWith(Arquillian.class)
public class ITCaseCrudResourceRDBMSTest {

    private static boolean notRegistered = true;

    public static class FileStreamProcessor implements IStreamProcessor {
        private FileOutputStream outputStream;

        public FileStreamProcessor(File file) throws FileNotFoundException {
            outputStream = new FileOutputStream(file);
        }

        @Override
        public void process(String block) {
            try {
                outputStream.write(block.getBytes());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void onProcessed() {
            try {
                outputStream.close();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

    }

    private static final String MONGO_HOST = "localhost";
    private static final int MONGO_PORT = 27777;
    private static final String IN_MEM_CONNECTION_URL = MONGO_HOST + ":" + MONGO_PORT;

    private static final String DB_NAME = "testmetadata";

    private static MongodExecutable mongodExe;
    private static MongodProcess mongod;
    private static Mongo mongo;
    private static DB db;

    static {
        try {
            IStreamProcessor mongodOutput = Processors.named("[mongod>]",
                    new FileStreamProcessor(File.createTempFile("mongod", "log")));
            IStreamProcessor mongodError = new FileStreamProcessor(File.createTempFile("mongod-error", "log"));
            IStreamProcessor commandsOutput = Processors.namedConsole("[console>]");

            IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                    .defaults(Command.MongoD)
                    .processOutput(new ProcessOutput(mongodOutput, mongodError, commandsOutput))
                    .build();

            MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);
            mongodExe = runtime.prepare(
                    new MongodConfigBuilder()
                            .version(de.flapdoodle.embed.mongo.distribution.Version.V2_6_0)
                            .net(new Net(MONGO_PORT, Network.localhostIsIPv6()))
                            .build()
            );
            try {
                mongod = mongodExe.start();
            } catch (Throwable t) {
                // try again, could be killed breakpoint in IDE
                mongod = mongodExe.start();
            }
            mongo = new Mongo(IN_MEM_CONNECTION_URL);

            MongoConfiguration config = new MongoConfiguration();
            // disable ssl for test (enabled by default)
            config.setDatabase(DB_NAME);
            config.setSsl(Boolean.FALSE);
            config.addServerAddress(MONGO_HOST, MONGO_PORT);

            db = config.getDB();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    super.run();
                    clearDatabase();
                }

            });
        } catch (IOException e) {
            throw new java.lang.Error(e);
        }
    }

    @Before
    public void setup() throws Exception {
        File folder = new File("/tmp");
        File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.startsWith("test.db");
            }
        });
        for (final File file : files) {
            if (!file.delete()) {
                System.out.println("Failed to remove " + file.getAbsolutePath());
            }
        }

        mongo.dropDatabase(DB_NAME);
        mongo.dropDatabase("local");
        mongo.dropDatabase("admin");
        mongo.dropDatabase("mongo");
        mongo.getDB(DB_NAME).dropDatabase();
        mongo.getDB("local").dropDatabase();
        mongo.getDB("admin").dropDatabase();
        mongo.getDB("mongo").dropDatabase();

        db.getCollection(MongoMetadata.DEFAULT_METADATA_COLLECTION).remove(new BasicDBObject());
        mongo.getDB("mongo").getCollection("metadata").remove(new BasicDBObject());

        db.createCollection(MongoMetadata.DEFAULT_METADATA_COLLECTION, null);
        BasicDBObject index = new BasicDBObject("name", 1);
        index.put("version.value", 1);
        db.getCollection(MongoMetadata.DEFAULT_METADATA_COLLECTION).ensureIndex(index, "name", true);

        if (notRegistered) {
            notRegistered = false;
            try {
                // Create initial context
                System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.naming.java.javaURLContextFactory");
                System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");
                // already tried System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.as.naming.InitialContextFactory");
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
        } else {
            Context initCtx = new InitialContext();
            DataSource ds = (DataSource) initCtx.lookup("java:/mydatasource");
            Connection conn = ds.getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute("DROP ALL OBJECTS ");
            stmt.close();
        }
    }

    @After
    public void teardown() {
        if (mongo != null) {
            mongo.dropDatabase(DB_NAME);
        }
    }

    public static void clearDatabase() {
        if (mongod != null) {
            mongod.stop();
            mongodExe.stop();
        }
        db = null;
        mongo = null;
        mongod = null;
        mongodExe = null;
    }

    @Deployment
    public static WebArchive createDeployment() {
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve().withTransitivity().asFile();
        final String PATH_BASE = "src/test/resources/" + ITCaseCrudResourceRDBMSTest.class.getSimpleName() + "/config/";

        WebArchive archive = ShrinkWrap.create(WebArchive.class, "test.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(new File(PATH_BASE + MetadataConfiguration.FILENAME), MetadataConfiguration.FILENAME)
                .addAsResource(new File(PATH_BASE + CrudConfiguration.FILENAME), CrudConfiguration.FILENAME)
                .addAsResource(new File(PATH_BASE + RestConfiguration.DATASOURCE_FILENAME), RestConfiguration.DATASOURCE_FILENAME)
                .addAsResource(new File(PATH_BASE + "config.properties"), "config.properties");
        for (File file : libs) {
            archive.addAsLibrary(file);
        }
        archive.addPackages(true, "com.redhat.lightblue");
        return archive;
    }

    private String readFile(String filename) throws IOException, URISyntaxException {
        return FileUtil.readFileAndTrim(this.getClass().getSimpleName() + "/" + filename);
    }

    private String readConfigFile(String filename) throws IOException, URISyntaxException {
        return readFile("config/" + filename);
    }

    @Inject
    private CrudResource cutCrudResource; //class under test

    @Test
    public void testInsert() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, URISyntaxException, JSONException {
        try {
            Context initCtx = new InitialContext();
            DataSource ds = (DataSource) initCtx.lookup("java:/mydatasource");
            Connection conn = ds.getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE Country ( name varchar(255), iso2code varchar(255), iso3code varchar(255) );");
            stmt.close();
            conn.close();

            Assert.assertNotNull("CrudResource was not injected by the container", cutCrudResource);
            RestConfiguration.setDatasources(new DataSourcesConfiguration(JsonUtils.json(readConfigFile(RestConfiguration.DATASOURCE_FILENAME))));
            RestConfiguration.setFactory(new LightblueFactory(RestConfiguration.getDatasources()));

            String expectedCreated = readFile("expectedCreated.json");
            String metadata = readFile("metadata.json").replaceAll("XXY", "INSERT INTO Country (NAME,ISO2CODE,ISO3CODE) VALUES (:name,:iso2code,:iso3code);");
            EntityMetadata em = RestConfiguration.getFactory().getJSONParser().parseEntityMetadata(JsonUtils.json(metadata));
            RestConfiguration.getFactory().getMetadata().createNewMetadata(em);
            EntityMetadata em2 = RestConfiguration.getFactory().getMetadata().getEntityMetadata("country", "1.0.0");
            String resultCreated = RestConfiguration.getFactory().getJSONParser().convert(em2).toString();
            JSONAssert.assertEquals(expectedCreated, resultCreated, false);

            String expectedInserted = readFile("expectedInserted.json");
            String resultInserted = cutCrudResource.insert("country", "1.0.0", readFile("resultInserted.json")).getEntity().toString();
            System.err.println("!!!!!!!!!!!!!!!!!" + resultInserted);

            ds = (DataSource) initCtx.lookup("java:/mydatasource");
            conn = ds.getConnection();
            stmt = conn.createStatement();
            stmt.execute("SELECT * FROM Country;");
            ResultSet resultSet = stmt.getResultSet();
            resultSet.next();
            Assert.assertEquals("Canad", resultSet.getString("name"));
            Assert.assertEquals("CA", resultSet.getString("iso2code"));
            Assert.assertEquals("CAN", resultSet.getString("iso3code"));

            JSONAssert.assertEquals(expectedInserted, resultInserted, false);
        } catch (NamingException ex) {
            throw new IllegalStateException(ex);
        } catch (SQLException ex) {
            throw new IllegalStateException(ex);
        }
    }


    @Test
    public void testSelect() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, URISyntaxException, JSONException {
        try {
            Context initCtx = new InitialContext();
            DataSource ds = (DataSource) initCtx.lookup("java:/mydatasource");
            Connection conn = ds.getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE Country ( name varchar(255), iso2code varchar(255), iso3code varchar(255) );");
            stmt.execute("INSERT INTO Country (name,iso2code,iso3code) VALUES ('a','CA','c');");
            stmt.close();
            conn.close();

            Assert.assertNotNull("CrudResource was not injected by the container", cutCrudResource);
            RestConfiguration.setDatasources(new DataSourcesConfiguration(JsonUtils.json(readConfigFile(RestConfiguration.DATASOURCE_FILENAME))));
            RestConfiguration.setFactory(new LightblueFactory(RestConfiguration.getDatasources()));

            String expectedCreated = readFile("expectedCreated.json");
            String metadata = readFile("metadata.json");
            EntityMetadata em = RestConfiguration.getFactory().getJSONParser().parseEntityMetadata(JsonUtils.json(metadata));
            RestConfiguration.getFactory().getMetadata().createNewMetadata(em);
            EntityMetadata em2 = RestConfiguration.getFactory().getMetadata().getEntityMetadata("country", "1.0.0");
            String resultCreated = RestConfiguration.getFactory().getJSONParser().convert(em2).toString();
            JSONAssert.assertEquals(expectedCreated, resultCreated, false);

            String expectedFound = readFile("expectedFound.json");
            String resultFound = cutCrudResource.find("country", "1.0.0", readFile("resultFound.json")).getEntity().toString();
            // TODO / NOTE we can change the result format if needed, now it return an array of arrays
            //System.err.println("!!!!!!!!!!!!!!!!!" + resultFound);
            JSONAssert.assertEquals(expectedFound, resultFound, false);
        } catch (NamingException | SQLException ex) {
            throw new IllegalStateException(ex);
        }
        mongo.dropDatabase(DB_NAME);
    }


    @Test
    public void testSelectAll() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, URISyntaxException, JSONException {
        try {
            Context initCtx = new InitialContext();
            DataSource ds = (DataSource) initCtx.lookup("java:/mydatasource");
            Connection conn = ds.getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE Country ( name varchar(255), iso2code varchar(255), iso3code varchar(255) );");
            stmt.execute("INSERT INTO Country (name,iso2code,iso3code) VALUES ('a','CA','c');");
            stmt.close();
            conn.close();

            Assert.assertNotNull("CrudResource was not injected by the container", cutCrudResource);
            RestConfiguration.setDatasources(new DataSourcesConfiguration(JsonUtils.json(readConfigFile(RestConfiguration.DATASOURCE_FILENAME))));
            RestConfiguration.setFactory(new LightblueFactory(RestConfiguration.getDatasources()));

            String expectedCreated = readFile("expectedCreated.json");
            String metadata = readFile("metadata.json");
            EntityMetadata em = RestConfiguration.getFactory().getJSONParser().parseEntityMetadata(JsonUtils.json(metadata));
            RestConfiguration.getFactory().getMetadata().createNewMetadata(em);
            EntityMetadata em2 = RestConfiguration.getFactory().getMetadata().getEntityMetadata("country", "1.0.0");
            String resultCreated = RestConfiguration.getFactory().getJSONParser().convert(em2).toString();
            JSONAssert.assertEquals(expectedCreated, resultCreated, false);

            String expectedFound = readFile("expectedFoundAll.json");
            String resultFound = cutCrudResource.find("country", "1.0.0", readFile("resultFoundAll.json")).getEntity().toString();
            // TODO / NOTE we can change the result format if needed, now it return an array of arrays
            //System.err.println("!!!!!!!!!!!!!!!!!" + resultFound);
            JSONAssert.assertEquals(expectedFound, resultFound, false);
        } catch (NamingException | SQLException ex) {
            throw new IllegalStateException(ex);
        }
        mongo.dropDatabase(DB_NAME);
    }

    @Test
    public void testUpdate() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, URISyntaxException, JSONException {
        try {
            Context initCtx = new InitialContext();
            DataSource ds = (DataSource) initCtx.lookup("java:/mydatasource");
            Connection conn = ds.getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE Country ( name varchar(255), iso2code varchar(255), iso3code varchar(255) );");
            stmt.execute("INSERT INTO Country (name,iso2code,iso3code) VALUES ('a','CA','c');");
            stmt.close();
            conn.close();

            Assert.assertNotNull("CrudResource was not injected by the container", cutCrudResource);
            RestConfiguration.setDatasources(new DataSourcesConfiguration(JsonUtils.json(readConfigFile(RestConfiguration.DATASOURCE_FILENAME))));
            RestConfiguration.setFactory(new LightblueFactory(RestConfiguration.getDatasources()));

            String expectedCreated = readFile("expectedCreated.json");
            String metadata = readFile("metadata.json").replaceAll("ZZY", " UPDATE Country SET NAME=:name  WHERE ISO2CODE=:ISO2CODE;");
            EntityMetadata em = RestConfiguration.getFactory().getJSONParser().parseEntityMetadata(JsonUtils.json(metadata));
            RestConfiguration.getFactory().getMetadata().createNewMetadata(em);
            EntityMetadata em2 = RestConfiguration.getFactory().getMetadata().getEntityMetadata("country", "1.0.0");
            String resultCreated = RestConfiguration.getFactory().getJSONParser().convert(em2).toString();
            JSONAssert.assertEquals(expectedCreated, resultCreated, false);

            String expectedUpdated = readFile("expectedUpdated.json");
            String resultUpdated = cutCrudResource.update("country", "1.0.0", readFile("resultUpdated.json")).getEntity().toString();
            System.err.println("!!!!!!!!!!!!!!!!!" + resultUpdated);

            ds = (DataSource) initCtx.lookup("java:/mydatasource");
            conn = ds.getConnection();
            stmt = conn.createStatement();
            stmt.execute("SELECT * FROM Country;");
            ResultSet resultSet = stmt.getResultSet();
            resultSet.next();
            Assert.assertEquals("Canada", resultSet.getString("name"));
            Assert.assertEquals("CA", resultSet.getString("iso2code"));
            Assert.assertEquals("c", resultSet.getString("iso3code"));

            JSONAssert.assertEquals(expectedUpdated, resultUpdated, false);
        } catch (NamingException | SQLException ex) {
            throw new IllegalStateException(ex);
        }
        mongo.dropDatabase(DB_NAME);
    }


    @Test
    public void testDelete() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, URISyntaxException, JSONException {
        try {
            Context initCtx = new InitialContext();
            DataSource ds = (DataSource) initCtx.lookup("java:/mydatasource");
            Connection conn = ds.getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE Country ( name varchar(255), iso2code varchar(255), iso3code varchar(255) );");
            stmt.execute("INSERT INTO Country (name,iso2code,iso3code) VALUES ('a','CA','c');");
            stmt.close();
            conn.close();

            Assert.assertNotNull("CrudResource was not injected by the container", cutCrudResource);
            RestConfiguration.setDatasources(new DataSourcesConfiguration(JsonUtils.json(readConfigFile(RestConfiguration.DATASOURCE_FILENAME))));
            RestConfiguration.setFactory(new LightblueFactory(RestConfiguration.getDatasources()));

            String expectedCreated = readFile("expectedCreated.json");
            System.err.println("!!!!!!expectedCreated.json!!!!!!!!!!!" + expectedCreated);
            String metadata = readFile("metadata.json").replaceAll("YYZ", " DELETE FROM Country WHERE ISO2CODE=:ISO2CODE;");
            System.err.println("!!!!!!metadata.json!!!!!!!!!!!" + metadata);
            EntityMetadata em = RestConfiguration.getFactory().getJSONParser().parseEntityMetadata(JsonUtils.json(metadata));
            RestConfiguration.getFactory().getMetadata().createNewMetadata(em);
            EntityMetadata em2 = RestConfiguration.getFactory().getMetadata().getEntityMetadata("country", "1.0.0");
            String resultCreated = RestConfiguration.getFactory().getJSONParser().convert(em2).toString();
            JSONAssert.assertEquals(expectedCreated, resultCreated, false);
            System.err.println("!!!!!!resultCreated!!!!!!!!!!!" + resultCreated);


            String expectedDeleted = readFile("expectedDeleted.json");
            System.err.println("!!!!!!expectedDeleted.json!!!!!!!!!!!" + expectedDeleted);
            System.err.println("!!!!!!resultDeleted.json!!!!!!!!!!!" + readFile("resultDeleted.json"));
            String resultDeleted = cutCrudResource.delete("country", "1.0.0", readFile("resultDeleted.json")).getEntity().toString();
            System.err.println("!!!!!!!!resultDeleted!!!!!!!!!" + resultDeleted);

            ds = (DataSource) initCtx.lookup("java:/mydatasource");
            conn = ds.getConnection();
            stmt = conn.createStatement();
            stmt.execute("SELECT * FROM Country;");
            ResultSet resultSet = stmt.getResultSet();

            Assert.assertEquals(false, resultSet.next());

            JSONAssert.assertEquals(expectedDeleted, resultDeleted, false);
        } catch (NamingException | SQLException ex) {
            throw new IllegalStateException(ex);
        }
        mongo.dropDatabase(DB_NAME);
    }
}
