package com.tlc.notification.service.provider;

import com.tlc.notification.model.message.WhatsappMessage;

/**
 * <p>
 * Provides the whatsapp services.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface WhatsappServiceProvider {

    /**
     * <p>
     * Sends the whatsapp message to the given receiver.
     * </p>
     *
     * @param whatsappMessage an entity that holds the whatsapp message attributes
     */
    void sendWhatsappMessage(final WhatsappMessage whatsappMessage);

    /**
     * <p>
     * Stops the external service client by assigning null value.
     * </p>
     */
    void stop();
}
