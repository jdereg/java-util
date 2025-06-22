package com.cedarsoftware.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Objects;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.cedarsoftware.util.SafeSimpleDateFormat;

/**
 * Configures java.util.logging to use a uniform log format similar to
 * popular frameworks like SLF4J/Logback.
 */
public final class LoggingConfig {
    private static final String DATE_FORMAT_PROP = "ju.log.dateFormat";
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    private static volatile boolean initialized = false;

    private LoggingConfig() {
    }

    /**
     * Initialize logging if not already configured.
     * The formatter pattern can be set via system property {@value #DATE_FORMAT_PROP}
     * or by calling {@link #init(String)}.
     */
    public static synchronized void init() {
        init(System.getProperty(DATE_FORMAT_PROP, DEFAULT_PATTERN));
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
