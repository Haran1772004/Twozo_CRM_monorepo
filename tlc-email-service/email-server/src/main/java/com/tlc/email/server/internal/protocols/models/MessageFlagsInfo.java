package com.tlc.email.server.internal.protocols.models;

import jakarta.mail.Flags;

/**
 * <p>
 * Holds message flag information extracted from a mail message.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class MessageFlagsInfo {

    private final boolean seen;
    private final boolean flagged;
    private final boolean deleted;
    private final boolean answered;
    private final boolean draft;

    /**
     * <p>
     * Creates a new {@link MessageFlagsInfo} from the given mail flags.
     * </p>
     *
     * @param flags Represents the {@link Flags} associated with a message.
     */
    public MessageFlagsInfo(final Flags flags) {
        this.seen = flags.contains(Flags.Flag.SEEN);
        this.flagged = flags.contains(Flags.Flag.FLAGGED);
        this.deleted = flags.contains(Flags.Flag.DELETED);
        this.answered = flags.contains(Flags.Flag.ANSWERED);
        this.draft = flags.contains(Flags.Flag.DRAFT);
    }

    /**
     * <p>
     * Indicates whether the message has been seen.
     * </p>
     *
     * @return {@code true} if the message is marked as seen.
     */
    public boolean isSeen() {
        return seen;
    }

    /**
     * <p>
     * Indicates whether the message is flagged.
     * </p>
     *
     * @return {@code true} if the message is flagged.
     */
    public boolean isFlagged() {
        return flagged;
    }

    /**
     * <p>
     * Indicates whether the message is marked for deletion.
     * </p>
     *
     * @return {@code true} if the message is deleted.
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * <p>
     * Indicates whether the message has been answered.
     * </p>
     *
     * @return {@code true} if the message is answered.
     */
    public boolean isAnswered() {
        return answered;
    }

    /**
     * <p>
     * Indicates whether the message is a draft.
     * </p>
     *
     * @return {@code true} if the message is a draft.
     */
    public boolean isDraft() {
        return draft;
    }
}