package com.tlc.notification.internal.twilio;

import com.tlc.commons.util.Configurations;
import com.tlc.notification.model.message.WhatsappMessage;
import com.tlc.notification.service.provider.WhatsappServiceProvider;
import com.twilio.http.TwilioRestClient;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides the twilio service for whatsapp.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class TwilioWhatsappService extends TwilioService implements WhatsappServiceProvider {

    private static TwilioWhatsappService whatsappService;
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    public TwilioWhatsappService(final Configurations configurations) {
        super(configurations);
    }

    /**
     * <p>
     * Provides the instance of {@link TwilioWhatsappService}.
     * </p>
     *
     * @param configurations properties for initialize the {@link TwilioWhatsappService} object
     * @return {@link TwilioWhatsappService} object
     */
    public static TwilioWhatsappService getInstance(final Configurations configurations) {
        final ReentrantReadWriteLock.WriteLock writeLock = LOCK.writeLock();

        writeLock.lock();

        try {
            if (Objects.isNull(whatsappService)) {
                whatsappService = new TwilioWhatsappService(configurations);
            }
        } finally {
            writeLock.unlock();
        }

        return whatsappService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendWhatsappMessage(final WhatsappMessage whatsappMessage) {
        //TODO
    }

    /**
     * <p>
     * Stops the {@link TwilioRestClient} by assigning null value.
     * </p>
     */
    public void stop() {
        twilioRestClient = null;
    }
}
