package com.tealium;

/**
 * Exception for cases where there is an issue serializing a Udo into a String
 *
 * Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class UdoSerializationException extends Exception {
    public UdoSerializationException(String message) {
        super(message);
    }

    public UdoSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
