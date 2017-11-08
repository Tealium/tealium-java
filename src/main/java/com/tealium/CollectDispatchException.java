package com.tealium;

/**
 * Exception for cases where there is a problem with a collect dispatch
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class CollectDispatchException extends Exception {
    public CollectDispatchException(String message) {
        super(message);
    }

    public CollectDispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
