package com.tlc.notification.service.notification;

import com.tlc.notification.model.message.WhatsappMessage;

/**
 * <p>
 * Provides the whatsapp services.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface WhatsappService extends NotificationSender<WhatsappMessage>, NotificationReceiver<WhatsappMessage, WhatsappMessage> {

    /**
     * <p>
     * Stops the external service client by assigning null value.
     * </p>
     */
    void stop();
}
