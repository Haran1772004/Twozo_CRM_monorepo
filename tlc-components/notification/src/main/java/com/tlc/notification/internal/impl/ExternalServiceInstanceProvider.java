package com.tlc.notification.internal.impl;

import com.tlc.commons.util.Configurations;
import com.tlc.notification.internal.twilio.TwilioSmsService;
import com.tlc.notification.internal.twilio.TwilioVoiceCallService;
import com.tlc.notification.internal.twilio.TwilioWhatsappService;
import com.tlc.notification.key.NotificationKeys;
import com.tlc.notification.service.provider.SmsServiceProvider;
import com.tlc.notification.service.provider.VoiceCallServiceProvider;
import com.tlc.notification.service.provider.WhatsappServiceProvider;

/**
 * <p>
 * Provides the instance of third party services.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
class ExternalServiceInstanceProvider {

    /**
     * <p>
     * Provides the {@link VoiceCallServiceProvider} object.
     * </p>
     *
     * @param configurations properties for create the service provider object
     * @return {@link VoiceCallServiceProvider} object
     */
    static VoiceCallServiceProvider getVoiceCallService(final Configurations configurations) {
        final Configurations callConfig = configurations.getNestedConfigs(NotificationKeys.TWILIO.getKey());

        return callConfig.getFirstKey().isPresent() ? TwilioVoiceCallService.getInstance(callConfig) : null; //TODO when changing external service provider for voice call
    }

    /**
     * <p>
     * Provides the {@link SmsServiceProvider} object.
     * </p>
     *
     * @param configurations properties for create the service provider object
     * @return {@link SmsServiceProvider} object
     */
    static SmsServiceProvider getSMSService(final Configurations configurations) {
        final Configurations callConfig = configurations.getNestedConfigs(NotificationKeys.TWILIO.getKey());

        return callConfig.getFirstKey().isPresent() ? TwilioSmsService.getInstance(callConfig) : null;  //TODO when changing external service provider for SMS
    }

    /**
     * <p>
     * Provides the {@link WhatsappServiceProvider} object.
     * </p>
     *
     * @param configurations properties for create the service provider object
     * @return {@link WhatsappServiceProvider} object
     */
    static WhatsappServiceProvider getWhatsappService(final Configurations configurations) {
        final Configurations callConfig = configurations.getNestedConfigs(NotificationKeys.TWILIO.getKey());

        return callConfig.getFirstKey().isPresent() ? TwilioWhatsappService.getInstance(callConfig) : null;  //TODO while changing external service provider for whatsapp
    }
}
