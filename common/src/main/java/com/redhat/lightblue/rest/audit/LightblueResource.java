package com.redhat.lightblue.rest.audit;

import java.util.regex.Pattern;

/**
 * Represent the '/crud' and '/metadata' rest context path
 * Created by lcestari on 4/10/15.
 */
public enum LightblueResource {
    CRUD("/+data/*"), METADATA("/+metadata/*");

    private final Pattern pattern;

    LightblueResource(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public Pattern getPattern() {
        return pattern;
    }
}
