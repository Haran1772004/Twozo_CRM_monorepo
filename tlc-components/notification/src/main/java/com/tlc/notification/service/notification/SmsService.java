package com.tlc.notification.service.notification;

import com.tlc.notification.model.message.Sms;

/**
 * <p>
 * Provides the SMS services.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SmsService extends NotificationSender<Sms> {

    /**
     * <p>
     * Stops the external service client by assigning null value.
     * </p>
     */
    void stop();
}
