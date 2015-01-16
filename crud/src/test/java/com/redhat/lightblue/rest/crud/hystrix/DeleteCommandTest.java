/*
 Copyright 2013 Red Hat, Inc. and/or its affiliates.

 This file is part of lightblue.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.redhat.lightblue.rest.crud.hystrix;

import com.redhat.lightblue.config.CrudConfiguration;
import com.redhat.lightblue.config.MetadataConfiguration;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/**
 *
 * @author nmalik
 */
@RunWith(Arquillian.class)
public class DeleteCommandTest extends AbstractRestCommandTest {

    @Deployment
    public static WebArchive createDeployment() {
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve().withTransitivity().asFile();

        WebArchive archive = ShrinkWrap.create(WebArchive.class, "test.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(new File(PATH + MetadataConfiguration.FILENAME), MetadataConfiguration.FILENAME)
                .addAsResource(new File(PATH + CrudConfiguration.FILENAME), CrudConfiguration.FILENAME)
                .addAsResource(new File(PATH + DATASOURCESJSON), DATASOURCESJSON)
                .addAsResource(new File(PATH + CONFIGPROPERTIES), CONFIGPROPERTIES);

        for (File file : libs) {
            archive.addAsLibrary(file);
        }
        archive.addPackages(true, "com.redhat.lightblue");
        return archive;

    }

    private static final String PATH = "src/test/resources/it/it-";
    private static final String CONFIGPROPERTIES = "config.properties";
    private static final String DATASOURCESJSON = "datasources.json";

    @Test
    public void execute() {
        DeleteCommand command = new DeleteCommand(null, mediator, "name", "version", "{\"request\":\"data\"}");

        String output = command.execute().toString();

        Assert.assertNotNull(output);

        Assert.assertEquals("delete", mediator.methodCalled);
    }
}
