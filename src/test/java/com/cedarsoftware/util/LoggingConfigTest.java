package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoggingConfigTest {

    private static String getPattern(Formatter formatter) throws Exception {
        assertTrue(formatter instanceof LoggingConfig.UniformFormatter);
        Field dfField = LoggingConfig.UniformFormatter.class.getDeclaredField("df");
        dfField.setAccessible(true);
        DateFormat df = (DateFormat) dfField.get(formatter);
        return df.toString();
    }

    @Test
    public void testUseUniformFormatter() throws Exception {
        ConsoleHandler handler = new ConsoleHandler();
        System.setProperty("ju.log.dateFormat", "MM/dd");
        try {
            LoggingConfig.useUniformFormatter(handler);
            assertTrue(handler.getFormatter() instanceof LoggingConfig.UniformFormatter);
            assertEquals("MM/dd", getPattern(handler.getFormatter()));
            LoggingConfig.useUniformFormatter(null);  // should not throw
        } finally {
            System.clearProperty("ju.log.dateFormat");
        }
    }

    @Test
    public void testInit() throws Exception {
        Logger root = LogManager.getLogManager().getLogger("");
        Handler[] original = root.getHandlers();
        for (Handler h : original) {
            root.removeHandler(h);
        }
        ConsoleHandler testHandler = new ConsoleHandler();
        root.addHandler(testHandler);

        Field initField = LoggingConfig.class.getDeclaredField("initialized");
        initField.setAccessible(true);
        boolean wasInitialized = initField.getBoolean(null);
        initField.setBoolean(null, false);

        try {
            LoggingConfig.init("MM/dd");
            assertEquals("MM/dd", getPattern(testHandler.getFormatter()));

            LoggingConfig.init("yyyy");
            assertEquals("MM/dd", getPattern(testHandler.getFormatter()));
        } finally {
            root.removeHandler(testHandler);
            for (Handler h : original) {
                root.addHandler(h);
            }
            initField.setBoolean(null, wasInitialized);
        }
    }
}
