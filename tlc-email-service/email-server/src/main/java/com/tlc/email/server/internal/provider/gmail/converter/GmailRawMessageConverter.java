package com.tlc.email.server.internal.provider.gmail.converter;

import com.tlc.attachment.io.File;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.email.common.converter.Converter;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Converts a {@link Email} to the raw message.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class GmailRawMessageConverter implements Converter<Email, String> {

    public static final Logger LOGGER = LoggerFactory.getLogger(GmailRawMessageConverter.class);

    private static final String FROM = "From: ";
    private static final String TO = "To: ";
    private static final String CC = "Cc: ";
    private static final String BCC = "Bcc: ";
    private static final String SUBJECT = "Subject: ";
    private static final String IN_REPLY_TO = "In-Reply-To: ";
    private static final String REFERENCES = "References: ";
    private static final String REPLY_TO = "Reply-To: ";

    private static final String MIME_VERSION = "MIME-Version: 1.0";
    private static final String CONTENT_TYPE_HTML = "Content-Type: text/html; charset=\"UTF-8\"";
    private static final String CONTENT_TYPE_MULTIPART_TEMPLATE = "Content-Type: multipart/mixed; boundary=\"%s\"";
    private static final String CONTENT_TYPE_ATTACHMENT = "Content-Type: ";
    private static final String CONTENT_TRANSFER_ENCODING_7BIT = "Content-Transfer-Encoding: 7bit";
    private static final String CONTENT_TRANSFER_ENCODING_BASE64 = "Content-Transfer-Encoding: base64";
    private static final String CONTENT_DISPOSITION_FILE_TEMPLATE = "Content-Disposition: file; filename=\"%s\"";
    private static final String UTF8_BASE64_ENCODED = "=?UTF-8?B?";
    private static final String END_MARKER = "?=";

    private static final String NEW_LINE = "\r\n";
    private static final String EMPTY_STRING = "";

    private static final String BOUNDARY_PREFIX = "====BOUNDARY_";
    private static final String BOUNDARY_SUFFIX = "====";
    private static final String BOUNDARY_SEPARATOR = "--";

    private static final String DELIMITER = ", ";

    private final Encoder encoder;

    protected GmailRawMessageConverter() {
        this.encoder = Base64.getEncoder();
    }

    private static class Instance {
        private static final GmailRawMessageConverter INSTANCE = new GmailRawMessageConverter();
    }

    public static GmailRawMessageConverter getInstance() {
        return GmailRawMessageConverter.Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts data from json to the target object.
     * </p>
     *
     * @param email represents the {@link Email}
     */
    @Override
    public String convert(final Email email) {
        final StringBuilder rawMessage = new StringBuilder();

        final EmailAddress senderAddress = email.getSender();
        final EmailMetaData metaData = email.getMetaData();
        final EmailRecipients recipients = email.getEmailRecipients();

        final Collection<EmailAddress> to = recipients.toRecipients();
        final Collection<EmailAddress> cc = recipients.ccRecipients();
        final Collection<EmailAddress> bcc = recipients.bccRecipients();

        final String subject = email.getSubject();
        final String content = email.getContent();
        final String references = metaData.getReferences();
        final String inReplyTo = metaData.getInReplyTo();
        final String replyTo = metaData.getReplyToAddress();
        final String sender = senderAddress.getEmailAddress();
        final String boundary = BOUNDARY_PREFIX + System.currentTimeMillis() + BOUNDARY_SUFFIX;

        appendHeader(rawMessage, FROM, sender);
        appendHeader(rawMessage, TO, joinAddresses(to));
        appendHeader(rawMessage, CC, joinAddresses(cc));
        appendHeader(rawMessage, BCC, joinAddresses(bcc));
        appendHeader(rawMessage, SUBJECT, subject);
        appendHeader(rawMessage, IN_REPLY_TO, inReplyTo);
        appendHeader(rawMessage, REFERENCES, Objects.isNull(references) ? inReplyTo : references);
        appendHeader(rawMessage, REPLY_TO, Objects.isNull(replyTo) ? sender : replyTo);

        rawMessage.append(MIME_VERSION).append(NEW_LINE);
        rawMessage.append(String.format(CONTENT_TYPE_MULTIPART_TEMPLATE, boundary)).append(NEW_LINE).append(NEW_LINE);

        rawMessage.append(BOUNDARY_SEPARATOR).append(boundary).append(NEW_LINE);
        rawMessage.append(CONTENT_TYPE_HTML).append(NEW_LINE);
        rawMessage.append(CONTENT_TRANSFER_ENCODING_7BIT).append(NEW_LINE).append(NEW_LINE);
        rawMessage.append(Objects.isNull(content) ? EMPTY_STRING : content).append(NEW_LINE).append(NEW_LINE);
        final Collection<File> files = email.getFiles();

        if (Objects.nonNull(files)) {

            for (final File file : files) {
                final String filename = file.name();
                final String mediaType = file.mediaType();

                rawMessage.append(BOUNDARY_SEPARATOR).append(boundary).append(NEW_LINE);
                rawMessage.append(CONTENT_TYPE_ATTACHMENT).append(mediaType).append(NEW_LINE);
                rawMessage.append(String.format(CONTENT_DISPOSITION_FILE_TEMPLATE, filename)).append(NEW_LINE);
                rawMessage.append(CONTENT_TRANSFER_ENCODING_BASE64).append(NEW_LINE).append(NEW_LINE);

                try (
                        final InputStream inputStream = file.inputStream();
                        final ByteArrayOutputStream encodedStream = new ByteArrayOutputStream();
                        final OutputStream base64Out = Base64.getMimeEncoder(NumericalConstant.SEVENTY_SIX, NEW_LINE.getBytes()).wrap(encodedStream)
                ) {
                    final byte[] buffer = new byte[8192];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        base64Out.write(buffer, 0, bytesRead);
                    }
                    base64Out.flush();

                    rawMessage.append(encodedStream.toString(StandardCharsets.UTF_8)).append(NEW_LINE);
                } catch (Throwable throwable) {
                    LoggerUtil.logError(LOGGER, throwable, "Failed to encode attachment, message: {}, trace: {}",
                            throwable.getMessage(), throwable);
                }

                rawMessage.append(NEW_LINE);
            }
        }
        rawMessage.append(BOUNDARY_SEPARATOR).append(boundary).append(BOUNDARY_SEPARATOR).append(NEW_LINE);
        final String rawEmail = rawMessage.toString();
        final byte[] encodedBytes = Base64.getUrlEncoder()
                .withoutPadding()
                .encode(rawEmail.getBytes(StandardCharsets.UTF_8));

        return new String(encodedBytes, StandardCharsets.UTF_8);
    }

    /**
     * <p>
     * Joins a collection of EmailAddress objects into a comma-separated string.
     * </p>
     *
     * @param addresses Collection of EmailAddress objects to join
     * @return Comma-separated string of email addresses, or empty string if collection is empty
     */
    private String joinAddresses(final Collection<EmailAddress> addresses) {
        return addresses.stream()
                .map(EmailAddress::getEmailAddress)
                .collect(Collectors.joining(DELIMITER));
    }

    /**
     * <p>
     * Appends a header line to the StringBuilder if both header name and value are valid.
     * </p>
     *
     * @param builder    The StringBuilder to append the header to
     * @param headerName The name of the header (e.g., "From: ", "To: ")
     * @param value      The value of the header
     */
    private void appendHeader(final StringBuilder builder, final String headerName, final String value) {
        if (Objects.nonNull(headerName) && Objects.nonNull(value) && !value.isEmpty()) {
            builder.append(headerName);

            if (SUBJECT.equals(headerName)) {
                builder.append(UTF8_BASE64_ENCODED)
                        .append(encoder.encodeToString(value.getBytes(StandardCharsets.UTF_8)))
                        .append(END_MARKER);
            } else {
                builder.append(value);
            }
            builder.append(NEW_LINE);
        }
    }
}