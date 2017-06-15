package com.redhat.lightblue.rest.auth.health;

public class LdapAuthHealth {

    private final boolean isHealthy;
    private final String details;

    public LdapAuthHealth(boolean isHealthy, String details) {
        this.isHealthy = isHealthy;
        this.details = details;
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    /**
     * @return details about the health
     */
    public String details() {
        return details;
    }
}
