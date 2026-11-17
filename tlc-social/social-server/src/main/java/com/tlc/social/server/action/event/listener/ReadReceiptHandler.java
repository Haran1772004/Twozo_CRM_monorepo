package com.tlc.social.server.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.event.CommonSocialEvents;
import com.tlc.social.common.fields.ChatFields;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.model.Platform;

import com.tlc.social.server.facebook.internal.service.FBService;
import com.tlc.social.server.facebook.manager.FBPageManager;
import com.tlc.social.server.instagram.internal.service.InstagramService;
import com.tlc.social.server.instagram.manager.InstagramPageManager;
import com.tlc.social.server.manager.ChatManager;

import com.tlc.social.server.whatsapp.internal.service.WAService;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.List;

/**
 * A listener which listens to the read receipt events
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = CommonSocialEvents.SEND_READ_RECEIPT)
public class ReadReceiptHandler implements EventConsumer<byte[]> {

    private final ChatManager chatManager;
    private final FBPageManager fbPageManager;
    private final FBService fbService;
    private final WABusinessManager waBusinessManager;
    private final WAService waService;
    private final InstagramPageManager instagramPageManager;
    private final InstagramService instagramService;

    public ReadReceiptHandler() {
        chatManager = ChatManager.getInstance();
        fbPageManager = FBPageManager.getInstance();
        fbService = FBService.getInstance();
        waBusinessManager = WABusinessManager.getInstance();
        waService = WAService.getInstance();
        instagramPageManager = InstagramPageManager.getInstance();
        instagramService = InstagramService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param bytes Data to be consumed
     * @return the response in bytes
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject data = Json.object(bytes);
        final JsonObject response = Json.object();
        final Long chatId = data.getLong(ChatFields.CHAT_ID);

        if (!chatManager.exists(List.of(chatId))) {
            throw ErrorCode.get(SocialErrorCodes.CHAT_NOT_FOUND, "i18n.social.chat.not.found");
        }

        if (!chatManager.hasMessageWindowExpired(chatId)) {
            switch (Platform.get(data.getShort(ChatFields.PLATFORM))) {
                case FACEBOOK -> fbService.sendReadReceipt(fbPageManager.getFBPageRecipient(chatId));
                case WHATSAPP -> waBusinessManager.getInboundMessage(chatId, data.containsKey(ChatFields.MESSAGE_ID)
                        ? data.getLong(ChatFields.MESSAGE_ID) : null).ifPresent(waService::sendReadReceipt);
                case INSTAGRAM -> instagramService.sendReadReceipt(instagramPageManager.getInstagramPageRecipient(chatId));
            }
        }
        chatManager.resetUnreadCount(chatId);

        return response.getBytes();
    }
}