package com.tlc.notification.internal.impl;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.notification.internal.calendar.GoogleCalendarService;
import com.tlc.notification.internal.config.SmsServiceImpl;
import com.tlc.notification.internal.config.VoiceCallServiceImpl;
import com.tlc.notification.internal.config.WhatsappServiceImpl;
import com.tlc.notification.internal.contact.GoogleContactService;
import com.tlc.notification.key.NotificationKeys;
import com.tlc.notification.service.NotificationService;
import com.tlc.notification.service.notification.SmsService;
import com.tlc.notification.service.notification.VoiceCallService;
import com.tlc.notification.service.notification.WhatsappService;
import com.tlc.notification.service.telephony.mcube.MCubeService;
import com.tlc.notification.service.telephony.telecmi.TeleCMIService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Provides the services of the notification.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class NotificationsServiceImpl implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationsServiceImpl.class);

    private final Configurations emailAppConfigs;

    private VoiceCallService voiceCallService;
    private SmsService smsService;
    private WhatsappService whatsappService;

    private final TeleCMIService teleCMIService;
    private final MCubeService mcubeService;

    public NotificationsServiceImpl() {
        this.emailAppConfigs = ConfHolder.getInstance().getProperties(NotificationKeys.SYNC_SERVICE.getKey());

        teleCMIService =  TeleCMIService.getInstance();
        mcubeService =  MCubeService.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        this.voiceCallService = new VoiceCallServiceImpl(ExternalServiceInstanceProvider.getVoiceCallService(configurations));
        this.smsService = new SmsServiceImpl(ExternalServiceInstanceProvider.getSMSService(configurations));
        this.whatsappService = new WhatsappServiceImpl(ExternalServiceInstanceProvider.getWhatsappService(configurations));

        /* Calendar */
        GoogleCalendarService.getInstance().setAppProperties(emailAppConfigs);

        /* Contact */
        GoogleContactService.getInstance().setAppProperties(emailAppConfigs);

        teleCMIService.start(configurations);
        mcubeService.start(configurations);

        LOGGER.info("Notification Service Activated");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        voiceCallService.stop();
        smsService.stop();
        whatsappService.stop();
        teleCMIService.stop();
        mcubeService.stop();
        LOGGER.info("Notification Service Deactivated");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link VoiceCallService}
     */
    @Override
    public VoiceCallService getVoiceCallService() {
        return voiceCallService;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SmsService}
     */
    @Override
    public SmsService getSmsService() {
        return smsService;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link WhatsappService}
     */
    @Override
    public WhatsappService getWhatsappService() {
        return whatsappService;
    }
}