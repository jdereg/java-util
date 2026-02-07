package com.cedarsoftware.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Configures java.util.logging to use a uniform log format similar to
 * popular frameworks like SLF4J/Logback.
 */
public final class LoggingConfig {
    private static final String DATE_FORMAT_PROP = "ju.log.dateFormat";
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    private static boolean initialized = false;

    private LoggingConfig() {
    }

    /**
     * Initialize logging if not already configured.
     * The formatter pattern can be set via system property {@value #DATE_FORMAT_PROP}
     * or by calling {@link #init(String)}.
     * 
     * If running in test environment (detected by system property), uses clean test format.
     */
    public static synchronized void init() {
        if (initialized) {
            return;
        }
        // Check if we're running in test environment
        String testProperty = System.getProperty("surefire.test.class.path");
        boolean isTestEnvironment = testProperty != null || 
                                   System.getProperty("maven.test.skip") != null ||
                                   isCalledFromTestClass();
        
        if (isTestEnvironment) {
            initForTests();
        } else {
            init(System.getProperty(DATE_FORMAT_PROP, DEFAULT_PATTERN));
        }
    }
    
    /**
     * Check if the current call stack includes test classes.
     */
    private static boolean isCalledFromTestClass() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            if (className.contains(".test.") || className.endsWith("Test")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Initialize logging with simple format for tests (no timestamps, no thread names).
     * Use this in test environments to get clean output similar to Maven's format.
     * This method will force the test formatter even if logging was already initialized.
     */
    public static synchronized void initForTests() {
        Logger root = LogManager.getLogManager().getLogger("");
        for (Handler h : root.getHandlers()) {
            if (h instanceof ConsoleHandler) {
                h.setFormatter(new SimpleTestFormatter());
            }
        }
        initialized = true;
    }

    /**
     * Initialize logging with the supplied date pattern if not already configured.
     *
     * @param datePattern pattern passed to {@link SafeSimpleDateFormat}
     */
    public static synchronized void init(String datePattern) {
        if (initialized) {
            return;
        }
        Logger root = LogManager.getLogManager().getLogger("");
        for (Handler h : root.getHandlers()) {
            if (h instanceof ConsoleHandler) {
                h.setFormatter(new UniformFormatter(datePattern));
            }
        }
        initialized = true;
    }

    /**
     * Set the {@link UniformFormatter} on the supplied handler.
     *
     * @param handler the handler to configure
     */
    public static void useUniformFormatter(Handler handler) {
        if (handler != null) {
            handler.setFormatter(new UniformFormatter(System.getProperty(DATE_FORMAT_PROP, DEFAULT_PATTERN)));
        }
    }

    /**
     * Simple formatter for tests that produces clean output similar to Maven's format:
     * {@code [LEVEL] message}
     */
    public static class SimpleTestFormatter extends Formatter {
        @Override
        public String format(LogRecord r) {
            String level = r.getLevel().getName();
            String msg = formatMessage(r);
            StringBuilder sb = new StringBuilder();
            sb.append('[').append(level).append("] ").append(msg);
            if (r.getThrown() != null) {
                StringWriter sw = new StringWriter();
                r.getThrown().printStackTrace(new PrintWriter(sw));
                sb.append(System.lineSeparator()).append(sw);
            }
            sb.append(System.lineSeparator());
            return sb.toString();
        }
    }

    /**
     * Formatter producing logs in the pattern:
     * {@code yyyy-MM-dd HH:mm:ss.SSS [thread] LEVEL logger - message}
     */
    public static class UniformFormatter extends Formatter {
        private final DateFormat df;

        public UniformFormatter() {
            this(DEFAULT_PATTERN);
        }

        public UniformFormatter(String pattern) {
            Objects.requireNonNull(pattern, "pattern");
            this.df = new SafeSimpleDateFormat(pattern);
        }

        @Override
        public String format(LogRecord r) {
            String ts = df.format(new Date(r.getMillis()));
            String level = r.getLevel().getName();
            String logger = r.getLoggerName();
            String msg = formatMessage(r);
            String thread = Thread.currentThread().getName();
            StringBuilder sb = new StringBuilder();
            sb.append(ts)
              .append(' ')
              .append('[').append(thread).append(']')
              .append(' ')
              .append(String.format("%-5s", level))
              .append(' ')
              .append(logger)
              .append(" - ")
              .append(msg);
            if (r.getThrown() != null) {
                StringWriter sw = new StringWriter();
                r.getThrown().printStackTrace(new PrintWriter(sw));
                sb.append(System.lineSeparator()).append(sw);
            }
            sb.append(System.lineSeparator());
            return sb.toString();
        }
    }
}
