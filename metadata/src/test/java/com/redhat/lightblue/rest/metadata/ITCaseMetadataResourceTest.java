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
import com.redhat.lightblue.config.DataSourcesConfiguration;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.config.MetadataConfiguration;
import com.redhat.lightblue.metadata.mongo.MongoMetadata;
import com.redhat.lightblue.mongo.test.EmbeddedMongo;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.util.JsonUtils;
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
import javax.ws.rs.core.SecurityContext;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.redhat.lightblue.util.test.FileUtil.readFile;

/**
 * @author lcestari
 */
@RunWith(Arquillian.class)
public class ITCaseMetadataResourceTest {

    private static EmbeddedMongo mongo = EmbeddedMongo.getInstance();

    @Before
    public void setup() {
        mongo.getDB().createCollection(MongoMetadata.DEFAULT_METADATA_COLLECTION, null);
        BasicDBObject index = new BasicDBObject("name", 1);
        index.put("version.value", 1);
        mongo.getDB().getCollection(MongoMetadata.DEFAULT_METADATA_COLLECTION).ensureIndex(index, "name", true);
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

    private String esc(String s) {
        return s.replace("'", "\"");
    }

    @Inject
    private AbstractMetadataResource cutMetadataResource;

    @Test
    public void testFirstIntegrationTest() throws IOException, URISyntaxException, JSONException {
        Assert.assertNotNull("MetadataResource was not injected by the container", cutMetadataResource);

        SecurityContext sc = new TestSecurityContext();

        RestConfiguration.setDatasources(new DataSourcesConfiguration(JsonUtils.json(readFile("datasources.json"))));
        RestConfiguration.setFactory(new LightblueFactory(RestConfiguration.getDatasources()));
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
}
