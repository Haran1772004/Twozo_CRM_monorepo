package com.tlc.social.server.instagram.internal.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.server.model.SocialAttachmentType;

/**
 * An enum which represents the Instagram attachment types with the allowed extensions
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum InstagramAttachmentType implements SocialAttachmentType {

    IMAGE(),
    AUDIO(),
    VIDEO(),
    ;

    /**
     * <p>
     * Gets the {@link InstagramAttachmentType} based on the provided extension
     * </p>
     *
     * @param extension Represents the extension
     * @return {@link InstagramAttachmentType}
     */
    public static InstagramAttachmentType getByExtension(final String extension) {
        return switch (extension.toLowerCase()) {
            case "png", "jpeg", "jpg", "gif" -> IMAGE;
            case "aac", "wav", "m4a" -> AUDIO;
            case "mov", "mp4", "avi", "webm" -> VIDEO;
            default -> throw ErrorCode.get(SocialErrorCodes.UNSUPPORTED_EXTENSION, "i18.social.unsupported.attachment.extension");
        };
    }
}