package com.tlc.attachment.storage.model;

/**
 * Holds the attributes required to delete the version of a object.
 *
 * @param keyName   Refers the key name of the object.
 * @param versionId Refers the version id of the object to be removed.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record ObjectVersionDeleteRequest(String keyName, String versionId, StorageVariant storageVariant) {

    public ObjectVersionDeleteRequest(final String keyName, final String versionId) {
        this(keyName, versionId, StorageVariant.GENERAL);
    }
}
