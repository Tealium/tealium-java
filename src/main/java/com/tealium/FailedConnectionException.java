package com.tealium;

/**
 * Exception for cases where failed to create connection to endpoint on dispatching collect calls
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class FailedConnectionException extends Exception {
    public FailedConnectionException(String message) {
        super(message);
    }

    public FailedConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
