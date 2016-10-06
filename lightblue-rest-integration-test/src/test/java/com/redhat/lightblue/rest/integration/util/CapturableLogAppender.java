package com.redhat.lightblue.rest.integration.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Simple extension of ConsoleAppender so that generated LoggingEvents can be unit tested.
 *
 * @author dcrissman
 *
 */
public class CapturableLogAppender extends AppenderSkeleton {

    private final List<LoggingEvent> events = new ArrayList<>();

    public static CapturableLogAppender createInstanceFor(String name) {
        Logger logger = Logger.getLogger(name);
        return createInstanceFor(logger);
    }

    public static CapturableLogAppender createInstanceFor(Class<?> clazz) {
        Logger logger = Logger.getLogger(clazz);
        return createInstanceFor(logger);
    }

    public static CapturableLogAppender createInstanceFor(Logger logger) {
        CapturableLogAppender appender = new CapturableLogAppender();
        logger.addAppender(appender);
        return appender;
    }

    public static void removeAppenderFor(Appender appender, String name) {
        Logger logger = Logger.getLogger(name);
        logger.removeAppender(appender);
    }

    public static void removeAppenderFor(Appender appender, Class<?> clazz) {
        Logger logger = Logger.getLogger(clazz);
        logger.removeAppender(appender);
    }

    @Override
    protected void append(LoggingEvent event) {
        event.getMDCCopy();
        events.add(event);
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    public void close() {
        clear();
    }

    public List<LoggingEvent> getEvents() {
        return new ArrayList<>(events);
    }

    public void clear() {
        events.clear();
    }

}
