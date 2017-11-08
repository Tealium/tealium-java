package com.tealium;

import javax.xml.ws.http.HTTPException;
import java.util.List;
import java.util.Map;

/**
 * Internal convenience exception for handling failed URL connections.
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
class FailedRequestException extends HTTPException {

    public Map<String, List<String>> headers;
    public String customMessage;

    public String getCustomMessage() {
        return customMessage;
    }

    public FailedRequestException(int responseCode) { super(responseCode); }

    public FailedRequestException(int responseCode, String message, Map<String, List<String>> headers) {

        super(responseCode);
        this.customMessage = message;
        this.headers = headers;
    }
}
