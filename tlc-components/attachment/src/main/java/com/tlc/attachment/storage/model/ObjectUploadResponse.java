package com.tlc.attachment.storage.model;

import java.net.URL;

/**
 * Holds the object upload response details.
 *
 * @param versionId         Refers the version id of the uploaded object.
 * @param objectName        Refers the key name under which the object is stored.
 * @param preSignedUrl      Refers the {@code URL} of the uploaded object.
 * @param objectStorageType Refers the object storage type.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record ObjectUploadResponse(String versionId, String objectName, URL preSignedUrl,
                                   ObjectStorageType objectStorageType) {
}