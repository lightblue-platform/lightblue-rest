package com.redhat.lightblue.rest.crud;

import com.redhat.lightblue.EntityVersion;
import com.redhat.lightblue.crud.FindRequest;
import com.redhat.lightblue.query.Projection;
import com.redhat.lightblue.query.QueryExpression;
import com.redhat.lightblue.query.Sort;
import com.redhat.lightblue.rest.util.QueryTemplateUtils;
import com.redhat.lightblue.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LightblueRequestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(LightblueRequestUtils.class);

    public static FindRequest buildSimpleRequest(String entity, String version, String q, String p, String s, Long from, Long to, Long maxResults)
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
