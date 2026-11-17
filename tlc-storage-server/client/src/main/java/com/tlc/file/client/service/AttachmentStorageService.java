package com.tlc.file.client.service;

import com.tlc.attachment.io.File;
import com.tlc.attachment.storage.model.ObjectVersionDeleteRequest;

import com.tlc.commons.service.Service;

import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.models.Attachment.StorageType;
import com.tlc.file.client.internal.AttachmentStorageServiceImpl;

import java.util.Collection;
import java.util.Map;

/**
 * Provides the storage services of {@link Attachment}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface AttachmentStorageService extends Service {

    /**
     * Gets the {@link AttachmentStorageServiceImpl} Instance.
     *
     * @return the {@link AttachmentStorageServiceImpl}.
     */
    static AttachmentStorageService getInstance() {
        return AttachmentStorageServiceImpl.getInstance();
    }

    /**
     * Retrieves the {@link StorageType}, which indicates whether the storage is file-based or object-based.
     *
     * @return The {@link StorageType} representing the type of storage.
     */
    StorageType storageType();

    /**
     * Uploads the {@link Attachment} according to the storage type.
     *
     * @param iamOrgId    Refers the organization id.
     * @param attachments Refers the attachments to be uploaded.
     * @param category    Refers the attachment category.
     */
    void uploadAttachment(final long iamOrgId, final Collection<Attachment> attachments, final String category);

    /**
     * Gets the {@link File} to performs the file related actions.
     *
     * @param orgId Organization id.
     * @param id    Attachment id.
     * @return {@link File}.
     */
    File getFile(final long orgId, final long id);

    /**
     * Gets the {@link File} to performs the file related actions.
     *
     * @param orgId Represents an Organization id.
     * @param ids   Represents the collection of {@link Attachment} id.
     * @return collection of {@link File}.
     */
    Collection<File> getFiles(final long orgId, final Collection<Long> ids);

    /**
     * Generates a pre-signed URL for accessing a file with the specified ID.
     *
     * @param fileId           The unique ID of the file to generate the presigned URL for.
     * @param duration         The duration (in minutes) for which the presigned URL will be valid.
     * @param additionalParams Additional query parameters to include in the presigned URL.
     * @return The pre-signed URL as a {@code String}.
     */
    String getPresignedUrl(final Long fileId, final int duration, final Map<String, String> additionalParams);

    /**
     * Deletes a specific version of an object in the storage system.
     *
     * @param versionDeleteRequest The request containing the details of the object version to delete.
     */
    void delete(final ObjectVersionDeleteRequest versionDeleteRequest);

    /**
     * Uploads the {@link Attachment} according to the storage type.
     *
     * @param attachments Refers the attachments to be uploaded.
     * @param category    Refers the attachment category.
     */
    void uploadAttachment(final Collection<Attachment> attachments, final String category);

    /**
     * Gets the {@link Collection} of {@link File} to performs the file related actions.
     *
     * @param id Attachment id.
     * @return {@link File}.
     */
    File getFile(final long id);

    /**
     * Gets the {@link Collection} of {@link File} to performs the file related actions.
     *
     * @param ids Represents the {@link Collection} of attachment ids.
     * @return {@link File}.
     */
    Collection<File> getFiles(final Collection<Long> ids);
}
