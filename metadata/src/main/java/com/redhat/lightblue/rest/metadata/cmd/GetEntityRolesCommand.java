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
package com.redhat.lightblue.rest.metadata.cmd;

import com.redhat.lightblue.Response;
import com.redhat.lightblue.metadata.Metadata;
import com.redhat.lightblue.rest.metadata.RestMetadataConstants;
import com.redhat.lightblue.util.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author nmalik
 */
public class GetEntityRolesCommand extends AbstractRestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetEntityRolesCommand.class);

    private final String entity;
    private final String version;

    public GetEntityRolesCommand(String entity, String version) {
        this(null, entity, version);
    }

    public GetEntityRolesCommand(Metadata metadata, String entity, String version) {
        super(metadata);
        this.entity = entity;
        this.version = version;
    }

    @Override
    public String run() {
        LOGGER.debug("run: entity={}, version={}", entity, version);
        Error.reset();
        Error.push(getClass().getSimpleName());
        if (entity != null) {
            Error.push(entity);
        }
        if (version != null) {
            Error.push(version);
        }
        try {
            Response r = getMetadata().getAccess(entity, version);
            return r.toJson().toString();
        } catch (Error e) {
            return e.toString();
        } catch (Exception e) {
            LOGGER.error("Failure: {}", e);
            return Error.get(RestMetadataConstants.ERR_REST_ERROR, e.toString()).toString();
        }
    }
}
