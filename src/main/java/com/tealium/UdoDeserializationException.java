package com.tealium;

/**
 * Exception for cases where there is an issue deserializing a string into a Udo
 *
 * Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class UdoDeserializationException extends Exception {
    public UdoDeserializationException(String message) {
        super(message);
    }

    public UdoDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
