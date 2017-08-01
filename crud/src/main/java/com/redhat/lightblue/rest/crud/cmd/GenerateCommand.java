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

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.lightblue.OperationStatus;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.crud.CrudConstants;
import com.redhat.lightblue.crud.Factory;
import com.redhat.lightblue.crud.valuegenerators.GeneratedFields;
import com.redhat.lightblue.mediator.DefaultMetadataResolver;
import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.FieldTreeNode;
import com.redhat.lightblue.metadata.Metadata;
import com.redhat.lightblue.metadata.SimpleField;
import com.redhat.lightblue.query.FieldProjection;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.rest.crud.RestCrudConstants;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.Path;

public class GenerateCommand extends AbstractRestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateCommand.class);

    private final String entity;
    private final String version;
    private final String field;
    private final int n;
    
    public GenerateCommand(String entity, String version, String field, int n) {
        super(null);
        this.entity = entity;
        this.version = version;
        this.field = field;
        this.n = n <= 0 ? 1 : n;
        this.metricNamespace=getMetricsNamespace("generate", entity, version);
        initializeMetrics(metricNamespace);
    }

    @Override
    public CallStatus run() {
        activeRequests.inc();
        final Timer.Context timer = requestTimer.time();
        LOGGER.debug("run: entity={}, version={}", entity, version);
        Error.reset();
        Error.push("rest");
        Error.push(getClass().getSimpleName());
        Error.push(entity);
        try {
            LightblueFactory lbf = RestConfiguration.getFactory();
            Factory factory = lbf.getFactory();
            Metadata md = lbf.getMetadata();
            DefaultMetadataResolver mdResolver = new DefaultMetadataResolver(md);
            Path fieldPath = new Path(field);
            mdResolver.initialize(entity, version, null, new FieldProjection(fieldPath, true, false));
            HttpServletRequest sreq = ResteasyProviderFactory.getContextData(HttpServletRequest.class);
            Set<String> userRoles = new HashSet<>();
            if (sreq != null) {
                for (String role : mdResolver.getMetadataRoles()) {
                    if (sreq.isUserInRole(role)) {
                        userRoles.add(role);
                    }
                }
            }
            LOGGER.debug("user roles:{}", userRoles);
            EntityMetadata emd = mdResolver.getCompositeMetadata();
            if (emd.getAccess().getInsert().hasAccess(userRoles)
                    || emd.getAccess().getUpdate().hasAccess(userRoles)) {
                LOGGER.debug("User has access, looking up {}", fieldPath);
                FieldTreeNode ftn = emd.resolve(fieldPath);
                LOGGER.debug("Field={}", ftn);
                SimpleField fld = (SimpleField) ftn;
                LOGGER.debug("Generator:{}", fld.getValueGenerator());
                if (fld.getValueGenerator() != null) {
                    LOGGER.debug("{} has generator: {}", fieldPath, fld.getValueGenerator().getValueGeneratorType());
                    ArrayNode arr = JsonNodeFactory.instance.arrayNode();
                    for (int i = 0; i < n; i++) {
                        JsonNode node = GeneratedFields.generate(factory, fld, emd);
                        arr.add(node);
                    }
                    com.redhat.lightblue.Response r = new com.redhat.lightblue.Response();
                    r.setStatus(OperationStatus.COMPLETE);
                    r.setEntityData(arr);
                    return new CallStatus(r);
                }
            } else {
                throw Error.get(CrudConstants.ERR_NO_ACCESS, "generate " + mdResolver.getTopLevelEntityName());
            }
        } catch (Error e) {
            metricsRegistry.meter(getErrorNamespace(metricNamespace, e)).mark();
            return new CallStatus(e);
        } catch (Exception ex) {
            metricsRegistry.meter(getErrorNamespace(metricNamespace, ex)).mark();
            return new CallStatus(Error.get(RestCrudConstants.ERR_REST_GENERATE, ex.toString()));
        } finally {
            timer.stop();
            activeRequests.dec();
        }
        com.redhat.lightblue.Response r = new com.redhat.lightblue.Response();
        r.setStatus(OperationStatus.COMPLETE);
        return new CallStatus(r);
    }
}
