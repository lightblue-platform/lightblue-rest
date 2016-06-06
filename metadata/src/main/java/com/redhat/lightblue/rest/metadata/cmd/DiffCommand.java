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

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.Metadata;
import com.redhat.lightblue.metadata.parser.JSONMetadataParser;
import com.redhat.lightblue.rest.metadata.RestMetadataConstants;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.JsonCompare;
import com.redhat.lightblue.util.DocComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiffCommand extends AbstractRestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiffCommand.class);
    private final String entity;
    private final String version1;
    private final String version2;

    public DiffCommand(String entity, String version1,String version2) {
        this(null, entity, version1, version2);
    }

    public DiffCommand(Metadata metadata, String entity, String version1,String version2) {
        super(metadata);
        this.entity = entity;
        this.version1 = version1;
        this.version2 = version2;
    }

    @Override
    public String run() {
        LOGGER.debug("run: entity={}, version1={}, version2={}", entity, version1, version2);
        Error.reset();
        Error.push(getClass().getSimpleName());
        try {
            EntityMetadata md1 = getMetadata().getEntityMetadata(entity, version1);
            if (md1 != null) {
                EntityMetadata md2 = getMetadata().getEntityMetadata(entity, version2);
                if(md2!=null) {
                    JSONMetadataParser parser = getJSONParser();
                    JsonNode jmd1=parser.convert(md1);
                    JsonNode jmd2=parser.convert(md2);
                    List<DocComparator.Delta<JsonNode>> l=new JsonCompare().compareNodes(jmd1,jmd2).getDelta();
                    ArrayNode ret=JsonNodeFactory.instance.arrayNode();
                    for(DocComparator.Delta<JsonNode> delta:l) {
                        if(delta instanceof DocComparator.Addition) {
                            ObjectNode node=JsonNodeFactory.instance.objectNode();
                            node.set("+"+delta.getField2(),((DocComparator.Addition<JsonNode>)delta).getAddedNode());
                            ret.add(node);
                        } else if(delta instanceof DocComparator.Removal) {
                            ObjectNode node=JsonNodeFactory.instance.objectNode();
                            node.set("-"+delta.getField1(),((DocComparator.Removal<JsonNode>)delta).getRemovedNode());
                            ret.add(node);
                        } else if(delta instanceof DocComparator.Modification) {
                            ObjectNode node=JsonNodeFactory.instance.objectNode();
                            node.set("-"+delta.getField1(),((DocComparator.Modification<JsonNode>)delta).getUnmodifiedNode());
                            node.set("+"+delta.getField2(),((DocComparator.Modification<JsonNode>)delta).getModifiedNode());
                            ret.add(node);
                        }
                    }
                    return ret.toString();
                } else {
                    throw Error.get(RestMetadataConstants.ERR_NO_ENTITY_VERSION, entity + ":" + version2);
                }
            } else {
                throw Error.get(RestMetadataConstants.ERR_NO_ENTITY_VERSION, entity + ":" + version1);
            }
        } catch (Error e) {
            return e.toString();
        } catch (Exception e) {
            LOGGER.error("Failure: {}", e);
            return Error.get(RestMetadataConstants.ERR_REST_ERROR, e.toString()).toString();
        }
    }
}
