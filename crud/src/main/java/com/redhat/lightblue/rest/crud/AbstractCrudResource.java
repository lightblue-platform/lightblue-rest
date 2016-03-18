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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.lightblue.EntityVersion;
import com.redhat.lightblue.crud.FindRequest;
import com.redhat.lightblue.query.Projection;
import com.redhat.lightblue.query.QueryExpression;
import com.redhat.lightblue.query.Sort;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.crud.hystrix.AcquireCommand;
import com.redhat.lightblue.rest.crud.hystrix.BulkRequestCommand;
import com.redhat.lightblue.rest.crud.hystrix.DeleteCommand;
import com.redhat.lightblue.rest.crud.hystrix.FindCommand;
import com.redhat.lightblue.rest.crud.hystrix.GetLockCountCommand;
import com.redhat.lightblue.rest.crud.hystrix.InsertCommand;
import com.redhat.lightblue.rest.crud.hystrix.LockPingCommand;
import com.redhat.lightblue.rest.crud.hystrix.ReleaseCommand;
import com.redhat.lightblue.rest.crud.hystrix.SaveCommand;
import com.redhat.lightblue.rest.crud.hystrix.UpdateCommand;
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

    private static final String FIELD_Q_EQ_TMPL = "{\"field\": \"${field}\", \"op\": \"=\",\"rvalue\": \"${value}\"}";
    private static final String FIELD_Q_IN_TMPL = "{\"field\":\"${field}\", \"op\":\"$in\", \"values\":[${value}]}";
    private static final String PROJECTION_TMPL = "{\"field\":\"${field}\",\"include\": ${include}, \"recursive\": ${recursive}}";
    private static final String SORT_TMPL = "{\"${field}\":\"${order}\"}";
    private static final String DEFAULT_PROJECTION_TMPL = "{\"field\":\"*\",\"recursive\":true}";

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
        CallStatus st=new AcquireCommand(null,domain,callerId,resourceId,ttl).execute();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @DELETE
    @Path("/lock/{domain}/{callerId}/{resourceId}")
    public Response release(@PathParam("domain") String domain,
                            @PathParam("callerId") String callerId,
                            @PathParam("resourceId") String resourceId) {
        Error.reset();
        CallStatus st=new ReleaseCommand(null,domain,callerId,resourceId).execute();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @GET
    @Path("/lock/{domain}/{callerId}/{resourceId}")
    public Response getLockCount(@PathParam("domain") String domain,
                                 @PathParam("callerId") String callerId,
                                 @PathParam("resourceId") String resourceId) {
        Error.reset();
        CallStatus st=new GetLockCountCommand(null,domain,callerId,resourceId).execute();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @PUT
    @Path("/lock/{domain}/{callerId}/{resourceId}/ping")
    public Response ping(@PathParam("domain") String domain,
                         @PathParam("callerId") String callerId,
                         @PathParam("resourceId") String resourceId) {
        Error.reset();
        CallStatus st=new LockPingCommand(null,domain,callerId,resourceId).execute();
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
        CallStatus st=new InsertCommand(null, entity, version, request).execute();
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
        CallStatus st=new SaveCommand(null, entity, version, request).execute();
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
        CallStatus st=new UpdateCommand(null, entity, version, request).execute();
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
        CallStatus st=new DeleteCommand(null, entity, version, req).execute();
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
        CallStatus st=new FindCommand(null, entity, version, request).execute();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    @POST
    @LZF
    @Path("/bulk")
    public Response bulk(String request) {
        Error.reset();
        CallStatus st=new BulkRequestCommand(null,request).execute();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
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
        String sq = null;
        if (q != null && !"".equals(q.trim())) {
            List<String> queryList = Arrays.asList(q.split(";"));
            if (queryList.size() > 1) {
                StringBuilder sbq = new StringBuilder("{ \"$and\" : [");
                Iterator<String> itr = queryList.iterator();
                while (itr.hasNext()) {
                    sbq.append(buildQueryFieldTemplate(itr.next()));
                    if (itr.hasNext()) {
                        sbq.append(',');
                    }
                }
                sbq.append("]}");
                sq = sbq.toString();

            } else {
                sq = buildQueryFieldTemplate(queryList.get(0));
            }
        }
        LOGGER.debug("query: {} -> {}", q, sq);

        String sp = DEFAULT_PROJECTION_TMPL;
        if (p != null && !"".equals(p.trim())) {
            List<String> projectionList = Arrays.asList(p.split(","));
            if (projectionList.size() > 1) {
                StringBuilder sbp = new StringBuilder("[");
                Iterator<String> itr = projectionList.iterator();
                while (itr.hasNext()) {
                    sbp.append(buildProjectionTemplate(itr.next()));
                    if (itr.hasNext()) {
                        sbp.append(',');
                    }
                }
                sbp.append("]");
                sp = sbp.toString();

            } else {
                sp = buildProjectionTemplate(projectionList.get(0));
            }
        }
        LOGGER.debug("projection: {} -> {}", p, sp);

        String ss = null;
        if (s != null && !"".equals(s.trim())) {
            List<String> sortList = Arrays.asList(s.split(","));
            if (sortList.size() > 1) {
                StringBuilder sbs = new StringBuilder("[");
                Iterator<String> itr = sortList.iterator();
                while (itr.hasNext()) {
                    sbs.append(buildSortTemplate(itr.next()));
                    if (itr.hasNext()) {
                        sbs.append(',');
                    }
                }
                sbs.append("]");
                ss = sbs.toString();

            } else {
                ss = buildSortTemplate(sortList.get(0));
            }
        }
        LOGGER.debug("sort:{} -> {}", s, ss);

        FindRequest findRequest = new FindRequest();
        findRequest.setEntityVersion(new EntityVersion(entity, version));
        findRequest.setQuery(sq == null ? null : QueryExpression.fromJson(JsonUtils.json(sq)));
        findRequest.setProjection(sp == null ? null : Projection.fromJson(JsonUtils.json(sp)));
        findRequest.setSort(ss == null ? null : Sort.fromJson(JsonUtils.json(ss)));
        findRequest.setFrom(from);
        findRequest.setTo(to);
        String request = findRequest.toString();

        CallStatus st=new FindCommand(null, entity, version, request).execute();
        return Response.status(st.getHttpStatus()).entity(st.toString()).build();
    }

    private String buildQueryFieldTemplate(String s1) {
        String sq;
        String template = null;

        String[] split = s1.split(":");

        Map<String, String> map = new HashMap<>();
        map.put("field", split[0]);
        String value = null;

        String[] comma = split[1].split(",");
        if (comma.length > 1) {
            template = FIELD_Q_IN_TMPL;
            value = "\"" + StringUtils.join(comma, "\",\"") + "\"";
        } else {
            template = FIELD_Q_EQ_TMPL;
            value = split[1];
        }
        map.put("value", value);

        StrSubstitutor sub = new StrSubstitutor(map);

        sq = sub.replace(template);
        return sq;
    }

    private String buildProjectionTemplate(String s1) {
        String sp;
        String[] split = s1.split(":");

        Map<String, String> map = new HashMap<>();
        map.put("field", split[0]);
        map.put("include", split[1].charAt(0) == '1' ? "true" : "false");
        map.put("recursive", split[1].length() < 2 ? "false" : (split[1].charAt(1) == 'r' ? "true" : "false"));

        StrSubstitutor sub = new StrSubstitutor(map);

        sp = sub.replace(PROJECTION_TMPL);
        return sp;
    }

    private String buildSortTemplate(String s1) {
        String ss;

        String[] split = s1.split(":");

        Map<String, String> map = new HashMap<>();
        map.put("field", split[0]);
        map.put("order", split[1].charAt(0) == 'd' ? "$desc" : "$asc");

        StrSubstitutor sub = new StrSubstitutor(map);

        ss = sub.replace(SORT_TMPL);
        return ss;
    }
}
