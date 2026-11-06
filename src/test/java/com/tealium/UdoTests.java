package com.tealium;

import com.fasterxml.jackson.jr.ob.JSON;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test logic related to Udo
 *
 * Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class UdoTests {
    @Test
    public void nonEmptyUdoRoundTripsThroughJson() throws IOException, UdoSerializationException {
        Map<String, Object> map1 = TestUtils.stringStringMap();
        Udo udo1 = new Udo(map1);
        String json1 = udo1.toJson();
        Map<String, Object> resultMap1 = JSON.std.mapFrom(json1);
        Udo resultUdo1 = new Udo(resultMap1);

        assertEquals(udo1, resultUdo1);

        Map<String, Object> map2 = TestUtils.stringArrayMap();
        Udo udo2 = new Udo(map2);
        String json2 = udo2.toJson();
        Map<String, Object> resultMap2 = JSON.std.mapFrom(json2);
        Udo resultUdo2 = new Udo(resultMap2);

        assertEquals(udo2, resultUdo2);

        Map<String, Object> map3 = TestUtils.stringMixedAcceptableMap();
        Udo udo3 = new Udo(map3);
        String json3 = udo3.toJson();
        Map<String, Object> resultMap3 = JSON.std.mapFrom(json3);
        Udo resultUdo3 = new Udo(resultMap3);

        assertEquals(udo3, resultUdo3);
    }

    @Test
    public void isEmptyReturnsTrueOnAnyEmptyUdo() throws IOException {
        // several ways to construct empty Udo objects
        Udo a = new Udo();

        Udo b = new Udo(new HashMap<>());

        Map<String, Object> m = JSON.std.mapFrom("{}");
        Udo c = new Udo(m);

        assertTrue(a.isEmpty());
        assertTrue(b.isEmpty());
        assertTrue(c.isEmpty());
    }

    @Test
    public void emptyUdoShouldBeEqualToAnyOtherEmptyUdo() throws IOException {
        // several ways to construct empty Udo objects
        Udo a = new Udo();

        Udo b = new Udo(new HashMap<>());

        Map<String, Object> m = JSON.std.mapFrom("{}");
        Udo c = new Udo(m);

        // Go through some permutations
        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(a, c);
        assertEquals(b, a);
        assertEquals(b, b);
        assertEquals(b, c);
        assertEquals(c, a);
        assertEquals(c, b);
        assertEquals(c, c);
    }

    @Test
    public void fromPercentEncodedDecodesPlainListAndEncodedValues() throws UdoDeserializationException {
        Udo decodedUdo = Udo.fromPercentEncoded("plainKey=plainValue&spacedKey=hello%20world&greek=alpha,beta,gamma");

        Udo expectedUdo = new Udo();
        expectedUdo.put("plainKey", "plainValue");
        expectedUdo.put("spacedKey", "hello world");
        expectedUdo.put("greek", new String[] { "alpha", "beta", "gamma" });

        assertEquals(expectedUdo, decodedUdo);
    }

    @Test(expected = UdoDeserializationException.class)
    public void fromPercentEncodedThrowsOnMalformedPairMissingEqualsSign() throws UdoDeserializationException {
        Udo.fromPercentEncoded("key");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromPercentEncodedThrowsIllegalArgumentExceptionOnInvalidHexEscape() throws UdoDeserializationException {
        Udo.fromPercentEncoded("key=%ZZ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromPercentEncodedThrowsIllegalArgumentExceptionOnIncompleteTrailingEscape() throws UdoDeserializationException {
        Udo.fromPercentEncoded("key=abc%2");
    }

    @Test
    public void fromPercentEncodedDecodesInvalidUtf8EscapeToReplacementCharacter() throws UdoDeserializationException {
        Udo decodedUdo = Udo.fromPercentEncoded("key=%FF");

        Udo expectedUdo = new Udo();
        expectedUdo.put("key", "\uFFFD");

        // an escape that isn't valid UTF-8 is replaced rather than rejected
        assertEquals(expectedUdo, decodedUdo);
    }
}
