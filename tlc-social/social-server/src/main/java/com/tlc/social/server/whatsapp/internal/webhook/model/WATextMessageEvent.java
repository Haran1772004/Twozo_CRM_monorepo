package com.tlc.social.server.whatsapp.internal.webhook.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.server.internal.webhook.event.model.TextMessageEvent;

/**
 * Represents a model for the WhatsApp webhook text message event
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WATextMessageEvent extends TextMessageEvent {

    private Attachment attachment;
    private String senderProfileName;

    public Attachment getAttachment() {
        return attachment;
    }

    public void setAttachment(final Attachment attachment) {
        this.attachment = attachment;
    }

    public String getSenderProfileName() {
        return senderProfileName;
    }

    public void setSenderProfileName(final String senderProfileName) {
        this.senderProfileName = senderProfileName;
    }

    /**
     * A record which encapsulates the attachment details
     *
     * @author Maharaja S
     * @version 1.0
     */
    public record Attachment(Long id, String mimeType) {
    }

    /**
     * An enum which represents the content type of the WhatsApp webhook event
     *
     * @author Maharaja S
     * @version 1.0
     */
    public enum ContentType {

        TEXT,
        IMAGE,
        AUDIO,
        LOCATION,
        DOCUMENT,
        VIDEO,
        BUTTON,
        ;

        /**
         * Gets the {@link ContentType} based on the provided name
         *
         * @param name Represents the type name
         * @return the {@link ContentType}
         */
        public static ContentType get(final String name) {
            return switch (name.toLowerCase()) {
                case "text" -> TEXT;
                case "image" -> IMAGE;
                case "audio" -> AUDIO;
                case "location" -> LOCATION;
                case "document" -> DOCUMENT;
                case "video" -> VIDEO;
                case "button" -> BUTTON;
                default -> throw ErrorCode.get(SocialErrorCodes.UNSUPPORTED_WEBHOOK_EVENT, "i18n.social.unsupported.webhook.event");
            };
        }
    }
}