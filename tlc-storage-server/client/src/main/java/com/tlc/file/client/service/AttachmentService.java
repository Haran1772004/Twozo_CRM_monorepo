package com.tlc.file.client.service;

import com.tlc.attachment.io.File;

import com.tlc.file.client.internal.AttachmentServiceImpl;
import com.tlc.file.common.models.Attachment;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Interface defining operations for managing attachments within an organization.
 * <ul>
 *     <li>Upload attachment data</li>
 *     <li>Retrieve details of specific attachments</li>
 *     <li>Check the existence of specific attachments</li>
 * </ul>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface AttachmentService {

    /**
     * Fetches the {@link AttachmentService} instance.
     *
     * @return the instance of {@link AttachmentService}.
     */
    static AttachmentService getInstance() {
        return AttachmentServiceImpl.getInstance();
    }

    /**
     * Uploads the given collection of {@link Attachment}.
     *
     * @param iamOrgId    Refers the iamOrg id.
     * @param attachments Refers the attachments to be uploaded.
     * @param category    Refers the category to which the attachments are stored.
     * @return The collection of attachments.
     */
    Collection<Attachment> upload(final long iamOrgId, final Collection<Attachment> attachments, final String category);

    /**
     * Retrieves a file by its ID for the specified organization.
     *
     * @param iamOrgId The IAM orgId to which the file belongs.
     * @param fileId   The unique ID of the file to retrieve.
     * @return the {@code File} object associated with the given file ID.
     */
    File getFile(final long iamOrgId, final long fileId);

    /**
     * Retrieves a files by its ID for the specified organization.
     *
     * @param iamOrgId The IAM orgId to which the file belongs.
     * @param fileIds  The unique IDs of the file to retrieve.
     * @return the {@code File} object associated with the given file ID.
     */
    Collection<File> getFiles(final long iamOrgId, final Collection<Long> fileIds);

    /**
     * Retrieves attachments by their IDs.
     *
     * @param attachmentIds A collection of unique attachment IDs to retrieve.
     * @return A collection of {@code Attachment} objects corresponding to the given IDs.
     */
    Collection<Attachment> getAttachments(final Collection<Long> attachmentIds);

    /**
     * Checks if a file with the specified ID exists.
     *
     * @param fileId The unique ID of the file to check.
     * @return {@code true} if the file exists; {@code false} otherwise.
     */
    boolean exists(final long fileId);

    /**
     * Checks if all files with the specified IDs exist.
     *
     * @param fileIds The set of unique file IDs to check.
     * @return {@code true} if all files exist; {@code false} if any file is missing.
     */
    boolean exists(final Set<Long> fileIds);

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
     * Uploads the given collection of {@link Attachment}.
     *
     * @param attachments Refers the attachments to be uploaded.
     * @param category    Refers the category to which the attachments are stored.
     * @return The collection of attachments.
     */
    Collection<Attachment> upload(final Collection<Attachment> attachments, final String category);

    /**
     * Gets the {@link File} for the given id.
     *
     * @param fileId Represents the file id.
     * @return {@link File}
     */
    File getFile(final long fileId);

    /**
     * Gets the {@link Collection} of {@link File} for the given collection of ids.
     *
     * @param fileIds Represents the {@link Collection} of file ids.
     * @return {@link Collection} of {@link File}
     */
    Collection<File> getFiles(final Collection<Long> fileIds);
}
