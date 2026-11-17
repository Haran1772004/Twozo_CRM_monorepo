package com.tlc.social.server.facebook.internal.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.server.model.SocialAttachmentType;

/**
 * An enum which contains the social page attachment type
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum FBAttachmentType implements SocialAttachmentType {

    IMAGE(),
    VIDEO(),
    AUDIO(),
    FILE();

    /**
     * <p>
     * Gets the {@link FBAttachmentType} based on the provided extension
     * </p>
     *
     * @param extension Represents the extension
     * @return {@link FBAttachmentType}
     */
    public static FBAttachmentType getByExtension(final String extension) {
        return switch (extension.toLowerCase()) {
            case "png", "jpeg", "jpg", "gif" -> IMAGE;
            case "mp4", "3gp", "mov", "avi", "webm" -> VIDEO;
            case "mp3", "ogg", "m4a", "aac", "wav" -> AUDIO;
            case "pdf", "pptx", "ppt", "txt", "xls", "xlsx", "doc", "docx" -> FILE;
            default -> throw ErrorCode.get(SocialErrorCodes.UNSUPPORTED_EXTENSION, "i18.social.unsupported.attachment.extension");
        };
    }
}