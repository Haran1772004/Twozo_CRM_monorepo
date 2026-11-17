package com.tlc.social.server.internal.webhook.event.service.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.model.Platform;
import com.tlc.social.server.facebook.internal.webhook.converter.FBDeliveryMessageEventConverter;
import com.tlc.social.server.facebook.internal.webhook.converter.FBReadMessageEventConverter;
import com.tlc.social.server.facebook.internal.webhook.converter.FBTextMessageEventConverter;
import com.tlc.social.server.instagram.internal.webhook.converter.InstagramDeliveryMessageEventConverter;
import com.tlc.social.server.instagram.internal.webhook.converter.InstagramReadMessageEventConverter;
import com.tlc.social.server.instagram.internal.webhook.converter.InstagramTextMessageEventConverter;
import com.tlc.social.server.internal.webhook.event.converter.impl.PageDeliveryMessageEventConverter;
import com.tlc.social.server.internal.webhook.event.converter.impl.PageReadMessageEventConverter;
import com.tlc.social.server.internal.webhook.event.converter.impl.PageTextMessageEventConverter;
import com.tlc.social.server.internal.webhook.event.model.WebhookMessageEvent;
import com.tlc.social.server.internal.webhook.event.service.PageWebhookMessageEventService;

public class PageWebhookMessageEventServiceImpl implements PageWebhookMessageEventService {

    private final FBTextMessageEventConverter fbTextMessageEventConverter;
    private final FBReadMessageEventConverter fbReadMessageEventConverter;
    private final FBDeliveryMessageEventConverter fbDeliveryMessageEventConverter;
    private final InstagramTextMessageEventConverter instagramTextMessageEventConverter;
    private final InstagramDeliveryMessageEventConverter instagramDeliveryMessageEventConverter;
    private final InstagramReadMessageEventConverter instagramReadMessageEventConverter;

    private PageWebhookMessageEventServiceImpl() {
        fbTextMessageEventConverter = FBTextMessageEventConverter.getInstance();
        fbReadMessageEventConverter = FBReadMessageEventConverter.getInstance();
        fbDeliveryMessageEventConverter = FBDeliveryMessageEventConverter.getInstance();
        instagramTextMessageEventConverter = InstagramTextMessageEventConverter.getInstance();
        instagramDeliveryMessageEventConverter = InstagramDeliveryMessageEventConverter.getInstance();
        instagramReadMessageEventConverter = InstagramReadMessageEventConverter.getInstance();
    }

    private static final class Instance {
        private static final PageWebhookMessageEventServiceImpl WEBHOOK_MESSAGE_EVENT_SERVICE = new PageWebhookMessageEventServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link PageWebhookMessageEventServiceImpl}
     * </p>
     *
     * @return the instance of {@link PageWebhookMessageEventServiceImpl}
     */
    public static PageWebhookMessageEventServiceImpl getInstance() {
        return Instance.WEBHOOK_MESSAGE_EVENT_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param data Represents the data to be converted
     * @return the {@link WebhookMessageEvent}.
     */
    @Override
    public WebhookMessageEvent get(final JsonObject data, final Platform platform) {
        final PageTextMessageEventConverter<?> textMessageEventConverter;
        final PageDeliveryMessageEventConverter<?> deliveryMessageEventConverter;
        final PageReadMessageEventConverter<?> readMessageEventConverter;

        switch (platform) {
            case FACEBOOK -> {
                textMessageEventConverter = fbTextMessageEventConverter;
                deliveryMessageEventConverter = fbDeliveryMessageEventConverter;
                readMessageEventConverter = fbReadMessageEventConverter;
            }
            case INSTAGRAM -> {
                textMessageEventConverter = instagramTextMessageEventConverter;
                deliveryMessageEventConverter = instagramDeliveryMessageEventConverter;
                readMessageEventConverter = instagramReadMessageEventConverter;
            }
            default -> throw ErrorCode.get(SocialErrorCodes.UNSUPPORTED_SOCIAL_PLATFORM, "i18n.social.unsupported.platform");
        }

        if (data.containsKey(FBFields.MESSAGE)) {
            return textMessageEventConverter.convert(data);
        } else if (data.containsKey(FBFields.READ)) {
            return readMessageEventConverter.convert(data);
        } else if (data.containsKey(FBFields.DELIVERY)) {
            return deliveryMessageEventConverter.convert(data);
        }

        return null;
    }
}
