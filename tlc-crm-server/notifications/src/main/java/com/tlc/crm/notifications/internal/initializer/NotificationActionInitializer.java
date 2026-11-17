package com.tlc.crm.notifications.internal.initializer;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.service.NotificationLoader;
import com.tlc.crm.notification.app.service.NotificationLoaderHolder;
import com.tlc.crm.notifications.fields.NotificationAttributes;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A consumer that consumes the notification requests and triggers the respective notification actions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class NotificationActionInitializer implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationActionInitializer.class);

    private final NotificationLoaderHolder notificationLoaderHolder;

    public NotificationActionInitializer() {
        notificationLoaderHolder = NotificationLoaderHolder.getInstance();
    }

    /**
     * <p>
     * Enables to process the {@link StreamMessage} data.
     * </p>
     *
     * @param streamMessage Represents {@link StreamMessage}.
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final JsonObject requestParameters = Json.object(streamMessage.data());
        final long orgId = requestParameters.getLong(NotificationAttributes.ORG_ID);
        final JsonArray jsonArray = requestParameters.getJsonArray(NotificationAttributes.TYPES);
        final Collection<AppNotificationEventType> appNotificationEventTypes = new ArrayList<>();

        for (int index = 0; index < jsonArray.size(); index++) {
            final JsonObject typeJson = jsonArray.getJsonObject(index);

            appNotificationEventTypes.add(AppNotificationEventType.valueOf(typeJson.getString(NotificationAttributes.NAME)));
        }
        requestParameters.remove(NotificationAttributes.TYPES);
        requestParameters.remove(NotificationAttributes.ORG_ID);

        try {
            for (final AppNotificationEventType appNotificationEventType : appNotificationEventTypes) {
                final NotificationLoader notificationLoader = notificationLoaderHolder.get(appNotificationEventType);

                if (Objects.nonNull(notificationLoader)) {
                    notificationLoader.load(orgId, requestParameters);
                }
            }
        } catch (Throwable throwable) {
            LOGGER.error("Notification load failed for Org ID : {}", orgId);
        }
    }
}
