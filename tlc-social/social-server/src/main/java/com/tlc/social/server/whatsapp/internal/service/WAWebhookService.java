package com.tlc.social.server.whatsapp.internal.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.internal.status.WAErrorCodes;
import com.tlc.social.common.whatsapp.model.WABusinessAccount;
import com.tlc.social.server.service.SocialPlatformService;

import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.whatsapp.WATokenReadRequest;

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
 * Provides the Whatsapp Business webhook related services
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAWebhookService extends SocialPlatformService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WAWebhookService.class);

    private final Map<String, String> properties;
    private final WASyncTokenService waSyncTokenService;

    private WAWebhookService() {
        properties = new HashMap<>();
        waSyncTokenService = WASyncTokenService.getInstance();
    }

    /**
     * Holds the instance of {@link WAWebhookService}
     */
    private static final class InstanceHolder {
        private static final WAWebhookService WA_WEBHOOK_SERVICE = new WAWebhookService();
    }

    /**
     * <p>
     * Gets the instance of {@link WAWebhookService}
     * </p>
     *
     * @return the instance of {@link WAWebhookService}
     */
    public static WAWebhookService getInstance() {
        return InstanceHolder.WA_WEBHOOK_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the configurations
     */
    @Override
    public void setProperties(final Configurations configurations) {
        properties.put(WAFields.GRAPH_API, configurations.getConfig(SyncServiceFields.APP_GRAPH_API_ENDPOINT));
    }

    /**
     * Subscribes the Whatsapp business account to the webhook
     *
     * @param waBusinessAccount Represents the {@link WABusinessAccount}
     */
    public void subscribe(final WABusinessAccount waBusinessAccount) {
        final Long waBusinessAccountId = waBusinessAccount.id();
        final WATokenReadRequest waTokenReadRequest = SyncTokenRequestInstanceProvider.getWATokenReadRequest(waBusinessAccountId, TokenRequest.TokenType.ACCESS);
        final String businessToken = waSyncTokenService.getAccessToken(waTokenReadRequest);
        final String url = getWebhookSubscriptionUrl(waBusinessAccount);
        final Map<String, String> headers = new HashMap<>();

        headers.put(WAFields.AUTHORIZATION, String.join(" ", WAFields.BEARER, businessToken));

        try {
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(url), Collections.emptyMap(), HttpMethod.POST, headers));

            if (response.getBoolean(WAFields.SUCCESS)) {
                LOGGER.info("Whatsapp: Webhook subscription for the WhatsApp Business Account completed successfully. WABusinessAccount ID:{}", waBusinessAccountId);
            } else {
                throw ErrorCode.get(WAErrorCodes.WEBHOOK_SUBSCRIPTION_FAILED, "i18n.social.whatsapp.webhook.subscription.failed");
            }
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Whatsapp: Failed to subscribe business account to the webhook. Reason: {}", throwable.getMessage());
            throw ErrorCode.get(WAErrorCodes.WEBHOOK_SUBSCRIPTION_FAILED, "i18n.social.whatsapp.webhook.subscription.failed");
        }
    }

    /**
     * <p>
     * Gets the webhook subscription url
     * </p>
     *
     * @param waBusinessAccount Represents the {@link WABusinessAccount}
     * @return the url
     */
    private String getWebhookSubscriptionUrl(final WABusinessAccount waBusinessAccount) {
        return String.format("%s/%s/subscribed_apps", properties.get(WAFields.GRAPH_API), waBusinessAccount.getExternalWABAId());
    }
}