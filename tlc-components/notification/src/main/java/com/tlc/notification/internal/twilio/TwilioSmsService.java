package com.tlc.notification.internal.twilio;

import com.tlc.commons.util.Configurations;
import com.tlc.notification.model.message.Sms;
import com.tlc.notification.service.provider.SmsServiceProvider;
import com.twilio.http.TwilioRestClient;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides the twilio service for SMS.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class TwilioSmsService extends TwilioService implements SmsServiceProvider {

    private static TwilioSmsService smsService;
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    public TwilioSmsService(final Configurations configurations) {
        super(configurations);
    }

    /**
     * <p>
     * Provides the instance of {@link TwilioSmsService}.
     * </p>
     *
     * @param configurations properties for initialize the {@link TwilioSmsService} object
     * @return {@link TwilioSmsService} object
     */
    public static TwilioSmsService getInstance(final Configurations configurations) {
        final ReentrantReadWriteLock.WriteLock writeLock = LOCK.writeLock();

        writeLock.lock();

        try {
            if (Objects.isNull(smsService)) {
                smsService = new TwilioSmsService(configurations);
            }
        } finally {
            writeLock.unlock();
        }

        return smsService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMessage(final Sms sms) {
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
