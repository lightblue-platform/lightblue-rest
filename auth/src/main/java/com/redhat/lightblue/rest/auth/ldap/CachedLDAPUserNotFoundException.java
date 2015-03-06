package com.redhat.lightblue.rest.auth.ldap;

/**
 * A simple exception to mark when the result of a query that hits the cache for users doesnt have the key
 *
 * Created by lcestari on 3/6/15.
 */
// TODO do we log error for each execption (even when it is something like CachedLDAPUserNotFoundException (this) class )
public class CachedLDAPUserNotFoundException extends Exception {
}
