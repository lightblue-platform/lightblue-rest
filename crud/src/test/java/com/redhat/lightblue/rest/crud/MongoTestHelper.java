package com.redhat.lightblue.rest.crud;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.redhat.lightblue.metadata.mongo.MongoMetadata;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class MongoTestHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoTestHelper.class);

    public static final String MONGO_HOST = "localhost";
    public static final int MONGO_PORT = 27777;
    public static final String IN_MEM_CONNECTION_URL = MONGO_HOST + ":" + MONGO_PORT;
    public static final String DB_NAME = "testmetadata";

    public static MongodExecutable mongodExe;
    public static MongodProcess mongod;
    public static Mongo mongo;
    public static DB db;
    public static boolean started = false;

    static {
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
                            .net(new Net(MONGO_PORT, Network.localhostIsIPv6()))
                            .build()
            );
            try {
                mongod = mongodExe.start();
            } catch (Throwable t) {
                // try again, could be killed breakpoint in IDE
                mongod = mongodExe.start();
            }
            mongo = new Mongo(IN_MEM_CONNECTION_URL);

            MongoConfiguration config = new MongoConfiguration();
            // disable ssl for test (enabled by default)
            config.setDatabase(DB_NAME);
            config.setSsl(Boolean.FALSE);
            config.addServerAddress(MONGO_HOST, MONGO_PORT);

            db = config.getDB();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    super.run();
                    clearDatabase();
                    statDatabase();
                }

            });
            started = true;
        } catch (IOException e) {
            throw new java.lang.Error(e);
        }
    }


    public static void clearDatabase() {
        try{ db.getCollection(MongoMetadata.DEFAULT_METADATA_COLLECTION).remove(new BasicDBObject());}catch (Exception e) {LOGGER.debug("Exception during the database cleaning", e);}
        try{ mongo.getDB("mongo").getCollection("metadata").remove(new BasicDBObject());}catch (Exception e) {LOGGER.debug("Exception during the database cleaning", e);}

        try{ mongo.dropDatabase(DB_NAME);}catch (Exception e) {LOGGER.debug("Exception during the database cleaning", e);}
        try{ mongo.dropDatabase("local");}catch (Exception e) {LOGGER.debug("Exception during the database cleaning", e);}
        try{ mongo.dropDatabase("admin");}catch (Exception e) {LOGGER.debug("Exception during the database cleaning", e);}
        try{ mongo.dropDatabase("mongo");}catch (Exception e) {LOGGER.debug("Exception during the database cleaning", e);}

        try{ mongo.getDB(DB_NAME).dropDatabase();}catch (Exception e) {LOGGER.debug("Exception during the database cleaning", e);}
        try{ mongo.getDB("local").dropDatabase();}catch (Exception e) {LOGGER.debug("Exception during the database cleaning", e);}
        try{ mongo.getDB("admin").dropDatabase();}catch (Exception e) {LOGGER.debug("Exception during the database cleaning", e);}
        try{ mongo.getDB("mongo").dropDatabase();}catch (Exception e) {LOGGER.debug("Exception during the database cleaning", e);}

    }

    public static void stopDatabase() {
        if (started) {
            mongod.stop();
            mongod.stopInternal();
            mongodExe.stop();
        }
        started = false;
    }


    public static void statDatabase() {
        if(!started) {
            try {
                try {
                    mongod = mongodExe.start();
                } catch (Throwable t) {
                    // try again, could be killed breakpoint in IDE
                    mongod = mongodExe.start();
                }
                mongo = new Mongo(IN_MEM_CONNECTION_URL);
            } catch (IOException e) {
                throw new java.lang.Error(e);
            }
        }
    }
}