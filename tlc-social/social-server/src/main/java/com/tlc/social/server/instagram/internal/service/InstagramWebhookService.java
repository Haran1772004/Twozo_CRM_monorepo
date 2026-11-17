package com.tlc.social.server.instagram.internal.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.instagram.model.InstagramPage;
import com.tlc.social.common.instagram.status.InstagramErrorCodes;
import com.tlc.social.server.service.SocialPlatformService;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Facilitates instagram's webhook related operations.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramWebhookService extends SocialPlatformService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstagramWebhookService.class);

    private final Map<String, String> properties;
    private final InstagramPageTokenService instagramPageTokenService;

    private InstagramWebhookService() {
        properties = new HashMap<>();
        instagramPageTokenService = InstagramPageTokenService.getInstance();
    }

    private static final class InstanceHolder {
        private static final InstagramWebhookService FACEBOOK_WEBHOOK_SERVICE = new InstagramWebhookService();
    }

    /**
     * <p>
     * Gets the instance of {@link InstagramWebhookService}
     * </p>
     *
     * @return the instance of {@link InstagramWebhookService}
     */
    public static InstagramWebhookService getInstance() {
        return InstanceHolder.FACEBOOK_WEBHOOK_SERVICE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param configurations Represents the configurations
     */
    @Override
    public void setProperties(final Configurations configurations) {
        properties.put(FBFields.GRAPH_API, configurations.getConfig(SyncServiceFields.APP_GRAPH_API_ENDPOINT));
        properties.put(FBFields.SUBSCRIBED_FIELDS, configurations.getConfig(SyncServiceFields.PAGE_SUBSCRIBED_FIELDS));
    }

    /**
     * Subscribes the instagram pages to the webhook
     *
     * @param instagramPage Represents the {@link InstagramPage}
     * @return the response
     */
    public JsonObject subscribe(final InstagramPage instagramPage) {
        final String url = getWebhookSubscriptionUrl(instagramPage);
        final Map<String, String> headers = new HashMap<>();

        headers.put(FBFields.CONTENT_TYPE, "application/json");

        try {
            return HttpUtil.makeRequest(new HttpRequest(new URL(url), Collections.emptyMap(), HttpMethod.POST, headers));
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Instagram: Failed to subscribe instagram page to the webhook. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(InstagramErrorCodes.WEBHOOK_PAGE_SUBSCRIPTION_FAILED, "i18n.social.instagram.webhook.page.subscription.failed");
        }
    }

    /**
     * Unsubscribes the instagram pages from the webhook
     *
     * @param page Represents the {@link InstagramPage}
     * @return the response
     */
    public JsonObject unsubscribe(final InstagramPage page) {
        final String url = getWebhookSubscriptionUrl(page);

        try {
            return HttpUtil.makeRequest(new HttpRequest(new URL(url), Collections.emptyMap(), HttpMethod.DELETE, Collections.emptyMap()));
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Facebook: Failed to unsubscribe instagram page from the webhook. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(InstagramErrorCodes.WEBHOOK_PAGE_UN_SUBSCRIPTION_FAILED, "i18n.social.instagram.webhook.page.unsubscription.failed");
        }
    }

    /**
     * <p>
     * Gets the webhook subscription url
     * </p>
     *
     * @param page Represents the {@link InstagramPage}
     * @return the url
     */
    private String getWebhookSubscriptionUrl(final InstagramPage page) {
        final String pageAccessToken = instagramPageTokenService.getLinkedPageToken(page.id());

        return String.format("%s/%s/subscribed_apps?subscribed_fields=%s&access_token=%s", properties.get(FBFields.GRAPH_API), page.getFbPageExternalId(), properties.get(FBFields.SUBSCRIBED_FIELDS), pageAccessToken);
    }
}
