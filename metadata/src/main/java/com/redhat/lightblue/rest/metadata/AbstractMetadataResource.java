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
package com.redhat.lightblue.rest.metadata;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import com.redhat.lightblue.rest.metadata.hystrix.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.lightblue.metadata.Metadata;
import com.redhat.lightblue.metadata.MetadataConstants;
import com.redhat.lightblue.metadata.MetadataRole;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.util.Error;

/**
 * @author nmalik
 * @author bserdar
 */
@Produces(MediaType.APPLICATION_JSON)
public abstract class AbstractMetadataResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMetadataResource.class);

    private static final String PARAM_ENTITY = "entity";
    private static final String PARAM_VERSION = "version";

    private static final Metadata metadata;
    static {
        try {
            metadata = RestConfiguration.getFactory().getMetadata();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw Error.get(RestMetadataConstants.ERR_CANT_GET_METADATA, e.getMessage());
        }
    }

    @GET
    @Path("/dependencies")
    public String getDepGraph(@Context SecurityContext sc) {
        return getDepGraph(sc, null, null);
    }

    @GET
    @Path("/{entity}/dependencies")
    public String getDepGraph(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity) {
        return getDepGraph(sc, entity, null);
    }

    @GET
    @Path("/{entity}/{version}/dependencies")
    public String getDepGraph(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, @PathParam(PARAM_VERSION) String version) {
        Error.reset();

        checkPermission(sc, MetadataRole.FIND_DEPENDENCIES);
        return new GetDependenciesCommand(null, entity, version).execute();
    }

    @GET
    @Path("/roles")
    public String getEntityRoles(@Context SecurityContext sc ) {
        return getEntityRoles(sc, null, null);
    }

    @GET
    @Path("/{entity}/roles")
    public String getEntityRoles(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity) {
        return getEntityRoles(sc, entity, null);
    }

    @GET
    @Path("/{entity}/{version}/roles")
    public String getEntityRoles(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, @PathParam(PARAM_VERSION) String version) {
        Error.reset();

        checkPermission(sc, MetadataRole.FIND_ROLES);
        return new GetEntityRolesCommand(null, entity, version).execute();
    }

    @GET
    @Path("/")
    public String getEntityNames(@Context SecurityContext sc) {
        Error.reset();

        checkPermission(sc, MetadataRole.FIND_ENTITY_NAMES);
        return new GetEntityNamesCommand(null, new String[0]).execute();
    }

    @GET
    @Path("/s={statuses}")
    public String getEntityNames(@Context SecurityContext sc, @PathParam("statuses") String statuses) {
        StringTokenizer tok = new StringTokenizer(" ,;:.");
        String[] s = new String[tok.countTokens()];
        int i = 0;
        while (tok.hasMoreTokens()) {
            s[i++] = tok.nextToken();
        }
        Error.reset();

        checkPermission(sc, MetadataRole.FIND_ENTITY_NAMES);
        return new GetEntityNamesCommand(null, s).execute();
    }

    @GET
    @Path("/{entity}")
    public String getEntityVersions(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity) {
        Error.reset();

        checkPermission(sc, MetadataRole.FIND_ENTITY_VERSIONS);
        return new GetEntityVersionsCommand(null, entity).execute();
    }

    @GET
    @Path("/{entity}/{version}")
    public String getMetadata(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, @PathParam(PARAM_VERSION) String version) {
        Error.reset();

        checkPermission(sc, MetadataRole.FIND_ENTITY_METADATA);
        return new GetEntityMetadataCommand(null, entity, version).execute();
    }

    @PUT
    @Path("/{entity}/{version}")
    @Consumes(MediaType.APPLICATION_JSON)
    public String createMetadata(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, @PathParam(PARAM_VERSION) String version, String data) {
        Error.reset();

        checkPermission(sc, MetadataRole.INSERT);
        return new CreateEntityMetadataCommand(null, entity, version, data).execute();
    }

    @PUT
    @Path("/{entity}/schema={version}")
    @Consumes(MediaType.APPLICATION_JSON)
    public String createSchema(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, @PathParam(PARAM_VERSION) String version, String schema) {
        Error.reset();

        checkPermission(sc, MetadataRole.INSERT_SCHEMA);
        return new CreateEntitySchemaCommand(null, entity, version, schema).execute();
    }


    @GET
    @Path("/{entity}/<version>?schema")
    @Consumes(MediaType.APPLICATION_JSON)
    public String getJsonSchema(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, @PathParam(PARAM_VERSION) String version) {
        Error.reset();

        checkPermission(sc, MetadataRoles.FIND_JSON_SCHEMA);
        return new GetJsonSchemaCommand(null, entity, version).execute();
    }


    @PUT
    @Path("/{entity}")
    @Consumes(MediaType.APPLICATION_JSON)
    public String updateEntityInfo(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, String info) {
        Error.reset();

        checkPermission(sc, MetadataRole.UPDATE_ENTITYINFO);
        return new UpdateEntityInfoCommand(null, entity, info).execute();
    }

    @PUT
    @Path("/{entity}/{version}/{status}")
    public String updateSchemaStatus(@Context SecurityContext sc,
            @PathParam(PARAM_ENTITY) String entity,
            @PathParam(PARAM_VERSION) String version,
            @PathParam("status") String status,
            @QueryParam("comment") String comment) {
        Error.reset();

        checkPermission(sc, MetadataRole.UPDATE_ENTITY_SCHEMASTATUS);
        return new UpdateEntitySchemaStatusCommand(null, entity, version, status, comment).execute();
    }

    @POST
    @Path("/{entity}/{version}/default")
    public String setDefaultVersion(@Context SecurityContext sc,
            @PathParam(PARAM_ENTITY) String entity,
            @PathParam(PARAM_VERSION) String version) {
        Error.reset();

        checkPermission(sc, MetadataRole.UPDATE_DEFAULTVERSION);
        return new SetDefaultVersionCommand(null, entity, version).execute();
    }

    @DELETE
    @Path("/{entity}")
    public String removeEntity(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity) {
        Error.reset();

        checkPermission(sc, MetadataRole.DELETE_ENTITY);
        return new RemoveEntityCommand(null, entity).execute();
    }

    @DELETE
    @Path("/{entity}/default")
    public String clearDefaultVersion(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity) {
        Error.reset();

        checkPermission(sc, MetadataRole.UPDATE_DEFAULTVERSION);
        return new SetDefaultVersionCommand(null, entity, null).execute();
    }

    private void checkPermission(SecurityContext sc, MetadataRole roleAllowed){
        final Map<MetadataRole, List<String>> mappedRoles = metadata.getMappedRoles();
        if(mappedRoles == null || mappedRoles.size() == 0){
            // No authorization was configured
            return;
        }

        List<String> roles = mappedRoles.get(roleAllowed);

        if(roles.contains(MetadataConstants.ROLE_NOONE)){
            throw new SecurityException("Unauthorized Request");
        }
        else if(roles.contains(MetadataConstants.ROLE_ANYONE)){
            return;
        }

        for(String role : roles){
            if (sc.isUserInRole(role)){
                return;
            }
        }

        throw new SecurityException("Unauthorized Request. One of the following roles is required: " + roles);
    }

}
