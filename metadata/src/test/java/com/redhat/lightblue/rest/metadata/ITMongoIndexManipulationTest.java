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

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.redhat.lightblue.config.MetadataConfiguration;
import com.redhat.lightblue.mongo.test.EmbeddedMongo;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.*;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.io.File;
import java.util.Iterator;
import java.util.Map;

import static com.redhat.lightblue.util.test.FileUtil.readFile;

/**
 * Test to verify index creation in mongo backend through front-end API's.
 *
 * @author nmalik
 */
@RunWith(Arquillian.class)
public class ITMongoIndexManipulationTest {

    private static EmbeddedMongo mongo = EmbeddedMongo.getInstance();

    @Before
    public void setup() {
        mongo.dropCollection("metadata");
        mongo.dropCollection("test");

        Assert.assertEquals("setup: metadata collection is not empty", 0, mongo.getDB().getCollection("metadata").find().count());
    }

    @After
    public void teardown() {
        mongo.reset();
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
        String entityName = "test";
        String entityVersion = "1.0.0";
        SecurityContext sc = new TestSecurityContext();

        Assert.assertNotNull(metadata);
        Assert.assertTrue(metadata.length() > 0);

        // create metadata without any non-default indexes
        metadataResource.createMetadata(sc, entityName, entityVersion, metadata);

        DBCollection metadataCollection = mongo.getDB().getCollection("metadata");
        Assert.assertTrue("Metadata was not created!", 2 <= metadataCollection.find().count());

        DBCollection entityCollection = mongo.getDB().getCollection(entityName);

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

        DBCollection metadataCollection = mongo.getDB().getCollection("metadata");
        Assert.assertTrue("Metadata was not created!", 2 <= metadataCollection.find().count());

        DBCollection entityCollection = mongo.getDB().getCollection(entityName);

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

        DBCollection metadataCollection = mongo.getDB().getCollection("metadata");
        Assert.assertTrue("Metadata was not created!", 2 <= metadataCollection.find().count());

        DBCollection entityCollection = mongo.getDB().getCollection(entityName);

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

        DBCollection metadataCollection = mongo.getDB().getCollection("metadata");
        Assert.assertTrue("Metadata was not created!", 2 <= metadataCollection.find().count());

        DBCollection entityCollection = mongo.getDB().getCollection(entityName);

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

        DBCollection metadataCollection = mongo.getDB().getCollection("metadata");
        Assert.assertTrue("Metadata was not created!", 2 <= metadataCollection.find().count());

        DBCollection entityCollection = mongo.getDB().getCollection(entityName);

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

        DBCollection metadataCollection = mongo.getDB().getCollection("metadata");
        Assert.assertTrue("Metadata was not created!", 2 <= metadataCollection.find().count());

        DBCollection entityCollection = mongo.getDB().getCollection(entityName);

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

        DBCollection metadataCollection = mongo.getDB().getCollection("metadata");
        Assert.assertTrue("Metadata was not created!", 2 <= metadataCollection.find().count());

        DBCollection entityCollection = mongo.getDB().getCollection(entityName);

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
            Iterator<Map.Entry<String, Object>> i = ((BasicDBObject) dbi.get("key")).entrySet().iterator();
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

        DBCollection metadataCollection = mongo.getDB().getCollection("metadata");
        Assert.assertTrue("Metadata was not created!", 2 <= metadataCollection.find().count());

        DBCollection entityCollection = mongo.getDB().getCollection(entityName);

        // verify no indexes, since collection hasn't been touched yet (no indexes, no data)
        Assert.assertEquals("indexes not created", 2, entityCollection.getIndexInfo().size());

        // update entityInfo to delete an index
        metadataResource.updateEntityInfo(sc, entityName, entityInfo);

        // verify has _id and field1 index by simply check on index count
        Assert.assertEquals("index not deleted", 1, entityCollection.getIndexInfo().size());
    }
}
