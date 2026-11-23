package com.tealium;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

/**
 * Tests how FailedRequestException builds its message, in particular the response body formatting.
 */
public class FailedRequestExceptionTests {

    @Test
    public void testMessageWithStatusCodeOnly() {
        FailedRequestException e = new FailedRequestException(500, null, null, null);

        assertEquals("Response code 500", e.getMessage());
    }

    @Test
    public void testMessageWithXError() {
        FailedRequestException e = new FailedRequestException(200, "invalid account", null, null);

        assertEquals("Response code 200; x-error: invalid account", e.getMessage());
    }

    @Test
    public void testMessageWithBody() {
        FailedRequestException e =
                new FailedRequestException(500, null, "oops".getBytes(StandardCharsets.UTF_8), null);

        assertEquals("Response code 500; body: oops", e.getMessage());
    }

    @Test
    public void testMessageWithXErrorAndBody() {
        FailedRequestException e =
                new FailedRequestException(200, "invalid account", "rejected".getBytes(StandardCharsets.UTF_8), null);

        assertEquals("Response code 200; x-error: invalid account; body: rejected", e.getMessage());
    }

    @Test
    public void testMessageWithEmptyBodyOmitsBodyPart() {
        FailedRequestException e = new FailedRequestException(500, null, new byte[0], null);

        assertEquals("Response code 500", e.getMessage());
    }

    @Test
    public void testMessageWithWhitespaceOnlyBodyIncludesBodyPart() {
        FailedRequestException e = new FailedRequestException(500, null, "   ".getBytes(StandardCharsets.UTF_8), null);

        assertEquals("Response code 500; body:    ", e.getMessage());
    }

    // formatBody

    @Test
    public void testFormatBodyReplacesLineBreaksWithSingleSpace() {
        byte[] body = "line1\r\nline2\rline3\nline4".getBytes(StandardCharsets.UTF_8);

        assertEquals("line1 line2 line3 line4", FailedRequestException.formatBody(body));
    }

    @Test
    public void testFormatBodyExactly256CharsHasNoEllipsis() {
        String text = repeat('x', FailedRequestException.MAX_BODY_MESSAGE_CHARS);

        assertEquals(text, FailedRequestException.formatBody(text.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testFormatBodyOver256CharsIsTruncatedWithEllipsis() {
        String text = repeat('x', FailedRequestException.MAX_BODY_MESSAGE_CHARS + 44);

        String expected = repeat('x', FailedRequestException.MAX_BODY_MESSAGE_CHARS) + "…";
        assertEquals(expected, FailedRequestException.formatBody(text.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testFormatBodyDoesNotSplitSurrogatePairAtTruncationBoundary() {
        // 255 ASCII chars, then a surrogate pair (one codepoint, two chars) straddling indexes 255/256.
        String prefix = repeat('x', FailedRequestException.MAX_BODY_MESSAGE_CHARS - 1);
        String emoji = "😀"; // U+1F600 GRINNING FACE, a surrogate pair
        String text = prefix + emoji + repeat('y', 10);

        String expected = prefix + "…";
        assertEquals(expected, FailedRequestException.formatBody(text.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testFormatBodyDecodesMultibyteUtf8() {
        String text = "café éèà üñ";

        assertEquals(text, FailedRequestException.formatBody(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static String repeat(char c, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}
