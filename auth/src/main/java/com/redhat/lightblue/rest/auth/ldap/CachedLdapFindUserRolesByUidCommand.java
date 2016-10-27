package com.redhat.lightblue.rest.auth.ldap;

import com.google.common.base.Strings;
import com.redhat.lightblue.rest.authz.RolesCache;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.directory.SearchControls;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * LDAP Hystrix command that can provide metrics for this service and fall back
 * in case the server was unreachable as well.
 * <p/>
 * Created by nmalik and lcestari and mpatercz
 */
public class CachedLdapFindUserRolesByUidCommand {

    public static final String GROUPKEY = "ldap";
    private static final String INVALID_PARAM = "%s is null or empty";
    private static final Logger LOGGER = LoggerFactory.getLogger(LightblueLdapRoleProvider.class);

    private final LDAPQuery ldapQuery;
    private final LdapRepository ldapRepository;

    public CachedLdapFindUserRolesByUidCommand(LdapConfiguration ldapConfiguration, String ldapSearchBase, String uid) throws Exception {
        LOGGER.debug("Creating CachedLdapFindUserRolesByUidCommand");
        //check if the informed parameters are valid
        if (Strings.isNullOrEmpty(uid)) {
            LOGGER.error("uid informed in LdapFindUserByUidCommand constructor is empty or null");
            throw new IllegalArgumentException(String.format(INVALID_PARAM, "uid"));
        } else if (Strings.isNullOrEmpty(ldapSearchBase)) {
            LOGGER.error("ldapSearchBase informed in LdapFindUserByUidCommand constructor is empty or null");
            throw new IllegalArgumentException(String.format(INVALID_PARAM, "ldapSearchBase"));
        } else if (ldapConfiguration == null) {
            LOGGER.error("ldapConfiguration cannot be null");
            throw new IllegalArgumentException(String.format(INVALID_PARAM, "contextProvider"));
        }
        this.ldapQuery = new LDAPQuery(uid, ldapSearchBase, "(uid=" + uid + ")", SearchControls.SUBTREE_SCOPE);
        this.ldapRepository = new LdapRepository(ldapConfiguration);
    }

    public List<String> execute() throws Exception {
        LOGGER.debug("CachedLdapFindUserRolesByUidCommand#run was invoked");

        List<String> roles = RolesCache.get(ldapQuery.uid);

        if (roles != null) {
            LOGGER.debug("Found roles in cache for uid=" + ldapQuery.uid);
            return roles;
        }

        try {
            LOGGER.debug("Cache missed for uid=" + ldapQuery.uid + ". Calling ldap.");
            // LDAPSearcher does the lookup and the call only when cache is missed

            roles = getUserRolesFromLdap(ldapQuery);

            if (roles != null) {
                RolesCache.put(ldapQuery.uid, roles);
            }

            return roles;

        } catch (LDAPException e) {
            LOGGER.error("User roles could not be retrieved from primary cache or LDAP, trying fallback " + ldapQuery.uid, e);
            return getFallback(ldapQuery);
        }
    }

    private List<String> getUserRolesFromLdap(LDAPQuery ldapQuery) throws Exception {
        LOGGER.debug("Invoking LightblueLdapRoleProvider#getUserRolesFromLdap");
        List<String> roles = new ArrayList<>();

        SearchResult searchResult = ldapRepository.search(ldapQuery.ldapSearchBase, ldapQuery.searchFilter);
        List<SearchResultEntry> searchResultEntries = searchResult.getSearchEntries();

        if(searchResultEntries.isEmpty()) {
            LOGGER.error("No result found roles for user: " + ldapQuery.uid);
            throw new LDAPUserNotFoundException();
        } else if (searchResultEntries.size() > 1) {
            LOGGER.error("Multiples users found and only one was expected for user: " + ldapQuery.uid);
            throw new LDAPMultipleUserFoundException("Multiples users found and only one was expected for user: " + ldapQuery.uid);
        } else {
            for(SearchResultEntry searchResultEntry : searchResultEntries) {
                String[] groups = searchResultEntry.getAttributeValues("memberOf");
                if(null != groups) {
                    for(String group : groups) {
                        for (RDN rdn : new DN(group).getRDNs()) {
                            if (rdn.hasAttribute("cn")) {
                                roles.addAll(Arrays.asList(rdn.getAttributeValues()));
                                break;
                            }
                        }
                    }
                }
            }
        }

        return roles;
    }

    /**
     * This methods is executed for all types of failure such as run() failure.
     */
    protected List<String> getFallback(LDAPQuery cacheKey) throws CachedLDAPUserNotFoundException {
        LOGGER.warn("Error during the execution of the command. Falling back to the cache");
        List<String> roles = RolesCache.getFromFallback(cacheKey.uid);
        if (roles == null) {
            CachedLDAPUserNotFoundException e = new CachedLDAPUserNotFoundException();
            LOGGER.error("Failed to connect to the server and no result found roles for user: " + cacheKey.uid, e);
            throw e;
        }
        // was able to use the cache or use the LDAP server on the second retry
        LOGGER.debug("FallbackViaLDAPServerProblemCommand#run : user found!");
        return roles;
    }
}
