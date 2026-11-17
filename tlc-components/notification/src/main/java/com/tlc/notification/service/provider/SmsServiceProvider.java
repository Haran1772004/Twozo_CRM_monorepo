package com.tlc.notification.service.provider;

import com.tlc.notification.model.message.Sms;

/**
 * <p>
 * Provides the sms services.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SmsServiceProvider {

    /**
     * <p>
     * Sends the SMS to the given receiver.
     * </p>
     *
     * @param sms an entity that holds the sms attributes
     */
    void sendMessage(final Sms sms);

    /**
     * <p>
     * Stops the external service client by assigning null value.
     * </p>
     */
    void stop();
}
