package com.tlc.email.server.internal.provider.amazonSES;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.server.internal.provider.EmailServiceProviderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * <p>
 * A client for sending emails using Amazon Simple Email Service (SES).
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public final class AmazonSESClient implements EmailServiceProviderClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonSESClient.class);
    private static final String UTF_8_CHARSET = "UTF-8";

    private final AmazonSimpleEmailService sesClient;

    private AmazonSESClient(final AmazonSimpleEmailService sesClient) {
        this.sesClient = sesClient;
    }

    /**
     * <p>
     * Sends an email using Amazon SES.
     * Supports To, Cc, and Bcc recipients.
     * </p>
     *
     * @param email The email entity containing sender, recipients, subject, and content.
     */
    public void send(final Email email) {

        try {
            final EmailRecipients recipients = email.getEmailRecipients();

            final Destination destination = new Destination()
                    .withToAddresses(toAddresses(recipients.toRecipients()))
                    .withCcAddresses(toAddresses(recipients.ccRecipients()))
                    .withBccAddresses(toAddresses(recipients.bccRecipients()));

            final Message message = new Message()
                    .withSubject(new Content().withCharset(UTF_8_CHARSET).withData(email.getSubject()))
                    .withBody(new Body().withHtml(new Content().withCharset(UTF_8_CHARSET).withData(email.getContent())));

            final SendEmailRequest request = new SendEmailRequest()
                    .withSource(email.getSender().getEmailAddress())
                    .withDestination(destination)
                    .withMessage(message);

            sesClient.sendEmail(request);

            LOGGER.debug("Amazon SES: Mail sent successfully");
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Amazon SES: Send failed, message: {}, trace: {}", exception.getMessage(), exception);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_SEND_FAILED, exception);
        }
    }

    /**
     * <p>
     * Converts a collection of {@link EmailAddress} objects to a list of valid string addresses.
     * </p>
     *
     * @param recipients The collection of recipients (To, Cc, or Bcc).
     * @return A collection of valid email address strings.
     */
    private Collection<String> toAddresses(final Collection<EmailAddress> recipients) {
        if (Objects.isNull(recipients) || recipients.isEmpty()) {
            return Collections.emptyList();
        }
        return recipients.stream()
                .map(EmailAddress::getEmailAddress)
                .filter(address -> Objects.nonNull(address) && !address.isBlank())
                .toList();
    }

    /**
     * <p>
     * Builder class for constructing a configured {@link AmazonSESClient} instance.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public static class Builder {

        private String accessKey;
        private String secretKey;
        private String region;

        /**
         * <p>
         * Sets the AWS access key.
         * </p>
         *
         * @param accessKey The AWS access key.
         * @return The Builder instance.
         */
        public Builder setAccessKey(final String accessKey) {
            this.accessKey = accessKey;

            return this;
        }

        /**
         * <p>
         * Sets the AWS secret key.
         * </p>
         *
         * @param secretKey The AWS secret key.
         * @return The Builder instance.
         */
        public Builder setSecretKey(final String secretKey) {
            this.secretKey = secretKey;

            return this;
        }

        /**
         * <p>
         * Sets the AWS region (e.g., "us-east-1").
         * </p>
         *
         * @param region The AWS region.
         * @return The Builder instance.
         */
        public Builder setRegion(final String region) {
            this.region = region;

            return this;
        }

        /**
         * <p>
         * Builds and returns a configured {@link AmazonSESClient} instance.
         * </p>
         *
         * @return AmazonSESClient instance ready to send emails.
         */
        public AmazonSESClient build() {
            final BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

            final AmazonSimpleEmailService sesClient = AmazonSimpleEmailServiceClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .withRegion(region)
                    .build();

            return new AmazonSESClient(sesClient);
        }
    }
}