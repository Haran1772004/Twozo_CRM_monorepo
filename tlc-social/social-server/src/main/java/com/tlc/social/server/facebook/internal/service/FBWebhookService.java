package com.tlc.social.server.facebook.internal.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.facebook.internal.status.FBErrorCodes;
import com.tlc.social.common.facebook.model.FBPage;

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
 * Provides the services related to facebook webhook service
 *
 * @author Maharaja S
 * @version 1.0
 */
public class FBWebhookService extends SocialPlatformService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FBWebhookService.class);

    private final Map<String, String> properties;
    private final FBPageTokenService fbPageTokenService;

    private FBWebhookService() {
        properties = new HashMap<>();
        fbPageTokenService = FBPageTokenService.getInstance();
    }

    private static final class InstanceHolder {
        private static final FBWebhookService FACEBOOK_WEBHOOK_SERVICE = new FBWebhookService();
    }

    /**
     * <p>
     * Gets the instance of {@link FBWebhookService}
     * </p>
     *
     * @return the instance of {@link FBWebhookService}
     */
    public static FBWebhookService getInstance() {
        return FBWebhookService.InstanceHolder.FACEBOOK_WEBHOOK_SERVICE;
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
     * Subscribes the facebook pages to the webhook
     *
     * @param fbPage Represents the {@link FBPage}
     * @return the response
     */
    public JsonObject subscribe(final FBPage fbPage) {
        final String url = getWebhookSubscriptionUrl(fbPage);
        final Map<String, String> headers = new HashMap<>();
        final Map<String, String> requestParams = new HashMap<>();
        final JsonObject requestBody = Json.object();

        requestBody.put(FBFields.SUBSCRIBED_FIELDS, properties.get(FBFields.SUBSCRIBED_FIELDS));
        headers.put(FBFields.CONTENT_TYPE, "application/json");
        requestParams.put(FBFields.REQUEST_BODY, requestBody.toString());

        try {
            return HttpUtil.makeRequest(new HttpRequest(new URL(url), requestParams, HttpMethod.POST, headers));
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Facebook: Failed to subscribe facebook page to the webhook. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(FBErrorCodes.WEBHOOK_PAGE_SUBSCRIPTION_FAILED, "i18n.social.facebook.webhook.page.subscription.failed");
        }
    }

    /**
     * Unsubscribes the facebook pages from the webhook
     *
     * @param page Represents the {@link FBPage}
     * @return the response
     */
    public JsonObject unsubscribe(final FBPage page) {
        final String url = getWebhookSubscriptionUrl(page);

        try {
            return HttpUtil.makeRequest(new HttpRequest(new URL(url), Collections.emptyMap(), HttpMethod.DELETE, Collections.emptyMap()));
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Facebook: Failed to unsubscribe facebook page from the webhook. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(FBErrorCodes.WEBHOOK_PAGE_UN_SUBSCRIPTION_FAILED, "i18n.social.facebook.webhook.page.unsubscription.failed");
        }
    }

    /**
     * <p>
     * Gets the webhook subscription url
     * </p>
     *
     * @param page Represents the {@link FBPage}
     * @return the url
     */
    private String getWebhookSubscriptionUrl(final FBPage page) {
        final String pageAccessToken = fbPageTokenService.get(page.id());

        return String.format("%s/%s/subscribed_apps?access_token=%s", properties.get(FBFields.GRAPH_API), page.getExternalPageId(), pageAccessToken);
    }
}
