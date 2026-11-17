package com.tlc.attachment.service;

import com.tlc.commons.service.Service;

/**
 * Provides the different storage services of attachment.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface StorageService extends Service {

    /**
     * Provides the object storage service.
     *
     * @return {@link ObjectStorageService}.
     */
    ObjectStorageService getObjectStorageService();

    /**
     * Provides the org dependent file storage service.
     *
     * @return {@link OrgFileStorageService}.
     */
    OrgFileStorageService getOrgFileStorageService();

    /**
     * Provides the file storage services.
     *
     * @return {@link FileStorageService}.
     */
    FileStorageService getFileStorageService();

}
