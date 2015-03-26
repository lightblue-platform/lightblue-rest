/*
 Copyright 2015 Red Hat, Inc. and/or its affiliates.

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
package com.redhat.lightblue.rest.test;

import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;

import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.rest.RestConfiguration;

/**
 * This {@link TestRule} implementation has one sole purpose, which is to ensure that
 * the {@link LightblueFactory} is reset on the {@link RestConfiguration} appropriately
 * after each test or test suite.
 *
 * @author dcrissman
 */
public class RestConfigurationRule extends ExternalResource {

    @Override
    protected void before() throws Throwable {
        RestConfiguration.setFactory(null);
    }

    @Override
    protected void after() {
        RestConfiguration.setFactory(null);
    }

}
