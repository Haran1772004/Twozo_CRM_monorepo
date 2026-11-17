package com.tlc.social.server.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.file.common.models.Attachment;
import com.tlc.social.common.model.Platform;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A model which contains message send request details
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@Getter
@Setter
public class MessageSendRequest implements MessageRequest {

    private Long chatId;
    private String message;
    private Collection<Attachment> attachments;
    private Collection<Long> attachmentIds;
    private Platform platform;
    private boolean isNewChat;
    private TemplateRequestData templateRequestData;
    private Long senderId;
    private Collection<String> recipientPhoneNumbers;

    /**
     * <p>
     * Checks whether the request has text message or not
     * </p>
     * @return true if it has message, false otherwise
     */
    public boolean hasMessage() {
        return Objects.nonNull(message);
    }

    /**
     * <p>
     * Checks whether the request has attachments or not
     * </p>
     *
     * @return true if it has attachments, false otherwise
     */
    public boolean hasAttachments() {
        return Objects.nonNull(attachments) && !attachments.isEmpty();
    }

    public boolean hasTemplate() {
        return Objects.nonNull(templateRequestData);
    }

    /**
     * A record which represents the template send request data
     *
     * @param templateId Represents the template id
     * @param paramData  Represents the json data which contains template parameter details
     * @author Maharaja S
     * @version 1.0
     */
    public record TemplateRequestData(Long templateId, JsonObject paramData) {
    }
}
