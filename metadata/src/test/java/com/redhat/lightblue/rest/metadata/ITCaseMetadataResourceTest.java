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
package com.redhat.lightblue.rest.metadata;

import static com.redhat.lightblue.util.test.FileUtil.readFile;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.redhat.lightblue.mongo.config.MongoConfiguration;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.Defaults;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.process.config.RuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.config.store.TimeoutConfig;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.StreamProcessor;
import de.flapdoodle.embed.process.runtime.Network;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import java.time.Duration;
import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.json.JSONException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.mongodb.BasicDBObject;
import com.redhat.lightblue.config.MetadataConfiguration;
import com.redhat.lightblue.mongo.metadata.MongoMetadata;
import com.redhat.lightblue.mongo.test.EmbeddedMongo;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.rest.test.RestConfigurationRule;
import com.redhat.lightblue.rest.test.support.Assets;
import com.redhat.lightblue.rest.test.support.CrudWebXmls;

/**
 * @author lcestari
 */
@RunWith(Arquillian.class)
public class ITCaseMetadataResourceTest {

    @Rule
    public final RestConfigurationRule resetRuleConfiguration = new RestConfigurationRule();

    public static class FileStreamProcessor implements StreamProcessor {
        private final FileOutputStream outputStream;

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
    private static final int MONGO_PORT = 27757;
    private static final String IN_MEM_CONNECTION_URL = MONGO_HOST + ":" + MONGO_PORT;

    private static final String DB_NAME = "mongo";

    private static MongodExecutable mongodExe;
    private static MongodProcess mongod;
    private static MongoClient mongo;
    private static DB db;

    static {
        try {
            StreamProcessor mongodOutput = Processors.named("[mongod>]",
                new FileStreamProcessor(File.createTempFile("mongod", "log")));
            StreamProcessor mongodError = new FileStreamProcessor(File.createTempFile("mongod-error", "log"));
            StreamProcessor commandsOutput = Processors.namedConsole("[console>]");

            Command mongoD = Command.MongoD;
            RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(mongoD)
                .processOutput(new ProcessOutput(mongodOutput, mongodError, commandsOutput))
                .artifactStore(Defaults.extractedArtifactStoreFor(mongoD)
                    .withDownloadConfig(Defaults.downloadConfigFor(mongoD)
                        .timeoutConfig(TimeoutConfig.builder()
                            .connectionTimeout((int) Duration.ofMinutes(1).toMillis())
                            .readTimeout((int) Duration.ofMinutes(5).toMillis())
                            .build()).build()))
                .build();

            MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);
            mongodExe = runtime.prepare(
                MongodConfig.builder()
                    .version(de.flapdoodle.embed.mongo.distribution.Version.V3_1_6)
                    .net(new Net(MONGO_PORT, Network.localhostIsIPv6()))
                    .build()
            );
            try {
                mongod = mongodExe.start();
            } catch (Throwable t) {
                // try again, could be killed breakpoint in IDE
                mongod = mongodExe.start();
            }
            MongoClient mongoClient = new MongoClient(IN_MEM_CONNECTION_URL);
            mongo = mongoClient;

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
        db.createCollection(MongoMetadata.DEFAULT_METADATA_COLLECTION, null);
        BasicDBObject index = new BasicDBObject("name", 1);
        index.put("version.value", 1);
        db.getCollection(MongoMetadata.DEFAULT_METADATA_COLLECTION).createIndex(index, "name", true);
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
    public static WebArchive createDeployment() throws Exception {
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve().withTransitivity().asFile();

        WebArchive archive = ShrinkWrap.create(WebArchive.class, "lightblue.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(Assets.forDocument(CrudWebXmls.forNonEE6Container(RestApplication.class)), "web.xml")
                .addAsLibraries(Maven.configureResolver()
                        .workOffline()
                        .loadPomFromFile("pom.xml")
                        .resolve("org.jboss.weld.servlet:weld-servlet")
                        .withTransitivity()
                        .asFile())
                .addPackages(true, "com.redhat.lightblue")
                .addAsResource(new File("src/test/resources/lightblue-metadata.json"), MetadataConfiguration.FILENAME)
                .addAsResource(new File("src/test/resources/datasources.json"), "datasources.json")
                .addAsResource(EmptyAsset.INSTANCE, "resources/test.properties");

        for (File file : libs) {
            archive.addAsLibrary(file);
        }
        return archive;

    }

    private String esc(String s) {
        return s.replace("'", "\"");
    }

    @Inject
    private AbstractMetadataResource cutMetadataResource;

    @Test
    public void testFirstIntegrationTest() throws IOException, URISyntaxException, JSONException {
        Assert.assertNotNull("MetadataResource was not injected by the container", cutMetadataResource);

        SecurityContext sc = new TestSecurityContext();

        System.out.println("factory:" + RestConfiguration.getFactory());
        String expectedCreated = readFile("expectedCreated.json");
        String resultCreated = cutMetadataResource.createMetadata(sc, "country", "1.0.0", readFile("resultCreated.json"));
        JSONAssert.assertEquals(expectedCreated, resultCreated, false);

        String expectedDepGraph = readFile("expectedDepGraph.json").replace("Notsupportedyet", " Not supported yet");
        String resultDepGraph = cutMetadataResource.getDepGraph(sc);
        JSONAssert.assertEquals(expectedDepGraph, resultDepGraph, false);

        String expectedDepGraph1 = readFile("expectedDepGraph1.json").replace("Notsupportedyet", " Not supported yet");
        String resultDepGraph1 = cutMetadataResource.getDepGraph(sc, "country");
        JSONAssert.assertEquals(expectedDepGraph1, resultDepGraph1, false);

        String expectedDepGraph2 = readFile("expectedDepGraph2.json").replace("Notsupportedyet", " Not supported yet");
        String resultDepGraph2 = cutMetadataResource.getDepGraph(sc, "country", "1.0.0");
        JSONAssert.assertEquals(expectedDepGraph2, resultDepGraph2, false);

        String expectedEntityNames = "{\"entities\":[\"country\"]}";
        String resultEntityNames = cutMetadataResource.getEntityNames(sc);
        JSONAssert.assertEquals(expectedEntityNames, resultEntityNames, false);

        // no default version
        String expectedEntityRoles = esc("{'status':'ERROR','modifiedCount':0,'matchCount':0,'dataErrors':[{'data':{'name':'country'},'errors':[{'objectType':'error','context':'GetEntityRolesCommand','errorCode':'ERR_NO_METADATA','msg':'Could not get metadata for given input. Error message: version'}]}]}");
        String expectedEntityRoles1 = esc("{'status':'ERROR','modifiedCount':0,'matchCount':0,'dataErrors':[{'data':{'name':'country'},'errors':[{'objectType':'error','context':'GetEntityRolesCommand/country','errorCode':'ERR_NO_METADATA','msg':'Could not get metadata for given input. Error message: version'}]}]}");
        String resultEntityRoles = cutMetadataResource.getEntityRoles(sc);
        String resultEntityRoles1 = cutMetadataResource.getEntityRoles(sc, "country");
        JSONAssert.assertEquals(expectedEntityRoles, resultEntityRoles, false);
        JSONAssert.assertEquals(expectedEntityRoles1, resultEntityRoles1, false);

        String expectedEntityRoles2 = readFile("expectedEntityRoles2.json");
        String resultEntityRoles2 = cutMetadataResource.getEntityRoles(sc, "country", "1.0.0");
        JSONAssert.assertEquals(expectedEntityRoles2, resultEntityRoles2, false);

        String expectedEntityVersions = esc("[{'version':'1.0.0','changelog':'blahblah'}]");
        String resultEntityVersions = cutMetadataResource.getEntityVersions(sc, "country");
        JSONAssert.assertEquals(expectedEntityVersions, resultEntityVersions, false);

        String expectedGetMetadata = esc("{'entityInfo':{'name':'country','indexes':[{'name':null,'unique':true,'fields':[{'field':'name','dir':'$asc'}]},{'name': null,'unique': true,'fields': [{'field': '_id','dir': '$asc'}]}],'datastore':{'backend':'mongo','datasource':'mongo','collection':'country'}},'schema':{'name':'country','version':{'value':'1.0.0','changelog':'blahblah'},'status':{'value':'active'},'access':{'insert':['anyone'],'update':['anyone'],'find':['anyone'],'delete':['anyone']},'fields':{'iso3code':{'type':'string'},'name':{'type':'string'},'iso2code':{'type':'string'},'objectType':{'type':'string','access':{'find':['anyone'],'update':['noone']},'constraints':{'required':true,'minLength':1}}}}}");
        String resultGetMetadata = cutMetadataResource.getMetadata(sc, "country", "1.0.0");
        JSONAssert.assertEquals(expectedGetMetadata, resultGetMetadata, false);

        String expectedCreateSchema = readFile("expectedCreateSchema.json");
        String resultCreateSchema = cutMetadataResource.createSchema(sc, "country", "1.1.0", readFile("expectedCreateSchemaInput.json"));
        JSONAssert.assertEquals(expectedCreateSchema, resultCreateSchema, false);

        String expectedUpdateEntityInfo = readFile("expectedUpdateEntityInfo.json");
        String resultUpdateEntityInfo = cutMetadataResource.updateEntityInfo(sc, "country", readFile("expectedUpdateEntityInfoInput.json"));
        JSONAssert.assertEquals(expectedUpdateEntityInfo, resultUpdateEntityInfo, false);

        String x = cutMetadataResource.setDefaultVersion(sc, "country", "1.0.0");
        String expected = esc("{'entityInfo':{'name':'country','defaultVersion':'1.0.0','indexes':[{'name':null,'unique':true,'fields':[{'field':'name','dir':'$asc'}]},{'name': null,'unique': true,'fields': [{'field': '_id','dir': '$asc'}]}],'datastore':{'backend':'mongo','datasource':'mongo','collection':'country'}},'schema':{'name':'country','version':{'value':'1.0.0','changelog':'blahblah'},'status':{'value':'active'},'access':{'insert':['anyone'],'update':['anyone'],'find':['anyone'],'delete':['anyone']},'fields':{'iso3code':{'type':'string'},'name':{'type':'string'},'iso2code':{'type':'string'},'objectType':{'type':'string','access':{'find':['anyone'],'update':['noone']},'constraints':{'required':true,'minLength':1}}}}}");
        JSONAssert.assertEquals(expected, x, false);

        x = cutMetadataResource.clearDefaultVersion(sc, "country");
        //System.out.println(x);
        expected = esc("{'name':'country','indexes':[{'name':null,'unique':true,'fields':[{'field':'name','dir':'$asc'}]},{'name': null,'unique': true,'fields': [{'field': '_id','dir': '$asc'}]}],'datastore':{'backend':'mongo','datasource':'mongo','collection':'country'}}");
        JSONAssert.assertEquals(expected, x, false);

        String expectedUpdateSchemaStatus = esc("{'entityInfo':{'name':'country','indexes':[{'name':null,'unique':true,'fields':[{'field':'name','dir':'$asc'}]},{'name': null,'unique': true,'fields': [{'field': '_id','dir': '$asc'}]}],'datastore':{'backend':'mongo','datasource':'mongo','collection':'country'}},'schema':{'name':'country','version':{'value':'1.0.0','changelog':'blahblah'},'status':{'value':'deprecated'},'access':{'insert':['anyone'],'update':['anyone'],'find':['anyone'],'delete':['anyone']},'fields':{'iso3code':{'type':'string'},'name':{'type':'string'},'iso2code':{'type':'string'},'objectType':{'type':'string','access':{'find':['anyone'],'update':['noone']},'constraints':{'required':true,'minLength':1}}}}}");
        String resultUpdateSchemaStatus = cutMetadataResource.updateSchemaStatus(sc, "country", "1.0.0", "deprecated", "No comment");
        JSONAssert.assertEquals(expectedUpdateSchemaStatus, resultUpdateSchemaStatus, false);

    }

    @Test
    public void createMetadataAlsoUpdates() throws Exception {
        Assert.assertNotNull("MetadataResource was not injected by the container", cutMetadataResource);

        SecurityContext sc = new TestSecurityContext();

        System.out.println("factory:" + RestConfiguration.getFactory());
        String expectedCreated = readFile("expectedCreated.json");
        String resultCreated = cutMetadataResource.createMetadata(sc, "country", "1.0.0", readFile("resultCreated.json"));
        JSONAssert.assertEquals(expectedCreated, resultCreated, false);

        String expectedUpdateEntityInfo = readFile("expectedCreateSchema.json");
        String resultUpdateEntityInfo = cutMetadataResource.createMetadata(sc, "country", "1.1.0", expectedUpdateEntityInfo);
        JSONAssert.assertEquals(expectedUpdateEntityInfo, resultUpdateEntityInfo, false);
    }
    
    @Test
    public void diff() throws Exception {
        Assert.assertNotNull("MetadataResource was not injected by the container", cutMetadataResource);

        SecurityContext sc = new TestSecurityContext();

        System.out.println("factory:" + RestConfiguration.getFactory());
        String expectedCreated = readFile("expectedCreated.json");
        cutMetadataResource.createMetadata(sc, "country", "1.0.0", readFile("resultCreated.json"));

        String expectedUpdateEntityInfo = readFile("expectedCreateSchema.json");
        cutMetadataResource.createMetadata(sc, "country", "1.1.0", expectedUpdateEntityInfo);

        String diff=cutMetadataResource.getDiff(sc,"country","1.0.0","1.1.0");
        System.out.println("Diff:"+diff);
        JSONAssert.assertEquals("[{\"-schema.version.value\":\"1.0.0\",\"+schema.version.value\":\"1.1.0\"},{\"+schema.fields.elementx\":{\"type\":\"string\",\"description\":null}},{\"-schema._id\":\"country|1.0.0\",\"+schema._id\":\"country|1.1.0\"}]]",diff,false);

                                   
    }
}
