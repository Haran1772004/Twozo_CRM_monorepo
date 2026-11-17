package com.tlc.notification.service;

import com.tlc.commons.service.Service;
import com.tlc.notification.service.notification.SmsService;
import com.tlc.notification.service.notification.VoiceCallService;
import com.tlc.notification.service.notification.WhatsappService;

/**
 * <p>
 * Provides the notification services.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface NotificationService extends Service {

    /**
     * <p>
     * Provides the {@link VoiceCallService} to access voice call related functionalities..
     * </p>
     *
     * @return {@link VoiceCallService}
     */
    VoiceCallService getVoiceCallService();

    /**
     * <p>
     * Provides the {@link SmsService} to access SMS-related functionalities..
     * </p>
     *
     * @return {@link SmsService}
     */
    SmsService getSmsService();

    /**
     * <p>
     * Provides the {@link WhatsappService} to access whatsapp-related functionalities..
     * </p>
     *
     * @return {@link WhatsappService}
     */
    WhatsappService getWhatsappService();
}
