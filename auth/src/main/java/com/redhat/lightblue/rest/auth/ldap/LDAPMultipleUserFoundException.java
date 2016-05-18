package com.redhat.lightblue.rest.auth.ldap;

/**
 * This simple exception is used when an unexpected behavior is found from the
 * LDAP server where more than one entry is returned
 * <p/>
 * Created by lcestari on 2/23/15.
 */
public class LDAPMultipleUserFoundException extends Exception {
    public LDAPMultipleUserFoundException(String message) {
        super(message);
    }
}
