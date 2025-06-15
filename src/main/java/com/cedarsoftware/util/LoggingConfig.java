package com.cedarsoftware.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    private static volatile boolean initialized = false;

    private LoggingConfig() {
    }

    /**
     * Initialize logging if not already configured.
     */
    public static synchronized void init() {
        if (initialized) {
            return;
        }
        Logger root = LogManager.getLogManager().getLogger("");
        for (Handler h : root.getHandlers()) {
            if (h instanceof ConsoleHandler) {
                h.setFormatter(new UniformFormatter());
            }
        }
        initialized = true;
    }

    private static class UniformFormatter extends Formatter {
        private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        @Override
        public synchronized String format(LogRecord r) {
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
