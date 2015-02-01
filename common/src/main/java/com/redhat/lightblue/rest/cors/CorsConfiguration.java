package com.redhat.lightblue.rest.cors;

import static java.util.Collections.unmodifiableList;

import com.redhat.lightblue.util.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Contains configuration for handling Cross Origin Resource Requests within a Lightblue service.
 *
 * <p>This class is fully immutable. To ensure immutability of collections exposed via getters, a
 * builder class is provided to create new configurations.
 *
 * @see com.redhat.lightblue.rest.cors.CorsConfiguration.Builder
 */
public final class CorsConfiguration implements Serializable{
    private static final long serialVersionUID = 1L;

    private final List<String> urlPatterns;
    private final List<String> allowedOrigins;
    private final List<String> allowedMethods;
    private final List<String> allowedHeaders;
    private final List<String> exposedHeaders;
    private final int preflightMaxAge;
    private final boolean supportCredentials;
    private final boolean enableLogging;
    private final boolean decorateRequests;

    /**
     * Chain setters together to configure a {@link com.redhat.lightblue.rest.cors.CorsConfiguration}
     * object.
     *
     * <p>To read values from a JSON configuration file, use {@link #fromJson(java.nio.file.Path)}.
     */
    public static class Builder {
        // Sensible defaults.
        private List<String> urlPatterns = unmodifiableList(Arrays.asList("/*"));;
        private List<String> allowedOrigins = unmodifiableList(Arrays.asList("*"));;
        private List<String> allowedMethods = unmodifiableList(Arrays.asList("GET", "PUT", "POST",
                "HEAD", "OPTIONS"));
        private List<String> allowedHeaders = unmodifiableList(Arrays.asList("Origin", "Accept",
                "X-Requested-With", "Content-Type", "Access-Control-Request-Method",
                "Access-Control-Request-Headers"));
        private List<String> exposedHeaders = Collections.emptyList();
        private int preflightMaxAge = 1800;
        private boolean supportCredentials = true;
        private boolean enableLogging = false;
        private boolean decorateRequests = true;

        /**
         * Sets the url patterns to use with a copy of the contents from the provided array,
         * preventing later modifications to the array from affecting the
         * {@link com.redhat.lightblue.rest.cors.CorsConfiguration}.
         */
        public Builder urlPatterns(String... patterns) {
            Objects.requireNonNull(patterns, "patterns");

            String[] copy = new String[patterns.length];
            System.arraycopy(patterns, 0, copy, 0, patterns.length);

            this.urlPatterns = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        /**
         * Sets the url patterns to use with a copy of the contents from the provided list,
         * preventing later modifications to the list from affecting the
         * {@link com.redhat.lightblue.rest.cors.CorsConfiguration}.
         */
        public Builder urlPatterns(List<String> patterns) {
            Objects.requireNonNull(patterns, "patterns");

            String[] copy = patterns.toArray(new String[patterns.size()]);

            this.urlPatterns = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        public Builder allowedOrigins(String... origins) {
            Objects.requireNonNull(origins, "origins");

            String[] copy = new String[origins.length];
            System.arraycopy(origins, 0, copy, 0, origins.length);

            this.allowedOrigins = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        public Builder allowedOrigins(List<String> origins) {
            Objects.requireNonNull(origins, "origins");

            String[] copy = origins.toArray(new String[origins.size()]);

            this.allowedOrigins = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        public Builder allowedHeaders(String... headers) {
            Objects.requireNonNull(headers, "headers");

            String[] copy = new String[headers.length];
            System.arraycopy(headers, 0, copy, 0, headers.length);

            this.allowedHeaders = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        public Builder allowedHeaders(List<String> headers) {
            Objects.requireNonNull(headers, "headers");

            String[] copy = headers.toArray(new String[headers.size()]);

            this.allowedHeaders = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        public Builder exposedHeaders(String... headers) {
            Objects.requireNonNull(headers, "headers");

            String[] copy = new String[headers.length];
            System.arraycopy(headers, 0, copy, 0, headers.length);

            this.exposedHeaders = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        public Builder exposedHeaders(List<String> headers) {
            Objects.requireNonNull(headers, "headers");

            String[] copy = headers.toArray(new String[headers.size()]);

            this.exposedHeaders = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        public Builder allowedMethods(String... methods) {
            Objects.requireNonNull(methods, "methods");

            String[] copy = new String[methods.length];
            System.arraycopy(methods, 0, copy, 0, methods.length);

            this.allowedHeaders = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        public Builder allowedMethods(List<String> methods) {
            Objects.requireNonNull(methods, "methods");

            String[] copied = methods.toArray(new String[methods.size()]);

            this.allowedHeaders = unmodifiableList(Arrays.asList(copied));

            return this;
        }

        public Builder preflightMaxAge(int maxAge) {
            this.preflightMaxAge = maxAge;
            return this;
        }

        public Builder supportCredentials(boolean supportCredentials) {
            this.supportCredentials = supportCredentials;
            return this;
        }

        public Builder enableLogging(boolean enableLogging) {
            this.enableLogging = enableLogging;
            return this;
        }

        public Builder decorateRequests(boolean decorateRequests) {
            this.decorateRequests = decorateRequests;
            return this;
        }

        /**
         * Reads configuration from a JSON file on the classpath of the current thread's context
         * class loader.
         *
         * @param resourcePath Follows the semantics of
         *         {@link java.lang.ClassLoader#getResourceAsStream(String)},
         *         which is to say it is a relative / separated path from the root of the class path
         *         and should <em>not</em> start with a forward slash (/).
         *
         * @see Thread#currentThread()
         * @see Thread#getContextClassLoader()
         */
        public Builder fromJsonResource(String resourcePath) throws IOException {
            return fromJsonResource(resourcePath, Thread.currentThread().getContextClassLoader());
        }

        /**
         * Reads configuration from a JSON file on the classpath of the provided classloader.
         *
         * @param resourcePath Follows the semantics of
         *         {@link java.lang.ClassLoader#getResourceAsStream(String)},
         *         which is to say it is a relative / separated path from the root of the class path
         *         and should <em>not</em> start with a forward slash (/).
         */
        public Builder fromJsonResource(String resourcePath, ClassLoader classLoader)
                throws IOException {
            Objects.requireNonNull(resourcePath, "resourcePath");
            Objects.requireNonNull(classLoader, "classLoader");

            return fromJson(classLoader.getResourceAsStream(resourcePath));
        }

        /**
         * Reads configuration from a JSON file found with the given path.
         *
         * @param pathToJson A file system path, relative to the working directory of the java
         *         process.
         */

        public Builder fromJson(Path pathToJson) throws IOException {
            Objects.requireNonNull(pathToJson, "pathToJson");

            return fromJson(Files.newInputStream(pathToJson));
        }

        /**
         * Reads configuration from a JSON file provided as an {@link java.io.InputStream}.
         *
         * <p>All of the expected fields are optional. Configuration will only be overwritten where
         * field values are provided.
         *
         * <dl>
         *     <dt>allowedOrigins</dt>
         *     <dd>Array of strings. See {@link #allowedOrigins(String...)}</dd>
         *     <dt>allowedMethods</dt>
         *     <dd>Array of strings. See {@link #allowedMethods(String...)}</dd>
         *     <dt>allowedHeaders</dt>
         *     <dd>Array of strings. See {@link #allowedHeaders(String...)}</dd>
         *     <dt>exposedHeaders</dt>
         *     <dd>Array of strings. See {@link #exposedHeaders(String...)}</dd>
         *     <dt>preflightMaxAge</dt>
         *     <dd>Integer. See {@link #preflightMaxAge(int)}</dd>
         *     <dt>supportCredentials</dt>
         *     <dd>Boolean. See {@link #supportCredentials(boolean)}</dd>
         *     <dt>enableLogging</dt>
         *     <dd>Boolean. See {@link #enableLogging(boolean)}</dd>
         *     <dt>decorateRequests</dt>
         *     <dd>Boolean. See {@link #decorateRequests(boolean)}</dd>
         * </dl>
         * @param json
         * @return
         * @throws IOException
         */
        public Builder fromJson(InputStream json) throws IOException {
            Objects.requireNonNull(json, "json");

            JsonNode configJson = JsonUtils.json(json);

            allowedOrigins = fromJsonArray(configJson.findPath("allowedOrigins"), allowedOrigins);
            allowedMethods = fromJsonArray(configJson.findPath("allowedMethods"), allowedMethods);
            allowedHeaders = fromJsonArray(configJson.findPath("allowedHeaders"), allowedHeaders);
            exposedHeaders = fromJsonArray(configJson.findPath("exposedHeaders"), exposedHeaders);

            preflightMaxAge = configJson.findPath("preflightMaxAge").asInt(preflightMaxAge);
            supportCredentials = configJson.findPath("preflightMaxAge").asBoolean(supportCredentials);
            enableLogging = configJson.findPath("preflightMaxAge").asBoolean(enableLogging);
            decorateRequests = configJson.findPath("preflightMaxAge").asBoolean(decorateRequests);

            return this;
        }

        public CorsConfiguration build() {
            return new CorsConfiguration(urlPatterns, allowedOrigins, allowedMethods,
                    allowedHeaders, exposedHeaders, preflightMaxAge, supportCredentials,
                    enableLogging, decorateRequests);
        }

        private List<String> fromJsonArray(JsonNode jsonNode, List<String> defaultList) {
            if (jsonNode.isMissingNode()) {
                return defaultList;
            }

            if (!jsonNode.isArray()) {
                throw new IllegalArgumentException("JSON node for field, " + jsonNode + ", " +
                        "expected to be an array, but was " + jsonNode.getNodeType() + ".");
            }

            List<String> strings = new ArrayList<>(jsonNode.size());
            Iterator<JsonNode> nodes = jsonNode.elements();

            while (nodes.hasNext()) {
                strings.add(nodes.next().asText());
            }

            return unmodifiableList(strings);
        }
    }

    /**
     * Private to ensure immutability of provided collections. It is expected that all collections
     * passed to this method are fully immutable.
     */
    private CorsConfiguration(List<String> urlPatterns, List<String> allowedOrigins,
            List<String> allowedMethods, List<String> allowedHeaders, List<String> exposedHeaders,
            int preflightMaxAge, boolean supportCredentials, boolean enableLogging,
            boolean decorateRequests) {
        this.urlPatterns = urlPatterns;
        this.allowedOrigins = allowedOrigins;
        this.allowedMethods = allowedMethods;
        this.allowedHeaders = allowedHeaders;
        this.exposedHeaders = exposedHeaders;
        this.preflightMaxAge = preflightMaxAge;
        this.supportCredentials = supportCredentials;
        this.enableLogging = enableLogging;
        this.decorateRequests = decorateRequests;
    }

    public List<String> getUrlPatterns() {
        return urlPatterns;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public List<String> getExposedHeaders() {
        return exposedHeaders;
    }

    public int getPreflightMaxAge() {
        return preflightMaxAge;
    }

    public boolean areCredentialsSupported() {
        return supportCredentials;
    }

    public boolean isLoggingEnabled() {
        return enableLogging;
    }

    public boolean shouldDecorateRequests() {
        return decorateRequests;
    }
}
