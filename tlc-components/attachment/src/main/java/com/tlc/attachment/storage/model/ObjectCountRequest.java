package com.tlc.attachment.storage.model;

import java.util.Map;

/**
 * Request to count objects in storage matching an optional prefix and metadata filter.
 *
 * @author sundar
 * @version 1.0
 */
public record ObjectCountRequest(
        String prefix,
        Map<String, String> metadataFilter,
        StorageVariant storageVariant
) {

    public ObjectCountRequest(final String prefix, final Map<String, String> metadataFilter) {
        this(prefix, metadataFilter, StorageVariant.GENERAL);
    }
}
