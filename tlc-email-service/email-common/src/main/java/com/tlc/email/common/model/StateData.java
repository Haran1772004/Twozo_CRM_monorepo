package com.tlc.email.common.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the state data, which is a collection of key-value pairs.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class StateData {

    private Map<String, Object> data;

    public StateData() {
        this.data = new HashMap<>();
    }

    public StateData(final JsonObject jsonObject) {
        this.data = new HashMap<>();

        this.data.putAll(jsonObject.toMap());
    }

    public Map<String, Object> getAll() {
        return data;
    }

    public Object get(final String key) {
        return data.get(key);
    }

    public String getString(final String key) {
        final Object value = data.get(key);

        return Objects.isNull(value) ? null : value instanceof String stringValue ? stringValue : value.toString();
    }

    public void put(final String key, final Object data) {
        this.data.put(key, data);
    }

    public void putAll(final Map<String, Object> data) {
        this.data = data;
    }

    /**
     * Converts the state data to a JsonObject.
     *
     * @return A JsonObject representation of the state data.
     */
    public JsonObject toJson() {
        return Json.object(data);
    }

    /**
     * Adds the key-value pairs from a given JsonObject to the state data.
     *
     * @param jsonObject The JsonObject whose data will be added to the state.
     * @return the state data
     */
    public Map<String, Object> toMap(final JsonObject jsonObject) {
        this.data.putAll(jsonObject.toMap());

        return data;
    }
}
