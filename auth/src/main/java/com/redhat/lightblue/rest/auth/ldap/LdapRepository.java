package com.redhat.lightblue.rest.auth.ldap;

import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustStoreTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocketFactory;

public class LdapRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapRepository.class);

    LdapConfiguration ldapConfiguration;
    LDAPConnectionPool connectionPool;

    public LdapRepository(LdapConfiguration ldapConfiguration) {
        this.ldapConfiguration = ldapConfiguration;
    }

    private void initialize() throws Exception {

        LDAPConnection ldapConnection;

        if(ldapConfiguration.getUseSSL()) {
            TrustStoreTrustManager trustStoreTrustManager = new TrustStoreTrustManager(
                    ldapConfiguration.getTrustStore(),
                    ldapConfiguration.getTrustStorePassword().toCharArray(),
                    "JKS",
                    true);
            SSLSocketFactory socketFactory = new SSLUtil(trustStoreTrustManager).createSSLSocketFactory();

            ldapConnection = new LDAPConnection(
                    socketFactory,
                    ldapConfiguration.getServer(),
                    ldapConfiguration.getPort(),
                    ldapConfiguration.getBindDn(),
                    ldapConfiguration.getBindDNPwd()
            );
        } else {
            ldapConnection = new LDAPConnection(
                    ldapConfiguration.getServer(),
                    ldapConfiguration.getPort(),
                    ldapConfiguration.getBindDn(),
                    ldapConfiguration.getBindDNPwd()
            );
        }

        BindRequest bindRequest = new SimpleBindRequest(ldapConfiguration.getBindDn(), ldapConfiguration.getBindDNPwd());
        BindResult bindResult = ldapConnection.bind(bindRequest);

        if (bindResult.getResultCode() != ResultCode.SUCCESS) {
            LOGGER.error("Error binding to LDAP" + bindResult.getResultCode());
            throw new Exception("Error binding to LDAP" + bindResult.getResultCode());
        }

        connectionPool = new LDAPConnectionPool(ldapConnection, ldapConfiguration.getPoolSize());

    }

    public SearchResult search(String baseDn, String filter) throws Exception {

        if(null == connectionPool) {
            initialize();
        }

        SearchRequest searchRequest = new SearchRequest(baseDn, SearchScope.SUB, filter);
        return connectionPool.search(searchRequest);
    }

}
