package com.tlc.sql.common.registrar.model;

/**
 * Enumeration representing the type of resource store, which can be either LOCAL or REMOTE.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum ResourceStoreType {

    LOCAL,
    REMOTE;

    /**
     * Retrieves the {@link ResourceStoreType} enum based on the provided name.
     *
     * @param name The name of the resource store type (case-insensitive).
     * @return {@link ResourceStoreType}.
     */
    public static ResourceStoreType get(final String name) {
        return switch (name.toLowerCase()) {
            case "remote" -> REMOTE;
            default -> LOCAL;
        };
    }
}