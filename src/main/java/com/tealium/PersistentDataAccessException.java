package com.tealium;

import java.io.IOException;

/**
 * Internal convenience exception for handling cases where there is an issue accessing persistent data
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class PersistentDataAccessException extends IOException {
    public PersistentDataAccessException(String message) {
        super(message);
    }

    public PersistentDataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
