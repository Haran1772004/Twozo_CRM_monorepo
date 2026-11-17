package com.tlc.notification.service.notification;

import com.tlc.notification.model.Notification;
import com.tlc.notification.model.Receiver;

import java.util.Collection;

/**
 * <p>
 * Provides the receiver service for notification.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
interface NotificationReceiver<T extends Notification<?, ?>, R extends Receiver> {

    /**
     * <p>
     * Receives the notification from the sender.
     * </p>
     *
     * @param notification An entity that holds the notification attributes
     * @return {@link Object}
     */
    Collection<R> receive(final T notification);
}
