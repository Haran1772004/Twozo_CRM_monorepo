package com.tlc.crm.email.core.handler;

import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.email.core.manager.EmailLinkerManager;
import com.tlc.email.common.model.EmailAttachment;
import com.tlc.email.common.model.type.Email;
import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.models.ObjectAttachment;
import com.tlc.l10n.temporal.model.DateTime;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Provides an abstract base implementation for handling email data load actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractEmailDataLoadActionHandler implements EmailAddOnServiceDataLoadActionHandler,
        SecureUserTaskProcessor {

    private final EmailLinkerManager emailLinkerManager;

    /**
     * <p>
     * Constructs an {@code AbstractEmailDataLoadActionHandler} and initializes
     * the {@link EmailLinkerManager} instance.
     * </p>
     */
    AbstractEmailDataLoadActionHandler() {
        this.emailLinkerManager = EmailLinkerManager.getInstance();
    }

    /**
     * <p>
     * Returns the {@link EntityEmail} instance used by this handler.
     * </p>
     *
     * @return The {@link EntityEmail} instance.
     */
    protected abstract EntityEmail getEmail();

    /**
     * <p>
     * Composes and returns an {@link EntityThread} object populated with the provided email details.
     * </p>
     *
     * @param orgId    The organization ID.
     * @param userId   The user ID.
     * @param threadId The thread ID.
     * @param email    The {@link Email} object containing email data.
     * @return An {@link EntityThread} object containing the email details.
     */
    protected EntityThread composeThread(final Long orgId, final Long userId, final Long threadId, final Email email) {
        final EntityThread thread = new EntityThread();
        final EntityEmail entityEmail = getEmail();
        final Collection<EmailAttachment> emailAttachments = email.getAttachments();

        if (Objects.nonNull(emailAttachments) && !emailAttachments.isEmpty()) {
            entityEmail.setEntityAttachments(buildAttachments(emailAttachments));
        }
        thread.setId(threadId);
        thread.setOrgId(orgId);
        thread.setUserId(userId);

        entityEmail.setId(email.id());
        entityEmail.setUserId(userId);
        entityEmail.setOrgId(orgId);
        entityEmail.setContent(email.getContent());
        entityEmail.setSubject(email.getSubject());
        entityEmail.setRecipients(List.of(email.getEmailRecipients()));
        entityEmail.setSender(email.getSender());
        entityEmail.setMetaData(email.getMetaData());
        entityEmail.setArchived(email.isArchived());
        entityEmail.setSyncedState(false);

        thread.setEntityEmails(List.of(entityEmail));

        return thread;
    }

    /**
     * <p>
     * Converts a collection of {@link EmailAttachment} objects into a collection of {@link Attachment} objects.
     * </p>
     *
     * @param emailAttachments The collection of {@link EmailAttachment} objects to be converted.
     * @return A collection of {@link Attachment} objects.
     */
    private Collection<Attachment> buildAttachments(final Collection<EmailAttachment> emailAttachments) {
        final Collection<Attachment> attachments = new HashSet<>();

        if (Objects.nonNull(emailAttachments)) {
            for (final EmailAttachment emailAttachment : emailAttachments) {

                if (Objects.nonNull(emailAttachment)) {
                    final String fileName = emailAttachment.getFileName();

                    attachments.add(getAttachment(emailAttachment, fileName));
                }
            }
        }

        return attachments;
    }

    /**
     * <p>
     * Creates and returns an {@link Attachment} object from the given {@link EmailAttachment}.
     * </p>
     *
     * @param emailAttachment The source {@link EmailAttachment} containing file details.
     * @param fileName        The name of the file to be set for the attachment.
     * @return The constructed {@link Attachment} object populated with the provided details.
     */
    private Attachment getAttachment(final EmailAttachment emailAttachment, final String fileName) {
        final Attachment attachment = new ObjectAttachment();

        attachment.setId(emailAttachment.id());
        attachment.setExtension(emailAttachment.getExtension());
        attachment.setFileName(fileName);
        attachment.setCreatedTime(new DateTime(emailAttachment.getCreatedTime()));
        attachment.setUpdatedTime(new DateTime(emailAttachment.getUpdatedTime()));
        attachment.setFileSize(emailAttachment.getFileSize());
        attachment.setMediaType(emailAttachment.getMediaType());
        attachment.setStorageType(Attachment.StorageType.OBJECT);
        attachment.setInputStream(emailAttachment.getInputStream());

        return attachment;
    }

    /**
     * <p>
     * Performs preprocessing on the given {@link EntityThread}, linking it with thread
     * and conversation identifiers.
     * </p>
     *
     * @param entityThread The {@link EntityThread} to be preprocessed.
     */
    protected void preProcess(final EntityThread entityThread) {
        emailLinkerManager.loadThreadAndConversationId(entityThread);
    }
}