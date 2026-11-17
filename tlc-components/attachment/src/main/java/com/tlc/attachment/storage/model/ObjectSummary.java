package com.tlc.attachment.storage.model;

import java.util.Map;

/**
 * Summary of a stored object without its content.
 *
 * @author sundar
 * @version 1.0
 */
public record ObjectSummary(
        String key,
        String versionId,
        long lastModified,
        Map<String, String> metadata
) {
}
