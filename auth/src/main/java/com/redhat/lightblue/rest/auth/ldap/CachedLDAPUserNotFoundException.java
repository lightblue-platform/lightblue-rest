package com.redhat.lightblue.rest.auth.ldap;

/**
 * A simple exception to mark when the result of a query that hits the cache for users doesnt have the key
 *
 * Created by lcestari on 3/6/15.
 */
// TODO REVIEWER check the Exception classes, log level used and related things are properly used/created all over the code
public class CachedLDAPUserNotFoundException extends Exception {
}
