package com.tlc.email.server.service;

import com.tlc.notification.model.Notification;
import com.tlc.notification.service.conversationmode.NotificationSender;

/**
 * <p>
 * Provides the sender service for email notification.
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public interface EmailNotificationSender<T extends Notification<?, ?>> extends NotificationSender<T> {

    /**
     * {@inheritDoc}
     *
     * @param email An entity that holds the email attributes.
     */
    @Override
    void send(final T email);
}
