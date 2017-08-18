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

import javax.servlet.http.HttpServletRequest;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.lightblue.ClientIdentification;
import com.redhat.lightblue.EntityVersion;
import com.redhat.lightblue.Request;
import com.redhat.lightblue.config.JsonTranslator;
import com.redhat.lightblue.mediator.Mediator;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.rest.crud.RestCrudConstants;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.metrics.RequestMetrics;

/**
 * Note that passing a Mediator in the constructor is optional. If not provided,
 * it is fetched from CrudManager object.
 *
 * @author nmalik
 */
public abstract class AbstractRestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRestCommand.class);

    protected static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.withExactBigDecimals(true);

    private final Mediator mediator;
    private final HttpServletRequest httpServletRequest;

    public AbstractRestCommand(Mediator mediator) {
        this.mediator = mediator;
        this.httpServletRequest = ResteasyProviderFactory.getContextData(HttpServletRequest.class);
    }

    public AbstractRestCommand() {
        this(null);
    }

    /**
     * Returns the mediator. If no mediator is set on the command uses
     * CrudManager#getMediator() method.
     *
     * @return
     * @throws Exception
     */
    protected Mediator getMediator(RequestMetrics metrics) {
        Mediator m = null;
        try {
            if (null != mediator) {
                m = mediator;
            } else {
                m = RestConfiguration.getFactory().getMediator(metrics);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw Error.get(RestCrudConstants.ERR_CANT_GET_MEDIATOR);
        }

        return m;
    }

    protected void validateReq(Request req, String entity, String version) {
        // If entity and/or version is not set in the request, this
        // code below sets it from the uri
        if (req.getEntityVersion() == null) {
            req.setEntityVersion(new EntityVersion());
        }
        if (req.getEntityVersion().getEntity() == null) {
            req.getEntityVersion().setEntity(entity);
        }
        if (req.getEntityVersion().getVersion() == null) {
            req.getEntityVersion().setVersion(version);
        }
        if (!req.getEntityVersion().getEntity().equals(entity)) {
            throw Error.get(RestCrudConstants.ERR_NO_ENTITY_MATCH, entity);
        }
        if (req.getEntityVersion().getVersion() != null && version != null
                && !req.getEntityVersion().getVersion().equals(version)) {
            throw Error.get(RestCrudConstants.ERR_NO_VERSION_MATCH, version);
        }
    }

    protected JsonTranslator getJsonTranslator() {
        JsonTranslator tx = null;
        try {
            tx = RestConfiguration.getFactory().getJsonTranslator();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw Error.get(RestCrudConstants.ERR_CANT_GET_TRANSLATOR, e.getMessage());
        }
        return tx;
    }

    public ClientIdentification getCallerId() {
        return new ClientIdentification() {
            @Override
            public boolean isUserInRole(String role) {
                return httpServletRequest == null ? false : httpServletRequest.isUserInRole(role);
            }
            
            @Override
            public String getPrincipal() {
                return httpServletRequest == null ? null : (httpServletRequest.getUserPrincipal() != null ? httpServletRequest.getUserPrincipal().getName() : null);
            }
        };
    }

    protected void addCallerId(Request req) {
        req.setClientId(getCallerId());
    }

    public abstract CallStatus run();
}
