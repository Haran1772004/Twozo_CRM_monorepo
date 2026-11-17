package com.tlc.email.server.internal.provider.jakarta.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.type.Email.MessageCategory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Holds IMAP folder names used by the email system.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum EmailFolder {

    INBOX,
    SENT,
    DRAFT,
    TRASH,
    SPAM,
    ARCHIVE;

    public static Collection<EmailFolder> get() {
        return List.of(INBOX, DRAFT, ARCHIVE, SENT);
    }

    public static EmailFolder getCategoryBasedFolder(final MessageCategory messageCategory) {
        return switch (messageCategory) {
            case INBOUND -> INBOX;
            case OUTBOUND -> SENT;
            case DRAFT -> DRAFT;
            case ARCHIVE -> ARCHIVE;
            default -> throw ErrorCode.get(EmailErrorCodes.FOLDER_NOT_FOUND);
        };
    }

    public boolean isInbox() {
        return Objects.equals(this, INBOX);
    }

    public boolean isSent() {
        return Objects.equals(this, SENT);
    }

    public boolean isDraft() {
        return Objects.equals(this, DRAFT);
    }

    public boolean isTrash() {
        return Objects.equals(this, TRASH);
    }

    public boolean isSpam() {
        return Objects.equals(this, SPAM);
    }

    public boolean isArchive() {
        return Objects.equals(this, ARCHIVE);
    }
}