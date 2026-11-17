package com.tlc.notification.internal.listener;

import com.tlc.notification.listeners.ErrorListener;
import com.tlc.notification.listeners.NotificationListener;
import com.tlc.notification.listeners.NotificationType;
import com.tlc.notification.listeners.PreWorkListener;
import com.tlc.notification.listeners.SuccessListener;
import com.tlc.notification.model.Notification;

/**
 * <p>
 * To handle the notification listener.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class NotificationListenerHandler {

    private final NotificationListenerContainer notificationListenerContainer;

    /**
     * <p>
     * Provides the instance of {@link com.tlc.notification.internal.listener.NotificationListenerHandler}.
     * </p>
     */
    private final static class Instance {
        private static final NotificationListenerHandler INSTANCE = new NotificationListenerHandler();
    }

    /**
     * <p>
     * Provides the {@link NotificationListenerHandler} object.
     * </p>
     *
     * @return {@link NotificationListenerHandler} object
     */
    public static NotificationListenerHandler getInstance() {
        return Instance.INSTANCE;
    }

    private NotificationListenerHandler() {
        this.notificationListenerContainer = NotificationListenerContainer.getInstance();
    }

    /**
     * <p>
     * Adds the listener for notification send.
     * </p>
     *
     * @param notificationType {@link NotificationType} object
     * @param listener         {@link NotificationListener} object
     */
    public void addListener(final NotificationType notificationType, final NotificationListener<Notification<?, ?>> listener) {
        notificationListenerContainer.addPreWorkListener(notificationType, listener);
        notificationListenerContainer.addSuccessListener(notificationType, listener);
        notificationListenerContainer.addErrorListener(notificationType, listener);
    }

    /**
     * <p>
     * Adds the listener for before send notification.
     * </p>
     *
     * @param notificationType {@link NotificationType} object
     * @param listener         {@link PreWorkListener} object
     */
    public void addPreWorkListener(final NotificationType notificationType, final PreWorkListener<Notification<?, ?>> listener) {
        notificationListenerContainer.addPreWorkListener(notificationType, listener);
    }

    /**
     * <p>
     * Adds the listener for after sent notification.
     * </p>
     *
     * @param notificationType {@link NotificationType} object
     * @param listener         {@link SuccessListener} object
     */
    public void addSuccessListener(final NotificationType notificationType, final SuccessListener<Notification<?, ?>> listener) {
        notificationListenerContainer.addSuccessListener(notificationType, listener);
    }

    /**
     * <p>
     * Adds the listener for error occurred when notification send.
     * </p>
     *
     * @param notificationType {@link NotificationType} object
     * @param listener         {@link ErrorListener} object
     */
    public void addErrorListener(final NotificationType notificationType, final ErrorListener<Notification<?, ?>> listener) {
        notificationListenerContainer.addErrorListener(notificationType, listener);
    }
}
