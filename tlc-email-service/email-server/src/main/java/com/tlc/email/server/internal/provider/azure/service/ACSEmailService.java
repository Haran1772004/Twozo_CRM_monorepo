package com.tlc.email.server.internal.provider.azure.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Configurations;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.server.internal.provider.AbstractDomainAuthenticatedEmailService;
import com.tlc.email.server.internal.provider.DomainAuthenticatedEmailService;
import com.tlc.email.server.internal.provider.azure.ACSEmailClient;
import com.tlc.notification.internal.status.NotificationErrorCodes;

/**
 * <p>
 * Implementation of {@link DomainAuthenticatedEmailService} using Azure Communication Services (ACS).
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class ACSEmailService extends AbstractDomainAuthenticatedEmailService {

    private static final String CONNECTION_STRING = "connectionString";

    private ACSEmailClient emailClient;

    private ACSEmailService() {
    }

    /**
     * <p>
     * Holds the single instance of ACSEmailService.
     * </p>
     */
    private static class Instance {
        private static final ACSEmailService INSTANCE = new ACSEmailService();
    }

    /**
     * <p>
     * Gets the {@link ACSEmailService} instance.
     * </p>
     *
     * @return {@link ACSEmailService}
     */
    public static ACSEmailService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        emailClient = new ACSEmailClient.Builder()
                .setConnectionString(configurations.getConfig(CONNECTION_STRING))
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * @param email An entity that holds the email attributes.
     */
    @Override
    public void send(final Email email) {
        try {
            emailClient.send(email);
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