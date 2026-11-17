package com.tlc.notification.internal.listener;

import com.tlc.commons.code.ErrorCode;

import com.tlc.notification.internal.status.NotificationErrorCodes;

import com.tlc.notification.listeners.NotificationType;
import com.tlc.notification.listeners.SuccessListener;
import com.tlc.notification.model.Notification;
import com.tlc.notification.listeners.PreWorkListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.Objects;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;

/**
 * <p>
 * Processor for post sent data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SuccessHandler {

    private final NotificationListenerContainer notificationListenerContainer;

    private static final Logger LOGGER = LoggerFactory.getLogger(SuccessHandler.class);

    public SuccessHandler() {
        this.notificationListenerContainer = NotificationListenerContainer.getInstance();
    }

    /**
     * <p>
     * Handles the {@link PreWorkListener}.
     * </p>
     *
     * @param dataMap to process {@link PreWorkListener} for corresponding {@link NotificationType}
     */
    public void handle(final SortedMap<NotificationType, List<Notification<?, ?>>> dataMap) {
        final long time = System.currentTimeMillis();

        dataMap.forEach((notification, notifications) -> handle(notification, notifications,
                Collections.unmodifiableSet(dataMap.keySet())));
        LOGGER.debug("Time taken to handle success for notifications {} {}", dataMap.keySet(), System.currentTimeMillis() - time);
    }

    /**
     * <p>
     * Handles the {@link SuccessListener}.
     * </p>
     *
     * @param notificationType  {@link NotificationType} object
     * @param notifications     collection of {@link Notification} object
     * @param notificationTypes set of {@link NotificationType} object
     */
    private void handle(final NotificationType notificationType, final Collection<Notification<?, ?>> notifications,
                        final Set<NotificationType> notificationTypes) {
        final Collection<List<SuccessListener<Notification<?, ?>>>> listenersList = notificationListenerContainer.getSuccessListeners(notificationType);

        if (Objects.nonNull(listenersList) && !notifications.isEmpty()) {
            final long time = System.currentTimeMillis();

            LOGGER.debug("Notification listener found for notification {} to handle success", notificationType);
            final Collection<Notification<?, ?>> unmodifiableNotification = Collections.unmodifiableCollection(notifications);

            for (final List<SuccessListener<Notification<?, ?>>> listeners : listenersList) {
                for (final SuccessListener<Notification<?, ?>> listener : listeners) {
                    try {
                        if (!listener.ignore(notificationTypes)) {
                            listener.listenNotifications(unmodifiableNotification);
                        }
                    } catch (Throwable throwable) {
                        throw ErrorCode.getLite(NotificationErrorCodes.UNABLE_TO_LISTEN_AFTER_SENT, throwable);
                    }
                }
            }
            LOGGER.info("Time taken to complete the adding listener for notification {} {}", notificationType
                    , System.currentTimeMillis() - time);
        }
    }
}
