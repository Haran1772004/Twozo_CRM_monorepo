package com.tlc.notification.service.conversationmode;

import com.tlc.notification.model.NotificationConfiguration;

/**
 * <p>
 * Provides the receiver service for notification.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface NotificationReceiver<T extends NotificationConfiguration, R> {

    /**
     * <p>
     * Receives the notification from the sender.
     * </p>
     *
     * @param configuration An entity that holds the configuration attributes
     * @return {@link Object}
     */
    R receive(final T configuration);
}