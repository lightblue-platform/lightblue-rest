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

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.NullNode;

import com.redhat.lightblue.EntityVersion;
import com.redhat.lightblue.crud.FindRequest;
import com.redhat.lightblue.query.Projection;
import com.redhat.lightblue.query.QueryExpression;
import com.redhat.lightblue.query.FieldProjection;
import com.redhat.lightblue.query.Sort;
import com.redhat.lightblue.query.SortKey;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.rest.crud.cmd.AcquireCommand;
import com.redhat.lightblue.rest.crud.cmd.BulkRequestCommand;
import com.redhat.lightblue.rest.crud.cmd.DeleteCommand;
import com.redhat.lightblue.rest.crud.cmd.ExplainCommand;
import com.redhat.lightblue.rest.crud.cmd.FindCommand;
import com.redhat.lightblue.rest.crud.cmd.GenerateCommand;
import com.redhat.lightblue.rest.crud.cmd.GetLockCountCommand;
import com.redhat.lightblue.rest.crud.cmd.InsertCommand;
import com.redhat.lightblue.rest.crud.cmd.LockPingCommand;
import com.redhat.lightblue.rest.crud.cmd.ReleaseCommand;
import com.redhat.lightblue.rest.crud.cmd.SaveCommand;
import com.redhat.lightblue.rest.crud.cmd.UpdateCommand;
import com.redhat.lightblue.rest.crud.cmd.RunSavedSearchCommand;
import com.redhat.lightblue.util.metrics.DropwizardRequestMetrics;
import com.redhat.lightblue.util.metrics.MetricRegistryFactory;
import com.redhat.lightblue.util.metrics.RequestMetrics;
import com.redhat.lightblue.rest.util.QueryTemplateUtils;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.JsonUtils;
import com.restcompress.provider.LZF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context;
import java.io.IOException;

import static com.redhat.lightblue.rest.crud.cmd.AbstractLockCommand.getLockCommand;

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
        java.security.Security.setProperty("networkaddress.cache.ttl", "30");
    }

    /**
     * Thread-safe, shared static instance for all requests. 
     */
    private static final RequestMetrics metrics =
            new DropwizardRequestMetrics(MetricRegistryFactory.getJmxMetricRegistry());

    @GET
    @LZF
    @Path("/search/{entity}")
    public Response getSearchesForEntity(@PathParam("entity") String entity,
                                         @QueryParam("P") String projection,
                                         @QueryParam("S") String sort) {
        FindRequest freq=new FindRequest();
        freq.setEntityVersion(new EntityVersion(RestConfiguration.getSavedSearchCache().savedSearchEntity,
                                                RestConfiguration.getSavedSearchCache().savedSearchVersion));

        try {
            freq.setProjection(projection==null?FieldProjection.ALL:Projection.fromJson(JsonUtils.json(QueryTemplateUtils.buildProjectionsTemplate(projection))));
            freq.setSort(sort==null?new SortKey(new com.redhat.lightblue.util.Path("name"),false):Sort.fromJson(JsonUtils.json(QueryTemplateUtils.buildSortsTemplate(sort))));
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
        }
        CallStatus st=new FindCommand(freq.getEntityVersion().getEntity(),
                                      freq.getEntityVersion().getVersion(),
                                      freq.toJson().toString(), metrics).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @GET
    @LZF
    @Path("/search/{entity}/{searchName}")
    public Response runSavedSearch(@PathParam("entity") String entity,
                                   @PathParam("searchName") String searchName,
                                   @QueryParam("P") String projection,
                                   @QueryParam("S") String sort,
                                   @QueryParam("from") Integer from,
                                   @QueryParam("to") Integer to,
                                   @QueryParam("maxResults") Integer maxResults,
                                   @Context UriInfo uriInfo) {
        return runSavedSearch(entity,null,searchName,projection,sort,from,to,maxResults,uriInfo);
    }

    @GET
    @LZF
    @Path("/search/{entity}/{version}/{searchName}")
    public Response runSavedSearch(@PathParam("entity") String entity,
                                   @PathParam("version") String version,
                                   @PathParam("searchName") String searchName,
                                   @QueryParam("P") String projection,
                                   @QueryParam("S") String sort,
                                   @QueryParam("from") Integer from,
                                   @QueryParam("to") Integer to,
                                   @QueryParam("maxResults") Integer maxResults,
                                   @Context UriInfo uriInfo) {
        Map<String,List<String>> qmap=uriInfo.getQueryParameters();
        Map<String,String> map=new HashMap<>();
        for(Map.Entry<String,List<String>> entry:qmap.entrySet()) {
            if(entry.getValue().size()!=1)
                return Response.status(Response.Status.BAD_REQUEST).build();
            map.put(entry.getKey(),entry.getValue().get(0));
        }
        return runSavedSearch(entity,version,searchName,projection,sort,from,to,maxResults,map);
    }

    @POST
    @LZF
    @Path("/search/{entity}/{searchName}")
    public Response runSavedSearch(@PathParam("entity") String entity,
                                   @PathParam("searchName") String searchName,
                                   @QueryParam("P") String projection,
                                   @QueryParam("S") String sort,
                                   @QueryParam("from") Integer from,
                                   @QueryParam("to") Integer to,
                                   @QueryParam("maxResults") Integer maxResults,
                                   String body) {
        return runSavedSearch(entity,null,searchName,projection,sort,from,to,maxResults,body);
    }

    @POST
    @LZF
    @Path("/search/{entity}/{version}/{searchName}")
    public Response runSavedSearch(@PathParam("entity") String entity,
                                   @PathParam("version") String version,
                                   @PathParam("searchName") String searchName,
                                   @QueryParam("P") String projection,
                                   @QueryParam("S") String sort,
                                   @QueryParam("from") Integer from,
                                   @QueryParam("to") Integer to,
                                   @QueryParam("maxResults") Integer maxResults,
                                   String body) {
        Map<String,String> map=new HashMap<>();
        try {
            JsonNode node=JsonUtils.json(body);
            if(node instanceof ObjectNode) {
                for(Iterator<Map.Entry<String,JsonNode>> itr=node.fields();itr.hasNext();) {
                    Map.Entry<String,JsonNode> entry=itr.next();
                    map.put(entry.getKey(),entry.getValue() instanceof NullNode?null:entry.getValue().asText());
                }
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        
        return runSavedSearch(entity,version,searchName,projection,sort,from,to,maxResults,map);
    }


    private Response runSavedSearch(String entity,
                                    String version,
                                    String searchName,
                                    String projection,
                                    String sort,
                                    Integer from,
                                    Integer to,
                                    Integer maxResults,
                                    Map<String,String> parameters) {
        Projection p=null;
        Sort s=null;
        Integer f=null;
        Integer t=null;
        try {
            if(projection!=null) {
                String x=QueryTemplateUtils.buildProjectionsTemplate(projection);
                if(x!=null)
                    p=Projection.fromJson(JsonUtils.json(x));
            }
            if(sort!=null) {
                String x=QueryTemplateUtils.buildSortsTemplate(sort);
                if(x!=null)
                    s=Sort.fromJson(JsonUtils.json(x));
            }
            if(from!=null)
                f=from;
            if(to!=null)
                t=to;
            if(maxResults!=null)
                t=new Integer(f==null?0:f+maxResults-1);
        } catch(Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Error.reset();
        CallStatus st=new RunSavedSearchCommand(searchName,entity,version,p,s,f,t,parameters, metrics).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }        
    

    @POST
    @Path("/lock/")
    public Response lock(String request) {
        Error.reset();
        CallStatus st = getLockCommand(request, metrics).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @PUT
    @Path("/lock/{domain}/{callerId}/{resourceId}")
    public Response acquire(@PathParam("domain") String domain,
                            @PathParam("callerId") String callerId,
                            @PathParam("resourceId") String resourceId,
                            @QueryParam("ttl") Long ttl) {
        Error.reset();
        CallStatus st = new AcquireCommand(domain, callerId, resourceId, ttl, metrics).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @DELETE
    @Path("/lock/{domain}/{callerId}/{resourceId}")
    public Response release(@PathParam("domain") String domain,
                            @PathParam("callerId") String callerId,
                            @PathParam("resourceId") String resourceId) {
        Error.reset();
        CallStatus st = new ReleaseCommand(domain, callerId, resourceId, metrics).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @GET
    @Path("/lock/{domain}/{callerId}/{resourceId}")
    public Response getLockCount(@PathParam("domain") String domain,
                                 @PathParam("callerId") String callerId,
                                 @PathParam("resourceId") String resourceId) {
        Error.reset();
        CallStatus st = new GetLockCountCommand(domain, callerId, resourceId, metrics).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @PUT
    @Path("/lock/{domain}/{callerId}/{resourceId}/ping")
    public Response ping(@PathParam("domain") String domain,
                         @PathParam("callerId") String callerId,
                         @PathParam("resourceId") String resourceId) {
        Error.reset();
        CallStatus st = new LockPingCommand(domain, callerId, resourceId, metrics).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    /**
     * @deprecated Deprecated due to inconsistent path. Use
     * {@link #insert(String, String)} instead.
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
     * @deprecated Deprecated due to inconsistent path. Use
     * {@link #insert(String, String, String)} instead.
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
        CallStatus st = new InsertCommand(entity, version, request, metrics).run();
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
        CallStatus st = new SaveCommand(entity, version, request, metrics).run();
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
        CallStatus st = new UpdateCommand(entity, version, request, metrics).run();
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
        CallStatus st = new DeleteCommand(entity, version, req, metrics).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    /**
     * This endpoint streams the results with chunked encoding if
     * stream query parameter is true
     */
    @POST
    @LZF
    @Path("/find/{entity}")
    public Response find(@PathParam(PARAM_ENTITY) String entity,
                         @QueryParam("stream") Boolean stream,
                         String request) {
        return find(entity, null, stream,request);
    }

    /**
     * This endpoint streams the results with chunked encoding if
     * stream query parameter is true
     */
    @POST
    @LZF
    @Path("/find/{entity}/{version}")
    public Response find(@PathParam(PARAM_ENTITY) String entity,
                         @PathParam(PARAM_VERSION) String version,
                         @QueryParam("stream") Boolean stream,
                         String request) {
        Error.reset();
        boolean bstream=stream!=null&&stream;
        FindCommand f=new FindCommand(entity, version, request, bstream, metrics);
        CallStatus st=f.run();
        if(!st.hasErrors()&&bstream) {
            // This is how you stream. You put a response stream into
            // the response, and data is streamed to the client
            return Response.ok().entity(f.getResponseStream()).build();
        } else {
            return Response.status(st.getHttpStatus()).entity(st.toString()).build();
        }
    }

    @POST
    @LZF
    @Path("/explain/{entity}")
    public Response explain(@PathParam(PARAM_ENTITY) String entity,
                            String request) {
        return explain(entity, null, request);
    }

    @POST
    @LZF
    @Path("/explain/{entity}/{version}")
    public Response explain(@PathParam(PARAM_ENTITY) String entity,
                            @PathParam(PARAM_VERSION) String version,
                            String request) {
        Error.reset();
        CallStatus st = new ExplainCommand(entity, version, request, metrics).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @POST
    @LZF
    @Path("/bulk")
    public Response bulk(String request) {
        Error.reset();
        CallStatus st = new BulkRequestCommand(request, metrics).run();
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
     * @return A lightblue response, with "processed" containing an array of
     * generated values.
     */
    @GET
    @LZF
    @Path("/generate/{entity}/{version}/{path}")
    public Response generate(@PathParam("entity") String entity,
                             @PathParam("version") String version,
                             @PathParam("path") String path,
                             @QueryParam("n") Integer n) {
        CallStatus st = new GenerateCommand(entity, version, path, n == null ? 1 : n, metrics).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @GET
    @LZF
    @Path("/generate/{entity}/{path}")
    public Response generate(@PathParam("entity") String entity,
                             @PathParam("path") String path,
                             @QueryParam("n") Integer n) {
        return generate(entity, null, path, n);
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
                               @QueryParam("to") Long to,
                               @QueryParam("maxResults") Long maxResults) throws IOException {
        return simpleFind(entity, null, q, p, s, from, to, maxResults);
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
                               @QueryParam("to") Long to,
                               @QueryParam("maxResults") Long maxResults) throws IOException {
        Error.reset();
        String request=buildSimpleRequest(entity,version,q,p,s,from,to,maxResults).toString();
        CallStatus st = new FindCommand(null, entity, version, request, metrics).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @GET
    @LZF
    @Path("/explain/{entity}")
    //?Q&P&S&from&to
    public Response simpleExplain(@PathParam(PARAM_ENTITY) String entity,
                                  @QueryParam("Q") String q,
                                  @QueryParam("P") String p,
                                  @QueryParam("S") String s,
                                  @QueryParam("from") Long from,
                                  @QueryParam("to") Long to,
                                  @QueryParam("maxResults") Long maxResults) throws IOException {
        return simpleExplain(entity, null, q, p, s, from, to,maxResults);
    }
    
    @GET
    @LZF
    @Path("/explain/{entity}/{version}")
    //?Q&P&S&from&to
    public Response simpleExplain(@PathParam(PARAM_ENTITY) String entity,
                                  @PathParam(PARAM_VERSION) String version,
                                  @QueryParam("Q") String q,
                                  @QueryParam("P") String p,
                                  @QueryParam("S") String s,
                                  @QueryParam("from") Long from,
                                  @QueryParam("to") Long to,
                                  @QueryParam("maxResults") Long maxResults) throws IOException {
        Error.reset();
        String request=buildSimpleRequest(entity,version,q,p,s,from,to,maxResults).toString();
        CallStatus st = new ExplainCommand(null, entity, version, request, metrics).run();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    private FindRequest buildSimpleRequest(String entity,String version, String q,String p, String s, Long from, Long to,Long maxResults)
        throws IOException {            
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
        if(to!=null) {
            findRequest.setTo(to);
        } else if(maxResults!=null&&maxResults>0) {
            findRequest.setTo((from == null ? 0 : from) + maxResults - 1);
        }
        return findRequest;
    }
}
