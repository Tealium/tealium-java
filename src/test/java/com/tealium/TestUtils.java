package com.tealium;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper methods for testing
 *
 * Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class TestUtils {
    public static Map<String, Object> stringStringMap() {

        Map<String, Object> map = new HashMap<>();

        map.put("key", "value");
        map.put("key1", "value1");
        map.put("key_2", "value_2");
        map.put("a key", "a value");
        map.put("specialKey", "$-_.+!*'(),");

        return map;

    }

    public static Map<String, Object> stringArrayMap() {

        Map<String, Object> map = new HashMap<>();

        map.put("array1", new String[] { "foo", "bar,\r\n\tand some extra stuff" });
        map.put("array2", new String[] { "123", "true", "!@#$%^&*()_+"});

        return map;

    }

    public static Map<String, Object> stringMixedAcceptableMap() {

        Map<String, Object> map = new HashMap<>();

        map.put("key_2", "value_2");
        map.put("a key", "a value");
        map.put("array1", new String[] { "foo", "bar,\r\n\tand some extra stuff" });

        return map;

    }

    public static TextStorage dummyTextStorage() {
        return new TextStorage(null) {
            private String text = null;

            @Override
            public String readText() throws IOException {
                if(this.text == null) throw new IOException("Can't access text that hasn't been written");
                return text;
            }

            @Override
            public void writeText(String text) throws IOException {
                if(text == null) throw new IOException("Can't write null text");
                this.text = text;
            }

            @Override
            public Boolean exists() {
                return this.text != null;
            }
        };
    }

    public static PersistentUdo dummyPersistentUdo() {
        return dummyPersistentUdo(null);
    }

    public static PersistentUdo dummyPersistentUdo(final Udo initialUdo) {
        return new PersistentUdo(null) {
            private Udo udo = initialUdo;

            @Override
            public Udo readOrCreateUdo(Udo defaultData) {
                if(this.udo == null) this.udo = defaultData;
                return this.udo;
            }

            @Override
            public void writeData(Udo data) throws UdoSerializationException {
                if(data == null) throw new UdoSerializationException("Cannot write null udo");
                this.udo = data;
            }

            @Override
            public Boolean exists() {
                return this.udo != null;
            }
        };
    }
}
