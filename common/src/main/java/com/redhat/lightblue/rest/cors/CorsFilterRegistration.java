package com.redhat.lightblue.rest.cors;

import javax.servlet.ServletContext;

public interface CorsFilterRegistration {
    void register(ServletContext context, CorsConfiguration configuration);
}
