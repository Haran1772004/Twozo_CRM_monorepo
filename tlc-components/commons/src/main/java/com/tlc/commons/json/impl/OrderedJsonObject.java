package com.tlc.commons.json.impl;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.json.JsonParser;

import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Json object that preserves the order of the map.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class OrderedJsonObject extends AbstractJsonObject {

    private final JsonParser jsonParser;

    public OrderedJsonObject(final JsonParser jsonParser, final LinkedHashMap<String, ?> data) {
        super(data);
        this.jsonParser = jsonParser;
    }

    public OrderedJsonObject(final JsonParser jsonParser) {
        super(new LinkedHashMap<>());
        this.jsonParser = jsonParser;
    }

    /**
     * <p>
     * Serializes the JSON object to its string representation.
     * </p>
     *
     * @return JSON string.
     */
    @Override
    public String toString() {
        return jsonParser.encodeAsString(getMap());
    }

    /**
     * <p>
     * Gets the bytes.
     * </p>
     *
     * @return bytes.
     */
    @Override
    public byte[] getBytes() {
        return jsonParser.encodeAsBytes(getMap());
    }

    /**
     * <p>
     * Writes the byte representation of the JSON object to the provided output stream.
     * </p>
     *
     * @param outputStream The outputStream to which the byte representation of the JSON object.
     */
    @Override
    public void writeBytes(final OutputStream outputStream) {
        jsonParser.encodeAsBytes(getMap(), outputStream);
    }

    /**
     * <p>
     * Converts the {@link JsonObject} to a {@link LinkedHashMap}.
     * </p>
     *
     * @return The {@link Map} containing the key-value pairs of the JSON object.
     */
    @Override
    public Map<String, Object> toMap() {
        return getMap();
    }

    /**
     * <p>
     * Wraps the given collection into a {@link JsonArray}.
     * </p>
     *
     * @param collection Contains the list of element.
     * @return {@link JsonArray}.
     */
    @Override
    protected JsonArray wrappedList(final List<?> collection) {
        return new JsonArrayImpl(jsonParser, collection);
    }

    /**
     * <p>
     * Wraps the provided map into a {@link JsonObject}.
     * </p>
     *
     * @param map The map to be wrapped as a {@link JsonObject}.
     * @return {@link JsonObject}.
     */
    @Override
    protected JsonObject wrappedMap(final Map<?, ?> map) {
        return new OrderedJsonObject(jsonParser, (LinkedHashMap<String, ?>) map);
    }
}