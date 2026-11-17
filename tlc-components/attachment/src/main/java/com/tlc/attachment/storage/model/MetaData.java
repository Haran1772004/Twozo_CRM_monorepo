package com.tlc.attachment.storage.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the custom metadata that is stored with object storage.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class MetaData {

    private final Map<String, String> metaData;

    public MetaData() {
        this.metaData = new HashMap<>();
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }

    public void add(final String key, final String value) {
        this.metaData.put(key, value);
    }
}
