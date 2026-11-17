package com.tlc.file.common.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.file.common.status.FileErrorCodes;

/**
 * Holds the pre-signed url details.
 *
 * @param url          Refers the pre-signed url.
 * @param versionId    Refers the version id.
 * @param keyName      Refers the object key name.
 * @param attachmentId Refers the attachment id for which the url be generated.
 */
public record PreSignedURLInfo(String url, String versionId, String keyName, Long attachmentId) {

    /**
     * Represents the action type, which indicates how the content should be displayed in the browser when accessed via the pre-signed URL.
     *
     * @author Maharaja S
     * @version 1.0
     */
    public enum ActionType {

        DOWNLOAD((short) 1),
        PREVIEW((short) 2);

        private final short id;

        ActionType(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * <p>
         * Gets the {@link ActionType} by its id.
         * </p>
         * @param id Refers the id of {@link ActionType}.
         * @return the {@link ActionType} corresponding to the given id.
         */
        public static ActionType getById(final short id) {
            return switch (id) {
                case 1 -> DOWNLOAD;
                case 2 -> PREVIEW;
                default -> throw ErrorCode.get(FileErrorCodes.INVALID_ACTION_TYPE, "i18n.attachment.invalid.action.type");
            };
        }
    }
}