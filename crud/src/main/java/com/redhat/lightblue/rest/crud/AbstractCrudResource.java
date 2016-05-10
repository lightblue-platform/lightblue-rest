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
package com.redhat.lightblue.rest.crud;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.lightblue.EntityVersion;
import com.redhat.lightblue.crud.FindRequest;
import com.redhat.lightblue.query.Projection;
import com.redhat.lightblue.query.QueryExpression;
import com.redhat.lightblue.query.Sort;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.crud.cmd.AcquireCommand;
import com.redhat.lightblue.rest.crud.cmd.BulkRequestCommand;
import com.redhat.lightblue.rest.crud.cmd.DeleteCommand;
import com.redhat.lightblue.rest.crud.cmd.FindCommand;
import com.redhat.lightblue.rest.crud.cmd.GenerateCommand;
import com.redhat.lightblue.rest.crud.cmd.GetLockCountCommand;
import com.redhat.lightblue.rest.crud.cmd.InsertCommand;
import com.redhat.lightblue.rest.crud.cmd.LockPingCommand;
import com.redhat.lightblue.rest.crud.cmd.ReleaseCommand;
import com.redhat.lightblue.rest.crud.cmd.SaveCommand;
import com.redhat.lightblue.rest.crud.cmd.UpdateCommand;
import com.redhat.lightblue.rest.util.QueryTemplateUtils;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.JsonUtils;
import com.restcompress.provider.LZF;

/**
 *
 * @author nmalik
 * @author bserdar
 */
//metadata/ prefix is the application context
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public abstract class AbstractCrudResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCrudResource.class);

    private static final String PARAM_ENTITY = "entity";
    private static final String PARAM_VERSION = "version";

    static {
        // by default JVM caches DNS forever.  hard code an override to refresh DNS cache every 30 seconds
        java.security.Security.setProperty("networkaddress.cache.ttl" , "30");
    }

    @PUT
    @Path("/lock/{domain}/{callerId}/{resourceId}")
    public Response acquire(@PathParam("domain") String domain,
                            @PathParam("callerId") String callerId,
                            @PathParam("resourceId") String resourceId,
                            @QueryParam("ttl") Long ttl) {
        Error.reset();
        CallStatus st=new AcquireCommand(domain,callerId,resourceId,ttl).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @DELETE
    @Path("/lock/{domain}/{callerId}/{resourceId}")
    public Response release(@PathParam("domain") String domain,
                            @PathParam("callerId") String callerId,
                            @PathParam("resourceId") String resourceId) {
        Error.reset();
        CallStatus st=new ReleaseCommand(domain,callerId,resourceId).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @GET
    @Path("/lock/{domain}/{callerId}/{resourceId}")
    public Response getLockCount(@PathParam("domain") String domain,
                                 @PathParam("callerId") String callerId,
                                 @PathParam("resourceId") String resourceId) {
        Error.reset();
        CallStatus st=new GetLockCountCommand(domain,callerId,resourceId).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @PUT
    @Path("/lock/{domain}/{callerId}/{resourceId}/ping")
    public Response ping(@PathParam("domain") String domain,
                         @PathParam("callerId") String callerId,
                         @PathParam("resourceId") String resourceId) {
        Error.reset();
        CallStatus st=new LockPingCommand(domain,callerId,resourceId).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }


    /**
     * @deprecated Deprecated due to inconsistent path. Use {@link #insert(String, String)} instead.
     */
    @PUT
    @Path("/{entity}")
    @LZF
    @Deprecated
    public Response insertAlt(@PathParam(PARAM_ENTITY) String entity,
                            String request) {
        return insert(entity, null, request);
    }

    /**
     * @deprecated Deprecated due to inconsistent path. Use {@link #insert(String, String, String)} instead.
     */
    @PUT
    @Path("/{entity}/{version}")
    @LZF
    @Deprecated
    public Response insertAlt(@PathParam(PARAM_ENTITY) String entity,
                              @PathParam(PARAM_VERSION) String version,
                              String request) {
        return insert(entity, version, request);
    }

    @PUT
    @LZF
    @Path("/insert/{entity}")
    public Response insert(@PathParam(PARAM_ENTITY) String entity,
                           String request) {
        return insert(entity, null, request);
    }

    @PUT
    @LZF
    @Path("/insert/{entity}/{version}")
    public Response insert(@PathParam(PARAM_ENTITY) String entity,
                           @PathParam(PARAM_VERSION) String version,
                           String request) {
        Error.reset();
        CallStatus st=new InsertCommand(entity, version, request).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @POST
    @LZF
    @Path("/save/{entity}")
    public Response save(@PathParam(PARAM_ENTITY) String entity,
                       String request) {
        return save(entity, null, request);
    }

    @POST
    @LZF
    @Path("/save/{entity}/{version}")
    public Response save(@PathParam(PARAM_ENTITY) String entity,
                         @PathParam(PARAM_VERSION) String version,
                         String request) {
        Error.reset();
        CallStatus st=new SaveCommand(entity, version, request).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @POST
    @LZF
    @Path("/update/{entity}")
    public Response update(@PathParam(PARAM_ENTITY) String entity,
                           String request) {
        return update(entity, null, request);
    }

    @POST
    @LZF
    @Path("/update/{entity}/{version}")
    public Response update(@PathParam(PARAM_ENTITY) String entity,
                           @PathParam(PARAM_VERSION) String version,
                           String request) {
        Error.reset();
        CallStatus st=new UpdateCommand(entity, version, request).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @POST
    @LZF
    @Path("/delete/{entity}")
    public Response delete(@PathParam(PARAM_ENTITY) String entity,
                           String request) {
        return delete(entity, null, request);
    }

    @POST
    @LZF
    @Path("/delete/{entity}/{version}")
    public Response delete(@PathParam(PARAM_ENTITY) String entity,
                           @PathParam(PARAM_VERSION) String version,
                           String req) {
        Error.reset();
        CallStatus st=new DeleteCommand(entity, version, req).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @POST
    @LZF
    @Path("/find/{entity}")
    public Response find(@PathParam(PARAM_ENTITY) String entity,
                         String request) {
        return find(entity, null, request);
    }

    @POST
    @LZF
    @Path("/find/{entity}/{version}")
    public Response find(@PathParam(PARAM_ENTITY) String entity,
                         @PathParam(PARAM_VERSION) String version,
                         String request) {
        Error.reset();
        CallStatus st=new FindCommand(entity, version, request).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @POST
    @LZF
    @Path("/bulk")
    public Response bulk(String request) {
        Error.reset();
        CallStatus st=new BulkRequestCommand(request).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    /**
     * GET /generate/<entity>/<version>/<path>?n=<n>
     *
     * @param entity name of the entity
     * @param version Entity version
     * @param path Path of the field in the entity containing the generator
     * @param n Number of values to be generated
     *
     * @return A lightblue response, with "processed" containing an array of generated values.
     */
    @GET
    @LZF
    @Path("/generate/{entity}/{version}/{path}")
    public Response generate(@PathParam("entity") String entity,
                             @PathParam("version") String version,
                             @PathParam("path") String path,
                             @QueryParam("n") Integer n) {
        CallStatus st=new GenerateCommand(entity,version,path,n==null?1:n).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @GET
    @LZF
    @Path("/generate/{entity}/{path}")
    public Response generate(@PathParam("entity") String entity,
                             @PathParam("path") String path,
                             @QueryParam("n") Integer n) {
        return generate(entity,null,path,n);
    }

    @GET
    @LZF
    @Path("/find/{entity}")
    //?Q&P&S&from&to
    public Response simpleFind(@PathParam(PARAM_ENTITY) String entity,
                               @QueryParam("Q") String q,
                               @QueryParam("P") String p,
                               @QueryParam("S") String s,
                               @QueryParam("from") Long from,
                               @QueryParam("to") Long to) throws IOException {
        return simpleFind(entity, null, q, p, s, from, to);
    }

    @GET
    @LZF
    @Path("/find/{entity}/{version}")
    //?Q&P&S&from&to
    public Response simpleFind(@PathParam(PARAM_ENTITY) String entity,
                               @PathParam(PARAM_VERSION) String version,
                               @QueryParam("Q") String q,
                               @QueryParam("P") String p,
                               @QueryParam("S") String s,
                               @QueryParam("from") Long from,
                               @QueryParam("to") Long to) throws IOException {
        Error.reset();
        // spec -> https://github.com/lightblue-platform/lightblue/wiki/Rest-Spec-Data#get-simple-find
        String sq = QueryTemplateUtils.buildQueryFieldsTemplate(q);
        LOGGER.debug("query: {} -> {}", q, sq);

        String sp = QueryTemplateUtils.buildProjectionsTemplate(p);
        LOGGER.debug("projection: {} -> {}", p, sp);

        String ss = QueryTemplateUtils.buildSortsTemplate(s);
        LOGGER.debug("sort:{} -> {}", s, ss);

        FindRequest findRequest = new FindRequest();
        findRequest.setEntityVersion(new EntityVersion(entity, version));
        findRequest.setQuery(sq == null ? null : QueryExpression.fromJson(JsonUtils.json(sq)));
        findRequest.setProjection(sp == null ? null : Projection.fromJson(JsonUtils.json(sp)));
        findRequest.setSort(ss == null ? null : Sort.fromJson(JsonUtils.json(ss)));
        findRequest.setFrom(from);
        findRequest.setTo(to);
        String request = findRequest.toString();

        CallStatus st=new FindCommand(null, entity, version, request).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }
}
