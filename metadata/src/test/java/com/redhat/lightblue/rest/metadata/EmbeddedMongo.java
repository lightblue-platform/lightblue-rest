package com.redhat.lightblue.rest.metadata;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.redhat.lightblue.mongo.config.MongoConfiguration;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.io.IStreamProcessor;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.runtime.Network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Test class that encapsulates the in memory mongo DB used for unit tests.
 *
 * Created by nmalik on 12/16/14.
 */
public final class EmbeddedMongo {

    private static EmbeddedMongo instance;

    private final String mongoHostname;
    private final int mongoPort;
    private final String dbName;

    private EmbeddedMongo(String mongoHostname, int mongoPort, String dbName) {
        this.mongoHostname = mongoHostname;
        this.mongoPort = mongoPort;
        this.dbName = dbName;
    }

    protected static class FileStreamProcessor implements IStreamProcessor {
        private FileOutputStream outputStream;

        public FileStreamProcessor(File file) throws FileNotFoundException {
            outputStream = new FileOutputStream(file);
        }

        @Override
        public void process(String block) {
            try {
                outputStream.write(block.getBytes());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void onProcessed() {
            try {
                outputStream.close();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

    }

    private MongodExecutable mongodExe;
    private MongodProcess mongod;
    private Mongo mongo;
    private DB db;

    public static final EmbeddedMongo getInstance() {
        if (instance == null) {
            bootstrap();
        }
        return instance;
    }

    private static final synchronized void bootstrap() {
        if (instance == null) {
            EmbeddedMongo temp = new EmbeddedMongo("localhost", 27777, "mongo");
            temp.initialize();
            instance = temp;
        }
    }

    private void initialize() {
        if (db != null) {
            return;
        }

        System.setProperty("mongodb.database", dbName);
        System.setProperty("mongodb.host", mongoHostname);
        System.setProperty("mongodb.port", String.valueOf(mongoPort));

        try {
            IStreamProcessor mongodOutput = Processors.named("[mongod>]",
                    new FileStreamProcessor(File.createTempFile("mongod", "log")));
            IStreamProcessor mongodError = new FileStreamProcessor(File.createTempFile("mongod-error", "log"));
            IStreamProcessor commandsOutput = Processors.namedConsole("[console>]");

            IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                    .defaults(Command.MongoD)
                    .processOutput(new ProcessOutput(mongodOutput, mongodError, commandsOutput))
                    .build();

            MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);
            mongodExe = runtime.prepare(
                    new MongodConfigBuilder()
                            .version(de.flapdoodle.embed.mongo.distribution.Version.V2_6_0)
                            .net(new Net(mongoPort, Network.localhostIsIPv6()))
                            .build()
            );
            try {
                mongod = mongodExe.start();
            } catch (Throwable t) {
                // try again, could be killed breakpoint in IDE
                mongod = mongodExe.start();
            }
            mongo = new Mongo(mongoHostname + ":" + mongoPort);

            MongoConfiguration config = new MongoConfiguration();
            config.setDatabase(dbName);
            // disable ssl for test (enabled by default)
            config.setSsl(Boolean.FALSE);
            config.addServerAddress(mongoHostname, mongoPort);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    super.run();
                    if (mongod != null) {
                        mongod.stop();
                        mongodExe.stop();
                    }
                    db = null;
                    mongo = null;
                    mongod = null;
                    mongodExe = null;
                }

            });

            db = config.getDB();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    /**
     * Drops the database.  Use between test executions.
     */
    public void reset() {
        if (mongo != null) {
            mongo.dropDatabase(dbName);
        }
    }

    public DB getDB() {
        return db;
    }

    public void dropCollection(String name) {
        DBCollection coll = db.getCollection(name);
        if (coll != null) {
            coll.drop();
        }
    }
}
