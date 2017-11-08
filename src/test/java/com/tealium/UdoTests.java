package com.tealium;

import com.fasterxml.jackson.jr.ob.JSON;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertTrue;

/**
 * Test logic related to Udo
 *
 * Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class UdoTests {
    @Test
    public void nonEmptyUdoEncodesProperly() throws IOException, UdoSerializationException {
        Map<String, Object> map1 = TestUtils.stringStringMap();
        Udo udo1 = new Udo(map1);
        String json1 = udo1.toJson();
        Map<String, Object> resultMap1 = JSON.std.mapFrom(json1);
        Udo resultUdo1 = new Udo(resultMap1);

        assertTrue(udo1.equals(resultUdo1));

        Map<String, Object> map2 = TestUtils.stringArrayMap();
        Udo udo2 = new Udo(map2);
        String json2 = udo2.toJson();
        Map<String, Object> resultMap2 = JSON.std.mapFrom(json2);
        Udo resultUdo2 = new Udo(resultMap2);

        assertTrue(udo2.equals(resultUdo2));

        Map<String, Object> map3 = TestUtils.stringMixedAcceptableMap();
        Udo udo3 = new Udo(map3);
        String json3 = udo3.toJson();
        Map<String, Object> resultMap3 = JSON.std.mapFrom(json3);
        Udo resultUdo3 = new Udo(resultMap3);

        assertTrue(udo3.equals(resultUdo3));
    }

    @Test
    public void isEmptyReturnsTrueOnAnyEmptyUdo() throws IOException {
        // several ways to construct empty Udo objects
        Udo a = new Udo();

        Udo b = new Udo(new HashMap<String, Object>());

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

        Udo b = new Udo(new HashMap<String, Object>());

        Map<String, Object> m = JSON.std.mapFrom("{}");
        Udo c = new Udo(m);

        // Go through some permutations
        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(a.equals(c));
        assertTrue(b.equals(a));
        assertTrue(b.equals(b));
        assertTrue(b.equals(c));
        assertTrue(c.equals(a));
        assertTrue(c.equals(b));
        assertTrue(c.equals(c));
    }
}
