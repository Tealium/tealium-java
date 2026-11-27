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
            message.append("; x-error: ").append(replaceControlCharacters(xError));
        }

        if (body != null && body.length > 0) {
            message.append("; body: ").append(formatBody(body));
        }

        return message.toString();
    }

    /**
     * Decodes a response body as UTF-8, replaces control characters (see
     * {@link #replaceControlCharacters(String)}) with spaces, and truncates the result to
     * {@value #MAX_BODY_MESSAGE_CHARS} characters without splitting a surrogate pair, appending "…" only when
     * the cleaned text was actually longer than that.
     *
     * @param body The response body's bytes; must be non-null and non-empty
     * @return The body text to use in the exception message
     */
    static String formatBody(byte[] body) {
        String decoded = new String(body, StandardCharsets.UTF_8);
        String cleaned = replaceControlCharacters(decoded);

        if (cleaned.length() <= MAX_BODY_MESSAGE_CHARS) {
            return cleaned;
        }

        int cut = MAX_BODY_MESSAGE_CHARS;
        if (Character.isHighSurrogate(cleaned.charAt(cut - 1)) && Character.isLowSurrogate(cleaned.charAt(cut))) {
            cut--;
        }

        return cleaned.substring(0, cut) + "…";
    }

    /**
     * Replaces each {@code \r\n} pair, and every other character for which {@link Character#isISOControl(char)}
     * is true (the C0 controls, including tab, CR and LF; DEL; and the C1 controls), with a single space.
     * Nothing else changes: there's no trimming and no collapsing of runs of spaces. This keeps control
     * characters from a server's response (e.g. terminal escape sequences) out of logs and callbacks.
     *
     * @param text The text to clean
     * @return The text with control characters replaced by spaces
     */
    static String replaceControlCharacters(String text) {
        StringBuilder cleaned = new StringBuilder(text.length());

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                cleaned.append(' ');
                i++;
            } else if (Character.isISOControl(c)) {
                cleaned.append(' ');
            } else {
                cleaned.append(c);
            }
        }

        return cleaned.toString();
    }
}
