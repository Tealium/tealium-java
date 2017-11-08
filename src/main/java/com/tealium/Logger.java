package com.tealium;

import java.io.PrintStream;

// Java convention for loggers is to expose methods based off of level, so logger.error(...), logger.warn(...), logger.debug(...)
/**
 * Tealium logger for debugging.
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
final class Logger {

    private final LogLevel level;
    private final PrintStream out;

    public LogLevel getLevel() {
        return level;
    }

    public Logger(LogLevel level) {
        super();
        this.level = level;
        out = System.out;
    }

    public boolean isLogging(LogLevel level) {
        return level.getPriority() >= this.level.getPriority();
    }

    /**
     * For printing at console logs related to just the Tealium object.
     * 
     * @param message
     *            String description.
     * @param level
     *            Level of message.
     */
    public String log(String message, LogLevel level) {
        if (isLogging(level)) {
            out.println(message);
            return message;
        }
        return null;
    };

    public void log(Throwable t, LogLevel level) {
        if (isLogging(level)) {
            out.println(t);
            t.printStackTrace(out);
        }
    }
}
