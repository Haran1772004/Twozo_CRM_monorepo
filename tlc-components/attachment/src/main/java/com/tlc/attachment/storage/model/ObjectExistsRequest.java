package com.tlc.attachment.storage.model;

import java.util.Map;

/**
 * Represents a request for existence of an object/blob.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record ObjectExistsRequest(
        String objectName,
        Map<String, String> metadataFilter,
        StorageVariant storageVariant
) {

    public ObjectExistsRequest(final String objectName) {
        this(objectName, Map.of(), StorageVariant.GENERAL);
    }

    public ObjectExistsRequest(final String objectName, final StorageVariant storageVariant) {
        this(objectName, Map.of(), storageVariant);
    }
}
