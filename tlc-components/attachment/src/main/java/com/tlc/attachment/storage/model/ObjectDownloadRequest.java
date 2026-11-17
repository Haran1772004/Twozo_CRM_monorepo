package com.tlc.attachment.storage.model;

/**
 * Represents a request for downloading an object.
 *
 * @param versionId      Refers the version id of the object.
 * @param objectKeyName  Refers the name under which the object is stored to be retrieved.
 * @param storageVariant Refers the {@code StorageVariant}.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record ObjectDownloadRequest(String versionId, String objectKeyName, StorageVariant storageVariant) {

    public ObjectDownloadRequest(final String versionId, final String objectKeyName) {
        this(versionId, objectKeyName, StorageVariant.GENERAL);
    }
}
