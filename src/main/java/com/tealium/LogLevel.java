package com.tealium;

/**
 * Data class for different log levels
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public enum LogLevel {
    NONE(3), ERRORS(2), WARNINGS(1), VERBOSE(0);

    private final int priority;

    private LogLevel(int priority) {
        this.priority = priority;
    }

    int getPriority() {
        return priority;
    }
}
