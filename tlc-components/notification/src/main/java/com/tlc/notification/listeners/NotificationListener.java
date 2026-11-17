package com.tlc.notification.listeners;

import com.tlc.notification.model.Notification;

/**
 * <p>
 * Listener for notification send.
 * </p>
 *
 * @param <T> subclasses of notification
 * @author Sathish
 * @version 1.0
 */
public interface NotificationListener<T extends Notification<?, ?>> extends SuccessListener<T>, PreWorkListener<T>, ErrorListener<T> {
}
