package com.redhat.lightblue.rest.metadata;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * Created by nmalik on 12/12/14.
 */
public class TestSecurityContext implements SecurityContext {
    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return role != null && !role.isEmpty();
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return null;
    }
}
