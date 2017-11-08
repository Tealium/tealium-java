package com.tealium;

import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Since a Udo is just a mapping of variable names to values, the
 * Udo class implements the map interface. The values in a Udo can
 * either be a string, or a list of strings. Unfortunately the most
 * specific common superclass is Object, which makes it difficult to
 * represent the type of a udo using existing Java interfaces and types
 * alone. This class works by wrapping a Map<String, Object>, while
 * providing formatting logic. It implements the Map<String, Object>
 * interface for compatibility with parsing libraries (and other logic
 * that works on maps).
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class Udo implements Map<String, Object> {
    private Map<String, Object> wrappedMap;

    /**
     * Construct an empty udo
     */
    public Udo() {
        this.wrappedMap = new HashMap<String, Object>();
    }

    /**
     * Use an existing map of data to construct a new udo instance
     *
     * @param data A map of names to values representing udo variables
     */
    public Udo(Map<String, ? extends Object> data) {
        this.wrappedMap = new HashMap<String, Object>();
        this.putAll(data);
    }

    /**
     * Clear this udo of data.
     * Implements the Map<String, Object> interface.
     *
     * @see Map
     */
    @Override
    public void clear() {
        this.wrappedMap.clear();
    }

    /**
     * Returns true if this udo contains a variable name (the key).
     * Implements the Map<String, Object> interface.
     *
     * @param key
     * @return true if the key exists, false otherwise
     * @see Map
     */
    @Override
    public boolean containsKey(Object key) {
        return this.wrappedMap.containsKey(key);
    }

    /**
     * Returns true if this udo contains a variable value.
     * Implements the Map<String, Object> interface.
     *
     * @param value
     * @return true if the value is contained, false otherwise
     * @see Map
     */
    @Override
    public boolean containsValue(Object value) {
        return this.wrappedMap.containsValue(value);
    }

    /**
     * Returns the set of name/value pairs of the variables in this udo
     * Implements the Map<String, Object> interface.
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
     * Implements the Map<String, Object> interface.
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
     * Implements the Map<String, Object> interface.
     *
     * @param key
     * @return the corresponding value to the variable named key if it exists, else null
     * @see Map
     */
    @Override
    public Object get(Object key) {
        return this.wrappedMap.get(key);
    }

    /**
     * Get the hashcode for this udo.
     * Implements the Map<String, Object> interface.
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
     * Implements the Map<String, Object> interface.
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
     * Implements the Map<String, Object> interface.
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
     * Implements the Map<String, Object> interface.
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
     * Implements the Map<String, Object> interface.
     *
     * @param m A map containing the new data.
     * @see Map
     */
    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        for (Map.Entry<? extends String, ? extends Object> entry : m.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();

            if ((value == null) || (key == null) || (key.length() == 0)) {
                continue;
            }

            this.put(key, value);
        }
    }

    /**
     * Remove the variable named key from this udo. Return its value.
     * Implements the Map<String, Object> interface.
     *
     * @param key
     * @return value of the removed variable.
     * @see Map
     */
    @Override
    public Object remove(Object key) {
        return this.wrappedMap.remove(key);
    }

    /**
     * Returns the number of variables contained in this udo.
     * Implements the Map<String, Object> interface.
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
     * Implements the Map<String, Object> interface.
     *
     * @return set of variable names
     * @see Map
     */
    @Override
    public Collection<Object> values() {
        return this.wrappedMap.values();
    }

    /**
     * Get the map that this udo wraps under the hood.
     * Using it can circumvent the logic this class is meant to provide.
     * If it's needed for anything besides testing,
     * refactoring is probably needed.
     *
     * @return the wrapped map
     */
    private Map<String, Object> getWrappedMap() {
        return this.wrappedMap;
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
     * @param collection
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
        for (Iterator<?> i = collection.iterator(); i.hasNext();) {
            if ((value = i.next()) != null) {
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
     * Decodes a Udo object from a json encoded string.
     *
     * @param json Json encoded Udo
     * @return Json decoded Udo
     * @throws UdoDeserializationException
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
     * Encodes a Udo into a json encoded string.
     *
     * @return Udo as json encoded string
     * @throws UdoSerializationException
     */
    public String toJson() throws UdoSerializationException {
        try {
            return JSON.std.asString(this);
        } catch (IOException e) {
            throw new UdoSerializationException("Error serializing udo to json", e);
        }
    }

    /**
     * Decodes a Udo object from a percent encoded string.
     *
     * @param persistentText
     * @return
     * @throws UdoDeserializationException
     */
    public static Udo fromPercentEncoded(String persistentText) throws UdoDeserializationException {
        String encodedKey, encodedValue, decodedKey;
        String[] keyAndValue, encodedValueElements, decodedValueElements;

        Udo decodedUdo = new Udo();

        for (String pair : persistentText.split("&")) {

            if (pair.length() == 0) {
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

    /**
     * Serialize Map to storage.
     *
     * Maps are persisted using percent encoding so as to be dependency
     * Independent; and immune to the vulnerabilities of the native
     * serialization API.
     *
     * @throws UdoSerializationException
     */
    String toPercentEncoded() throws UdoSerializationException {
        String serialized = "";

        List<String> listValue;
        String[] arrayValue;
        boolean isAppending = false;

        try {
            for (Map.Entry<String, ?> entry : this.entrySet()) {

                if (entry.getValue() == null) {
                    continue;
                }

                if (isAppending) {
                    serialized += '&';
                } else {
                    isAppending = true;
                }

                serialized += entry.getKey();
                serialized += '=';

                listValue = Udo.coerceIntoStringList(entry.getValue());
                if (listValue != null) {
                    final int lastIndex = listValue.size() - 1;
                    for (int i = 0; i <= lastIndex; i++) {
                        serialized += URLEncoder.encode(listValue.get(i), StandardCharsets.UTF_8.name());
                        if (i != lastIndex) {
                            serialized += ',';
                        }
                    }
                } else {
                    serialized += URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8.name());
                }
            }
        } catch(UnsupportedEncodingException e) {
            throw new UdoSerializationException("Error serializing udo to percent encoding", e);
        }

        return serialized;
    }
}
