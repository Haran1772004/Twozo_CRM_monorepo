package com.tlc.notification.internal.twilio;

import com.tlc.commons.util.Configurations;
import com.tlc.notification.key.NotificationKeys;
import com.twilio.http.TwilioRestClient;

import java.util.Objects;

/**
 * <p>
 * Provides the twilio service for notification services.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class TwilioService {

    protected static TwilioRestClient twilioRestClient;
    protected static String accountServiceId;

    protected TwilioService(final Configurations configurations) {
        if (Objects.isNull(twilioRestClient) && configurations.getFirstKey().isPresent()) {
            final String authToken = configurations.getConfig(NotificationKeys.AUTH_TOKEN.getKey());

            accountServiceId = configurations.getConfig(NotificationKeys.ACCOUNT_SERVICE_ID.getKey());
            twilioRestClient = new TwilioRestClient.Builder(accountServiceId, authToken).build();
        }
    }

    /**
     * <p>
     * Stops the {@link TwilioRestClient} by assigning null value.
     * </p>
     */
    public abstract void stop();
}
