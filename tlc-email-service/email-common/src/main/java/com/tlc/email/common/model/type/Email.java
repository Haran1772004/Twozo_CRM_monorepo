package com.tlc.email.common.model.type;

import com.tlc.commons.code.ErrorCode;

import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailModel;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.EmailAttachment;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.address.EmailRecipients;

import com.tlc.attachment.io.File;

import com.tlc.email.common.model.config.FolderType;
import com.tlc.notification.model.Notification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A model that represents the email.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class Email extends Notification<SyncAddressInfo, EmailRecipients> implements EmailModel {

    private boolean isArchived;
    private boolean isStarred;
    private boolean isRead;
    private boolean isActive;
    private boolean isDeleted;
    private int recipientsCount;
    private Long id;
    private String subject;
    private String content;
    private MessageCategory messageCategory;
    private EmailMetaData metaData;
    private Collection<EmailAttachment> attachments;
    private Collection<File> files;
    private Type conversationType = Type.MAILBOX;

    @Override
    public Long id() {
        return id;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(final boolean archived) {
        isArchived = archived;
    }

    public boolean isStarred() {
        return isStarred;
    }

    public void setStarred(final boolean starred) {
        isStarred = starred;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(final boolean read) {
        isRead = read;
    }

    public int getRecipientsCount() {
        return recipientsCount;
    }

    public void setRecipientsCount(final int recipientsCount) {
        this.recipientsCount = recipientsCount;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public MessageCategory getMessageCategory() {
        return messageCategory;
    }

    public void setMessageCategory(final MessageCategory messageCategory) {
        this.messageCategory = messageCategory;
    }

    public EmailMetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(final EmailMetaData metaData) {
        this.metaData = metaData;
    }

    public Collection<EmailAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(final Collection<EmailAttachment> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(final EmailAttachment attachment) {
        this.attachments = this.attachments == null ? new ArrayList<>() : attachments;
        this.attachments.add(attachment);
    }

    public Collection<File> getFiles() {
        return files;
    }

    public void setFiles(final Collection<File> files) {
        this.files = files;
    }

    public EmailRecipients getEmailRecipients() {
        return this.recipients == null ? null : this.recipients.stream().findFirst().orElseGet(() -> null);
    }

    public boolean isDraft() {
        return false;
    }

    public final <T extends Email> T get() {
        return (T) this;
    }

    public boolean isActive() {
        return isActive;
    }

    public void isActive(final boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public Type getConversationType() {
        return conversationType;
    }

    public void setConversationType(final Type conversationType) {
        this.conversationType = conversationType;
    }

    /**
     * <p>
     * Defines read, delete, archive state of email stored
     * </p>
     * <p>
     * {@link #READ}
     * {@link #ACTIVE}
     * {@link #ARCHIVE}
     * {@link #ACTIVE}
     */
    public enum State {
        READ,
        ACTIVE,
        ARCHIVE,
        STARRED,
        INACTIVE,
        UNARCHIVE
    }

    /**
     * <p>
     * Represents the message category
     * </p>
     */
    public enum MessageCategory {

        OUTBOUND((short) 1),
        INBOUND((short) 2),
        DRAFT((short) 3),
        SCHEDULED((short) 4),
        UNSENT((short) 5),
        ARCHIVE((short) 6),
        ;

        private final short id;

        MessageCategory(final short id) {
            this.id = id;
        }

        /**
         * <p>
         * Gets the message category id
         * </p>
         *
         * @return the category id
         */
        public short getId() {
            return this.id;
        }

        /**
         * Gets the value of {@link MessageCategory} for the given id
         *
         * @param id Represents the id of {@link MessageCategory}
         * @return {@link MessageCategory}
         */
        public static MessageCategory get(final short id) {

            return switch (id) {
                case 1 -> OUTBOUND;
                case 2 -> INBOUND;
                case 3 -> DRAFT;
                case 4 -> SCHEDULED;
                case 5 -> UNSENT;
                case 6 -> ARCHIVE;
                default -> throw ErrorCode.get(EmailErrorCodes.INVALID_MESSAGE_CATEGORY);
            };
        }

        /**
         * <p>
         * Checks if the message category is related to outgoing emails.
         * </p>
         *
         * @return true, if the category is related to send type; otherwise false
         */
        public boolean isSendCategory() {
            return switch (this) {
                case OUTBOUND, SCHEDULED, DRAFT -> true;
                default -> false;
            };
        }

        /**
         * <p>
         * Checks if the message category is related to incoming emails.
         * </p>
         *
         * @return true, if the category is related to receive type; otherwise false
         */
        public boolean isReceiveCategory() {
            return switch (this) {
                case INBOUND -> true;
                default -> false;
            };
        }

        /**
         * <p>
         * Checks whether the current message category is {@code DRAFT}.
         * </p>
         *
         * @return {@code true} if the category is {@code DRAFT}, otherwise {@code false}.
         */
        public boolean isDraft() {
            return Objects.equals(this, DRAFT);
        }

        /**
         * <p>
         * Checks whether the current message category is {@code ARCHIVE}.
         * </p>
         *
         * @return {@code true} if the category is {@code ARCHIVE}, otherwise {@code false}.
         */
        public boolean isArchive() {
            return Objects.equals(this, ARCHIVE);
        }

        /**
         * <p>
         * Checks whether the current message category is {@code INBOUND}.
         * </p>
         *
         * @return {@code true} if the category is {@code INBOUND}, otherwise {@code false}.
         */
        public boolean isInbound() {
            return Objects.equals(this, INBOUND);
        }

        /**
         * <p>
         * Fetches the {@link Email.MessageCategory} based on the {@link FolderType}
         * </p>
         *
         * @param folder Represents the {@link FolderType}
         * @return The {@link Email.MessageCategory}
         */
        public static Email.MessageCategory getMessageCategory(final FolderType folder) {
            return switch (folder) {
                case INBOX -> INBOUND;
                case SENT -> OUTBOUND;
                case ARCHIVE -> ARCHIVE;
                case DRAFT -> DRAFT;
            };
        }
    }

    /**
     * <p>
     * Represents the conversation type.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public enum Type {
        MAILBOX,
        INTERNAL,
    }
}