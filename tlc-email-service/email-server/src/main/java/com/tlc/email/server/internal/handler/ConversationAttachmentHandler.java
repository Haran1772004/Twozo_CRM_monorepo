package com.tlc.email.server.internal.handler;

import com.tlc.commons.code.ErrorCode;

import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailAttachment;
import com.tlc.email.common.model.type.Email;

import com.tlc.file.client.service.AttachmentService;

import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.models.ObjectAttachment;

import com.tlc.l10n.temporal.model.DateTime;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * The {@code ConversationAttachmentHandler} class is responsible for managing
 * email attachments by interacting with the {@code AttachmentService}, which
 * communicates with the S3 storage service.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class ConversationAttachmentHandler {

    private final AttachmentService attachmentService;

    private ConversationAttachmentHandler() {
        this.attachmentService = AttachmentService.getInstance();
    }

    /**
     * Holds the singleton instance of {@link ConversationAttachmentHandler}.
     */
    private static class Instance {
        private static final ConversationAttachmentHandler INSTANCE = new ConversationAttachmentHandler();
    }

    /**
     * Provides the instance of {@link ConversationAttachmentHandler}.
     *
     * @return the instance of {@link ConversationAttachmentHandler}
     */
    public static ConversationAttachmentHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Uploads the provided {@link Collection} of {@link EmailAttachment} objects
     * to S3 storage by converting them into {@link Attachment} objects and sending
     * them via the {@link AttachmentService}.
     *
     * @param emailId Represent the email id
     * @param email   the email containing the attachments to be uploaded
     */
    public void upload(final Long emailId, final Email email) {
        final Collection<EmailAttachment> emailAttachments = email.getAttachments();

        if (Objects.nonNull(emailAttachments) && !emailAttachments.isEmpty()) {
            email.setAttachments(upload(emailId, buildAttachments(emailAttachments)));
        }
    }

    /**
     * Uploads the given collection of {@link Attachment} objects to S3 storage
     * using the {@link AttachmentService}.
     *
     * @param emailId     the unique identifier for the synced email
     * @param attachments the collection of {@link Attachment} objects to upload
     * @return a collection of {@link EmailAttachment} objects created from the uploaded attachments
     */
    private Collection<EmailAttachment> upload(final Long emailId, final Collection<Attachment> attachments) {
        try {
            attachmentService.upload(attachments, String.valueOf(emailId));
            return buildEmailAttachments(attachments);
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.UPLOAD_ACTION_FAILED, exception);
        }
    }

    /**
     * Converts a {@link Collection} of {@link EmailAttachment} objects into a
     * {@link Collection} of {@link Attachment} objects for further processing.
     *
     * @param emailAttachments the collection of {@link EmailAttachment} objects to convert
     * @return a collection of {@link Attachment} objects
     */
    private Collection<Attachment> buildAttachments(final Collection<EmailAttachment> emailAttachments) {
        final Collection<Attachment> attachments = new HashSet<>();

        for (final EmailAttachment emailAttachment : emailAttachments) {

            if (Objects.nonNull(emailAttachment)) {
                final Attachment attachment = new ObjectAttachment();

                attachment.setExtension(emailAttachment.getExtension());
                attachment.setFileName(emailAttachment.getFileName());
                attachment.setCreatedTime(new DateTime(emailAttachment.getCreatedTime()));
                attachment.setUpdatedTime(new DateTime(emailAttachment.getUpdatedTime()));
                attachment.setFileSize(emailAttachment.getFileSize());
                attachment.setMediaType(emailAttachment.getMediaType());
                attachment.setStorageType(Attachment.StorageType.OBJECT);
                attachment.setInputStream(emailAttachment.getInputStream());

                attachments.add(attachment);
            }
        }

        return attachments;
    }

    /**
     * Converts a {@link Collection} of {@link Attachment} objects into a
     * {@link Collection} of {@link EmailAttachment} objects for use within the
     * application.
     *
     * @param attachments the collection of {@link Attachment} objects to convert
     * @return a collection of {@link EmailAttachment} objects
     */
    private Collection<EmailAttachment> buildEmailAttachments(final Collection<Attachment> attachments) {
        final Collection<EmailAttachment> emailAttachments = new HashSet<>();

        for (final Attachment attachment : attachments) {

            if (Objects.nonNull(attachment)) {
                final EmailAttachment emailAttachment = new EmailAttachment();

                emailAttachment.setId(attachment.id());
                emailAttachment.setExtension(attachment.getExtension());
                emailAttachment.setFileName(attachment.getFileName());
                emailAttachment.setCreatedTime(attachment.getCreatedTime().getTimeStamp());
                emailAttachment.setUpdatedTime(attachment.getUpdatedTime().getTimeStamp());
                emailAttachment.setFileSize(attachment.getFileSize());
                emailAttachment.setMediaType(attachment.getMediaType());
                emailAttachment.setInputStream(attachment.getInputStream());

                emailAttachments.add(emailAttachment);
            }
        }

        return emailAttachments;
    }
}
