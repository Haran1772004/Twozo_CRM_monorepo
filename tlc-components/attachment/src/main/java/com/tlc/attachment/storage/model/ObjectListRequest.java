package com.tlc.attachment.storage.model;

import java.util.Map;

/**
 * Request to list object summaries matching an optional prefix and metadata filter.
 *
 * @author sundar
 * @version 1.0
 */
public record ObjectListRequest(
        String prefix,
        String continuationToken,
        Map<String, String> metadataFilter,
        int maxKeys,
        StorageVariant storageVariant
) {

    public static final int MAX_KEYS_LIMIT = 100;

    public ObjectListRequest(
            final String prefix,
            final String continuationToken,
            final Map<String, String> metadataFilter,
            final int maxKeys
    ) {
        this(prefix, continuationToken, metadataFilter, maxKeys, StorageVariant.GENERAL);
    }
}
