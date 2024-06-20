package com.tealium;

import java.util.List;
import java.util.Map;

/**
 * Internal convenience exception for handling failed URL connections.
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
class FailedRequestException extends RuntimeException {

    private final int statusCode;

    public Map<String, List<String>> headers;
    public String customMessage;

    public String getCustomMessage() {
        return customMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public FailedRequestException(int statusCode) {
        this.statusCode = statusCode;
    }

    public FailedRequestException(int statusCode, String message, Map<String, List<String>> headers) {
        this.statusCode = statusCode;
        this.customMessage = message;
        this.headers = headers;
    }
}
