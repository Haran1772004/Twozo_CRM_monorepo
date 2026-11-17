package com.tlc.email.common.model.config;

import com.tlc.commons.code.ErrorCode;

import com.tlc.email.common.internal.status.EmailErrorCodes;

/**
 * Representing different types of folders in an email
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public enum FolderType {

    INBOX((short) 1),
    SENT((short) 2),
    ARCHIVE((short) 3),
    DRAFT((short) 4);

    final short id;

    FolderType(final short id) {
        this.id = id;
    }

    /**
     * <p>
     * Retrieves the folder id
     * </p>
     *
     * @return the folder id
     */
    public short getId() {
        return id;
    }

    /**
     * <p>
     * Checks if the folder type is either SENT or DRAFT
     * </p>
     *
     * @return true if the folder type is SENT or DRAFT, false otherwise
     */
    public boolean isSentOrDraft() {
        return this == SENT || this == DRAFT;
    }

    /**
     * <p>
     * Gets the {@link FolderType} by id
     * </p>
     *
     * @param id Represents the folder id of the mailbox
     * @return {@link FolderType}
     */
    public static FolderType get(final short id) {
        return switch (id) {
            case 1 -> INBOX;
            case 2 -> SENT;
            case 3 -> ARCHIVE;
            case 4 -> DRAFT;
            default -> throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_FOLDER_TYPE);
        };
    }

    /**
     * <p>
     * Represents filter modes for folder-based operations.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public enum FilterMode {

        ONLY_DRAFT,
        EXCLUDE_DRAFT,
        ;
    }
}