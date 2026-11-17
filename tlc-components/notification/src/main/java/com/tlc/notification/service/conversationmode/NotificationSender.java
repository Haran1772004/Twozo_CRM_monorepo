package com.tlc.notification.service.conversationmode;

import com.tlc.notification.model.Notification;

/**
 * <p>
 * Provides the sender service for notification.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface NotificationSender<T extends Notification<?, ?>> {

    /**
     * <p>
     * Sends the notification to the given receiver.
     * </p>
     *
     * @param notification An entity that holds the notification attributes.
     */
    void send(final T notification);
}