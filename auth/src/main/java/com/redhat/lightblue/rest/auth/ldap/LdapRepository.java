package com.redhat.lightblue.rest.auth.ldap;

import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.util.DebugType;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustStoreTrustManager;

public class LdapRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapRepository.class);

    LdapConfiguration ldapConfiguration;
    // Connection pool is a singleton
    static LDAPConnectionPool connectionPool;

    /**
     * Subsequent LdapRepository initializations will ignore LdapConfiguration, because connection pool is already initialized.
     *
     * @param ldapConfiguration
     */
    public static LdapRepository getInstance(LdapConfiguration ldapConfiguration) {
        return new LdapRepository(ldapConfiguration);
    }

    private LdapRepository(LdapConfiguration ldapConfiguration) {
        this.ldapConfiguration = ldapConfiguration;
    }

    private void initialize() throws Exception {

        if (ldapConfiguration.isDebug()) {
            // bridge java.util.Logger output to log4j
            System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

            // setting the ldap debug level...
            System.setProperty("com.unboundid.ldap.sdk.debug.enabled", "true");
            System.setProperty("com.unboundid.ldap.sdk.debug.level", "FINEST");
            System.setProperty("com.unboundid.ldap.sdk.debug.type", DebugType.getTypeNameList());
        }

        LDAPConnection ldapConnection;

        LDAPConnectionOptions options = new LDAPConnectionOptions();

        // A value which specifies the maximum length of time in milliseconds that an attempt to establish a connection should be allowed to block before failing. By default, a timeout of 60,000 milliseconds (1 minute) will be used.
        options.setConnectTimeoutMillis(ldapConfiguration.getConnectionTimeoutMS());
        // A value which specifies the default timeout in milliseconds that the SDK should wait for a response from the server before failing. By default, a timeout of 300,000 milliseconds (5 minutes) will be used.
        options.setResponseTimeoutMillis(ldapConfiguration.getResponseTimeoutMS());
        // A flag that indicates whether to use the SO_KEEPALIVE socket option to attempt to more quickly detect when idle TCP connections have been lost or to prevent them from being unexpectedly closed by intermediate network hardware. By default, the SO_KEEPALIVE socket option will be used.
        options.setUseKeepAlive(ldapConfiguration.isKeepAlive());


        if(ldapConfiguration.getUseSSL()) {
            TrustStoreTrustManager trustStoreTrustManager = new TrustStoreTrustManager(
                    ldapConfiguration.getTrustStore(),
                    ldapConfiguration.getTrustStorePassword().toCharArray(),
                    "JKS",
                    true);
            SSLSocketFactory socketFactory = new SSLUtil(trustStoreTrustManager).createSSLSocketFactory();

            ldapConnection = new LDAPConnection(
                    socketFactory,
                    options,
                    ldapConfiguration.getServer(),
                    ldapConfiguration.getPort(),
                    ldapConfiguration.getBindDn(),
                    ldapConfiguration.getBindDNPwd()
            );
        } else {
            ldapConnection = new LDAPConnection(
                    options,
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
        connectionPool.setMaxConnectionAgeMillis(ldapConfiguration.getPoolMaxConnectionAgeMS());
        LOGGER.info("Initialized LDAPConnectionPool: poolSize={}, poolMaxAge={}, connectionTimeout={}, responseTimeout={}, debug={}, keepAlive={}",
                ldapConfiguration.getPoolSize(), ldapConfiguration.getPoolMaxConnectionAgeMS(), ldapConfiguration.getConnectionTimeoutMS(), ldapConfiguration.getResponseTimeoutMS(),
                ldapConfiguration.isDebug(), ldapConfiguration.isKeepAlive());

    }

    public SearchResult search(String baseDn, String filter) throws Exception {

        if (null == connectionPool) {
            synchronized (LdapRepository.class) {
                if(null == connectionPool) {
                    initialize();
                }
            }
        }

        SearchRequest searchRequest = new SearchRequest(baseDn, SearchScope.SUB, filter);
        return connectionPool.search(searchRequest);
    }

}
