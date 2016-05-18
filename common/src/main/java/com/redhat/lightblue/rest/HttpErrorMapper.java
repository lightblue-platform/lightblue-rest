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

import javax.ws.rs.core.Response;

import com.redhat.lightblue.crud.CrudConstants;

import com.redhat.lightblue.util.Error;

/**
 * Maps some of the Lightblue errors to meaningful Http error codes
 */
public final class HttpErrorMapper {

    /**
     * Maps the error code to an Http error code
     *
     * @param errorCode The Lightblue error code
     *
     * If errorCode is null, returns OK. Otherwise, this method tries to map the
     * lightblue error to an http error. If it cannot map to a suitable error,
     * then INTERNAL_SERVER_ERROR is returned.
     */
    public static Response.Status getStatus(String errorCode) {
        if (CrudConstants.ERR_NO_ACCESS.equals(errorCode)
                || CrudConstants.ERR_NO_FIELD_INSERT_ACCESS.equals(errorCode)
                || CrudConstants.ERR_NO_FIELD_UPDATE_ACCESS.equals(errorCode)) {
            return Response.Status.FORBIDDEN;
        } else if (CrudConstants.ERR_DISABLED_METADATA.equals(errorCode)
                || CrudConstants.ERR_UNKNOWN_ENTITY.equals(errorCode)) {
            return Response.Status.NOT_FOUND;
        } else if (errorCode != null) {
            return Response.Status.INTERNAL_SERVER_ERROR;
        } else {
            return Response.Status.OK;
        }
    }

    /**
     * Maps the error code of the error to an http error code. Returns OK if err
     * is null.
     */
    public static Response.Status getStatus(Error err) {
        if (err != null) {
            return getStatus(err.getErrorCode());
        } else {
            return Response.Status.OK;
        }
    }

    /**
     * Returns a suitable HTTP error code based on the exception. Returns OK if
     * x is null.
     */
    public static Response.Status getStatus(Exception x) {
        if (x != null) {
            if (x instanceof Error) {
                return getStatus((Error) x);
            } else {
                return Response.Status.INTERNAL_SERVER_ERROR;
            }
        } else {
            return Response.Status.OK;
        }
    }
}
