package com.tealium;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Internal convenience exception for a collect call that received a non-2xx response
 * or an "x-error" response header.
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
class FailedRequestException extends RuntimeException {

    /** The most characters of a response body kept in the exception message. */
    static final int MAX_BODY_MESSAGE_CHARS = 256;

    public final int statusCode;

    public final Map<String, List<String>> headers;

    /**
     * @param statusCode The response's HTTP status code
     * @param xError The response's "x-error" header value, or null if absent
     * @param body The response body's bytes, or null/empty if none was read
     * @param headers The response headers
     */
    FailedRequestException(int statusCode, String xError, byte[] body, Map<String, List<String>> headers) {
        super(buildMessage(statusCode, xError, body));
        this.statusCode = statusCode;
        this.headers = headers;
    }

    private static String buildMessage(int statusCode, String xError, byte[] body) {
        StringBuilder message = new StringBuilder("Response code ").append(statusCode);

        if (xError != null) {
            message.append("; x-error: ").append(xError);
        }

        if (body != null && body.length > 0) {
            message.append("; body: ").append(formatBody(body));
        }

        return message.toString();
    }

    /**
     * Decodes a response body as UTF-8, collapses each {@code \r\n}, {@code \r} or {@code \n} into a single
     * space, and truncates the result to {@value #MAX_BODY_MESSAGE_CHARS} characters without splitting a
     * surrogate pair, appending "…" only when the collapsed text was actually longer than that.
     *
     * @param body The response body's bytes; must be non-null and non-empty
     * @return The body text to use in the exception message
     */
    static String formatBody(byte[] body) {
        String decoded = new String(body, StandardCharsets.UTF_8);
        StringBuilder collapsed = new StringBuilder(decoded.length());

        for (int i = 0; i < decoded.length(); i++) {
            char c = decoded.charAt(i);
            if (c == '\r') {
                collapsed.append(' ');
                if (i + 1 < decoded.length() && decoded.charAt(i + 1) == '\n') {
                    i++;
                }
            } else if (c == '\n') {
                collapsed.append(' ');
            } else {
                collapsed.append(c);
            }
        }

        if (collapsed.length() <= MAX_BODY_MESSAGE_CHARS) {
            return collapsed.toString();
        }

        int cut = MAX_BODY_MESSAGE_CHARS;
        if (Character.isHighSurrogate(collapsed.charAt(cut - 1)) && Character.isLowSurrogate(collapsed.charAt(cut))) {
            cut--;
        }

        return collapsed.substring(0, cut) + "…";
    }
}
