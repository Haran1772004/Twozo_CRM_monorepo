package com.tlc.social.server.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.event.CommonSocialEvents;
import com.tlc.social.common.fields.MessageFields;
import com.tlc.social.common.internal.status.SocialErrorCodes;

import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.internal.status.WAErrorCodes;
import com.tlc.social.common.whatsapp.model.template.Body;
import com.tlc.social.common.whatsapp.model.template.Button;
import com.tlc.social.common.whatsapp.model.template.Header;
import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.social.server.converter.MessageSendRequestConverter;
import com.tlc.social.server.facebook.internal.service.FBOutboundService;
import com.tlc.social.server.instagram.internal.service.InstagramOutboundService;
import com.tlc.social.server.manager.ChatManager;
import com.tlc.social.server.model.MessageSendRequest;

import com.tlc.social.server.whatsapp.internal.service.WAOutboundService;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A handler which listens to the outbound message event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = CommonSocialEvents.SEND_OUTBOUND_MESSAGE)
public class OutboundMessageHandler implements EventConsumer<byte[]> {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{.+?}}");

    private final FBOutboundService fbOutboundService;
    private final WAOutboundService waOutboundService;
    private final InstagramOutboundService instagramOutboundService;
    private final ChatManager chatManager;
    private final MessageSendRequestConverter messageSendRequestConverter;
    private final WABusinessManager waBusinessManager;

    public OutboundMessageHandler() {
        fbOutboundService = FBOutboundService.getInstance();
        waOutboundService = WAOutboundService.getInstance();
        instagramOutboundService = InstagramOutboundService.getInstance();
        chatManager = ChatManager.getInstance();
        messageSendRequestConverter = MessageSendRequestConverter.getInstance();
        waBusinessManager = WABusinessManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param bytes Data to be consumed
     * @return the response in byte array
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject data = Json.object(bytes);
        final MessageSendRequest messageSendRequest = messageSendRequestConverter.convert(data);
        final Long chatId = messageSendRequest.getChatId();

        if (Objects.nonNull(chatId) && !chatManager.exists(List.of(messageSendRequest.getChatId()))) {
            throw ErrorCode.get(SocialErrorCodes.CHAT_NOT_FOUND, "i18n.social.chat.not.found");
        }

        if (!messageSendRequest.hasMessage() && !messageSendRequest.hasAttachments() && !messageSendRequest.hasTemplate()) {
            throw ErrorCode.get(SocialErrorCodes.MESSAGE_OR_ATTACHMENT_IS_REQUIRED, "i18n.social.message.or.attachment.required");
        }

        final JsonObject response = switch (messageSendRequest.getPlatform()) {
            case FACEBOOK -> fbOutboundService.send(messageSendRequest);
            case INSTAGRAM -> instagramOutboundService.send(messageSendRequest);
            case WHATSAPP -> {
                if (isValidDetails(messageSendRequest)) {
                    yield waOutboundService.send(messageSendRequest);
                } else {
                    yield Json.object().put(MessageFields.ERRORS, List.of("i18n.social.whatsapp.template.placeholder.values.required"));
                }
            }
        };

        return response.getBytes();
    }

    /**
     * <p>
     * Checks whether the message request is valid and contains all the necessary data
     * </p>
     *
     * @param messageSendRequest Represents the {@link MessageSendRequest}
     */
    private boolean isValidDetails(final MessageSendRequest messageSendRequest) {

        switch (messageSendRequest.getPlatform()) {
            case WHATSAPP -> {
                final MessageSendRequest.TemplateRequestData templateRequestData = messageSendRequest.getTemplateRequestData();

                if (Objects.nonNull(templateRequestData)) {
                    final Optional<WATemplate> optionalTemplate = waBusinessManager.getTemplate(templateRequestData.templateId());

                    if (optionalTemplate.isPresent()) {
                        final WATemplate template = optionalTemplate.get();
                        final JsonObject paramData = templateRequestData.paramData();
                        final Header header = template.getHeader();
                        final Body body = template.getBody();
                        final Collection<Button> buttons = template.getButtons();

                        if (Objects.nonNull(header)) {
                            if (!validateHeaderParam(header, paramData)) {
                                return false;
                            }
                        }

                        if (Objects.nonNull(body)) {
                            if(!validateBodyParam(body, paramData)) {
                                return false;
                            }
                        }

                        if (Objects.nonNull(buttons) && !buttons.isEmpty()) {
                            if (!validateButtonParam(buttons, paramData)) {
                                return false;
                            }
                        }
                    } else {
                        throw ErrorCode.get(WAErrorCodes.TEMPLATE_NOT_FOUND, "i18n.social.whatsapp.template.not.found");
                    }
                }
            }
        }

        return true;
    }

    /**
     * <p>
     * Validate the header params in the request data
     * </p>
     *
     * @param header    Represents the template header
     * @param paramData Represents the param data
     */
    private boolean validateHeaderParam(final Header header, final JsonObject paramData) {
        final boolean containsHeader = paramData.containsKey(WAFields.HEADER);

        switch (header.getFormat()) {
            case TEXT -> {
                if (header.hasPlaceholder() && !containsHeader) {
                    return false;
                }
            }
            case IMAGE, VIDEO, DOCUMENT, LOCATION -> {
                if (!containsHeader) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * <p>
     * Validate the body params in the request data
     * </p>
     *
     * @param body      Represents the template body
     * @param paramData Represents the param data
     */
    private boolean validateBodyParam(final Body body, final JsonObject paramData) {
        if (body.hasPlaceholder()) {
            final JsonObject jsonObject = paramData.optJsonObject(WAFields.BODY);

            if (Objects.isNull(jsonObject)) {
                return false;
            } else {
                return jsonObject.size() == PLACEHOLDER_PATTERN.matcher(body.getText()).results().count();
            }
        }

        return true;
    }

    /**
     * <p>
     * Validate the button params in the request data
     * </p>
     *
     * @param buttons   Represents the collection of template buttons
     * @param paramData Represents the param data
     */
    private boolean validateButtonParam(final Collection<Button> buttons, final JsonObject paramData) {
        final Collection<Button> filteredButtons = buttons.stream().filter(button -> {
            final Button.ButtonType buttonType = button.getButtonType();

            if (Button.ButtonType.COPY_CODE == buttonType) {
                return true;
            } else {
                return Button.ButtonType.URL == buttonType && button.hasPlaceholder();
            }
        }).toList();

        if (filteredButtons.isEmpty()) {
            return true;
        }
        final JsonArray buttonParamArray = paramData.optJsonArray(WAFields.BUTTONS);

        return Objects.nonNull(buttonParamArray) && Objects.equals(filteredButtons.size(), buttonParamArray.size());
    }
}
