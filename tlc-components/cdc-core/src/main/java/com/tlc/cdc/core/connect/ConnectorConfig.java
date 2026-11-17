package com.tlc.cdc.core.connect;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import java.util.Objects;

/**
 * Represents the configuration required to manage a Kafka Connect connector.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class ConnectorConfig {

    private String name;
    private String url;
    private JsonObject config;

    private final Type type;

    public ConnectorConfig(final Type type) {
        this.type = type;
    }

    public enum Type {
        SOURCE,
        SINK
    }

    public Type getType() {
        return type;
    }

    public JsonObject getConfig() {
        return config;
    }

    public void setConfig(final JsonObject config) {
        this.config = config;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public JsonObject toRequestJson() {
        Objects.requireNonNull(name);
        Objects.requireNonNull(config);

        final JsonObject jsonObject = Json.object();

        jsonObject.put("name", name);
        jsonObject.put("config", config);

        return jsonObject;
    }
}
