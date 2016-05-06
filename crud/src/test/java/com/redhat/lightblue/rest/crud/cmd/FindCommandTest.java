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
package com.redhat.lightblue.rest.crud.cmd;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author nmalik
 */
public class FindCommandTest extends AbstractRestCommandTest {

    @Test
    public void runFindWithReturn() {
        FindCommand command = new FindCommand(mediator, "name", "version", "{\"request\":\"data\"}");

        String output = command.run().toString();

        Assert.assertNotNull(output);

        Assert.assertEquals("find", mediator.methodCalled);
    }

    @Test
    public void runFindWithParseProblem() {
        FindCommand command = new FindCommand(mediator, "name", "version", "{\"request\":\"invalid}");

        String output = command.run().toString();

        Assert.assertNotNull(output);

        Assert.assertTrue(output.contains("Error during the parse of the request"));
    }

    @Test
    public void runFindWithInvalid() {
        FindCommand command = new FindCommand(mediator, null, "version", "{\"request\":\"invalid\"}");

        String output = command.run().toString();

        Assert.assertNotNull(output);

        Assert.assertTrue(output.contains("Request is not valid"));
    }
}
