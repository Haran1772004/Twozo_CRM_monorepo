package com.tlc.email.server.internal.provider.amazonSES.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Configurations;

import com.tlc.email.common.model.type.Email;

import com.tlc.email.server.internal.provider.AbstractDomainAuthenticatedEmailService;
import com.tlc.email.server.internal.provider.amazonSES.AmazonSESClient;
import com.tlc.email.server.service.EmailNotificationSender;

import com.tlc.notification.internal.status.NotificationErrorCodes;

/**
 * <p>
 * Implements of {@link EmailNotificationSender} using AWS SES (Simple Email Service) for sending and receiving notifications
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class AmazonSESService extends AbstractDomainAuthenticatedEmailService {

    private static final String ACCESS_KEY = "accessKey";
    private static final String SECRETE_KEY = "secretKey";
    private static final String REGION = "region";

    private AmazonSESClient mailClient;

    private AmazonSESService() {
    }

    /**
     * <p>
     * Holds the single instance of AmazonSESService.
     * </p>
     */
    private static class Instance {
        private static final AmazonSESService INSTANCE = new AmazonSESService();
    }

    /**
     * <p>
     * Gets the {@link AmazonSESService} Instance
     * </p>
     *
     * @return {@link AmazonSESService}
     */
    public static AmazonSESService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {// TODO: Add other credentials as needed, such as API keys, OAuth tokens, service account credentials, or encryption keys.
        mailClient = new AmazonSESClient.Builder()
                .setAccessKey(configurations.getConfig(ACCESS_KEY))
                .setSecretKey(configurations.getConfig(SECRETE_KEY))
                .setRegion(configurations.getConfig(REGION))
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * @param email An entity that holds the notification attributes
     */
    @Override
    public void send(final Email email) {
        try {
            mailClient.send(email);
        } catch (final Exception exception) {
            throw ErrorCode.get(NotificationErrorCodes.EMAIL_SEND_FAILED, exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
    }
}