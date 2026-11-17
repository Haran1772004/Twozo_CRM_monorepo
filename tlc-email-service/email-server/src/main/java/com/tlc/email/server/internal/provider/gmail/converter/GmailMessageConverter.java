package com.tlc.email.server.internal.provider.gmail.converter;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.tlc.attachment.io.Extension;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.StringUtil;
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
import com.tlc.email.server.internal.field.EmailKeys;
import com.tlc.email.server.internal.field.GmailHeaderFields;
import com.tlc.email.server.internal.provider.gmail.model.Label;
import com.tlc.email.server.internal.provider.gmail.service.GmailMessageContentExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.tlc.email.common.model.type.Email.MessageCategory.DRAFT;
import static com.tlc.email.common.model.type.Email.MessageCategory.INBOUND;
import static com.tlc.email.common.model.type.Email.MessageCategory.OUTBOUND;
import static com.tlc.email.server.internal.field.EmailKeys.INBOX;
import static com.tlc.email.server.internal.field.EmailKeys.SENT;

/**
 * <p>
 * Converts a Gmail {@link Message} to the internal {@link Email} model.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class GmailMessageConverter implements Converter<Message, Email> {

    private static final char GREATER_THAN = '>';
    private static final char LESS_THAN = '<';

    private static final String GREATER_THAN_STRING = ">";
    private static final String LESS_THAN_STRING = "<";

    private static final String EMPTY_STRING = "";

    private static final Logger LOGGER = LoggerFactory.getLogger(GmailMessageConverter.class);
    private static final Map<String, Supplier<Email>> EMAIL_MAP = new HashMap<>();

    private final GmailMessageContentExtractor gmailMessageContentExtractor;

    static {
        EMAIL_MAP.put(INBOX.getKey(), InboundEmail::new);
        EMAIL_MAP.put(SENT.getKey(), OutboundEmail::new);
        EMAIL_MAP.put(EmailKeys.DRAFT.getKey(), DraftEmail::new);
    }

    private GmailMessageConverter() {
        this.gmailMessageContentExtractor = GmailMessageContentExtractor.getInstance();
    }

    private static class Instance {
        private static final GmailMessageConverter INSTANCE = new GmailMessageConverter();
    }

    public static GmailMessageConverter getInstance() {
        return GmailMessageConverter.Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts the given Gmail {@link Message} into an internal {@link Email} object.
     * </p>
     *
     * @param message the Gmail message object
     * @return converted message object with email metadata set
     */
    @Override
    public Email convert(final Message message) {
        final Map<String, String> headers = gmailMessageContentExtractor.extractHeaders(message);

        headers.put(EmailHeaderFields.BODY, gmailMessageContentExtractor.extractMessageBody(message));
        final Email email = buildEmail(message, headers);

        setAttachmentDetails(message, email);

        return email;
    }

    /**
     * <p>
     * Extracts attachment details from a Gmail {@link Message} and sets them on the provided {@link Email} object.
     * </p>
     *
     * @param message The Gmail {@link Message} containing attachments.
     * @param email   The {@link Email} object to set the extracted attachments on.
     */
    private void setAttachmentDetails(final Message message, final Email email) {
        try {
            final List<MessagePart> attachments = extractAttachments(message, email.getContent());
            final Collection<EmailAttachment> emailAttachments = new ArrayList<>();

            for (final MessagePart part : attachments) {
                final String fileName = part.getFilename();

                if (Objects.nonNull(fileName)) {
                    final String extension = Extension.getFileExtension(fileName);

                    if (Extension.contains(extension)) {
                        final String contentType = part.getMimeType();
                        final Optional<MessagePartBody> optionalMessagePartBody = Optional.ofNullable(part.getBody());

                        if (optionalMessagePartBody.isPresent()) {
                            final MessagePartBody messagePartBody = optionalMessagePartBody.get();
                            final Long currentTime = Instant.now().toEpochMilli();
                            final EmailAttachment emailAttachment = new EmailAttachment();

                            emailAttachment.setFileName(fileName);
                            emailAttachment.setCreatedTime(currentTime);
                            emailAttachment.setUpdatedTime(currentTime);
                            emailAttachment.setExtension(extension);
                            emailAttachment.setMediaType(contentType);
                            emailAttachment.setInternalId(messagePartBody.getAttachmentId());

                            emailAttachments.add(emailAttachment);
                        }
                    } else {
                        LOGGER.error("Email Sync: Attachment retrieval failed due to unsupported extension. Provider: Gmail, File name:{}", fileName);
                    }
                }
            }

            email.setAttachments(emailAttachments);
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.ATTACHMENT_RETRIEVAL_FAILED, exception);
        }
    }

    /**
     * <p>
     * Extracts the list of attachments from the given {@link Message} instance.
     * </p>
     *
     * @param message The {@link Message} object from which attachments are extracted
     * @param body    The message body used to determine if an attachment is inline
     * @return a {@link List} of {@link MessagePart} objects representing the attachments found
     */
    private List<MessagePart> extractAttachments(final Message message, final String body) {
        final List<MessagePart> attachments = new ArrayList<>();
        final MessagePart messagePart = message.getPayload();

        if (Objects.nonNull(messagePart)) {
            findAttachments(messagePart, attachments, body);
        }

        return attachments;
    }

    /**
     * <p>
     * Recursively finds attachments in the given {@link MessagePart} and collects them into the results list.
     * </p>
     *
     * @param part       The {@link MessagePart} to search for attachments
     * @param results    The {@link List} to collect found attachments
     * @param body       The message body used to determine if an attachment is inline
     */
    private void findAttachments(final MessagePart part, final List<MessagePart> results, final String body) {
        if (Objects.isNull(part)) {
            return;
        }

        final String filename = part.getFilename();
        final String attachmentId = Optional.ofNullable(part.getBody())
                .map(MessagePartBody::getAttachmentId)
                .orElseGet((() -> null));
        final boolean hasFilename = Objects.nonNull(filename);
        final boolean hasAttachmentId = Objects.nonNull(attachmentId);
        final boolean isInline = Objects.nonNull(body) && Objects.nonNull(filename) && body.contains("cid:" + filename);

        if (hasFilename && hasAttachmentId && !isInline) {
            results.add(part);
        }

        Optional.ofNullable(part.getParts())
                .ifPresent(subParts -> subParts.forEach(subPart -> findAttachments(subPart, results, body)));
    }

    /**
     * <p>
     * Builds a single internal Email object from a Gmail API Message, synchronizing metadata and headers.
     * </p>
     *
     * @param message The Gmail API Message
     * @param headers The extracted headers map from the message
     * @return The constructed internal Email object
     */
    private Email buildEmail(final Message message, final Map<String, String> headers) {
        final Email email = buildEmail(message, getFromAddress(headers));
        final EmailMetaData metaData = new EmailMetaData();
        final Function<String, String> getHeaderValueFunction = headerName -> headers.getOrDefault(headerName, null);

        String returnPath = getHeaderValueFunction.apply(EmailHeaderFields.RETURN_PATH);

        if (Objects.nonNull(returnPath) && !returnPath.isEmpty()) {

            if (returnPath.startsWith(LESS_THAN_STRING) && returnPath.endsWith(GREATER_THAN_STRING)) {
                returnPath = returnPath.substring(1, returnPath.length() - 1);
            }
            metaData.setReturnPath(returnPath.isEmpty() ? null : returnPath);
        }
        metaData.setConversationThreadId(message.getThreadId());
        metaData.setInternalMessageId(message.getId());
        metaData.setInReplyTo(getHeaderValueFunction.apply(EmailHeaderFields.IN_REPLY_TO));
        metaData.setReferences(getHeaderValueFunction.apply(EmailHeaderFields.IN_REPLY_TO));
        metaData.setDate(message.getInternalDate());
        metaData.setInitialMessage();
        metaData.setReplyToAddress(getHeaderValueFunction.apply(EmailHeaderFields.REPLY_TO));
        Optional.ofNullable(getHeaderValueFunction.apply(GmailHeaderFields.SENT_MESSAGE_ID))
                .or(() -> Optional.ofNullable(getHeaderValueFunction.apply(GmailHeaderFields.DRAFT_MESSAGE_ID)))
                .or(() -> Optional.ofNullable(getHeaderValueFunction.apply(GmailHeaderFields.MESSAGE_ID)))
                .ifPresent(metaData::setMessageId);

        email.setSubject(getHeaderValueFunction.apply(GmailHeaderFields.SUBJECT));
        email.setContent(getHeaderValueFunction.apply(EmailHeaderFields.BODY));
        email.setRecipients(List.of(populateRecipients(headers)));

        email.setMetaData(metaData);

        return email;
    }

    /**
     * <p>
     * Populates the EmailRecipients object based on headers for TO, CC, and BCC fields.
     * </p>
     *
     * @param headers map containing email headers
     * @return an EmailRecipients instance containing collections of email addresses for each address type
     */
    private EmailRecipients populateRecipients(final Map<String, String> headers) {
        final EmailRecipients emailRecipients = new EmailRecipients();
        final Map<AddressType, Collection<EmailAddress>> recipients = new EnumMap<>(AddressType.class);

        for (final AddressType addressType : AddressType.getRecipientTypes()) {
            recipients.put(addressType, getRecipients(addressType, headers));
        }
        emailRecipients.set(recipients);

        return emailRecipients;
    }

    /**
     * <p>
     * Retrieves a collection of {@link EmailAddress} instances from the specified header type in the provided headers map.
     * </p>
     *
     * @param addressType The {@link AddressType} indicating which header to extract (TO, CC, BCC)
     * @param headers     The map of header names to header values
     * @return A collection of {@link EmailAddress} objects parsed from the header; empty if none found or blank
     */
    private Collection<EmailAddress> getRecipients(final AddressType addressType, final Map<String, String> headers) {
        final Collection<EmailAddress> emailAddresses = new ArrayList<>();

        if (addressType == AddressType.TO) {
            final String failedRecipientValue = findHeaderValueIgnoreCase(headers, EmailHeaderFields.FAILED_RECIPIENTS);
            final String headerValue = Objects.isNull(failedRecipientValue)
                    ? findHeaderValueIgnoreCase(headers, addressType.getType())
                    : failedRecipientValue;

            loadEmailAddresses(headerValue, emailAddresses);
        } else {
            loadEmailAddresses(findHeaderValueIgnoreCase(headers, addressType.getType()), emailAddresses);
        }

        return emailAddresses;
    }

    /**
     * <p>
     * Finds a header value from the map in a case-insensitive manner,
     * </p>
     *
     * @param headers The map of header names to header values
     * @param key     The header key to search for (case-insensitive)
     * @return The header value if found, null otherwise
     */
    private String findHeaderValueIgnoreCase(final Map<String, String> headers, final String key) {
        if (Objects.isNull(headers) || Objects.isNull(key)) {
            return null;
        }

        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseGet(() -> null);
    }

    /**
     * <p>
     * Loads the recipients email addresses
     * </p>
     *
     * @param headerValue    Represents the {@link String} of email addresses
     * @param emailAddresses Represents the loaded {@link Collection} of email addresses
     */
    private void loadEmailAddresses(final String headerValue, final Collection<EmailAddress> emailAddresses) {
        if (Objects.isNull(headerValue) || headerValue.isBlank()) {
            return;
        }
        final String[] recipientArray = headerValue.split(StringUtil.Delimiter.COMMA.getDelimiter());

        for (String recipient : recipientArray) {
            recipient = recipient.trim();

            if (recipient.isEmpty()) {
                continue;
            }

            if (recipient.contains(LESS_THAN_STRING) && recipient.contains(GREATER_THAN_STRING)) {
                final int start = recipient.indexOf(LESS_THAN);
                final int end = recipient.indexOf(GREATER_THAN);

                if (start < end) {
                    emailAddresses.add(new EmailAddress(recipient.substring(start + 1, end).trim()));
                } else {
                    emailAddresses.add(new EmailAddress(recipient));
                }
            } else {
                emailAddresses.add(new EmailAddress(recipient));
            }
        }
    }

    /**
     * <p>
     * Creates the email for the provided message.
     * </p>
     *
     * @param message         Refers to the message.
     * @param syncAddressInfo Refers the sync address.
     * @return {@link Email}
     */
    private Email buildEmail(final Message message, final SyncAddressInfo syncAddressInfo) {
        final Optional<String> folderOptional = getFolderFromLabels(message);
        final Supplier<Email> supplier = EMAIL_MAP.getOrDefault(folderOptional.orElseGet(() -> null), Email::new);
        final Email email = supplier.get();

        folderOptional.ifPresent(folder -> {
            switch (folder) {
                case EmailHeaderFields.INBOX -> {
                    email.setSender(syncAddressInfo);
                    email.setMessageCategory(INBOUND);
                    email.setRead(isRead(message));
                }
                case GmailHeaderFields.SENT -> {
                    email.setMessageCategory(OUTBOUND);
                }
                case GmailHeaderFields.DRAFT -> {
                    email.setMessageCategory(DRAFT);
                }
            }
        });

        return email;
    }

    /**
     * <p>
     * Checks whether the given message is marked as read.
     * </p>
     *
     * @param message the Gmail message object.
     * @return true if the message is read, false if it is unread.
     */
    private boolean isRead(final Message message) {
        return !Optional.ofNullable(message.getLabelIds()).orElseGet(Collections::emptyList).contains(Label.Name.UNREAD.getValue());
    }

    /**
     * <p>
     * Determines the folder name based on specific Gmail labels present in the message.
     * Note : Although a Gmail message can have multiple labels, this method considers only one
     * folder label of the following: DRAFT, then SENT, and INBOX.
     * If none of these labels are found, it returns an empty {@link Optional}.
     * </p>
     *
     * @param message The Gmail message object.
     * @return an {@link Optional} containing the resolved folder name, or empty if no matching label is found.
     */
    private Optional<String> getFolderFromLabels(final Message message) {
        final List<String> labels = Optional.ofNullable(message.getLabelIds()).orElse(Collections.emptyList());

        if (labels.contains(Label.Name.DRAFT.getValue())) {
            return Optional.of(GmailHeaderFields.DRAFT);
        } else if (labels.contains(Label.Name.SENT.getValue())) {
            return Optional.of(GmailHeaderFields.SENT);
        } else if (labels.contains(Label.Name.INBOX.getValue())) {
            return Optional.of(EmailHeaderFields.INBOX);
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Extracts the "From" email address from the given headers map.
     * </p>
     *
     * @param headers map containing email headers
     * @return a SyncAddressInfo object containing the extracted email address.
     */
    private SyncAddressInfo getFromAddress(final Map<String, String> headers) {
        final String fromHeader = headers.get(GmailHeaderFields.FROM);

        if (Objects.isNull(fromHeader) || fromHeader.isEmpty()) {
            return null;
        }
        final String fromAddress;

        if (fromHeader.contains(LESS_THAN + EMPTY_STRING) && fromHeader.contains(GREATER_THAN + EMPTY_STRING)) {
            fromAddress = fromHeader.substring(fromHeader.indexOf(LESS_THAN) + 1, fromHeader.indexOf(GREATER_THAN));
        } else {
            fromAddress = fromHeader;
        }
        return new SyncAddressInfo(fromAddress);
    }
}