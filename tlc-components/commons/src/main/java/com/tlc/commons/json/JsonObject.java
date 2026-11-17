package com.tlc.commons.json;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;


/**
 * @author Abishek
 * @version 1.0
 */
public interface JsonObject extends Serializable
{
    boolean containsKey(String key);

    JsonObject put(String key, Boolean value);

    JsonObject put(String key, Short value);

    JsonObject put(String key, Integer value);

    JsonObject put(String key, Long value);

    JsonObject put(String key, Float value);

    JsonObject put(String key, Double value);

    JsonObject put(String key, Number value);

    JsonObject put(String key, String value);

    JsonObject put(String key, JsonArray value);

    JsonObject put(String key, JsonObject value);

    JsonObject put(String key, Object value);

    JsonObject append(JsonObject json);

    boolean getBoolean(String key);

    boolean optBoolean(String key);

    boolean optBoolean(String key, boolean defaultValue);

    short getShort(String key);

    short optShort(String key, short defaultValue);

    int getInt(String key);

    int optInt(String key, int defaultValue);

    long getLong(String key);

    long optLong(String key, long defaultValue);

    float getFloat(String key);

    float optFloat(String key, float defaultValue);

    double getDouble(String key);

    double optDouble(String key, double defaultValue);

    String getString(String key);

    String optString(String key, String defaultValue);

    Object get(String key);

    Object opt(String key);

    JsonArray getJsonArray(String key);

    JsonArray optJsonArray(String key);

    JsonObject getJsonObject(String key);

    JsonObject optJsonObject(String key);

    JsonObject remove(String key);

    int size();

    void forEach(BiConsumer<String, Object> consumer);

    String toString();

    byte[] getBytes();

    void writeBytes(OutputStream outputStream);

    Set<String> keySet();

    Map<String, Object> toMap();

    /**
     * Prunes the JSON tree by identifying and deleting all {@code JsonObject}/{@code JsonArray}
     * instances with a size of zero.
     */
    void removeEmptyObjects();

    /**
     * <p>
     * Gets the nullable value from json object
     * </p>
     *
     * @param key Represents the key
     * @param <T> Represents the type of the value associated with the given key
     * @return the value
     */
    <T> T getNullableValue(final String key);

    /**
     * Retrieves a nullable string value from the {@code JsonObject}.
     *
     * @param key Refers the key used to retrieve the value.
     * @return The trimmed string if valid, otherwise {@code null}.
     */
    String getNullableString(final String key);

    /**
     * <p>
     * Check whether the key has null value or not
     * </p>
     *
     * @param key Represents the key
     * @return true if the key has null value
     */
    boolean isKeyPresentWithNullValue(final String key);

    /**
     * <p>
     * Returns the value associated with the specified key if it exists,
     * or the provided default value if the key is not present in the data map.
     * </p>
     *
     * @param <T>          the type of the value associated with the given key
     * @param key          represents the key
     * @param defaultValue the default value to return if the key is not found in the data map
     * @return the value associated with the key, or the default value if the key is not present
     */
    <T> T optNullableValue(final String key, final T defaultValue);

    /**
     * <p>
     * Checks whether the given {@link String} is json formatted string or not.
     * </p>
     *
     * @param valueAsString Represents the string value.
     * @return A flag that indicates whether the given {@link String} is json formatted string or not.
     */
    static boolean isJsonString(final String valueAsString) {
        try {
            new ObjectMapper().readTree(valueAsString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * <p>
     * Returns the value associated with the specified key if it is present in the data map,
     * otherwise computes the value using the given function, stores it, and returns it.
     * </p>
     *
     * @param <T>           the type of the value associated with the given key
     * @param key           represents the key
     * @param valueFunction the function used to compute a value if the key is not present
     * @return the value associated with the key, or the newly computed and stored value
     */
    <T> T computeIfAbsent(final String key, final Function<String, T> valueFunction);
}
