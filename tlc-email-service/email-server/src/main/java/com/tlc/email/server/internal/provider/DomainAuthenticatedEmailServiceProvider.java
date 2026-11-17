package com.tlc.email.server.internal.provider;

import com.tlc.email.common.model.ServiceProvider;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.email.server.internal.provider.amazonSES.service.AmazonSESService;
import com.tlc.email.server.internal.provider.azure.service.ACSEmailService;
import com.tlc.email.server.internal.provider.sendgrid.service.SendGridService;

/**
 * <p>
 * Provides the domain-authenticated email service based on the configured client.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class DomainAuthenticatedEmailServiceProvider implements Service {

    private static final String CLIENT = "client";

    private DomainAuthenticatedEmailService emailService;

    private DomainAuthenticatedEmailServiceProvider() {
    }

    /**
     * <p>
     * Holds the singleton instance of {@link DomainAuthenticatedEmailServiceProvider}.
     * </p>
     */
    private static class Instance {
        private static final DomainAuthenticatedEmailServiceProvider INSTANCE =
                new DomainAuthenticatedEmailServiceProvider();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link DomainAuthenticatedEmailServiceProvider}.
     * </p>
     *
     * @return {@link DomainAuthenticatedEmailServiceProvider} instance
     */
    public static DomainAuthenticatedEmailServiceProvider getInstance() {
        return DomainAuthenticatedEmailServiceProvider.Instance.INSTANCE;
    }

    /**
     * <p>
     * Initializes the underlying domain-authenticated email service.
     * </p>
     *
     * @param configurations The configuration object containing client and nested settings
     */
    @Override
    public void start(final Configurations configurations) {
        final ServiceProvider client = ServiceProvider.get(configurations.getConfig(CLIENT));

        emailService = switch (client) {
            case SEND_GRID -> SendGridService.getInstance();
            case SES -> AmazonSESService.getInstance();
            case ACS -> ACSEmailService.getInstance();
        };
        emailService.start(configurations.getNestedConfigs(client.getName()));
    }

    /**
     * <p>
     * Stops the currently active domain-authenticated email service.
     * </p>
     */
    @Override
    public void stop() {
        emailService.stop();
    }

    /**
     * <p>
     * Retrieves the initialized {@link DomainAuthenticatedEmailService}.
     * </p>
     *
     * @return Active {@link DomainAuthenticatedEmailService} instance
     */
    public DomainAuthenticatedEmailService getService() {
        return emailService;
    }
}