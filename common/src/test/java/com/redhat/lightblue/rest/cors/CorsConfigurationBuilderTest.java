package com.redhat.lightblue.rest.cors;

import static com.redhat.lightblue.rest.cors.CorsConfiguration.Builder.DEFAULT_ALLOWED_HEADERS;
import static com.redhat.lightblue.rest.cors.CorsConfiguration.Builder.DEFAULT_ALLOWED_METHODS;
import static com.redhat.lightblue.rest.cors.CorsConfiguration.Builder.DEFAULT_ALLOWED_ORIGINS;
import static com.redhat.lightblue.rest.cors.CorsConfiguration.Builder.DEFAULT_ALLOW_CREDENTIALS;
import static com.redhat.lightblue.rest.cors.CorsConfiguration.Builder.DEFAULT_ENABLE_LOGGING;
import static com.redhat.lightblue.rest.cors.CorsConfiguration.Builder.DEFAULT_EXPOSED_HEADERS;
import static com.redhat.lightblue.rest.cors.CorsConfiguration.Builder.DEFAULT_PREFLIGHT_MAX_AGE;
import static com.redhat.lightblue.rest.cors.CorsConfiguration.Builder.DEFAULT_URL_PATTERNS;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.Arrays;

@RunWith(JUnit4.class)
public class CorsConfigurationBuilderTest {
    @Test
    public void shouldParseAllValuesFromJson() throws IOException {
        CorsConfiguration config = new CorsConfiguration.Builder()
                .fromJsonResource("corsConfigWithNoDefaultValues.json")
                .build();

        assertThat(config.getUrlPatterns(), contains("/test", "/data"));
        assertThat(config.getAllowedOrigins(), contains("http://test.com", "http://test2.com"));
        assertThat(config.getAllowedHeaders(), contains("Origin", "Accept", "X-Requested-With"));
        assertThat(config.getAllowedMethods(), contains("GET", "POST"));
        assertThat(config.getExposedHeaders(), contains("test", "exposed", "headers"));
        assertThat(config.getPreflightMaxAge(), is(10));
        assertThat(config.areCredentialsAllowed(), is(false));
        assertThat(config.isLoggingEnabled(), is(true));
    }

    @Test
    public void shoulNotOverrideDefaultsIfNotProvidedInJsonConfiguration() throws IOException {
        CorsConfiguration config = new CorsConfiguration.Builder()
                .fromJsonResource("emptyCorsConfig.json")
                .build();

        assertThat(config.getUrlPatterns(), equalTo(DEFAULT_URL_PATTERNS));
        assertThat(config.getAllowedOrigins(), equalTo(DEFAULT_ALLOWED_ORIGINS));
        assertThat(config.getAllowedHeaders(), equalTo(DEFAULT_ALLOWED_HEADERS));
        assertThat(config.getAllowedMethods(), equalTo(DEFAULT_ALLOWED_METHODS));
        assertThat(config.getExposedHeaders(), equalTo(DEFAULT_EXPOSED_HEADERS));
        assertThat(config.getPreflightMaxAge(), equalTo(DEFAULT_PREFLIGHT_MAX_AGE));
        assertThat(config.areCredentialsAllowed(), equalTo(DEFAULT_ALLOW_CREDENTIALS));
        assertThat(config.isLoggingEnabled(), equalTo(DEFAULT_ENABLE_LOGGING));
    }

    @Test
    public void shouldAllowCustomizedConfigWithVarargMethods() {
        CorsConfiguration config = new CorsConfiguration.Builder()
                .urlPatterns("/test", "/data")
                .allowedOrigins("origin1", "origin2")
                .allowedMethods("method1", "method2")
                .allowedHeaders("header1", "header2")
                .exposedHeaders("header3", "header4", "header5")
                .preflightMaxAge(100)
                .allowCredentials(false)
                .enableLogging(true)
                .build();

        assertThat(config.getUrlPatterns(), contains("/test", "/data"));
        assertThat(config.getAllowedOrigins(), contains("origin1", "origin2"));
        assertThat(config.getAllowedHeaders(), contains("header1", "header2"));
        assertThat(config.getAllowedMethods(), contains("method1", "method2"));
        assertThat(config.getExposedHeaders(), contains("header3", "header4", "header5"));
        assertThat(config.getPreflightMaxAge(), is(100));
        assertThat(config.areCredentialsAllowed(), is(false));
        assertThat(config.isLoggingEnabled(), is(true));
    }

    @Test
    public void shouldAllowCustomizedConfigWithListMethods() {
        CorsConfiguration config = new CorsConfiguration.Builder()
                .urlPatterns(Arrays.asList("/test", "/data"))
                .allowedOrigins(Arrays.asList("origin1", "origin2"))
                .allowedMethods(Arrays.asList("method1", "method2"))
                .allowedHeaders(Arrays.asList("header1", "header2"))
                .exposedHeaders(Arrays.asList("header3", "header4", "header5"))
                .build();

        assertThat(config.getUrlPatterns(), contains("/test", "/data"));
        assertThat(config.getAllowedOrigins(), contains("origin1", "origin2"));
        assertThat(config.getAllowedHeaders(), contains("header1", "header2"));
        assertThat(config.getAllowedMethods(), contains("method1", "method2"));
        assertThat(config.getExposedHeaders(), contains("header3", "header4", "header5"));
    }
}
