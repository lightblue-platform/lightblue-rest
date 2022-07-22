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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.redhat.lightblue.config.MetadataConfiguration;
import com.redhat.lightblue.mongo.config.MongoConfiguration;
import com.redhat.lightblue.mongo.metadata.MongoMetadata;
import com.redhat.lightblue.rest.test.RestConfigurationRule;
import com.redhat.lightblue.rest.test.support.Assets;
import com.redhat.lightblue.rest.test.support.CrudWebXmls;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.Defaults;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.packageresolver.Command;
import de.flapdoodle.embed.process.config.RuntimeConfig;
import de.flapdoodle.embed.process.config.store.TimeoutConfig;
import de.flapdoodle.embed.process.extract.DirectoryAndExecutableNaming;
import de.flapdoodle.embed.process.extract.UserTempNaming;
import de.flapdoodle.embed.process.io.directories.PlatformTempDir;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.embed.process.store.Downloader;
import de.flapdoodle.embed.process.store.ExtractedArtifactStore;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test to verify index creation in mongo backend through front-end API's.
 *
 * @author nmalik
 */
@RunWith(Arquillian.class)
public class ITMongoIndexManipulationTest {

  @Rule
  public final RestConfigurationRule resetRuleConfiguration = new RestConfigurationRule();

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
      RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(Command.MongoD)
          .artifactStore(ExtractedArtifactStore.builder()
              .extraction(DirectoryAndExecutableNaming.builder()
                  .executableNaming(new UserTempNaming())
                  .directory(new PlatformTempDir()).build())
              .downloader(Downloader.platformDefault())
              .temp(DirectoryAndExecutableNaming.builder()
                  .executableNaming(new UserTempNaming())
                  .directory(new PlatformTempDir()).build())
              .downloadConfig(Defaults.downloadConfigFor(Command.MongoD)
                  .timeoutConfig(TimeoutConfig.builder()
                      .connectionTimeout((int) Duration.ofMinutes(1).toMillis())
                      .readTimeout((int) Duration.ofMinutes(5).toMillis())
                      .build())
                  .fileNaming(new UserTempNaming()).build())
              .build())
          .build();

      MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);
      mongodExe = runtime.prepare(
          MongodConfig.builder()
              .version(Version.V5_0_2)
              .net(new Net(MONGO_PORT, Network.localhostIsIPv6()))
              .build()
      );
      try {
        mongod = mongodExe.start();
      } catch (Throwable t) {
        // try again, could be killed breakpoint in IDE
        mongod = mongodExe.start();
      }
      mongo = new MongoClient(IN_MEM_CONNECTION_URL);

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
  public void setup() {
    db.createCollection(MongoMetadata.DEFAULT_METADATA_COLLECTION, null);
    BasicDBObject index = new BasicDBObject("name", 1);
    index.put("version.value", 1);
    db.getCollection(MongoMetadata.DEFAULT_METADATA_COLLECTION).createIndex(index, "name", true);
    db.createCollection("test", null);
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
    File[] libs = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve()
        .withTransitivity().asFile();

    WebArchive archive = ShrinkWrap.create(WebArchive.class, "lightblue.war")
        .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
        .addAsWebInfResource(
            Assets.forDocument(CrudWebXmls.forNonEE6Container(RestApplication.class)), "web.xml")
        .addAsLibraries(Maven.configureResolver()
            .workOffline()
            .loadPomFromFile("pom.xml")
            .resolve("org.jboss.weld.servlet:weld-servlet")
            .withTransitivity()
            .asFile())
        .addPackages(true, "com.redhat.lightblue")
        .addAsResource(new File("src/test/resources/lightblue-metadata.json"),
            MetadataConfiguration.FILENAME)
        .addAsResource(new File("src/test/resources/datasources.json"), "datasources.json")
        .addAsResource(EmptyAsset.INSTANCE, "resources/test.properties");

    for (File file : libs) {
      archive.addAsLibrary(file);
    }
    return archive;
  }

  @Inject
  private AbstractMetadataResource metadataResource;

  @Test
  public void createWithSimpleIndex() throws Exception {
    String metadata = readFile(getClass().getSimpleName() + "-createWithSimpleIndex-metadata.json");
    String entityName = "test";
    String entityVersion = "1.0.0";
    SecurityContext sc = new TestSecurityContext();

    Assert.assertNotNull(metadata);
    Assert.assertTrue(metadata.length() > 0);

    // create metadata without any non-default indexes
    metadataResource.createMetadata(sc, entityName, entityVersion, metadata);

    DBCollection metadataCollection = mongo.getDB(DB_NAME).getCollection("metadata");
    Assert.assertTrue("Metadata was not created!", 2 <= metadataCollection.find().count());

    DBCollection entityCollection = mongo.getDB(DB_NAME).getCollection(entityName);

    // verify has _id and field1 index by simply check on index count
    Assert.assertEquals("indexes not created", 2, entityCollection.getIndexInfo().size());
  }

  @Test
  public void addSimpleIndex_forced() throws Exception {
    String metadata = readFile(getClass().getSimpleName() + "-addSimpleIndex-metadata.json");
    String entityName = "test";
    String entityVersion = "1.0.0";
    SecurityContext sc = new TestSecurityContext();

    Assert.assertNotNull(metadata);
    Assert.assertTrue(metadata.length() > 0);

    // create metadata without any non-default indexes
    metadataResource.createMetadata(sc, entityName, entityVersion, metadata);

    DBCollection metadataCollection = mongo.getDB(DB_NAME).getCollection("metadata");
    Assert.assertTrue("Metadata was not created!", 2 <= metadataCollection.find().count());

    DBCollection entityCollection = mongo.getDB(DB_NAME).getCollection(entityName);

    Assert.assertEquals("expected no indexes", 0, entityCollection.getIndexInfo().size());

    entityCollection.createIndex(new BasicDBObject("x", 1));

    // since index was forced the collection is initialized and we don't need to create a dummy doc
    Assert.assertEquals("indexes not created", 2, entityCollection.getIndexInfo().size());
  }

  @Test
  public void addSimpleIndex() throws Exception {
    String metadata = readFile(getClass().getSimpleName() + "-addSimpleIndex-metadata.json");
    String entityInfo = readFile(getClass().getSimpleName() + "-addSimpleIndex-entityInfo.json");
    String entityName = "test";
    String entityVersion = "1.0.0";
    SecurityContext sc = new TestSecurityContext();

    Assert.assertNotNull(metadata);
    Assert.assertTrue(metadata.length() > 0);

    // create metadata without any non-default indexes
    metadataResource.createMetadata(sc, entityName, entityVersion, metadata);

    DBCollection metadataCollection = mongo.getDB(DB_NAME).getCollection("metadata");
    Assert.assertTrue("Metadata was not created!", 2 <= metadataCollection.find().count());

    DBCollection entityCollection = mongo.getDB(DB_NAME).getCollection(entityName);

    // verify no indexes, since collection hasn't been touched yet (no indexes, no data)
    Assert.assertEquals("expected no indexes", 0, entityCollection.getIndexInfo().size());

    // update entityInfo to add an index
    metadataResource.updateEntityInfo(sc, entityName, entityInfo);

    // verify has _id and field1 index by simply check on index count
    Assert.assertEquals("indexes not created", 2, entityCollection.getIndexInfo().size());
  }

  @Test

  public void deleteSimpleIndex() throws Exception {
    String metadata = readFile(getClass().getSimpleName() + "-deleteSimpleIndex-metadata.json");
    String entityInfo = readFile(getClass().getSimpleName() + "-deleteSimpleIndex-entityInfo.json");
    String entityName = "test";
    String entityVersion = "1.0.0";
    SecurityContext sc = new TestSecurityContext();

    Assert.assertNotNull(metadata);
    Assert.assertTrue(metadata.length() > 0);

    // create metadata with one non-default index
    metadataResource.createMetadata(sc, entityName, entityVersion, metadata);

    DBCollection metadataCollection = mongo.getDB(DB_NAME).getCollection("metadata");
    Assert.assertTrue("Metadata was not created!", 2 <= metadataCollection.find().count());

    DBCollection entityCollection = mongo.getDB(DB_NAME).getCollection(entityName);

    // verify no indexes, since collection hasn't been touched yet (no indexes, no data)
    Assert.assertEquals("indexes not created", 2, entityCollection.getIndexInfo().size());

    // update entityInfo to remove index
    metadataResource.updateEntityInfo(sc, entityName, entityInfo);

    // verify has _id and field1 index by simply check on index count
    Assert.assertEquals("index not deleted", 1, entityCollection.getIndexInfo().size());
  }

  @Test

  public void createWithArrayIndex() throws Exception {
    String metadata = readFile(getClass().getSimpleName() + "-createWithArrayIndex-metadata.json");
    String entityName = "test";
    String entityVersion = "1.0.0";
    SecurityContext sc = new TestSecurityContext();

    Assert.assertNotNull(metadata);
    Assert.assertTrue(metadata.length() > 0);

    // create metadata without any non-default indexes
    metadataResource.createMetadata(sc, entityName, entityVersion, metadata);

    DBCollection metadataCollection = mongo.getDB(DB_NAME).getCollection("metadata");
    Assert.assertTrue("Metadata was not created!", 2 <= metadataCollection.find().count());

    DBCollection entityCollection = mongo.getDB(DB_NAME).getCollection(entityName);

    // verify has _id and field1 index by simply check on index count
    Assert.assertEquals("indexes not created", 2, entityCollection.getIndexInfo().size());
  }

  @Test

  public void addArrayIndex() throws Exception {
    String metadata = readFile(getClass().getSimpleName() + "-addArrayIndex-metadata.json");
    String entityInfo = readFile(getClass().getSimpleName() + "-addArrayIndex-entityInfo.json");
    String entityName = "test";
    String entityVersion = "1.0.0";
    SecurityContext sc = new TestSecurityContext();

    Assert.assertNotNull(metadata);
    Assert.assertTrue(metadata.length() > 0);

    // create metadata without any non-default indexes
    metadataResource.createMetadata(sc, entityName, entityVersion, metadata);

    DBCollection metadataCollection = mongo.getDB(DB_NAME).getCollection("metadata");
    Assert.assertTrue("Metadata was not created!", 2 <= metadataCollection.find().count());

    DBCollection entityCollection = mongo.getDB(DB_NAME).getCollection(entityName);

    // verify no indexes, since collection hasn't been touched yet (no indexes, no data)
    Assert.assertEquals("expected no indexes", 0, entityCollection.getIndexInfo().size());

    // update entityInfo to add an index
    metadataResource.updateEntityInfo(sc, entityName, entityInfo);

    // verify has _id and field1 index by simply check on index count
    Assert.assertEquals("indexes not created", 2, entityCollection.getIndexInfo().size());
  }

  @Test

  public void esbMessage() throws Exception {
    String metadata = readFile(getClass().getSimpleName() + "-esbMessage-metadata.json");
    String entityInfo = readFile(getClass().getSimpleName() + "-esbMessage-entityInfo.json");
    String entityName = "test";
    String entityVersion = "0.4.0-SNAPSHOT";
    SecurityContext sc = new TestSecurityContext();

    Assert.assertNotNull(metadata);
    Assert.assertTrue(metadata.length() > 0);

    // create metadata without any non-default indexes
    metadataResource.createMetadata(sc, entityName, entityVersion, metadata);

    DBCollection metadataCollection = mongo.getDB(DB_NAME).getCollection("metadata");
    Assert.assertTrue("Metadata was not created!", 2 <= metadataCollection.find().count());

    DBCollection entityCollection = mongo.getDB(DB_NAME).getCollection(entityName);

    // verify no indexes, since collection hasn't been touched yet (no indexes, no data)
    Assert.assertEquals("expected no indexes", 0, entityCollection.getIndexInfo().size());

    // update entityInfo to add an index
    metadataResource.updateEntityInfo(sc, entityName, entityInfo);

    // verify has _id and field1 index by simply check on index count
    Assert.assertEquals("indexes not created", 2, entityCollection.getIndexInfo().size());

    // verify specific index
    boolean verified = false;
    for (DBObject dbi : entityCollection.getIndexInfo()) {
      if (((BasicDBObject) dbi.get("key")).entrySet().iterator().next().getKey().equals("_id")) {
        continue;
      }
      // non _id index is the one we want to verify with
      Iterator<Map.Entry<String, Object>> i = ((BasicDBObject) dbi.get("key")).entrySet()
          .iterator();
      Assert.assertEquals("esbMessageSearchable.value", i.next().getKey());
      Assert.assertEquals("esbMessageSearchable.path", i.next().getKey());
      verified = true;
    }
    Assert.assertTrue(verified);
  }

  @Test

  public void deleteArrayIndex() throws Exception {
    String metadata = readFile(getClass().getSimpleName() + "-deleteArrayIndex-metadata.json");
    String entityInfo = readFile(getClass().getSimpleName() + "-deleteArrayIndex-entityInfo.json");
    String entityName = "test";
    String entityVersion = "1.0.0";
    SecurityContext sc = new TestSecurityContext();

    Assert.assertNotNull(metadata);
    Assert.assertTrue(metadata.length() > 0);

    // create metadata without any non-default indexes
    metadataResource.createMetadata(sc, entityName, entityVersion, metadata);

    DBCollection metadataCollection = mongo.getDB(DB_NAME).getCollection("metadata");
    Assert.assertTrue("Metadata was not created!", 2 <= metadataCollection.find().count());

    DBCollection entityCollection = mongo.getDB(DB_NAME).getCollection(entityName);

    // verify no indexes, since collection hasn't been touched yet (no indexes, no data)
    Assert.assertEquals("indexes not created", 2, entityCollection.getIndexInfo().size());

    // update entityInfo to delete an index
    metadataResource.updateEntityInfo(sc, entityName, entityInfo);

    // verify has _id and field1 index by simply check on index count
    Assert.assertEquals("index not deleted", 1, entityCollection.getIndexInfo().size());
  }
}
