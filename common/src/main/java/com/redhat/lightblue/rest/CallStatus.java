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
package com.redhat.lightblue.rest;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;

import static javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import com.redhat.lightblue.Response;

import com.redhat.lightblue.util.JsonObject;
import com.redhat.lightblue.util.Error;

/**
 * Status information returned from Hystrix commands. It contains the return
 * value, and errors. It is expected that either errors or the return value will
 * be populated.
 */
public class CallStatus<T extends JsonObject> {

    private List<Error> errors;
    private T returnValue;

    private static JsonNodeFactory factory = JsonNodeFactory.withExactBigDecimals(true);

    /**
     * Construct a CallStatus with the given return value
     */
    public CallStatus(T ret) {
        this.returnValue = ret;
    }

    /**
     * Construct a CallStatus witht the given error
     */
    public CallStatus(Error x) {
        this.errors = new ArrayList<>();
        errors.add(x);
    }

    /**
     * Construct empty call status
     */
    public CallStatus() {
    }

    /**
     * Adds an error to the call status
     */
    public void addError(Error error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }

    /**
     * Adds errors to the call status
     */
    public void addErrors(Collection<Error> errors) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.addAll(errors);
    }

    /**
     * Sets the return value of the call status
     */
    public void setReturnValue(T v) {
        this.returnValue = v;
    }

    /**
     * Gets the return value
     */
    public T getReturnValue() {
        return returnValue;
    }

    /**
     * Gets the errors
     */
    public List<Error> getErrors() {
        return errors;
    }

    /**
     * Returns if there are errors in the call status
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * If there are errors, returns an object containing an array of errors, and
     * a status field with value "ERROR". If there are no errors, returns the
     * json representation of the return value.
     */
    public JsonNode toJson() {
        if (hasErrors()) {
            ObjectNode node = factory.objectNode();
            ArrayNode arr = factory.arrayNode();
            for (Error e : errors) {
                arr.add(e.toJson());
            }
            node.put("errors", arr);
            node.put("status", "ERROR");
            return node;
        } else {
            return returnValue == null ? factory.objectNode() : returnValue.toJson();
        }

    }

    /**
     * Returns an http status based on the error information in the return value
     */
    public Status getHttpStatus() {
        if (hasErrors()) {
            return HttpErrorMapper.getStatus(errors.get(0));
        } else if (returnValue instanceof Response) {
            List<Error> l = ((Response) returnValue).getErrors();
            if (l != null && !l.isEmpty()) {
                return HttpErrorMapper.getStatus(l.get(0));
            }
        }
        return Status.OK;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
