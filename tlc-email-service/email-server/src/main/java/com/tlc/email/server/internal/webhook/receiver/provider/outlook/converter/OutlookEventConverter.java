package com.tlc.email.server.internal.webhook.receiver.provider.outlook.converter;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.email.common.converter.Converter;

import com.tlc.email.server.fields.WebhookReceiverFields;
import com.tlc.email.server.internal.webhook.receiver.provider.outlook.model.EncryptedContent;
import com.tlc.email.server.internal.webhook.receiver.provider.outlook.model.OutlookEvent;
import com.tlc.email.server.internal.webhook.receiver.provider.outlook.model.OutlookEventNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Converts the outlook webhook notification.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see Converter
 */
public class OutlookEventConverter implements Converter<JsonObject, OutlookEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutlookEventConverter.class);

    private OutlookEventConverter() {
    }

    /**
     * <p>
     * Holds the single instance of {@link OutlookEventConverter}.
     * </p>
     */
    private static class Instance {
        private static final OutlookEventConverter INSTANCE = new OutlookEventConverter();
    }

    /**
     * Gets the instance of {@link OutlookEventConverter}
     *
     * @return {@link OutlookEventConverter}
     */
    public static OutlookEventConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject represents the {@link JsonObject}
     * @return {@link OutlookEvent}
     */
    @Override
    public OutlookEvent convert(final JsonObject jsonObject) {
        final OutlookEvent outlookEvent = new OutlookEvent();

        if (jsonObject.containsKey(WebhookReceiverFields.VALUE)) {
            final JsonArray jsonArray = jsonObject.getJsonArray(WebhookReceiverFields.VALUE);
            final int size = jsonArray.size();
            final Collection<OutlookEventNotification> outlookEventNotifications = new ArrayList<>();

            for (int valueIndex = 0; valueIndex < size; valueIndex++) {
                final OutlookEventNotification outlookEventNotification = new OutlookEventNotification();
                final JsonObject notificationJson = jsonArray.getJsonObject(valueIndex);

                outlookEventNotification.setSubscriptionId(notificationJson.getString(WebhookReceiverFields.SUBSCRIPTION_ID));
                outlookEventNotification.setChangeType(OutlookEventNotification.ChangeType.valueOf(notificationJson.getString(WebhookReceiverFields.CHANGE_TYPE).toUpperCase()));
                outlookEventNotification.setExpirationDateTime(notificationJson.getString(WebhookReceiverFields.SUBSCRIPTION_EXPIRATION_DATE_TIME));
                outlookEventNotification.setTenantId(notificationJson.getString(WebhookReceiverFields.TENANT_ID));
                outlookEventNotification.setResource(notificationJson.getString(WebhookReceiverFields.RESOURCE));
                outlookEventNotification.setEncryptedContent(convertEncryptedContent(notificationJson.getJsonObject(WebhookReceiverFields.ENCRYPTED_CONTENT)));

                outlookEventNotifications.add(outlookEventNotification);
            }

            outlookEvent.setValue(outlookEventNotifications);
        }

        return outlookEvent;
    }

    /**
     * Converts the encrypted content from given json.
     *
     * @param jsonObject Represents the {@link JsonObject}
     * @return {@link EncryptedContent}
     */
    private EncryptedContent convertEncryptedContent(final JsonObject jsonObject) {
        final EncryptedContent encryptedContent = new EncryptedContent();

        encryptedContent.setData(jsonObject.getString(WebhookReceiverFields.DATA));
        encryptedContent.setEncryptionCertificateId(jsonObject.getString(WebhookReceiverFields.ENCRYPTED_CERTIFICATE_ID));
        encryptedContent.setKey(jsonObject.getString(WebhookReceiverFields.DATA_KEY));
        encryptedContent.setSignature(jsonObject.getString(WebhookReceiverFields.DATA_SIGNATURE));

        return encryptedContent;
    }
}

