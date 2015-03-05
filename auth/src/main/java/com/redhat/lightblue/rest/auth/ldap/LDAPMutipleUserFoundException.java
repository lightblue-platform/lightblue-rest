package com.redhat.lightblue.rest.auth.ldap;

/**
 * This simple exception is used when an unexpected behavior is found from the LDAP server where more than one entry is returned
 *
 * Created by lcestari on 2/23/15.
 */
public class LDAPMutipleUserFoundException extends Exception {
    public LDAPMutipleUserFoundException(String message) {
        super(message);
    }
}
