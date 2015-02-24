package com.redhat.lightblue.rest.metadata;

import com.mongodb.BasicDBObject;
import com.redhat.lightblue.config.MetadataConfiguration;
import com.redhat.lightblue.metadata.mongo.MongoMetadata;
import com.redhat.lightblue.mongo.test.EmbeddedMongo;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.SecurityContext;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by lcestari on 2/24/15.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ITClientCaseMetadataResourceTest {

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

    private static final String RESOURCE_PREFIX = "metadata";

    @ArquillianResource(MetadataResource.class)
    URL deploymentUrl;

    @BeforeClass
    public static void initResteasyClient() {
        RegisterBuiltin.register(ResteasyProviderFactory.getInstance());
        HttpClient httpClient = new DefaultHttpClient();
        ApacheHttpClient4Executor executor = new ApacheHttpClient4Executor(httpClient) {
            @Override
            public ClientResponse execute(ClientRequest request) throws Exception {
                request.header("Accept-Encoding", "lzf");
                ClientResponse execute = super.execute(request);
                return execute;
            }

        };

    }

    @Test
    public void testGetCustomerByIdUsingClientProxy() throws Exception {
        SecurityContext sc = new TestSecurityContext();

        MetadataResource client = ProxyFactory.create(MetadataResource.class, deploymentUrl.toString() + RESOURCE_PREFIX);
        // GET http://localhost:8080/test/rest/customer/1
        String response = client.getEntityRoles(sc);

        assertNotNull(response);

        System.out.println("GET /customer/1 HTTP/1.1\n\n" + response);

        response = response.replaceAll("<\\?xml.*\\?>", "").trim();
        assertEquals("<customer><id>1</id><name>Acme Corporation</name></customer>", response);
    }
}
