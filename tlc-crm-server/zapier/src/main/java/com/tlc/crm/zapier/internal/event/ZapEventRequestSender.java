package com.tlc.crm.zapier.internal.event;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.crm.zapier.api.models.ZapEventWebhook;
import com.tlc.crm.zapier.fields.ZapAttributes;

import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for sending Zapier event requests.
 * It wraps the logic to send HTTP payloads to Zapier Webhook URLs.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ZapEventRequestSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZapEventRequestSender.class);

    private ZapEventRequestSender() {
    }

    private static final class Instance {
        private static final ZapEventRequestSender ZAP_EVENT_REQUEST_SENDER = new ZapEventRequestSender();
    }

    /**
     * Returns the singleton instance of {@code ZapEventRequestSender}.
     *
     * @return The singleton instance.
     */
    public static ZapEventRequestSender getInstance() {
        return Instance.ZAP_EVENT_REQUEST_SENDER;
    }

    /**
     * Sends the given ZapEventRequest to the configured webhook URL.
     *
     * @param zapEventRequest The request object containing URL, payload.
     */
    public void send(final ZapEventRequest zapEventRequest) {
        for (final ZapEventWebhook eventWebhook : zapEventRequest.zapEventWebhooks()) {
            final String url = eventWebhook.getUrl();
            final Long orgId = eventWebhook.orgId();

            for (final JsonObject requestBody : zapEventRequest.payloads()) {
                final Map<String, String> headers = new HashMap<>();
                final Map<String, String> requestParams = new HashMap<>();

                headers.put(ZapAttributes.CONTENT_TYPE, ZapAttributes.JSON_CONTENT_TYPE);
                requestParams.put(ZapAttributes.REQUEST_BODY, requestBody.toString());

                try {
                    HttpUtil.makeRequest(new HttpRequest(new URL(url), requestParams, HttpMethod.POST, headers));
                } catch (Throwable throwable) {
                    LoggerUtil.logError(LOGGER, throwable, "Zapier: Error occurred while sending data to the hook URL. OrgId: {}, Reason: {}, Stack: ", orgId, throwable.getMessage(), throwable);
                }
            }
        }
    }
}
