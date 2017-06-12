package com.redhat.lightblue.rest.crud;

import java.io.File;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

@RunWith(Arquillian.class)
public class TestHealthCheckServlet {

	@Deployment
	public static WebArchive createDeployment() {

		File[] libs = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve()
				.withTransitivity().asFile();

		WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war").addAsWebInfResource(EmptyAsset.INSTANCE,
				"beans.xml");

		for (File file : libs) {
			if (file.toString().indexOf("lightblue-") == -1) {
				war.addAsLibrary(file);
			}
		}

		war.addPackages(true, "com.redhat.lightblue");
		for (Object x : war.getContent().keySet()) {
			System.out.println(x.toString());
		}

		war.merge(ShrinkWrap.create(GenericArchive.class).as(ExplodedImporter.class).importDirectory("src/main/webapp")
				.as(GenericArchive.class), "/", Filters.includeAll());

		return war;
	}

    @Test
    public void testHealthCheckEndpoint(/* @ArquillianResource URL deploymentUrl */) throws Exception {
//            WebClient webClient = new WebClient();
//            HtmlPage page = webClient.getPage(deploymentUrl.toExternalForm() + "/healthcheck");
//
//            System.out.println(page);
//            webClient.closeAllWindows();
    }
}
