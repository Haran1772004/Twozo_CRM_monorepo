package com.tlc.social.server.whatsapp.internal.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.server.model.SocialAttachmentType;

/**
 * An enum which represents the WhatsApp attachment types with the allowed extensions
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum WAAttachmentType implements SocialAttachmentType {

    IMAGE(),
    AUDIO(),
    VIDEO(),
    DOCUMENT();

    /**
     * <p>
     * Gets the {@link WAAttachmentType} based on the provided extension
     * </p>
     *
     * @param extension Represents the extension
     * @return {@link WAAttachmentType}
     */
    public static WAAttachmentType getByExtension(final String extension) {
        return switch (extension.toLowerCase()) {
            case "png", "jpeg", "jpg" -> IMAGE;
            case "3gp", "mp4" -> VIDEO;
            case "pdf", "pptx", "ppt", "txt", "xls", "xlsx", "doc", "docx", "zip" -> DOCUMENT;
            case "aac", "mp3", "m4a" -> AUDIO;
            default -> throw ErrorCode.get(SocialErrorCodes.UNSUPPORTED_EXTENSION, "i18.social.unsupported.attachment.extension");
        };
    }
}