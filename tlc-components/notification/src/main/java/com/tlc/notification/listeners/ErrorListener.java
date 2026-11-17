package com.tlc.notification.listeners;

import com.tlc.notification.model.Notification;

import java.util.Collection;

/**
 * <p>
 * Listener for notification send on error.
 * </p>
 *
 * @param <T> subclasses of notification
 * @author Sathish
 * @version 1.0
 */
public interface ErrorListener<T extends Notification<?, ?>> extends ListenerOrder, IgnoreIfExists {

    /**
     * <p>
     * Listens the notifications on failure.
     * </p>
     *
     * @param notifications collection of {@link Notification}.
     */
    void listenNotifications(final Collection<T> notifications);
}
