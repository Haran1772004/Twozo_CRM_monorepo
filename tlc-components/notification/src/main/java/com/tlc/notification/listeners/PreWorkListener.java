package com.tlc.notification.listeners;

import com.tlc.notification.model.Notification;

import java.util.Collection;

/**
 * <p>
 * Listener for post send notification.
 * </p>
 *
 * @param <T> subclasses of notification
 * @author Sathish
 * @version 1.0
 */
public interface PreWorkListener<T extends Notification<?, ?>> extends ListenerOrder, IgnoreIfExists {

    /**
     * <p>
     * Listens the notifications on success.
     * </p>
     *
     * @param notifications collection of {@link Notification}.
     */
    void listenNotifications(final Collection<T> notifications);
}
