package com.tlc.email.server.internal.provider.sendgrid.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.dns.model.DnsRecord;
import com.tlc.commons.util.Configurations;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailDnsRecord;
import com.tlc.email.common.model.EmailDomain;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.server.internal.provider.AbstractDomainAuthenticatedEmailService;
import com.tlc.email.server.internal.provider.sendgrid.SendGridClient;

import java.util.Collection;

/**
 * <p>
 * A service implementation for sending emails and managing domains using the SendGrid API.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class SendGridService extends AbstractDomainAuthenticatedEmailService {

    private static final String API_KEY = "apiKey";

    private String apiKeyValue;
    //TODO this has be to changed by including the resolver and cache that have the multiple api keys.
    // so that we can utilize the resource as much as possible.

    /**
     * <p>
     * Singleton holder for {@link SendGridService}.
     * </p>
     */
    private static class Instance {
        private static final SendGridService INSTANCE = new SendGridService();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link SendGridService}.
     * </p>
     *
     * @return The single {@link SendGridService} instance.
     */
    public static SendGridService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Initializes the service with configuration values.
     * </p>
     *
     * @param configurations Configuration object containing API key and other settings.
     */
    @Override
    public void start(final Configurations configurations) {
        this.apiKeyValue = configurations.getConfig(API_KEY);
    }

    /**
     * <p>
     * Sends an email using the configured SendGrid client.
     * </p>
     *
     * @param email The email object containing sender, recipients, subject, and content.
     */
    @Override
    public void send(final Email email) {
        try {
            final SendGridClient sendGridClient = getClient();

            sendGridClient.send(email);
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.EMAIL_SEND_FAILED, exception);
        }
    }

    /**
     * <p>
     * Adds and authenticates a domain in SendGrid using the configured client.
     * </p>
     *
     * @param emailDomain The domain entity to add and authenticate.
     */
    @Override
    public void addDomain(final EmailDomain emailDomain) {
        try {
            getClient().addDomain(emailDomain);
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.DOMAIN_CREATE_ACTION_FAILED);
        }
    }

    /**
     * <p>
     * Retrieves DNS records for a domain based on its internal ID in SendGrid.
     * </p>
     *
     * @param id The internal ID of the domain in SendGrid.
     * @return A collection of {@link EmailDnsRecord} objects for the domain.
     */
    public Collection<DnsRecord> getDnsRecords(final long id) {
        try {
            return getClient().getDnsRecords(id);
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.DNS_RECORDS_RETRIEVE_ACTION_FAILED);
        }
    }

    /**
     * <p>
     * Returns a new {@link SendGridClient} instance using the configured API key.
     * </p>
     *
     * @return A new {@link SendGridClient} object.
     */
    private SendGridClient getClient() {
        return new SendGridClient.Builder()
                .setApiKey(apiKeyValue)
                .build();
    }

    /**
     * <p>
     * Stops the service. No specific resources to clean up in this implementation.
     * </p>
     */
    @Override
    public void stop() {
    }
}