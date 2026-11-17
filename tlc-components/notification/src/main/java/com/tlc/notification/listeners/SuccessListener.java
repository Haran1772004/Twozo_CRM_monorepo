package com.tlc.notification.listeners;

import com.tlc.notification.model.Notification;

import java.util.Collection;

/**
 * <p>
 * Listener for pre send notification.
 * </p>
 *
 * @param <T> subclasses of notification
 * @author Sathish
 * @version 1.0
 */
public interface SuccessListener<T extends Notification<?, ?>> extends ListenerOrder, IgnoreIfExists {

    /**
     * <p>
     * Listens the notifications before send.
     * </p>
     *
     * @param notifications collection of {@link Notification}.
     */
    void listenNotifications(final Collection<T> notifications);
}
