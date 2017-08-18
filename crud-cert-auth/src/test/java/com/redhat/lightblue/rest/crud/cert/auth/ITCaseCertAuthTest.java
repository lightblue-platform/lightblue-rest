package com.redhat.lightblue.rest.crud.cert.auth;
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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.lightblue.rest.test.RestConfigurationRule;
import com.redhat.lightblue.rest.test.support.Assets;
import com.redhat.lightblue.rest.test.support.CrudWebXmls;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class ITCaseCertAuthTest {
     
    @Rule
    public final RestConfigurationRule resetRuleConfiguration = new RestConfigurationRule();

    @Before
    public void setup() throws Exception {

    }

    @Deployment
    public static WebArchive createDeployment() throws Exception {
        File[] libs = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .importRuntimeDependencies()
                .resolve()
                .withTransitivity()
                .asFile();

        Path configBase = Paths.get("src/test/resources/");

        WebArchive archive = ShrinkWrap.create(WebArchive.class, "lightblue.war")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsWebInfResource(Assets.forDocument(CrudWebXmls.forNonEE6Container(null)), "web.xml")
            .addAsLibraries(Maven.configureResolver()
                    .workOffline()
                    .loadPomFromFile("pom.xml")
                    .resolve("org.jboss.weld.servlet:weld-servlet")
                    .withTransitivity()
                    .asFile())
            .addPackages(true, "com.redhat.lightblue")
            
            .addAsResource(configBase.resolve("config.properties").toFile());

        for (File file : libs) {
            if (!file.toString().contains("lightblue-")) {
                archive.addAsLibrary(file);
            }
        }
        
        return archive;
    }

    @Test
    @RunAsClient
    public void testHealthCheck(@ArquillianResource URL url) throws Exception {
        ClientRequest request = new ClientRequest(UriBuilder.fromUri(url.toURI())
                .path("healthcheck")
                .build()
                .toString());
        request.accept(MediaType.APPLICATION_JSON);
        ClientResponse<String> response = request.get(String.class);
        ObjectNode jsonNode = (ObjectNode) new ObjectMapper().readTree(response.getEntity());
        
        System.out.println("HealthCheckMessage: " + jsonNode.elements().next().get("message").asText());
        assertTrue(jsonNode.elements().next().get("healthy").asBoolean());
    }
}
