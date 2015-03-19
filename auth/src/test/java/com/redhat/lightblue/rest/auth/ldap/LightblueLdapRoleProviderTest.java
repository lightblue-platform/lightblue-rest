package com.redhat.lightblue.rest.auth.ldap;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.shared.ldap.entry.DefaultServerEntry;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;

import static org.junit.Assert.*;


import java.io.File;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.List;

public class LightblueLdapRoleProviderTest {

    public static final String LDAP_LOCALHOST_11389 = "ldap://localhost:11389";
    private static DirectoryService directoryService;
    private static LdapServer ldapServer;

    @BeforeClass
    public static void startApacheDs() throws Exception {
        String buildDirectory = System.getProperty("buildDirectory");
        File workingDirectory = new File(buildDirectory, "apacheds-work");
        workingDirectory.mkdir();

        directoryService = new DefaultDirectoryService();
        directoryService.setWorkingDirectory(workingDirectory);

        SchemaPartition schemaPartition = directoryService.getSchemaService().getSchemaPartition();

        LdifPartition ldifPartition = new LdifPartition();
        String workingDirectoryPath = directoryService.getWorkingDirectory().getPath();
        ldifPartition.setWorkingDirectory(workingDirectoryPath + "/schema");

        File schemaRepository = new File(workingDirectory, "schema");
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(workingDirectory);
        extractor.extractOrCopy(true);

        schemaPartition.setWrappedPartition(ldifPartition);

        SchemaLoader loader = new LdifSchemaLoader(schemaRepository);
        SchemaManager schemaManager = new DefaultSchemaManager(loader);
        directoryService.setSchemaManager(schemaManager);

        schemaManager.loadAllEnabled();

        schemaPartition.setSchemaManager(schemaManager);

        List<Throwable> errors = schemaManager.getErrors();

        if (!errors.isEmpty()) {
            throw new Exception("Schema load failed : " + errors);
        }

        JdbmPartition systemPartition = new JdbmPartition();
        systemPartition.setId("system");
        systemPartition.setPartitionDir(new File(directoryService.getWorkingDirectory(), "system"));
        systemPartition.setSuffix(ServerDNConstants.SYSTEM_DN);
        systemPartition.setSchemaManager(schemaManager);
        directoryService.setSystemPartition(systemPartition);



        /*
        // TODO some snippets trying to make testGetUserRolesValidLDAPDirectory test work
        Partition testp = new JdbmPartition();
        testp.setId("testp");
        testp.setSuffix("dc=testp,dc=com");
        contextDn = new LdapDN( "dc=testp,dc=com" );
        contextDn.normalize( service.getRegistries().getAttributeTypeRegistry().getNormalizerMapping() );
        contextEntry = new DefaultServerEntry( service.getRegistries(), contextDn );
        contextEntry.add( "objectClass", "top", "domain" );
        contextEntry.add( "dc", "testp" );
        testp.setContextEntry(contextEntry);
        service.addPartition( testp );
        */

        directoryService.setShutdownHookEnabled(false);
        directoryService.getChangeLog().setEnabled(false);


        ldapServer = new LdapServer();
        ldapServer.setTransports(new TcpTransport(11389));
        ldapServer.setDirectoryService(directoryService);

        directoryService.startup();
        ldapServer.start();
    }

    @AfterClass
    public static void stopApacheDs() throws Exception {
        ldapServer.stop();
        directoryService.shutdown();
        directoryService.getWorkingDirectory().delete();
    }

    @Test
    public void testGetUserRolesInvalidLDAPDirectory() throws Exception {
        LightblueLdapRoleProvider provider = new LightblueLdapRoleProvider(LDAP_LOCALHOST_11389, "schema", null, null);

        List<String> userRoles = provider.getUserRoles("nameHere");
        assertNotNull(userRoles);
        assertEquals("getUserRoles method should return an empty list",0, userRoles.size());
    }

    @Test
    @Ignore
    public void testGetUserRolesValidLDAPDirectory() throws Exception {
        LightblueLdapRoleProvider provider = new LightblueLdapRoleProvider(LDAP_LOCALHOST_11389, "schema", null, null);
        printLdap();
        List<String> userRoles = provider.getUserRoles("nameHere");
        assertNotNull(userRoles);
        assertEquals("getUserRoles method should return an list with one element",1, userRoles.size());
    }

    private void printLdap() throws Exception {
        PrintWriter out = new  PrintWriter(System.out);

        out.println("*** ApacheDS RootDSE ***\n");
        Hashtable<String, Object> env = new Hashtable<>();
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, LDAP_LOCALHOST_11389);
        DirContext ctx = new InitialDirContext(env);

        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes(new String[] { "*", "+" });
        ctls.setSearchScope(SearchControls.OBJECT_SCOPE);

        NamingEnumeration<SearchResult> result = ctx.search("",
                "(objectClass=*)", ctls);
        if (result.hasMore()) {
            SearchResult entry = result.next();
            Attributes as = entry.getAttributes();

            NamingEnumeration<String> ids = as.getIDs();
            while (ids.hasMore()) {
                String id = ids.next();
                Attribute attr = as.get(id);
                for (int i = 0; i < attr.size(); ++i) {
                    out.println(id + ": " + attr.get(i));
                }
            }
        }
        ctx.close();

        out.flush();
    }
    /*
    // TODO some snippets trying to make testGetUserRolesValidLDAPDirectory test work
        static void runServer() throws Exception {
        DefaultDirectoryService service = new DefaultDirectoryService();
        service.getChangeLog().setEnabled(false);

        Partition partition = new JdbmPartition();
        partition.setId("apache");
        partition.setSuffix("dc=apache,dc=org");
        service.addPartition(partition);

        LdapServer ldapService = new LdapServer();
        ldapService.setTransports(new TcpTransport(1400));
        ldapService.setDirectoryService(service);

        service.startup();

        // Inject the apache root entry if it does not already exist
        try {
            service.getAdminSession().lookup(partition.getSuffixDn());
        } catch (Exception lnnfe) {
            DN dnApache = new DN("dc=Apache,dc=Org");
            ServerEntry entryApache = service.newEntry(dnApache);
            entryApache.add("objectClass", "top", "domain", "extensibleObject");
            entryApache.add("dc", "Apache");
            service.getAdminSession().add(entryApache);
        }

        DN dnApache = new DN("dc=Apache,dc=Org");
        ServerEntry entryApache = service.newEntry(dnApache);
        entryApache.add("objectClass", "top", "domain", "extensibleObject");
        entryApache.add("dc", "Apache");
        service.getAdminSession().add(entryApache);

        ldapService.start();
    }


    static void testClient() throws NamingException {
        Properties p = new Properties();
        p.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        p.setProperty(Context.PROVIDER_URL, "ldap://localhost:1400/");
        p.setProperty(Context.SECURITY_PRINCIPAL, "uid=admin,ou=system");
        p.setProperty(Context.SECURITY_CREDENTIALS, "secret");
        p.setProperty(Context.SECURITY_AUTHENTICATION, "simple");

        DirContext rootCtx = new InitialDirContext(p);
        DirContext ctx = (DirContext) rootCtx.lookup("dc=apache,dc=org");
        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration<SearchResult> searchResults = ctx.search("", "(objectclass=*)", sc);

        while (searchResults.hasMoreElements()) {
            SearchResult searchResult = searchResults.next();
            Attributes attributes = searchResult.getAttributes();
            System.out.println("searchResult.attributes: " + attributes) ;
        }
    }
     */
    /*
            Attributes attrs = new BasicAttributes(true);
        attrs.put("NUMERICOID", "1.3.6.1.4.1.18060.0.4.3.3.1");
        attrs.put("NAME", "ship");
        attrs.put("DESC", "An entry which represents a ship");
        attrs.put("SUP", "top");
        attrs.put("STRUCTURAL", "true");

        Attribute must = new BasicAttribute("MUST");
        must.add("cn");
        attrs.put(must);

        Attribute may = new BasicAttribute("MAY");
        may.add("numberOfGuns");
        may.add("numberOfGuns2");
        may.add("description");
        attrs.put(may);

        //add
        schema.createSubcontext("ClassDefinition/ship", attrs);
     */
}