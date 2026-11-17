package com.tlc.file.client.internal;

import com.tlc.attachment.io.File;

import com.tlc.commons.code.ErrorCode;
import com.tlc.file.client.service.AttachmentStorageService;
import com.tlc.file.client.service.AttachmentService;

import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.status.FileErrorCodes;
import com.tlc.file.server.api.manager.AttachmentManager;
import com.tlc.validator.ProcessOutcome;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Defining services for managing attachments related callback actions within an organization.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 * @see AttachmentService
 */
public final class AttachmentServiceImpl implements AttachmentService {

    private AttachmentServiceImpl() {
    }

    private static class Instance {
        private static final AttachmentServiceImpl ATTACHMENT_SERVICE = new AttachmentServiceImpl();
    }

    public static AttachmentServiceImpl getInstance() {
        return Instance.ATTACHMENT_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param iamOrgId    Refers the iamOrg id.
     * @param attachments Refers the attachments to be uploaded.
     * @param category    Refers the category to which the attachments are stored.
     */
    @Override
    public Collection<Attachment> upload(final long iamOrgId, final Collection<Attachment> attachments, final String category) {
        AttachmentStorageService.getInstance().uploadAttachment(iamOrgId, attachments, category);

        try {
            AttachmentManager.getInstance().save(attachments);

            return attachments;
        } catch (Exception exception) {
            throw ErrorCode.get(FileErrorCodes.FILE_UPLOAD_FAILED, "i18n.attachment.upload.failed", exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param iamOrgId The IAM orgId to which the file belongs.
     * @param fileId   The unique ID of the file to retrieve.
     */
    @Override
    public File getFile(final long iamOrgId, final long fileId) {
        return AttachmentStorageService.getInstance().getFile(iamOrgId, fileId);
    }

    /**
     * {@inheritDoc}
     *
     * @param iamOrgId The IAM orgId to which the file belongs.
     * @param fileIds  The unique IDs of the file to retrieve.
     * @return
     */
    @Override
    public Collection<File> getFiles(final long iamOrgId, final Collection<Long> fileIds) {
        return AttachmentStorageService.getInstance().getFiles(iamOrgId, fileIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param attachmentIds A collection of unique attachment IDs to retrieve.
     * @return
     */
    @Override
    public Collection<Attachment> getAttachments(final Collection<Long> attachmentIds) {
        return AttachmentManager.getInstance().getAttachments(attachmentIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param fileId The unique ID of the file to check.
     * @return
     */
    @Override
    public boolean exists(long fileId) {
        return AttachmentManager.getInstance().exists(fileId);
    }

    /**
     * {@inheritDoc}
     *
     * @param fileIds The set of unique file IDs to check.
     * @return
     */
    @Override
    public boolean exists(final Set<Long> fileIds) {
        return AttachmentManager.getInstance().exists(fileIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param fileId           The unique ID of the file to generate the presigned URL for.
     * @param duration         The duration (in minutes) for which the presigned URL will be valid.
     * @param additionalParams Additional query parameters to include in the presigned URL.
     * @return
     */
    @Override
    public String getPresignedUrl(final Long fileId, final int duration, final Map<String, String> additionalParams) {
        return AttachmentStorageService.getInstance().getPresignedUrl(fileId, duration, additionalParams);
    }

    /**
     * {@inheritDoc}
     *
     * @param attachments Refers the attachments to be uploaded.
     * @param category    Refers the category to which the attachments are stored.
     */
    @Override
    public Collection<Attachment> upload(final Collection<Attachment> attachments, final String category) {
        AttachmentStorageService.getInstance().uploadAttachment(attachments, category);
        final ProcessOutcome processOutcome = AttachmentManager.getInstance().save(attachments);
        final Collection<Object> processedRecords = processOutcome.getProcessedRecords();

        if ((Objects.isNull(processedRecords) || processedRecords.isEmpty()) && Objects.nonNull(processOutcome.getErrorSummary())) {
            throw ErrorCode.get(FileErrorCodes.FILE_UPLOAD_FAILED, "i18n.attachment.upload.failed");
        }

        return attachments;
    }

    /**
     * {@inheritDoc}
     *
     * @param fileId   The unique ID of the file to retrieve.
     */
    @Override
    public File getFile(final long fileId) {
        return AttachmentStorageService.getInstance().getFile(fileId);
    }

    /**
     * {@inheritDoc}
     *
     * @param fileIds  The unique IDs of the file to retrieve.
     * @return
     */
    @Override
    public Collection<File> getFiles(final Collection<Long> fileIds) {
        return AttachmentStorageService.getInstance().getFiles(fileIds);
    }
}
