package com.tealium;

import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Since a Udo is just a mapping of variable names to values, the
 * Udo class implements the map interface. The values in a Udo can
 * either be a string, or a list of strings. Unfortunately the most
 * specific common superclass is Object, which makes it difficult to
 * represent the type of a udo using existing Java interfaces and types
 * alone. This class works by wrapping a {@code Map<String, Object>}, while
 * providing formatting logic. It implements the {@code Map<String, Object>}
 * interface for compatibility with parsing libraries (and other logic
 * that works on maps).
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class Udo implements Map<String, Object> {
    private final Map<String, Object> wrappedMap;

    /**
     * Construct an empty udo
     */
    public Udo() {
        this.wrappedMap = new HashMap<>();
    }

    /**
     * Use an existing map of data to construct a new udo instance
     *
     * @param data A map of names to values representing udo variables
     */
    public Udo(Map<String, ?> data) {
        this.wrappedMap = new HashMap<>();
        this.putAll(data);
    }

    /**
     * Clear this udo of data.
     * Implements the {@code Map<String, Object>} interface.
     *
     * @see Map
     */
    @Override
    public void clear() {
        this.wrappedMap.clear();
    }

    /**
     * Returns true if this udo contains a variable name (the key).
     * Implements the {@code Map<String, Object>} interface.
     *
     * @param key The variable name to check for
     * @return true if the key exists, false otherwise
     * @see Map
     */
    @Override
    public boolean containsKey(Object key) {
        return this.wrappedMap.containsKey(key);
    }

    /**
     * Returns true if this udo contains a variable value.
     * Implements the {@code Map<String, Object>} interface.
     *
     * @param value The variable value to check for
     * @return true if the value is contained, false otherwise
     * @see Map
     */
    @Override
    public boolean containsValue(Object value) {
        return this.wrappedMap.containsValue(value);
    }

    /**
     * Returns the set of name/value pairs of the variables in this udo
     * Implements the {@code Map<String, Object>} interface.
     *
     * @return A set of name/value pairs of all the variables in this udo
     * @see Map
     */
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return this.wrappedMap.entrySet();
    }

    /**
     * Returns true if this udo is equal to some other object.
     * Implements the {@code Map<String, Object>} interface.
     *
     * @param o The other object to compare to
     * @return true if equal, false otherwise.
     * @see Map
     */
    @Override
    public boolean equals(Object o) {
        return this.wrappedMap.equals(o);
    }

    /**
     * If this udo contains a variable named key,
     * return the corresponding value.
     * Implements the {@code Map<String, Object>} interface.
     *
     * @param key The variable name to look up
     * @return the corresponding value to the variable named key if it exists, else null
     * @see Map
     */
    @Override
    public Object get(Object key) {
        return this.wrappedMap.get(key);
    }

    /**
     * Get the hashcode for this udo.
     * Implements the {@code Map<String, Object>} interface.
     *
     * @return the hashcode
     * @see Map
     */
    @Override
    public int hashCode() {
        return this.wrappedMap.hashCode();
    }

    /**
     * Return true if this udo is empty.
     * Implements the {@code Map<String, Object>} interface.
     *
     * @return true if this udo is empty, false otherwise.
     * @see Map
     */
    @Override
    public boolean isEmpty() {
        return this.wrappedMap.isEmpty();
    }

    /**
     * Return the set of variable names in this udo.
     * Implements the {@code Map<String, Object>} interface.
     *
     * @return the set of variable names in this udo.
     * @see Map
     */
    @Override
    public Set<String> keySet() {
        return this.wrappedMap.keySet();
    }

    /**
     * Add a new variable to this udo.
     * Implements the {@code Map<String, Object>} interface.
     *
     * @param key The name of the variable
     * @param value The value of the variable
     * @return The old value contained by the variable named key if exists, else null.
     * @see Map
     */
    @Override
    public Object put(String key, Object value) {
        Object oldValue = this.get(key);

        List<String> stringList = coerceIntoStringList(value);

        if (stringList == null) {
            this.wrappedMap.put(key, value.toString());
        } else {
            this.wrappedMap.put(key, stringList);
        }

        return oldValue;
    }

    /**
     * Add multiple variables to this udo.
     * Implements the {@code Map<String, Object>} interface.
     *
     * @param m A map containing the new data.
     * @see Map
     */
    @Override
    public void putAll(Map<? extends String, ?> m) {
        for (Map.Entry<? extends String, ?> entry : m.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();

            if ((value == null) || (key == null) || (key.isEmpty())) {
                continue;
            }

            this.put(key, value);
        }
    }

    /**
     * Remove the variable named key from this udo. Return its value.
     * Implements the {@code Map<String, Object>} interface.
     *
     * @param key The variable name to remove
     * @return value of the removed variable.
     * @see Map
     */
    @Override
    public Object remove(Object key) {
        return this.wrappedMap.remove(key);
    }

    /**
     * Returns the number of variables contained in this udo.
     * Implements the {@code Map<String, Object>} interface.
     *
     * @return number of variables contained in this udo
     * @see Map
     */
    @Override
    public int size() {
        return this.wrappedMap.size();
    }

    /**
     * Return set of variable values contained in this udo.
     * Implements the {@code Map<String, Object>} interface.
     *
     * @return set of variable names
     * @see Map
     */
    @Override
    public Collection<Object> values() {
        return this.wrappedMap.values();
    }

    /**
     * @param source
     *            The object to be copied into a String array of non-null
     *            values.
     * @return An array copy or null if the source is null, or not coerceable
     *         into an array.
     */
    private static List<String> coerceIntoStringList(Object source) {
        return source instanceof Collection ?
                coerceCollection((Collection<?>) source) : coerceArray(source);
    }

    /**
     * Converts a {@link Collection} into a String array with non-null elements.
     *
     * @param collection The collection to convert
     * @return List of Strings of non-null values, otherwise null if collection was
     *         null.
     */
    private static List<String> coerceCollection(Collection<?> collection) {

        if (collection == null) {
            return null;
        }

        final String[] array = new String[collection.size()];
        int count = 0;

        Object value;
        for (Object o : collection) {
            if ((value = o) != null) {
                array[count++] = value.toString();
            }
        }

        // Ensure entire array is non-null
        return Arrays.asList(array.length == count ? array : Arrays.copyOf(array, count));
    }

    /**
     * Converts a native array into a String list with non-null elements.
     *
     * @param array Array of strings to convert to list of strings
     * @return List of Strings of non-null values, otherwise null if array was null
     *         or not an array.
     */
    private static List<String> coerceArray(Object array) {

        if (array == null || !array.getClass().isArray()) {
            return null;
        }

        final String[] copy = new String[Array.getLength(array)];
        int count = 0;

        Object value;
        for (int i = 0; i < copy.length; i++) {
            if ((value = Array.get(array, i)) != null) {
                copy[count++] = value.toString();
            }
        }

        // Ensure entire array is non-null
        return Arrays.asList(copy.length == count ? copy : Arrays.copyOf(copy, count));
    }

    /**
     * Decodes a Udo object from a JSON encoded string.
     *
     * @param json JSON encoded Udo
     * @return JSON decoded Udo
     * @throws UdoDeserializationException if JSON cannot be parsed
     */
    public static Udo fromJson(String json) throws UdoDeserializationException {
        try {
            Map<String, Object> map = JSON.std.mapFrom(json);
            return new Udo(map);
        } catch(IOException e) {
            throw new UdoDeserializationException("Could not deserialize udo from json", e);
        }
    }

    /**
     * Encodes a Udo into a JSON encoded string.
     *
     * @return Udo as JSON encoded string
     * @throws UdoSerializationException if this udo can't be serialized to JSON
     */
    public String toJson() throws UdoSerializationException {
        try {
            return JSON.std.asString(this);
        } catch (IOException e) {
            throw new UdoSerializationException("Error serializing udo to json", e);
        }
    }

    /**
     * Decodes a Udo object from the legacy percent encoded persistent data format
     * used before SDK 1.3.0 (JSON since then). Kept so persistent files written by
     * those older versions can still be loaded.
     *
     * Escapes are decoded as UTF-8; byte sequences that aren't valid UTF-8 (e.g. {@code %FF})
     * are replaced with U+FFFD rather than rejected.
     *
     * @param persistentText the percent encoded udo, in the legacy {@code key=value&key2=a,b} format
     * @return the decoded Udo
     * @throws UdoDeserializationException if a non-empty {@code &}-separated entry isn't a single {@code key=value} pair
     * @throws IllegalArgumentException if persistentText contains an invalid percent escape (e.g. {@code %ZZ})
     */
    public static Udo fromPercentEncoded(String persistentText) throws UdoDeserializationException {
        String encodedKey, encodedValue, decodedKey;
        String[] keyAndValue, encodedValueElements, decodedValueElements;

        Udo decodedUdo = new Udo();

        for (String pair : persistentText.split("&")) {

            if (pair.isEmpty()) {
                continue;
            }

            keyAndValue = pair.split("=");
            if (keyAndValue.length != 2) {
                throw new UdoDeserializationException("Keys and values must come in pairs when parsing a percent encoded udo");
            } else {
                encodedKey = keyAndValue[0];
                encodedValue = keyAndValue[1];
            }

            try {
                decodedKey = URLDecoder.decode(encodedKey, StandardCharsets.UTF_8.name());

                if (encodedValue.contains(",")) {
                    encodedValueElements = encodedValue.split(",");
                    decodedValueElements = new String[encodedValueElements.length];
                    for (int i = 0; i < encodedValueElements.length; i++) {
                        decodedValueElements[i] = URLDecoder.decode(encodedValueElements[i], StandardCharsets.UTF_8.name());
                    }
                    decodedUdo.put(decodedKey, decodedValueElements);
                } else {
                    decodedUdo.put(decodedKey, URLDecoder.decode(encodedValue, StandardCharsets.UTF_8.name()));
                }
            } catch (UnsupportedEncodingException e) {
                throw new UdoDeserializationException("Could not parse udo from string that is not encoded as utf 8", e);
            }
        }
        return decodedUdo;
    }
}
