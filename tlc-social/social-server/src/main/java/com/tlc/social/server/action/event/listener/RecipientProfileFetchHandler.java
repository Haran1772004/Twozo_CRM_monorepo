package com.tlc.social.server.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.file.common.models.Attachment;
import com.tlc.social.common.event.CommonSocialEvents;
import com.tlc.social.common.fields.ChatFields;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.model.Channel;
import com.tlc.social.common.model.RecipientProfile;
import com.tlc.social.server.manager.ChatManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Objects;
import java.util.Optional;

/**
 * A listener which listens to the recipient profile fetch event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = CommonSocialEvents.RETRIEVE_PLATFORM_AND_RECIPIENT_PROFILE)
public class RecipientProfileFetchHandler implements EventConsumer<byte[]> {

    private final ChatManager chatManager;

    public RecipientProfileFetchHandler() {
        chatManager = ChatManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param bytes Data to be consumed
     * @return the response as bytes
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject data = Json.object(bytes);
        final JsonObject response = Json.object();
        final Long chatId = data.getLong(ChatFields.CHAT_ID);
        final Optional<RecipientProfile> optionalRecipientProfile = chatManager.getRecipientProfile(chatId);

        if (optionalRecipientProfile.isPresent()) {
            final RecipientProfile recipientProfile = optionalRecipientProfile.get();
            final Attachment profilePicture = recipientProfile.getProfilePicture();

            chatManager.getChannel(chatId).ifPresent(channel -> response.put(ChatFields.PLATFORM,
                    Channel.ChannelType.getPlatform(channel.getType().getId()).getId()));
            response.put(ChatFields.RECIPIENT_NAME, recipientProfile.getName());

            if (Objects.nonNull(profilePicture)) {
                response.put(ChatFields.PROFILE_PICTURE_ID, profilePicture.id());
            }
        } else {
            throw ErrorCode.get(SocialErrorCodes.RECIPIENT_PROFILE_NOT_FOUND, "i18n.social.chat.recipient.profile.not.found");
        }

        return response.getBytes();
    }
}
