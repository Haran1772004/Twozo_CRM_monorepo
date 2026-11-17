package com.tlc.web.undertow.cors;

import java.util.Collection;

/**
 * Represents a configuration for a CORS preflight rule.
 *
 * @param path  The API endpoint path this config applies to.
 * @param types The list of allowed operation types for this path.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record PreflightConfig(String path, Collection<String> types) {
}
