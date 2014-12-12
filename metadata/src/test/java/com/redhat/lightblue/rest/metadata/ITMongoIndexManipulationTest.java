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

import com.mongodb.*;
import com.redhat.lightblue.config.MetadataConfiguration;
import com.redhat.lightblue.metadata.mongo.MongoMetadata;
import com.redhat.lightblue.mongo.config.MongoConfiguration;
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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.redhat.lightblue.util.test.FileUtil.readFile;
import javax.ws.rs.core.SecurityContext;

/**
 * Test to verify index creation in mongo backend through front-end API's.
 *
 * @author nmalik
 */
@RunWith(Arquillian.class)
public class ITMongoIndexManipulationTest {

    public static class FileStreamProcessor implements IStreamProcessor {
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
    private static final int MONGO_PORT = 27777;
    private static final String IN_MEM_CONNECTION_URL = MONGO_HOST + ":" + MONGO_PORT;

    private static final String DB_NAME = "testmetadata";

    private static final MongodExecutable mongodExe;
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
            } catch (IOException t) {
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
                    super.start();
                    if (mongod != null) {
                        mongod.stop();
                        mongodExe.stop();
                    }
                    db = null;
                    mongo = null;
                    mongod = null;
                }

            });
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    @Before
    public void setup() throws Exception {
        db.createCollection(MongoMetadata.DEFAULT_METADATA_COLLECTION, null);
        BasicDBObject index = new BasicDBObject("name", 1);
        index.put("version.value", 1);
        db.getCollection(MongoMetadata.DEFAULT_METADATA_COLLECTION).ensureIndex(index, "name", true);
    }

    @After
    public void teardown() {
        if (mongo != null) {
            mongo.dropDatabase(DB_NAME);
        }
    }

    @Deployment
    public static WebArchive createDeployment() {
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve().withTransitivity().asFile();

        WebArchive archive = ShrinkWrap.create(WebArchive.class, "test.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(new File("src/test/resources/lightblue-metadata.json"), MetadataConfiguration.FILENAME)
                .addAsResource(new File("src/test/resources/datasources.json"), "datasources.json")
                .addAsResource(EmptyAsset.INSTANCE, "resources/test.properties");

        for (File file : libs) {
            archive.addAsLibrary(file);
        }
        archive.addPackages(true, "com.redhat.lightblue");
        return archive;

    }

    @Inject
    private AbstractMetadataResource metadataResource;

    @Test
    public void createWithSimpleIndex() throws Exception {
        String metadata = readFile(getClass().getSimpleName() + "-createWithSimpleIndex-metadata.json");
        String entityName = "createWithSimpleIndex";
        String entityVersion = "1.0.0";
        SecurityContext sc = new TestSecurityContext();

        // create metadata without any non-default indexes
        metadataResource.createMetadata(sc, entityName, entityVersion, metadata);

        DBCollection entityCollection = db.getCollection(entityName);

        // verify has _id and field1 index by simply check on index count
        Assert.assertEquals(2, entityCollection.getIndexInfo().size());
    }

    @Test
    public void addSimpleIndex_forced() throws Exception {
        String metadata = readFile(getClass().getSimpleName() + "-addSimpleIndex-metadata.json");
        String entityName = "addSimpleIndex";
        String entityVersion = "1.0.0";
        SecurityContext sc = new TestSecurityContext();

        // create metadata without any non-default indexes
        metadataResource.createMetadata(sc, entityName, entityVersion, metadata);

        DBCollection entityCollection = db.getCollection(entityName);
        
        Assert.assertEquals(0, entityCollection.getIndexInfo().size());

        entityCollection.createIndex(new BasicDBObject("x", 1));
        
        // since index was forced the collection is initialized and we don't need to create a dummy doc
        Assert.assertEquals(2, entityCollection.getIndexInfo().size());
    }

    @Test
    public void addSimpleIndex() throws Exception {
        String metadata = readFile(getClass().getSimpleName() + "-addSimpleIndex-metadata.json");
        String entityInfo1 = readFile(getClass().getSimpleName() + "-addSimpleIndex-entityInfo.json");
        String entityName = "addSimpleIndex";
        String entityVersion = "1.0.0";
        SecurityContext sc = new TestSecurityContext();

        // create metadata without any non-default indexes
        metadataResource.createMetadata(sc, entityName, entityVersion, metadata);

        DBCollection entityCollection = db.getCollection(entityName);
        
        // verify no indexes, since collection hasn't been touched yet (no indexes, no data)
        Assert.assertEquals(0, entityCollection.getIndexInfo().size());

        // update entityInfo to add an index
        metadataResource.updateEntityInfo(sc, entityName, entityInfo1);

        // verify has _id and field1 index by simply check on index count
        Assert.assertEquals(2, entityCollection.getIndexInfo().size());
    }
}
