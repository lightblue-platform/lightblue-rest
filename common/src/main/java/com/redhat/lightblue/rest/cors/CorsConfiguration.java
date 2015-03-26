/*
 Copyright 2015 Red Hat, Inc. and/or its affiliates.

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
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS">HTTP
 *         access control documentation on MDN.</a>
 */
public final class CorsConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<String> urlPatterns;
    private final List<String> allowedOrigins;
    private final List<String> allowedMethods;
    private final List<String> allowedHeaders;
    private final List<String> exposedHeaders;
    private final int preflightMaxAge;
    private final boolean allowCredentials;
    private final boolean enableLogging;

    /**
     * Chain setters together to configure a {@link com.redhat.lightblue.rest.cors.CorsConfiguration}
     * object.
     *
     * <p>To read values from a JSON configuration file, use {@link #fromJson(java.nio.file.Path)}.
     *
     * <p>To understand what these configurations mean in more depth, see
     * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS">HTTP
     * access control documentation on MDN.</a>
     */
    public static class Builder {
        static final List<String> DEFAULT_URL_PATTERNS = unmodifiableList(Arrays.asList("/*"));
        static final List<String> DEFAULT_ALLOWED_ORIGINS = unmodifiableList(Arrays.asList("*"));
        static final List<String> DEFAULT_ALLOWED_METHODS = unmodifiableList(Arrays.asList("GET",
                "PUT", "POST", "HEAD", "OPTIONS"));;
        static final List<String> DEFAULT_ALLOWED_HEADERS = unmodifiableList(Arrays.asList("Origin",
                "Accept", "X-Requested-With", "Content-Type", "Access-Control-Request-Method",
                "Access-Control-Request-Headers"));
        static final List<String> DEFAULT_EXPOSED_HEADERS = Collections.emptyList();
        static final int DEFAULT_PREFLIGHT_MAX_AGE = 1800;
        static final boolean DEFAULT_ALLOW_CREDENTIALS = true;
        static final boolean DEFAULT_ENABLE_LOGGING = false;

        private List<String> urlPatterns = DEFAULT_URL_PATTERNS;
        private List<String> allowedOrigins = DEFAULT_ALLOWED_ORIGINS;
        private List<String> allowedMethods = DEFAULT_ALLOWED_METHODS;
        private List<String> allowedHeaders = DEFAULT_ALLOWED_HEADERS;
        private List<String> exposedHeaders = DEFAULT_EXPOSED_HEADERS;
        private int preflightMaxAge = DEFAULT_PREFLIGHT_MAX_AGE;
        private boolean allowCredentials = DEFAULT_ALLOW_CREDENTIALS;
        private boolean enableLogging = DEFAULT_ENABLE_LOGGING;

        /**
         * Sets the URL patterns to use with a copy of the contents from the provided array,
         * preventing later modifications to the array from affecting the
         * {@link com.redhat.lightblue.rest.cors.CorsConfiguration}.
         *
         * <p>URL matching rules follow that of servlet and filter mappings' URL patterns.
         *
         * <p>Defaults to "/*".
         */
        public Builder urlPatterns(String... patterns) {
            Objects.requireNonNull(patterns, "patterns");

            String[] copy = new String[patterns.length];
            System.arraycopy(patterns, 0, copy, 0, patterns.length);

            this.urlPatterns = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        /**
         * Sets the URL patterns to use with a copy of the contents from the provided list,
         * preventing later modifications to the list from affecting the
         * {@link com.redhat.lightblue.rest.cors.CorsConfiguration}.
         *
         * <p>URL matching rules follow that of servlet and filter mappings' URL patterns.
         *
         * <p>Defaults to "/*".
         */
        public Builder urlPatterns(List<String> patterns) {
            Objects.requireNonNull(patterns, "patterns");

            String[] copy = patterns.toArray(new String[patterns.size()]);

            this.urlPatterns = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        /**
         * Sets the allowed origins to use with a copy of the contents from the provided array,
         * preventing later modifications to the array from affecting the
         * {@link com.redhat.lightblue.rest.cors.CorsConfiguration}.
         *
         * <p>Defaults to "*".
         *
         * @param origins An array of origins to allow. To allow any origin, pass an array who's
         *         sole element is "*". Otherwise, a cross-origin request's "Origin" header must
         *         exactly match one of the provided origins in order to be allowed.
         * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Access-Control-Allow-Origin">
         *         Access-Control-Allow-Origin on MDN.</a>
         */
        public Builder allowedOrigins(String... origins) {
            Objects.requireNonNull(origins, "origins");

            String[] copy = new String[origins.length];
            System.arraycopy(origins, 0, copy, 0, origins.length);

            this.allowedOrigins = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        /**
         * Sets the allowed origins to use with a copy of the contents from the provided list,
         * preventing later modifications to the list from affecting the
         * {@link com.redhat.lightblue.rest.cors.CorsConfiguration}.
         *
         * <p>Defaults to "*".
         *
         * @param origins A list of origins to allow. To allow any origin, pass a list who's sole
         *         element is "*". Otherwise, a cross-origin request's "Origin" header must exactly
         *         match one of the provided origins in order to be allowed.
         * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Access-Control-Allow-Origin">
         *         Access-Control-Allow-Origin on MDN.</a>
         */
        public Builder allowedOrigins(List<String> origins) {
            Objects.requireNonNull(origins, "origins");

            String[] copy = origins.toArray(new String[origins.size()]);

            this.allowedOrigins = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        /**
         * Sets the allowed origins to use with a copy of the contents from the provided array,
         * preventing later modifications to the array from affecting the
         * {@link com.redhat.lightblue.rest.cors.CorsConfiguration}.
         *
         * <p>Defaults to "Origin", "Accept", "X-Requested-With", "Content-Type",
         * "Access-Control-Request-Method", and "Access-Control-Request-Headers".
         *
         * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Access-Control-Allow-Headers">
         *         Access-Control-Allow-Header on MDN.</a>
         */
        public Builder allowedHeaders(String... headers) {
            Objects.requireNonNull(headers, "headers");

            String[] copy = new String[headers.length];
            System.arraycopy(headers, 0, copy, 0, headers.length);

            this.allowedHeaders = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        /**
         * Sets the allowed origins to use with a copy of the contents from the provided list,
         * preventing later modifications to the list from affecting the
         * {@link com.redhat.lightblue.rest.cors.CorsConfiguration}.
         *
         * <p>Defaults to "Origin", "Accept", "X-Requested-With", "Content-Type",
         * "Access-Control-Request-Method", and "Access-Control-Request-Headers".
         *
         * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Access-Control-Allow-Headers">
         *         Access-Control-Allow-Header on MDN.</a>
         */
        public Builder allowedHeaders(List<String> headers) {
            Objects.requireNonNull(headers, "headers");

            String[] copy = headers.toArray(new String[headers.size()]);

            this.allowedHeaders = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        /**
         * Sets the exposed headers to use with a copy of the contents from the provided array,
         * preventing later modifications to the array from affecting the
         * {@link com.redhat.lightblue.rest.cors.CorsConfiguration}.
         *
         * <p>Defaults to none.
         *
         * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Access-Control-Expose-Headers">
         *         Access-Control-Expose-Headers on MDN.</a>
         */
        public Builder exposedHeaders(String... headers) {
            Objects.requireNonNull(headers, "headers");

            String[] copy = new String[headers.length];
            System.arraycopy(headers, 0, copy, 0, headers.length);

            this.exposedHeaders = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        /**
         * Sets the exposed headers to use with a copy of the contents from the provided list,
         * preventing later modifications to the list from affecting the
         * {@link com.redhat.lightblue.rest.cors.CorsConfiguration}.
         *
         * <p>Defaults to none.
         *
         * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Access-Control-Expose-Headers">
         *         Access-Control-Expose-Headers on MDN.</a>
         */
        public Builder exposedHeaders(List<String> headers) {
            Objects.requireNonNull(headers, "headers");

            String[] copy = headers.toArray(new String[headers.size()]);

            this.exposedHeaders = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        /**
         * Sets the allowed methods to use with a copy of the contents from the provided array,
         * preventing later modifications to the array from affecting the
         * {@link com.redhat.lightblue.rest.cors.CorsConfiguration}.
         *
         * <p>Defaults to "GET", "PUT", "POST", "HEAD", and "OPTIONS".
         *
         * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Access-Control-Allow-Methods">
         *         Access-Control-Allow-Methods on MDN.</a>
         */
        public Builder allowedMethods(String... methods) {
            Objects.requireNonNull(methods, "methods");

            String[] copy = new String[methods.length];
            System.arraycopy(methods, 0, copy, 0, methods.length);

            this.allowedMethods = unmodifiableList(Arrays.asList(copy));

            return this;
        }

        /**
         * Sets the allowed methods to use with a copy of the contents from the provided array,
         * preventing later modifications to the array from affecting the
         * {@link com.redhat.lightblue.rest.cors.CorsConfiguration}.
         *
         * <p>Defaults to "GET", "PUT", "POST", "HEAD", and "OPTIONS".
         *
         * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Access-Control-Allow-Methods">
         *         Access-Control-Allow-Methods on MDN.</a>
         */
        public Builder allowedMethods(List<String> methods) {
            Objects.requireNonNull(methods, "methods");

            String[] copied = methods.toArray(new String[methods.size()]);

            this.allowedMethods = unmodifiableList(Arrays.asList(copied));

            return this;
        }

        /**
         * Defaults to 1800 seconds.
         *
         * @param maxAge In seconds.
         * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Access-Control-Max-Age">
         *         Access-Control-Max-Age on MDN.</a>
         */
        public Builder preflightMaxAge(int maxAge) {
            this.preflightMaxAge = maxAge;
            return this;
        }

        /**
         * Defaults to true.
         *
         * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#Access-Control-Allow-Credentials">
         *         Access-Control-Allow-Credentials on MDN.</a>
         */
        public Builder allowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
            return this;
        }

        /**
         * Enable or disable implementation-specific logging if it is supported. Defaults to false.
         */
        public Builder enableLogging(boolean enableLogging) {
            this.enableLogging = enableLogging;
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
         *     <dt>urlPatterns</dt>
         *     <dd>Array of strings. See {@link #urlPatterns}</dd>
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
         *     <dt>allowCredentials</dt>
         *     <dd>Boolean. See {@link #allowCredentials(boolean)}</dd>
         *     <dt>enableLogging</dt>
         *     <dd>Boolean. See {@link #enableLogging(boolean)}</dd>
         * </dl>
         */
        public Builder fromJson(InputStream json) throws IOException {
            Objects.requireNonNull(json, "json");

            JsonNode configJson = JsonUtils.json(json);

            urlPatterns = fromJsonArray(configJson.findPath("urlPatterns"), urlPatterns);
            allowedOrigins = fromJsonArray(configJson.findPath("allowedOrigins"), allowedOrigins);
            allowedMethods = fromJsonArray(configJson.findPath("allowedMethods"), allowedMethods);
            allowedHeaders = fromJsonArray(configJson.findPath("allowedHeaders"), allowedHeaders);
            exposedHeaders = fromJsonArray(configJson.findPath("exposedHeaders"), exposedHeaders);

            preflightMaxAge = configJson.findPath("preflightMaxAge").asInt(preflightMaxAge);
            allowCredentials = configJson.findPath("allowCredentials").asBoolean(allowCredentials);
            enableLogging = configJson.findPath("enableLogging").asBoolean(enableLogging);

            return this;
        }

        public CorsConfiguration build() {
            return new CorsConfiguration(urlPatterns, allowedOrigins, allowedMethods,
                    allowedHeaders, exposedHeaders, preflightMaxAge, allowCredentials,
                    enableLogging);
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
            int preflightMaxAge, boolean allowCredentials, boolean enableLogging) {
        this.urlPatterns = urlPatterns;
        this.allowedOrigins = allowedOrigins;
        this.allowedMethods = allowedMethods;
        this.allowedHeaders = allowedHeaders;
        this.exposedHeaders = exposedHeaders;
        this.preflightMaxAge = preflightMaxAge;
        this.allowCredentials = allowCredentials;
        this.enableLogging = enableLogging;
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

    public boolean areCredentialsAllowed() {
        return allowCredentials;
    }

    public boolean isLoggingEnabled() {
        return enableLogging;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CorsConfiguration that = (CorsConfiguration) o;

        return allowCredentials == that.allowCredentials &&
                enableLogging == that.enableLogging &&
                preflightMaxAge == that.preflightMaxAge &&
                allowedHeaders.equals(that.allowedHeaders) &&
                allowedMethods.equals(that.allowedMethods) &&
                allowedOrigins.equals(that.allowedOrigins) &&
                exposedHeaders.equals(that.exposedHeaders) &&
                urlPatterns.equals(that.urlPatterns);

    }

    @Override
    public int hashCode() {
        return Objects.hash(allowCredentials, enableLogging, preflightMaxAge, allowedHeaders,
                allowedMethods, allowedOrigins, exposedHeaders, urlPatterns);
    }

    @Override
    public String toString() {
        return "CorsConfiguration{" +
                "urlPatterns=" + urlPatterns +
                ", allowedOrigins=" + allowedOrigins +
                ", allowedMethods=" + allowedMethods +
                ", allowedHeaders=" + allowedHeaders +
                ", exposedHeaders=" + exposedHeaders +
                ", preflightMaxAge=" + preflightMaxAge +
                ", allowCredentials=" + allowCredentials +
                ", enableLogging=" + enableLogging +
                '}';
    }
}
