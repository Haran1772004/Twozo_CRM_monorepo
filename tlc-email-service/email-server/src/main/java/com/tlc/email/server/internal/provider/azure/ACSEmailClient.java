package com.tlc.email.server.internal.provider.azure;

import com.azure.communication.email.EmailAsyncClient;
import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailMessage;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.server.internal.provider.EmailServiceProviderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * A client wrapper around Azure Communication Services (ACS) Email Client.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public final class ACSEmailClient implements EmailServiceProviderClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ACSEmailClient.class);

    private final EmailAsyncClient emailClient;

    /**
     * <p>
     * Creates a new {@link ACSEmailClient} with the underlying Azure {@link EmailClient}.
     * </p>
     *
     * @param emailClient The Azure EmailClient instance.
     */
    private ACSEmailClient(final EmailAsyncClient emailClient) {
        this.emailClient = emailClient;
    }

    /**
     * <p>
     * Sends an email using Azure Communication Services.
     * </p>
     *
     * @param email The email object containing sender, recipients, subject, and HTML content.
     */
    public void send(final Email email) {
        try {
            final com.tlc.email.common.model.address.EmailRecipients recipients = email.getEmailRecipients();

            final EmailMessage message = new EmailMessage()
                    .setSenderAddress(email.getSender().getEmailAddress())
                    .setToRecipients(getRecipients(recipients.toRecipients()))
                    .setCcRecipients(getRecipients(recipients.ccRecipients()))
                    .setBccRecipients(getRecipients(recipients.bccRecipients()))
                    .setSubject(email.getSubject())
                    .setBodyHtml(email.getContent());

            emailClient.beginSend(message)
                    .subscribe(
                            pollResponse -> {
                                LOGGER.info("ACS send action, status: {}", pollResponse.getStatus().getValue());
                            },
                            error -> {
                                LOGGER.info("ACS send action failed, message{}, trace: {}", error.getMessage(), error);
                            }
                    );
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "ACS: Email send action failed: {}, trace: {}", exception.getMessage(), exception);
        }
    }

    /**
     * <p>
     * Converts a collection of internal email address objects into
     * ACS-compatible {@link EmailAddress} instances.
     * </p>
     *
     * @param recipients List of internal email addresses.
     * @return List of ACS {@link EmailAddress} objects.
     */
    private List<EmailAddress> getRecipients(final Collection<com.tlc.email.common.model.address.EmailAddress> recipients) {
        if (Objects.isNull(recipients) || recipients.isEmpty()) {
            return Collections.emptyList();
        }

        return recipients.stream()
                .map(emailAddress -> new EmailAddress(emailAddress.getEmailAddress()))
                .collect(Collectors.toList());
    }

    /**
     * <p>
     * Builder class for creating an instance of {@link ACSEmailClient}.
     * </p>
     *
     * @author Mohamed Yasar
     * @version 1.0
     */
    public static class Builder {

        private String connectionString;

        /**
         * <p>
         * Sets the ACS connection string for initializing the underlying
         * {@link EmailClient}.
         * </p>
         *
         * @param connectionString The ACS connection string.
         * @return The same builder instance.
         */
        public Builder setConnectionString(final String connectionString) {
            this.connectionString = connectionString;
            return this;
        }

        /**
         * <p>
         * Builds and returns an instance of {@link ACSEmailClient}.
         * </p>
         *
         * @return A fully initialized {@link ACSEmailClient}.
         */
        public ACSEmailClient build() {
            return new ACSEmailClient(
                    new EmailClientBuilder()
                            .connectionString(connectionString)
                            .buildAsyncClient()
            );
        }
    }
}