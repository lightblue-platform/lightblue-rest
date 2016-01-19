package com.redhat.lightblue.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.redhat.lightblue.util.JsonInitializable;
import com.redhat.lightblue.util.JsonUtils;

/**
 * <p>Parses a configuration file containing paths to external jar files to be loaded into memory
 * at runtime.</p>
 *
 * <p>
 * Expected format is:
 * [
 *    "path/to/file.jar",
 *    "path/to/directory/"
 * ]
 * </p>
 *
 * @author dcrissman
 */
public class ExternalResourceConfiguration implements JsonInitializable {

    private final Set<String> externalPaths = new HashSet<>();

    public Set<String> getExternalPaths() {
        return Collections.unmodifiableSet(externalPaths);
    }

    public ExternalResourceConfiguration(String resourceName) throws IOException {
        this(Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName));
    }

    public ExternalResourceConfiguration(InputStream in) throws IOException {
        if (in == null) {
            //There are no external resources, this is ok.
            return;
        }

        initializeFromJson(JsonUtils.json(in));
    }

    @Override
    public void initializeFromJson(JsonNode node) {
        if (node instanceof NullNode) {
            //There are no external resources, this is ok.
            return;
        }

        // Node must be an array node
        if (node instanceof ArrayNode) {
            for (JsonNode child : node) {
                externalPaths.add(child.textValue());
            }
        } else {
            throw new IllegalArgumentException("node must be instanceof ArrayNode: " + node.toString());
        }
    }

}
