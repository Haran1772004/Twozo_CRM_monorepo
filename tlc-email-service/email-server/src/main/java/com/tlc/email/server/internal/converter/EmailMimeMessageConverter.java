package com.tlc.email.server.internal.converter;

import com.tlc.attachment.io.File;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.email.common.converter.Converter;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailAttachment;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.InboundEmail;
import com.tlc.email.common.model.type.OutboundEmail;
import com.tlc.email.server.internal.field.EmailHeaderFields;
import com.tlc.email.server.internal.field.GmailHeaderFields;
import com.tlc.email.server.internal.provider.jakarta.ImapFolderResolver;
import com.tlc.email.server.internal.provider.jakarta.models.EmailFolder;
import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.key.NotificationKeys;
import jakarta.activation.DataHandler;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static com.tlc.email.common.model.EmailMetaData.Headers.IN_REPLY_TO;
import static com.tlc.email.common.model.EmailMetaData.Headers.REFERENCES;
import static com.tlc.email.common.model.address.AddressType.BCC;
import static com.tlc.email.common.model.address.AddressType.CC;
import static com.tlc.email.common.model.address.AddressType.TO;
import static com.tlc.email.common.model.type.Email.MessageCategory.DRAFT;
import static com.tlc.email.common.model.type.Email.MessageCategory.INBOUND;
import static com.tlc.email.common.model.type.Email.MessageCategory.OUTBOUND;

/**
 * <p>
 * Provides functionality to convert {@link Email} objects to {@link MimeMessage} instances
 * and vice versa. Supports inbound, outbound, and draft emails including attachments.
 * </p>
 *
 * @author Mohmaed Yasar K
 * @version 1.1
 */
public class EmailMimeMessageConverter implements Converter<MimeMessage, Email> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailMimeMessageConverter.class);
    private static final Map<EmailFolder, Supplier<Email>> EMAIL_MAP = new HashMap<>();

    private static final String MIME_TEXT_HTML = "text/html";
    private static final String MIME_TEXT_PLAIN = "text/plain";
    private static final String MIME_MULTIPART = "multipart/*";
    private static final String CID_PREFIX = "cid:";
    private static final String BASE64_PREFIX = ";base64,";
    private static final String DATA_PREFIX = "data:";
    private static final String CONTENT_ID_HEADER = "Content-ID";
    private static final String HTML_CONTENT_TYPE = "text/html";
    private static final String ANGLE_BRACKETS_REGEX = "[<>]";

    static {
        EMAIL_MAP.put(EmailFolder.INBOX, InboundEmail::new);
        EMAIL_MAP.put(EmailFolder.SENT, OutboundEmail::new);
        EMAIL_MAP.put(EmailFolder.DRAFT, DraftEmail::new);
    }

    private static class Instance {
        private static final EmailMimeMessageConverter INSTANCE = new EmailMimeMessageConverter();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link EmailMimeMessageConverter}.
     * </p>
     *
     * @return Singleton instance of {@link EmailMimeMessageConverter}.
     */
    public static EmailMimeMessageConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Private constructor to enforce singleton pattern.
     * </p>
     */
    private EmailMimeMessageConverter() {
    }

    /**
     * <p>
     * Converts a {@link MimeMessage} to an {@link Email}.
     * Populates subject, content, recipients, and metadata.
     * </p>
     *
     * @param mimeMessage The {@link MimeMessage} to convert.
     * @return Converted {@link Email} object.
     */
    @Override
    public Email convert(final MimeMessage mimeMessage) {

        try {
            final Session session = mimeMessage.getSession();

            if (Objects.isNull(session)) {
                return convert(mimeMessage, null);
            } else {
                try (final Store store = session.getStore()) {
                    return convert(mimeMessage, store);
                }
            }
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.EMAIL_PARSE_FAILED);
        }
    }

    /**
     * <p>
     * Converts a {@link MimeMessage} to an {@link Email} using the provided
     * mail {@link Store}.
     * </p>
     *
     * @param mimeMessage The {@link MimeMessage} to convert.
     * @param store       The mail {@link Store} associated with the message.
     * @return Converted {@link Email} object.
     */
    public Email convert(final MimeMessage mimeMessage, final Store store) {
        final Email email = getEmail(mimeMessage, store);
        final StringBuilder content = new StringBuilder();
        final Collection<EmailAttachment> attachments = extractEmailAttachments(mimeMessage, content);

        email.setSubject(getSubject(mimeMessage));
        email.setContent(content.toString());
        email.setRecipients(List.of(getRecipients(mimeMessage)));
        email.setMetaData(getMetaData(mimeMessage));
        email.setAttachments(attachments);

        return email;
    }

    /**
     * <p>
     * Converts an {@link InputStream} containing a MIME message
     * into an {@link Email}.
     * </p>
     *
     * @param inputStream The input stream containing the MIME message data.
     * @return Converted {@link Email} object.
     */
    public Email convert(final InputStream inputStream) {

        try {
            final MimeMessage mimeMessage = new MimeMessage(null, inputStream);
            final Email email = convert(mimeMessage);
            final StringBuilder content = new StringBuilder();
            final Collection<EmailAttachment> attachments = extractEmailAttachments(mimeMessage, content);

            email.setContent(content.toString());
            email.setAttachments(attachments);

            return email;
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.FAILED_TO_PARSE_MIME_MESSAGE);
        }
    }

    /**
     * <p>
     * Converts an {@link Email} object into a {@link MimeMessage} for sending via Jakarta Mail.
     * </p>
     *
     * @param email   The {@link Email} object to convert.
     * @param session The {@link Session} instance used to create the MimeMessage.
     * @return A fully constructed {@link MimeMessage} representing the email.
     */
    public MimeMessage convert(final Email email, final Session session) {
        final MimeMessage message = new MimeMessage(session);
        final EmailRecipients recipients = email.getEmailRecipients();

        setRecipients(message, Message.RecipientType.TO, recipients.getToRecipients());
        setRecipients(message, Message.RecipientType.CC, recipients.getCcRecipients());
        setRecipients(message, Message.RecipientType.BCC, recipients.getBccRecipients());

        setFrom(message, email);
        setSubject(message, email.getSubject());
        setMultipart(message, email);
        setMetaData(message, email);

        return message;
    }

    /**
     * Creates an {@link Email} object based on the folder of the {@link MimeMessage}.
     * Uses ImapFolderResolver for provider-specific folder names.
     *
     * @param message The {@link MimeMessage} to convert.
     * @return {@link Email} instance corresponding to the folder.
     */
    private Email getEmail(final MimeMessage message, final Store store) {

        try {
            EmailFolder emailFolder = null;

            if (Objects.nonNull(store)) {
                final String actualFolderName = message.getFolder().getFullName();
                final ImapFolderResolver imapFolderResolver = new ImapFolderResolver.Builder()
                        .store(store)
                        .build();

                for (final EmailFolder folder : EmailFolder.get()) {
                    final String mappedFolderName = imapFolderResolver.getFolderName(folder);

                    if (mappedFolderName.equalsIgnoreCase(actualFolderName)) {
                        emailFolder = folder;
                        break;
                    }
                }
            }
            final Email email = Objects.isNull(emailFolder) || emailFolder.isArchive() ? new Email() : EMAIL_MAP.get(emailFolder).get();

            if (Objects.nonNull(emailFolder)) {
                switch (emailFolder) {
                    case INBOX -> {
                        email.setMessageCategory(INBOUND);
                        email.setRead(isRead(message));
                    }
                    case SENT -> email.setMessageCategory(OUTBOUND);
                    case DRAFT -> email.setMessageCategory(DRAFT);
                    case ARCHIVE -> {
                        email.setMessageCategory(Email.MessageCategory.ARCHIVE);
                        email.setArchived(true);
                    }
                }
            }
            email.setSender(new SyncAddressInfo(resolveSenderAddress(message)));

            return email;
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.MESSAGE_FETCH_ACTION_FAILED, exception);
        }
    }

    /**
     * <p>
     * Resolves the sender email address from the provided {@link MimeMessage}.
     * </p>
     *
     * @param message The {@link MimeMessage} from which the sender address
     *                is to be resolved.
     * @return The sender email address if available; {@code null} otherwise.
     */
    private String resolveSenderAddress(final MimeMessage message) {

        try {
            if (message.getSender() instanceof InternetAddress sender) {
                return sender.getAddress();
            }
            final Address[] from = message.getFrom();

            if (Objects.nonNull(from) && from.length > 0 && from[0] instanceof InternetAddress internetAddress) {
                return internetAddress.getAddress();
            }

            return null;
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.FROM_ADDRESS_FETCH_FAILED);
        }
    }

    /**
     * <p>
     * Checks whether the message is read by evaluating the
     * {@link Flags.Flag#SEEN} flag.
     * </p>
     *
     * @param message Refers to the {@link Message}.
     * @return {@code true} if the message has been read, {@code false} otherwise.
     */
    private boolean isRead(final Message message) {
        return getFlags(message).contains(Flags.Flag.SEEN);
    }

    /**
     * <p>
     * Retrieves flags from the {@link Message}.
     * </p>
     *
     * @param message Refers to the {@link Message}.
     * @return {@link Flags} of the message.
     */
    private Flags getFlags(final Message message) {

        try {
            return message.getFlags();
        } catch (Exception exception) {
            throw ErrorCode.get(NotificationErrorCodes.INVALID_FLAG, exception);
        }
    }

    /**
     * <p>
     * Fetches all recipients from the {@link Message} and groups them by {@link AddressType}.
     * </p>
     *
     * @param message Refers to the {@link Message}.
     * @return {@link EmailRecipients} containing all recipients.
     */
    private EmailRecipients getRecipients(final Message message) {
        final EmailRecipients recipients = new EmailRecipients();
        final Map<AddressType, Collection<EmailAddress>> recipientMap = new EnumMap<>(AddressType.class);

        for (final AddressType type : new AddressType[]{TO, CC, BCC}) {
            recipientMap.put(type, parseAddresses(message, type));
        }
        recipients.set(recipientMap);

        return recipients;
    }

    /**
     * <p>
     * Parses recipient addresses from the message for the specified {@link AddressType}.
     * </p>
     *
     * @param message Refers to the {@link Message}.
     * @param type    Refers to {@link AddressType}.
     * @return {@link Collection} of {@link EmailAddress}.
     */
    private Collection<EmailAddress> parseAddresses(final Message message, final AddressType type) {

        try {
            final Address[] addresses = message.getRecipients(EmailRecipients.fetchRecipientType(type));

            if (Objects.isNull(addresses) || addresses.length == 0) {
                return Collections.emptyList();
            }
            final Collection<EmailAddress> emailAddresses = new ArrayList<>();

            for (final Address address : addresses) {

                if (address instanceof InternetAddress internetAddress) {
                    final String email = internetAddress.getAddress();

                    if (Objects.nonNull(email) && !email.isBlank()) {
                        emailAddresses.add(new EmailAddress(email.trim()));
                    }
                }
            }

            return emailAddresses;
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_EMAIL_ADDRESS, exception);
        }
    }

    /**
     * <p>
     * Retrieves the subject of the given {@link MimeMessage}.
     * </p>
     *
     * @param message The {@link MimeMessage} to extract the subject from.
     * @return The subject as a {@link String}.
     */
    private String getSubject(final MimeMessage message) {

        try {
            return message.getSubject();
        } catch (Exception exception) {
            throw ErrorCode.get(NotificationErrorCodes.INVALID_FLAG, exception);
        }
    }

    /**
     * <p>
     * Retrieves the body content of the message.
     * Supports both plain text and HTML multipart messages.
     * </p>
     *
     * @param message Refers to the {@link Message}.
     * @return The body content as a {@link String}.
     */
    private String getBody(final Message message) {

        try {
            final Object content = message.getContent();

            if (content instanceof MimeMultipart multipart) {
                return extractTextFromMultipart(multipart);
            }
            return Objects.toString(content, StringUtil.Delimiter.EMPTY_STRING.getDelimiter());
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_CONTENT_TYPE, exception);
        }
    }

    /**
     * <p>
     * Recursively extracts text content from a {@link MimeMultipart}.
     * </p>
     *
     * @param multipart Represents the {@link MimeMultipart} data.
     * @return Extracted text content.
     */
    private String extractTextFromMultipart(final MimeMultipart multipart) throws MessagingException, IOException {
        final StringBuilder result = new StringBuilder();
        final int size = multipart.getCount();

        for (int index = 0; index < size; index++) {
            final BodyPart bodyPart = multipart.getBodyPart(index);
            final Object content = bodyPart.getContent();

            if (bodyPart.isMimeType(HTML_CONTENT_TYPE)) {
                result.append(content);
            } else if (content instanceof MimeMultipart inner) {
                result.append(extractTextFromMultipart(inner));
            }
        }

        return result.toString();
    }

    /**
     * <p>
     * Extracts metadata from the given {@link MimeMessage} and populates an {@link EmailMetaData} object.
     * </p>
     *
     * @param mimeMessage The {@link MimeMessage} to extract metadata from.
     * @return Populated {@link EmailMetaData}.
     */
    private EmailMetaData getMetaData(final MimeMessage mimeMessage) {
        final EmailMetaData metaData = new EmailMetaData();

        try {
            metaData.setInReplyTo(safeGetHeader(mimeMessage, IN_REPLY_TO.getName()));
            metaData.setReferences(safeGetHeader(mimeMessage, REFERENCES.getName()));
            final String returnPath = getFirstHeader(mimeMessage, EmailHeaderFields.RETURN_PATH);

            if (Objects.nonNull(returnPath)) {
                final String email = getHeader(returnPath);

                metaData.setReturnPath(Objects.nonNull(email) && email.isEmpty() ? null : email);
            }
            metaData.setMessageId(getFirstHeader(mimeMessage, GmailHeaderFields.DRAFT_MESSAGE_ID));
            final Date date = mimeMessage.getSentDate();

            if (Objects.nonNull(date)) {
                metaData.setDate(date.getTime());
            }
            metaData.setInitialMessage();
            final Address[] replyTo = mimeMessage.getReplyTo();

            if (Objects.nonNull(replyTo) && replyTo.length > 0) {
                metaData.setReplyToAddress(replyTo[0].toString());
            }
            metaData.setReceived(getFirstHeader(mimeMessage, EmailMetaData.Headers.RECEIVED.getName()));
            final String resentFrom = getFirstHeader(mimeMessage, EmailMetaData.Headers.RESEND_FROM.getName());
            final String deliveredTo = getFirstHeader(mimeMessage, EmailMetaData.Headers.DELIVERED_TO.getName());
            final String forwardingLoop = getFirstHeader(mimeMessage, EmailMetaData.Headers.FORWARDING_LOOP.getName());

            metaData.setSenderId(getHeader(getSenderSource(resentFrom, deliveredTo, forwardingLoop)));
        } catch (Exception exception) {
            LoggerUtil.logError(
                    LOGGER, exception,
                    "Jakarta mail: Failed to load meta data: {}, trace: {}", exception.getMessage(), exception
            );
            throw ErrorCode.get(EmailErrorCodes.HEADER_EXCEPTION, exception);
        }

        return metaData;
    }

    /**
     * <p>
     * Determines the source of the sender based on the provided headers.
     * </p>
     *
     * @param resentFrom     The "Resent-From" header value, may be {@code null}.
     * @param deliveredTo    The "Delivered-To" header value, may be {@code null}.
     * @param forwardingLoop A semicolon-delimited string representing forwarding history, may be {@code null}.
     * @return The first non-empty sender source found, or {@code null} if none are available.
     */
    private static String getSenderSource(final String resentFrom, final String deliveredTo, final String forwardingLoop) {
        String senderSource = null;

        if (Objects.nonNull(resentFrom) && !resentFrom.isEmpty()) {
            senderSource = resentFrom;
        } else if (Objects.nonNull(deliveredTo) && !deliveredTo.isEmpty()) {
            senderSource = deliveredTo;
        } else if (Objects.nonNull(forwardingLoop) && !forwardingLoop.isEmpty()) {
            final String[] parts = forwardingLoop.split(";");

            if (parts.length > 0 && parts[0].contains("@")) {
                senderSource = parts[0].trim();
            }
        }

        return senderSource;
    }

    /**
     * <p>
     * Strips angle brackets from the given header value and trims whitespace.
     * </p>
     *
     * @param header The raw header value to process.
     * @return Cleaned header value, or {@code null} if input is {@code null}.
     */
    private String getHeader(final String header) {
        if (Objects.isNull(header)) {
            return null;
        }
        return header.replaceAll(ANGLE_BRACKETS_REGEX, StringUtil.Delimiter.EMPTY_STRING.getDelimiter()).trim();
    }

    /**
     * <p>
     * Retrieves the first value of the specified header from the {@link MimeMessage}.
     * </p>
     *
     * @param message    The {@link MimeMessage} from which to retrieve the header.
     * @param headerName The name of the header to retrieve.
     * @return The first header value if present; {@code null} otherwise.
     */
    private String getFirstHeader(MimeMessage message, String headerName) throws MessagingException {
        final String[] headers = message.getHeader(headerName);

        return (headers != null && headers.length > 0) ? headers[0] : null;
    }

    /**
     * <p>
     * Sets metadata headers on the given {@link MimeMessage} based on the {@link Email} object.
     * </p>
     *
     * @param message The {@link MimeMessage} to set metadata on.
     * @param email   The {@link Email} containing metadata.
     */
    private void setMetaData(final MimeMessage message, final Email email) {
        final EmailMetaData metaData = email.getMetaData();

        final String replyToAddress = metaData.getReplyToAddress();
        final String references = metaData.getReferences();
        final String inReplyTo = metaData.getInReplyTo();

        setReplyTo(message, Objects.isNull(replyToAddress) ? email.getSender().getEmailAddress() : metaData.getReplyToAddress());

        if (Objects.nonNull(references)) {
            setHeader(message, REFERENCES.getName(), metaData.getReferences());
        }

        if (Objects.nonNull(inReplyTo)) {
            setHeader(message, IN_REPLY_TO.getName(), metaData.getInReplyTo());
        }
    }

    /**
     * <p>
     * Sets a header on a {@link Message}.
     * </p>
     *
     * @param message The {@link Message} to modify.
     * @param name    Header name.
     * @param value   Header value.
     */
    private void setHeader(final Message message, final String name, final String value) {

        try {
            message.setHeader(name, value);
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.HEADER_EXCEPTION, exception);
        }
    }

    /**
     * <p>
     * Sets the reply-to address on a {@link Message}.
     * </p>
     *
     * @param message The {@link Message} to modify.
     * @param email   Email address to set as reply-to.
     */
    private void setReplyTo(final Message message, final String email) {

        try {
            message.setReplyTo(new Address[]{new InternetAddress(email)});
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.REPLY_TO_WRITE_FAILED, exception);
        }
    }

    /**
     * <p>
     * Sets the multipart content of the {@link MimeMessage}, including the HTML body and attachments.
     * </p>
     *
     * @param message The {@link MimeMessage} to modify.
     * @param email   The {@link Email} object containing content and attachments.
     */
    private void setMultipart(final MimeMessage message, final Email email) {

        try {
            final Multipart multipart = new MimeMultipart();
            final Optional<MimeBodyPart> optionalBodyPart = buildContent(email.getContent());

            if (optionalBodyPart.isPresent()) {
                multipart.addBodyPart(optionalBodyPart.get());
            }

            for (final BodyPart part : buildAttachments(email.getFiles())) {
                multipart.addBodyPart(part);
            }
            message.setContent(multipart);
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.FAILED_TO_BUILD_MUTI_PART, exception);
        }
    }

    /**
     * <p>
     * Builds a {@link MimeBodyPart} for the main HTML content, wrapped in {@link Optional}.
     * </p>
     *
     * @param content The HTML content string.
     * @return {@link Optional} containing the {@link MimeBodyPart} if content is valid.
     */
    private Optional<MimeBodyPart> buildContent(final String content) {
        if (Objects.isNull(content)) {
            return Optional.empty();
        }

        try {
            final MimeBodyPart part = new MimeBodyPart();

            part.setContent(content, NotificationKeys.HTML_CONTENT_TYPE.getKey());

            return Optional.of(part);
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.FAILED_TO_BUILD_CONTENT, exception);
        }
    }

    /**
     * <p>
     * Builds {@link MimeBodyPart} objects for file attachments.
     * </p>
     *
     * @param files Collection of {@link File} attachments.
     * @return Collection of {@link BodyPart} representing each attachment.
     */
    private Collection<BodyPart> buildAttachments(Collection<File> files) {
        final Collection<BodyPart> attachments = new ArrayList<>();

        if (Objects.isNull(files) || files.isEmpty()) {
            return attachments;
        }

        for (final File file : files) {
            try (final InputStream bufferedInputStream = new BufferedInputStream(file.inputStream());
                 final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

                final byte[] buffer = new byte[8192];
                int read;

                while ((read = bufferedInputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, read);
                }
                final ByteArrayDataSource byteArrayDataSource = new ByteArrayDataSource(byteArrayOutputStream.toByteArray(), file.mediaType());
                final MimeBodyPart part = new MimeBodyPart();

                part.setDataHandler(new DataHandler(byteArrayDataSource));
                part.setFileName(file.name());

                attachments.add(part);
            } catch (Exception exception) {
                throw ErrorCode.get(EmailErrorCodes.FILE_UPLOAD_FAILED, exception);
            }
        }

        return attachments;
    }

    /**
     * <p>
     * Sets the subject of the {@link Message}.
     * </p>
     *
     * @param message The {@link Message} to modify.
     * @param subject The subject string.
     */
    private void setSubject(final Message message, final String subject) {

        try {
            message.setSubject(subject);
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.FAILED_TO_SET_RECIPIENTS, exception);
        }
    }

    /**
     * <p>
     * Sets the "From" address of the {@link Message}.
     * </p>
     *
     * @param message The {@link Message} to modify.
     * @param email   The {@link Email} containing sender information.
     */
    private void setFrom(final Message message, final Email email) {

        try {
            message.setFrom(new InternetAddress(Objects.requireNonNull(email.getSender().getEmailAddress())));
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.FAILED_TO_SET_FROM_ADDRESS, exception);
        }
    }

    /**
     * <p>
     * Sets the recipients of the {@link Message} for the given {@link Message.RecipientType}.
     * </p>
     *
     * @param message   The {@link Message} to modify.
     * @param type      The recipient type (TO, CC, BCC).
     * @param addresses Collection of recipient email addresses.
     */
    private void setRecipients(final Message message, final Message.RecipientType type, final Collection<String> addresses) {

        try {
            final Optional<String> recipients = StringUtil.join(StringUtil.Delimiter.COMMA, addresses);

            if (recipients.isEmpty()) {
                return;
            }
            message.setRecipients(type, InternetAddress.parse(recipients.get()));
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.FAILED_TO_SET_RECIPIENTS, exception);
        }
    }

    /**
     * <p>
     * Safely fetches a single header value from a {@link MimeMessage}.
     * </p>
     *
     * @param mimeMessage The message to fetch the header from.
     * @param header      Header name.
     * @return Header value or null if missing.
     */
    private String safeGetHeader(final MimeMessage mimeMessage, final String header) throws MessagingException {
        final String[] values = mimeMessage.getHeader(header);

        return (Objects.isNull(values) || values.length == 0) ? null : values[0];
    }

    /**
     * <p>
     * Extracts attachments from the given MimeMessage.
     * Also builds HTML content by embedding inline images.
     * </p>
     *
     * @param message     the email message
     * @param htmlContent StringBuilder to accumulate HTML content
     * @return collection of EmailAttachment (only real attachments)
     */
    private Collection<EmailAttachment> extractEmailAttachments(final MimeMessage message, final StringBuilder htmlContent) {
        final Collection<EmailAttachment> attachments = new ArrayList<>();

        try {
            final Object content = message.getContent();

            if (content instanceof Multipart multipart) {
                processMultipart(multipart, attachments, htmlContent);
            } else if (message.isMimeType(MIME_TEXT_HTML)) {
                htmlContent.append(Objects.toString(content, StringUtil.Delimiter.EMPTY_STRING.getDelimiter()));
            } else if (message.isMimeType(MIME_TEXT_PLAIN)) {
                htmlContent.append(Objects.toString(content, StringUtil.Delimiter.EMPTY_STRING.getDelimiter()));
            }
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.ATTACHMENT_RETRIEVAL_FAILED, exception);
        }

        return attachments;
    }

    /**
     * <p>
     * Recursively processes multipart content to attachments.
     * </p>
     *
     * @param multipart   Represent the multipart.
     * @param attachments Represent the attachments.
     * @param htmlContent Represent the content.
     *
     */
    private void processMultipart(final Multipart multipart,
                                  final Collection<EmailAttachment> attachments,
                                  final StringBuilder htmlContent) {
        try {
            for (int index = 0; index < multipart.getCount(); index++) {
                final BodyPart part = multipart.getBodyPart(index);

                if (part.isMimeType(MIME_TEXT_HTML)) {
                    htmlContent.append(part.getContent());
                    continue;
                }

                if (part.isMimeType(MIME_MULTIPART)) {
                    final Object partContent = part.getContent();

                    if (partContent instanceof Multipart nestedMultipart) {
                        processMultipart(nestedMultipart, attachments, htmlContent);
                    }
                    continue;
                }
                final String fileName = part.getFileName();
                final String contentId = getContentId(part);
                final String disposition = part.getDisposition();
                final boolean isInline = Part.INLINE.equalsIgnoreCase(disposition)
                        || isContentIdReferenced(htmlContent, contentId);

                if (isInline) {
                    replaceInlineImage(htmlContent, part, contentId);
                } else if (isAttachmentPart(fileName, disposition)) {
                    attachments.add(buildEmailAttachment(part));
                }
            }
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.FAILED_TO_BUILD_MUTI_PART, exception);
        }
    }

    /**
     * <p>
     * Determines whether a MIME part should be treated as a downloadable attachment.
     * </p>
     *
     * @param fileName    Filename extracted from the part headers.
     * @param disposition Content disposition of the part.
     * @return {@code true} when part is an explicit attachment or has a non-blank filename.
     */
    private boolean isAttachmentPart(final String fileName, final String disposition) {
        return Part.ATTACHMENT.equalsIgnoreCase(disposition) || (Objects.nonNull(fileName) && !fileName.isBlank());
    }

    /**
     * <p>
     * Checks whether the current HTML already references the part's Content-ID via a CID URL.
     * </p>
     *
     * @param htmlContent Parsed HTML content builder.
     * @param contentId   Content-ID of the MIME part.
     * @return {@code true} when the HTML contains a matching {@code cid:} reference.
     */
    private boolean isContentIdReferenced(final StringBuilder htmlContent, final String contentId) {
        return Objects.nonNull(contentId) && !htmlContent.isEmpty() && htmlContent.indexOf(CID_PREFIX + contentId) >= 0;
    }

    /**
     * <p>
     * Replaces CID references in HTML with Base64 embedded images.
     * </p>
     *
     * @param htmlContent Represent the HTML content.
     * @param part        Represent the mime part.
     * @param contentId   Represent the content id.
     */
    private void replaceInlineImage(final StringBuilder htmlContent, final BodyPart part,
                                    final String contentId) {
        try {
            if (Objects.isNull(contentId) || htmlContent.isEmpty()) {
                return;
            }
            final byte[] bytes = part.getInputStream().readAllBytes();
            final String base64 = Base64.getEncoder().encodeToString(bytes);

            String contentType = part.getContentType();
            if (Objects.nonNull(contentType) && contentType.contains(";")) {
                contentType = contentType.split(";")[0];
            }
            final String dataUri = DATA_PREFIX + contentType + BASE64_PREFIX + base64;
            final String updatedHtml = htmlContent.toString().replace(CID_PREFIX + contentId, dataUri);

            htmlContent.setLength(NumericalConstant.ZERO);
            htmlContent.append(updatedHtml);
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.FAILED_TO_PARSE_MIME_MESSAGE, exception);
        }
    }

    /**
     * <p>
     * Builds EmailAttachment object from BodyPart.
     * </p>
     *
     * @param part Represent the mime part.
     */
    private EmailAttachment buildEmailAttachment(final BodyPart part) throws Exception {
        final EmailAttachment attachment = new EmailAttachment();
        String fileName = part.getFileName();

        if (Objects.nonNull(fileName)) {
            fileName = MimeUtility.decodeText(fileName);
        }
        final byte[] bytes = part.getInputStream().readAllBytes();

        attachment.setFileName(fileName);
        attachment.setExtension(getExtension(fileName));
        attachment.setMediaType(part.getContentType().split(";")[0]);
        attachment.setFileSize((long) bytes.length);
        attachment.setInputStream(new ByteArrayInputStream(bytes));

        return attachment;
    }

    /**
     * <p>
     * Extracts Content-ID header (used for inline images).
     * </p>
     *
     * @param part Represent the mime part.
     */
    private String getContentId(final BodyPart part) {
        try {
            final String[] headers = part.getHeader(CONTENT_ID_HEADER);

            if (Objects.nonNull(headers) && headers.length > 0) {
                return headers[0].replaceAll("[<>]", StringUtil.Delimiter.EMPTY_STRING.getDelimiter());
            }

            return null;
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.ATTACHMENT_RETRIEVAL_FAILED, exception);
        }
    }

    /**
     * <p>
     * Extracts file extension from filename.
     * </p>
     *
     * @param fileName Represents the file name
     */
    private String getExtension(final String fileName) {
        if (Objects.nonNull(fileName) && fileName.contains(StringUtil.Delimiter.DOT.getDelimiter())) {
            return fileName.substring(fileName.lastIndexOf('.') + NumericalConstant.ONE);
        }

        return StringUtil.Delimiter.EMPTY_STRING.getDelimiter();
    }
}