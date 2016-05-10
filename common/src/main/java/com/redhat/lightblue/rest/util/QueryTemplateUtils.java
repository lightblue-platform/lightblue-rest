/**
 *
 */
package com.redhat.lightblue.rest.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;

/**
 * @author bvulaj
 *
 */
public class QueryTemplateUtils {

    public static final String FIELD_Q_EQ_TMPL = "{\"field\": \"${field}\", \"op\": \"=\",\"rvalue\": \"${value}\"}";
    public static final String FIELD_Q_IN_TMPL = "{\"field\":\"${field}\", \"op\":\"$in\", \"values\":[${value}]}";
    public static final String PROJECTION_TMPL = "{\"field\":\"${field}\",\"include\": ${include}, \"recursive\": ${recursive}}";
    public static final String SORT_TMPL = "{\"${field}\":\"${order}\"}";
    public static final String DEFAULT_PROJECTION_TMPL = "{\"field\":\"*\",\"recursive\":true}";

    public static String buildQueryFieldsTemplate(String q) {
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
                return buildQueryFieldTemplate(queryList.get(0));
            }
        }
        return sq;
    }

    private static String buildQueryFieldTemplate(String q) {
        String sq;
        String template = null;

        String[] split = q.split(":");

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

    public static String buildProjectionsTemplate(String p) {
        String sp = QueryTemplateUtils.DEFAULT_PROJECTION_TMPL;
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
                return buildProjectionTemplate(projectionList.get(0));
            }
        }
        return sp;
    }

    private static String buildProjectionTemplate(String p) {
        String sp;
        String[] split = p.split(":");

        Map<String, String> map = new HashMap<>();
        map.put("field", split[0]);
        map.put("include", split[1].charAt(0) == '1' ? "true" : "false");
        map.put("recursive", split[1].length() < 2 ? "false" : (split[1].charAt(1) == 'r' ? "true" : "false"));

        StrSubstitutor sub = new StrSubstitutor(map);

        sp = sub.replace(PROJECTION_TMPL);
        return sp;
    }

    public static String buildSortsTemplate(String s) {
        String ss = null;
        if (s != null && !"".equals(s.trim())) {
            List<String> sortList = Arrays.asList(s.split(","));
            if (sortList.size() > 1) {
                StringBuilder sbs = new StringBuilder("[");
                Iterator<String> itr = sortList.iterator();
                while (itr.hasNext()) {
                    sbs.append(QueryTemplateUtils.buildSortTemplate(itr.next()));
                    if (itr.hasNext()) {
                        sbs.append(',');
                    }
                }
                sbs.append("]");
                ss = sbs.toString();

            } else {
                return QueryTemplateUtils.buildSortTemplate(sortList.get(0));
            }
        }
        return ss;
    }

    private static String buildSortTemplate(String s) {
        String ss;

        String[] split = s.split(":");

        Map<String, String> map = new HashMap<>();
        map.put("field", split[0]);
        map.put("order", split[1].charAt(0) == 'd' ? "$desc" : "$asc");

        StrSubstitutor sub = new StrSubstitutor(map);

        ss = sub.replace(SORT_TMPL);
        return ss;
    }
}
