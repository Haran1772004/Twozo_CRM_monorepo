package com.tlc.attachment.storage.model;

/**
 * Represents different variants of storage used in the application.
 * <p>
 * This enum helps distinguish between storage intended for schema-related
 * files (like {@code meta.xml}, {@code data.xml}, etc.) and general-purpose
 * storage for other files.
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum StorageVariant {

    SCHEMA,   // For schema-related files (meta.xml, data.xml, etc.)
    GENERAL,  // For normal storage files
    EVENT_STREAM, //For event stream files (Kafka msg backup, etc.)
    ;
}
