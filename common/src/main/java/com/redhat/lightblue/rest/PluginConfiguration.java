package com.redhat.lightblue.rest;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.redhat.lightblue.util.JsonInitializable;

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
public class PluginConfiguration implements JsonInitializable {

    public final static String PROTOCOL_FILE = "file";

    private final Set<URL> externalPaths = new HashSet<>();

    public Set<URL> getPluginUrls() {
        return Collections.unmodifiableSet(externalPaths);
    }

    public PluginConfiguration() {}

    public PluginConfiguration(JsonNode node) throws IOException {
        initializeFromJson(node);
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
                String urlPath = child.textValue();
                try {
                    URL url = new URL(urlPath);
                    switch (url.getProtocol().toLowerCase()) {
                        case PROTOCOL_FILE:
                            collectJarPaths(new File(url.getPath()), externalPaths, true);
                            break;
                        default:
                            externalPaths.add(url);
                    }
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("Not a valid url: " + urlPath, e);
                }
            }
        } else {
            throw new IllegalArgumentException("node must be instanceof ArrayNode: " + node.toString());
        }
    }

    private static void collectJarPaths(File file, Set<URL> paths, final boolean recursiveDirSearch) throws MalformedURLException {
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    if (name.endsWith(".jar")
                            || (recursiveDirSearch && dir.isDirectory())) {
                        return true;
                    }
                    return false;
                }

            });
            if (files != null) {
                for (File f : files) {
                    collectJarPaths(f, paths, recursiveDirSearch);
                }
            }
        } else {
            paths.add(new URL(PROTOCOL_FILE, null, file.getAbsolutePath()));
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("PluginConfiguration [externalPaths=[");
        sb.append(StringUtils.join(externalPaths, ", "));
        sb.append("]]");
        return sb.toString();
    }

}
