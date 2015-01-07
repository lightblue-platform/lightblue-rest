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

import com.redhat.lightblue.metadata.Metadata;
import com.redhat.lightblue.metadata.MetadataRoles;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.rest.metadata.hystrix.*;
import com.redhat.lightblue.util.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

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

        final Map<MetadataRoles, List<String>> mappedRoles = metadata.getMappedRoles();
        if(mappedRoles == null || mappedRoles.size() == 0){
            // No authorization was configured
            return new GetDependenciesCommand(null, entity, version).execute();
        }

        List<String> roles = mappedRoles.get(MetadataRoles.FIND_DEPENDENCIES.toString());
        for (String role : roles) {
            if (sc.isUserInRole(role)){
                return new GetDependenciesCommand(null, entity, version).execute();
            }
        }
        throw new SecurityException("Unauthorized request for 'GET /dependencies'. One of the following roles is needed: "+roles);
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

        final Map<MetadataRoles, List<String>> mappedRoles = metadata.getMappedRoles();
        if(mappedRoles == null || mappedRoles.size() == 0){
            // No authorization was configured
            return new GetEntityRolesCommand(null, entity, version).execute();
        }

        List<String> roles = mappedRoles.get(MetadataRoles.FIND_ROLES.toString());
        for (String role : roles) {
            if (sc.isUserInRole(role)){
                return new GetEntityRolesCommand(null, entity, version).execute();
            }
        }
        throw new SecurityException("Unauthorized request for 'GET /roles'. One of the following roles is needed: "+roles);
    }

    @GET
    @Path("/")
    public String getEntityNames(@Context SecurityContext sc) {
        Error.reset();

        final Map<MetadataRoles, List<String>> mappedRoles = metadata.getMappedRoles();
        if(mappedRoles == null || mappedRoles.size() == 0){
            // No authorization was configured
            return new GetEntityNamesCommand(null, new String[0]).execute();
        }

        List<String> roles = mappedRoles.get(MetadataRoles.FIND_ENTITY_NAMES.toString());
        for (String role : roles) {
            if (sc.isUserInRole(role)){
                return new GetEntityNamesCommand(null, new String[0]).execute();
            }
        }
        throw new SecurityException("Unauthorized request for 'GET /'. One of the following roles is needed: "+roles);
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

        final Map<MetadataRoles, List<String>> mappedRoles = metadata.getMappedRoles();
        if(mappedRoles == null || mappedRoles.size() == 0){
            // No authorization was configured
            return new GetEntityNamesCommand(null, s).execute();
        }

        List<String> roles = mappedRoles.get(MetadataRoles.FIND_ENTITY_NAMES.toString());
        for (String role : roles) {
            if (sc.isUserInRole(role)){
                return new GetEntityNamesCommand(null, s).execute();
            }
        }
        throw new SecurityException("Unauthorized request for 'GET /s=?status'. One of the following roles is needed: "+roles);
    }

    @GET
    @Path("/{entity}")
    public String getEntityVersions(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity) {
        Error.reset();

        final Map<MetadataRoles, List<String>> mappedRoles = metadata.getMappedRoles();
        if(mappedRoles == null || mappedRoles.size() == 0){
            // No authorization was configured
            return new GetEntityVersionsCommand(null, entity).execute();
        }

        List<String> roles = mappedRoles.get(MetadataRoles.FIND_ENTITY_VERSIONS.toString());
        for (String role : roles) {
            if (sc.isUserInRole(role)){
                return new GetEntityVersionsCommand(null, entity).execute();
            }
        }
        throw new SecurityException("Unauthorized request for 'GET /?entity'. One of the following roles is needed: "+roles);
    }

    @GET
    @Path("/{entity}/{version}")
    public String getMetadata(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, @PathParam(PARAM_VERSION) String version) {
        Error.reset();

        final Map<MetadataRoles, List<String>> mappedRoles = metadata.getMappedRoles();
        if(mappedRoles == null || mappedRoles.size() == 0){
            // No authorization was configured
            return new GetEntityMetadataCommand(null, entity, version).execute();
        }

        List<String> roles = mappedRoles.get(MetadataRoles.FIND_ENTITY_METADATA.toString());
        for (String role : roles) {
            if (sc.isUserInRole(role)){
                return new GetEntityMetadataCommand(null, entity, version).execute();
            }
        }
        throw new SecurityException("Unauthorized request for 'GET /?entity/?version'. One of the following roles is needed: "+roles);
    }

    @PUT
    @Path("/{entity}/{version}")
    @Consumes(MediaType.APPLICATION_JSON)
    public String createMetadata(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, @PathParam(PARAM_VERSION) String version, String data) {
        Error.reset();

        final Map<MetadataRoles, List<String>> mappedRoles = metadata.getMappedRoles();
        if(mappedRoles == null || mappedRoles.size() == 0){
            // No authorization was configured
            return new CreateEntityMetadataCommand(null, entity, version, data).execute();
        }

        List<String> roles = mappedRoles.get(MetadataRoles.INSERT.toString());
        for (String role : roles) {
            if (sc.isUserInRole(role)){
                return new CreateEntityMetadataCommand(null, entity, version, data).execute();
            }
        }
        throw new SecurityException("Unauthorized request for 'PUT /?entity/?version'. One of the following roles is needed: "+roles);
    }

    @PUT
    @Path("/{entity}/schema={version}")
    @Consumes(MediaType.APPLICATION_JSON)
    public String createSchema(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, @PathParam(PARAM_VERSION) String version, String schema) {
        Error.reset();

        final Map<MetadataRoles, List<String>> mappedRoles = metadata.getMappedRoles();
        if(mappedRoles == null || mappedRoles.size() == 0){
            // No authorization was configured
            return new CreateEntitySchemaCommand(null, entity, version, schema).execute();
        }

        List<String> roles = mappedRoles.get(MetadataRoles.INSERT_SCHEMA.toString());
        for (String role : roles) {
            if (sc.isUserInRole(role)){
                return new CreateEntitySchemaCommand(null, entity, version, schema).execute();
            }
        }
        throw new SecurityException("Unauthorized request for 'PUT /?entity/schema=?version'. One of the following roles is needed: "+roles);
    }

    @PUT
    @Path("/{entity}")
    @Consumes(MediaType.APPLICATION_JSON)
    public String updateEntityInfo(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, String info) {
        Error.reset();

        final Map<MetadataRoles, List<String>> mappedRoles = metadata.getMappedRoles();
        if(mappedRoles == null || mappedRoles.size() == 0){
            // No authorization was configured
            return new UpdateEntityInfoCommand(null, entity, info).execute();
        }

        List<String> roles = mappedRoles.get(MetadataRoles.UPDATE_ENTITYINFO.toString());
        for (String role : roles) {
            if (sc.isUserInRole(role)){
                return new UpdateEntityInfoCommand(null, entity, info).execute();
            }
        }
        throw new SecurityException("Unauthorized request for 'PUT /?entity/?version'. One of the following roles is needed: "+roles);
    }

    @PUT
    @Path("/{entity}/{version}/{status}")
    public String updateSchemaStatus(@Context SecurityContext sc,
                                     @PathParam(PARAM_ENTITY) String entity,
                                     @PathParam(PARAM_VERSION) String version,
                                     @PathParam("status") String status,
                                     @QueryParam("comment") String comment) {
        Error.reset();

        final Map<MetadataRoles, List<String>> mappedRoles = metadata.getMappedRoles();
        if(mappedRoles == null || mappedRoles.size() == 0){
            // No authorization was configured
            return new UpdateEntitySchemaStatusCommand(null, entity, version, status, comment).execute();
        }

        List<String> roles = mappedRoles.get(MetadataRoles.UPDATE_ENTITY_SCHEMASTATUS.toString());
        for (String role : roles) {
            if (sc.isUserInRole(role)){
                return new UpdateEntitySchemaStatusCommand(null, entity, version, status, comment).execute();
            }
        }
        throw new SecurityException("Unauthorized request for 'PUT /?entity/?version/?status'. One of the following roles is needed: "+roles);
    }

    @POST
    @Path("/{entity}/{version}/default")
    public String setDefaultVersion(@Context SecurityContext sc,
                                    @PathParam(PARAM_ENTITY) String entity,
                                    @PathParam(PARAM_VERSION) String version) {
        Error.reset();

        final Map<MetadataRoles, List<String>> mappedRoles = metadata.getMappedRoles();
        if(mappedRoles == null || mappedRoles.size() == 0){
            // No authorization was configured
            return new SetDefaultVersionCommand(null, entity, version).execute();
        }

        List<String> roles = mappedRoles.get(MetadataRoles.UPDATE_DEFAULTVERSION.toString());
        for (String role : roles) {
            if (sc.isUserInRole(role)){
                return new SetDefaultVersionCommand(null, entity, version).execute();
            }
        }
        throw new SecurityException("Unauthorized request for 'POST /?entity/?version/default'. One of the following roles is needed: "+roles);
    }

    @DELETE
    @Path("/{entity}")
    public String removeEntity(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity) {
        Error.reset();

        final Map<MetadataRoles, List<String>> mappedRoles = metadata.getMappedRoles();
        if(mappedRoles == null || mappedRoles.size() == 0){
            // No authorization was configured
            return new RemoveEntityCommand(null, entity).execute();
        }

        List<String> roles = mappedRoles.get(MetadataRoles.DELETE_ENTITY.toString());
        for (String role : roles) {
            if (sc.isUserInRole(role)){
                return new RemoveEntityCommand(null, entity).execute();
            }
        }
        throw new SecurityException("Unauthorized request for 'DELETE /?entity'. One of the following roles is needed: "+roles);
    }

    @DELETE
    @Path("/{entity}/default")
    public String clearDefaultVersion(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity) {
        Error.reset();

        final Map<MetadataRoles, List<String>> mappedRoles = metadata.getMappedRoles();
        if(mappedRoles == null || mappedRoles.size() == 0){
            // No authorization was configured
            return new SetDefaultVersionCommand(null, entity, null).execute();
        }

        List<String> roles = mappedRoles.get(MetadataRoles.UPDATE_DEFAULTVERSION.toString());
        for (String role : roles) {
            if (sc.isUserInRole(role)){
                return new SetDefaultVersionCommand(null, entity, null).execute();
            }
        }
        throw new SecurityException("Unauthorized request for 'DELETE /?entity/default'. One of the following roles is needed: "+roles);
    }
}
