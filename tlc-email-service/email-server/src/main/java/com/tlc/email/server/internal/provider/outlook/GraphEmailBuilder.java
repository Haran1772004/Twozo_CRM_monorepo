package com.tlc.email.server.internal.provider.outlook;

import com.microsoft.graph.models.InternetMessageHeader;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.requests.GraphServiceClient;

import com.tlc.attachment.io.Extension;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.ConsentType;
import com.tlc.email.common.model.EmailAttachment;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.EmailConfiguration;
import com.tlc.email.common.model.config.TenantConfiguration;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.InboundEmail;
import com.tlc.email.common.model.type.OutboundEmail;

import com.tlc.email.server.internal.field.EmailHeaderFields;

import com.tlc.email.server.internal.field.OutlookHeaderFields;
import com.tlc.email.server.internal.webhook.receiver.provider.outlook.model.OutlookEventNotification;
import com.tlc.notification.fields.NotificationKeys;

import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static com.tlc.email.common.model.address.AddressType.BCC;
import static com.tlc.email.common.model.address.AddressType.CC;
import static com.tlc.email.common.model.address.AddressType.TO;
import static com.tlc.email.common.model.type.Email.MessageCategory.DRAFT;
import static com.tlc.email.common.model.type.Email.MessageCategory.INBOUND;
import static com.tlc.email.common.model.type.Email.MessageCategory.OUTBOUND;
import static com.tlc.email.server.internal.field.EmailKeys.ARCHIVE;
import static com.tlc.email.server.internal.field.EmailKeys.OUTLOOK_DRAFTS;
import static com.tlc.email.server.internal.field.EmailKeys.OUTLOOK_INBOX;
import static com.tlc.email.server.internal.field.EmailKeys.OUTLOOK_SENT_FOLDER;

/**
 * Responsible for constructing {@link Email} from Microsoft Graph API message data.
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public class GraphEmailBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphEmailBuilder.class);
    private static final Map<String, Supplier<Email>> EMAIL_MAP = new HashMap<>();

    private static final char GREATER_THAN = '>';
    private static final char LESS_THAN = '<';
    private static final String AT_SYMBOL = "@";

    static {
        EMAIL_MAP.put(OUTLOOK_INBOX.getKey(), InboundEmail::new);
        EMAIL_MAP.put(OUTLOOK_SENT_FOLDER.getKey(), OutboundEmail::new);
        EMAIL_MAP.put(OUTLOOK_DRAFTS.getKey(), DraftEmail::new);
        EMAIL_MAP.put(ARCHIVE.getKey(), Email::new);
    }

    private GraphEmailBuilder() {
    }

    private static class Instance {
        private static final GraphEmailBuilder INSTANCE = new GraphEmailBuilder();
    }

    /**
     * <p>
     * Gets the {@link GraphEmailBuilder} Instance
     * </p>
     *
     * @return {@link GraphEmailBuilder}
     */
    public static GraphEmailBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Constructs the email from retrieved message
     * </p>
     *
     * @param graphMessage    Refers to the {@link EmailGraphMessage}
     * @param accessToken     Refers to the access token
     * @param syncAddressInfo Refers to the {@link SyncAddressInfo}
     * @return {@link Email} email
     */
    public Email buildEmail(final EmailGraphMessage graphMessage, final String accessToken, final SyncAddressInfo syncAddressInfo) {
        final Email email = createEmailBasedOnFolder(graphMessage, accessToken, syncAddressInfo);

        return buildEmail(email, graphMessage);
    }

    /**
     * <p>
     * Constructs the email from retrieved message
     * </p>
     *
     * @param graphMessage  Refers to the {@link EmailGraphMessage}
     * @param accessToken   Refers to the access token
     * @param configuration Refers to the {@link EmailConfiguration}
     * @return {@link Email} email
     */
    public Email buildEmail(final EmailGraphMessage graphMessage, final String accessToken, final EmailConfiguration configuration) {
        final Email email = createEmailBasedOnFolder(graphMessage, accessToken, configuration);

        return buildEmail(email, graphMessage);
    }

    /**
     * Populates and returns an updated Email using data extracted from
     * the provided Graph message payload.
     *
     * @param email        the base Email to populate
     * @param graphMessage the parsed Graph message containing relevant email metadata
     * @return the updated Email with fields set from the Graph message
     */
    private Email buildEmail(final Email email, final EmailGraphMessage graphMessage) {
        final EmailMetaData metaData = email.getMetaData();
        final Map<String, String> internetMessageHeaders = graphMessage.getInternetMessageHeaders();
        final EmailAddress replyToAddress = graphMessage.getReplyTo();

        if (Objects.nonNull(internetMessageHeaders) && !internetMessageHeaders.isEmpty()) {
            metaData.setInReplyTo(internetMessageHeaders.get(EmailHeaderFields.IN_REPLY_TO));
            metaData.setReferences(internetMessageHeaders.get(EmailHeaderFields.REFERENCES));
            metaData.setReturnPath(internetMessageHeaders.get(EmailHeaderFields.RETURN_PATH));
        }
        metaData.setInitialMessage();

        email.setSubject(graphMessage.getSubject());
        email.setContent(graphMessage.getBody());
        email.setRecipients(List.of(populateRecipients(graphMessage)));
        email.setAttachments(graphMessage.getEmailAttachments());

        if (Objects.nonNull(replyToAddress)) {
            metaData.setReplyToAddress(replyToAddress.getEmailAddress());
        }

        metaData.setParentFolderId(graphMessage.getParentFolderId());
        metaData.setMessageId(graphMessage.getInternetMessageId());
        metaData.setExternalReference(graphMessage.getId());
        metaData.setConversationThreadId(graphMessage.getConversationId());
        metaData.setDate(graphMessage.getSentDateTime());
        final SyncAddressInfo fromAddress = graphMessage.getFrom();

        if (Objects.nonNull(fromAddress)) {
            email.setSender(fromAddress);
        }

        email.setMetaData(metaData);
        setOutboundType(email, metaData);

        return email;
    }

    /**
     * Sets the outbound type for the synced email data.
     *
     * @param email    Represents the {@link Email}
     * @param metaData Represents the {@link EmailMetaData}
     */
    private void setOutboundType(final Email email, final EmailMetaData metaData) {
        final Email.MessageCategory messageCategory = email.getMessageCategory();

        if (Objects.nonNull(messageCategory) && messageCategory.isSendCategory()) {
            final OutboundEmail outboundEmail = (OutboundEmail) email;
            final String inReplyTo = metaData.getInReplyTo();
            final String conversationId = metaData.getConversationThreadId();

            if (Objects.nonNull(inReplyTo) && !inReplyTo.isEmpty()) {
                final EmailRecipients emailRecipients = outboundEmail.getEmailRecipients();

                if (Objects.nonNull(emailRecipients)) {
                    final Collection<EmailAddress> toRecipients = emailRecipients.toRecipients();
                    final Collection<EmailAddress> ccRecipients = emailRecipients.toRecipients();
                    final int recipientCount = (Objects.nonNull(toRecipients) ? toRecipients.size() : 0) +
                            (Objects.nonNull(ccRecipients) ? ccRecipients.size() : 0);

                    if (recipientCount > 1) {
                        outboundEmail.setOutboundType(OutboundEmail.OutboundType.REPLY_ALL);
                    } else {
                        outboundEmail.setOutboundType(OutboundEmail.OutboundType.REPLY);
                    }
                }
            } else if (Objects.nonNull(conversationId) && !conversationId.isEmpty()) {
                outboundEmail.setOutboundType(OutboundEmail.OutboundType.FORWARD);
            } else {
                outboundEmail.setOutboundType(OutboundEmail.OutboundType.SEND);
            }
        }
    }

    /**
     * <p>
     * Creates the email based on the folder provided
     * </p>
     *
     * @param graphMessage  Refers to the {@link EmailGraphMessage}
     * @param accessToken   Represents the access token
     * @param configuration Refers to the {@link EmailConfiguration}
     * @return {@link Email}
     */
    private Email createEmailBasedOnFolder(final EmailGraphMessage graphMessage, final String accessToken, final EmailConfiguration configuration) {
        return getFolder(graphMessage, accessToken, configuration).map(folderName -> instantiateEmail(graphMessage, folderName, configuration.getSyncEmailAddress()))
                .orElseGet(Email::new);
    }

    /**
     * Creates an Email based on the folder information (e.g., Inbox, Sent Items, etc.)
     * contained in the Graph message.Interprets the folder context to determine how the Email should be constructed or classified.
     *
     * @param graphMessage    the message received from Microsoft Graph containing metadata
     * @param accessToken     the OAuth2 access token used to fetch additional email details if needed
     * @param syncAddressInfo information about the email address used for syncing or routing
     * @return a constructed Email object representing the Graph message, adjusted for its folder context
     */
    private Email createEmailBasedOnFolder(final EmailGraphMessage graphMessage, final String accessToken, final SyncAddressInfo syncAddressInfo) {
        return getFolder(graphMessage, accessToken, syncAddressInfo).map(folderName -> instantiateEmail(graphMessage, folderName, syncAddressInfo))
                .orElseGet(Email::new);
    }

    /**
     * <p>
     * Obtains the folder of the provided message using access token
     * </p>
     *
     * @param graphMessage Refers to the {@link EmailGraphMessage}
     * @param accessToken  Represents the access token
     * @return {@link Optional} of string
     */
    private Optional<String> getFolder(final EmailGraphMessage graphMessage, final String accessToken, final EmailConfiguration configuration) {
        final EmailConfiguration emailConfiguration = configuration.get();
        final SyncAddressInfo emailAddress = emailConfiguration.getSyncEmailAddress();
        final GraphServiceClient<Request> requestGraphServiceClient = GraphClient.build(accessToken);
        final Optional<String> value;

        if (emailAddress.getConsentType() == ConsentType.USER_GRANTED) {
            value = Optional.ofNullable(graphMessage.getParentFolderId())
                    .map(parentFolderId -> requestGraphServiceClient.me().mailFolders(parentFolderId).buildRequest().get())
                    .map(mailFolder -> mailFolder.displayName);
        } else {
            final TenantConfiguration tenantConfiguration = configuration.get();
            value = Optional.ofNullable(graphMessage.getParentFolderId())
                    .map(parentFolderId -> requestGraphServiceClient.users(tenantConfiguration.getEmailUserId()).mailFolders(parentFolderId).buildRequest().get())
                    .map(mailFolder -> mailFolder.displayName);
        }

        return value;
    }

    /**
     * Retrieves the folder name (e.g., Inbox, SentItems, Drafts) associated with the given
     * Graph message. This may involve using the message metadata directly or making a
     * Microsoft Graph API call using the provided access token.
     *
     * @param graphMessage    the email message object received from Microsoft Graph
     * @param accessToken     the OAuth2 token used for making authorized requests to Microsoft Graph, if needed
     * @param syncAddressInfo information about the email address used for syncing, which may influence folder resolution
     * @return an Optional containing the folder name if it could be resolved, or Optional.empty() otherwise
     */
    private Optional<String> getFolder(final EmailGraphMessage graphMessage, final String accessToken, final SyncAddressInfo syncAddressInfo) {
        final GraphServiceClient<Request> requestGraphServiceClient = GraphClient.build(accessToken);

        if (Objects.nonNull(syncAddressInfo)) {

            if (syncAddressInfo.getConsentType() == ConsentType.USER_GRANTED) {
                return Optional.ofNullable(graphMessage.getParentFolderId())
                        .map(parentFolderId -> requestGraphServiceClient.me().mailFolders(parentFolderId).buildRequest().get())
                        .map(mailFolder -> mailFolder.displayName);
            }

            return Optional.empty();
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Populates the recipients into a map from the message provided
     * </p>
     *
     * @param message Refers to the message
     * @return {@link Map} key refers to the {@link AddressType}, value has the recipients {@link EmailAddress}
     */
    private EmailRecipients populateRecipients(final EmailGraphMessage message) {
        final EmailRecipients emailRecipients = new EmailRecipients();
        final Map<String, String> internetMessageHeaders = message.getInternetMessageHeaders();
        final Map<AddressType, Collection<EmailAddress>> recipients = new EnumMap<>(AddressType.class);

        recipients.put(TO, message.getToRecipients());
        recipients.put(CC, message.getCcRecipients());

        if (Objects.isNull(internetMessageHeaders)) {
            recipients.put(BCC, message.getBccRecipients());
        } else {
            recipients.put(BCC, message.bccRecipients(internetMessageHeaders));
        }

        emailRecipients.set(recipients);

        return emailRecipients;
    }

    /**
     * <p>
     * Builds the {@link EmailAttachment} from the attachment object.
     * </p>
     *
     * @param attachmentJson Represents the attachment object.
     * @return Entity of {@link EmailAttachment}
     */
    private EmailAttachment buildEmailAttachment(final JsonObject attachmentJson) {
        final String contentBytesKey = NotificationKeys.CONTENT_BYTES.getKey();

        if (attachmentJson.containsKey(contentBytesKey)) {
            final String fileName = attachmentJson.getString(NotificationKeys.NAME.getKey());
            final String extension = Extension.getFileExtension(fileName);

            if (Extension.contains(extension)) {
                final String contentBytes = attachmentJson.getString(contentBytesKey);
                final Long currentTime = Instant.now().toEpochMilli();
                final String contentType = attachmentJson.getString(NotificationKeys.MEDIA_CONTENT_TYPE.getKey());
                final EmailAttachment emailAttachment = new EmailAttachment();

                if (Objects.nonNull(contentBytes)) {

                    try (final InputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(contentBytes))) {
                        emailAttachment.setFileName(fileName);
                        emailAttachment.setFileSize(attachmentJson.getLong(NotificationKeys.SIZE.getKey()));
                        emailAttachment.setCreatedTime(currentTime);
                        emailAttachment.setUpdatedTime(currentTime);
                        emailAttachment.setExtension(extension);
                        emailAttachment.setMediaType(contentType);
                        emailAttachment.setInputStream(inputStream);
                    } catch (Exception exception) {
                        throw ErrorCode.get(EmailErrorCodes.ATTACHMENT_RETRIEVAL_FAILED, exception);
                    }

                    return emailAttachment;
                }
                return null;
            }
        }

        return null;
    }

    /**
     * <p>
     * Creates the email for the provided message
     * </p>
     *
     * @param graphMessage Refers to the message
     * @param folder       Refers to the folder in which the message exists
     * @param emailAddress Refers to the {@link SyncAddressInfo}
     * @return {@link Email}
     */
    private Email instantiateEmail(final EmailGraphMessage graphMessage, final String folder, final SyncAddressInfo emailAddress) {
        final String folderName = folder.toLowerCase();
        final Supplier<Email> emailSupplier = EMAIL_MAP.getOrDefault(folderName, Email::new);
        final Email email = emailSupplier.get();
        final EmailMetaData metaData = new EmailMetaData();

        switch (folderName) {
            case EmailHeaderFields.INBOX -> {
                email.setMessageCategory(INBOUND);
                email.setRead(graphMessage.isRead());
                email.setSender(graphMessage.getFrom());
            }
            case EmailHeaderFields.SENT_ITEMS -> {
                email.setMessageCategory(OUTBOUND);
                email.setRead(graphMessage.isRead());
                email.setSender(emailAddress);
            }
            case EmailHeaderFields.DRAFTS -> {
                email.setMessageCategory(DRAFT);
                email.setSender(emailAddress);
            }
            case EmailHeaderFields.ARCHIVE -> {
                final SyncAddressInfo fromAddress = graphMessage.getFrom();

                if (Objects.nonNull(fromAddress)) {

                    if (fromAddress.getEmailAddress().equals(emailAddress.getEmailAddress())) {
                        email.setSender(emailAddress);
                    } else {
                        email.setRead(graphMessage.isRead());
                        email.setSender(fromAddress);
                    }
                } else {
                    email.setRead(graphMessage.isRead());
                    email.setSender(emailAddress);
                }
                email.setMessageCategory(Email.MessageCategory.ARCHIVE);
                email.setArchived(true);
            }
        }
        email.setMetaData(metaData);

        return email;
    }

    /**
     * <p>
     * Fetches the message
     * </p>
     *
     * @param graphServiceClient Represents the {@link GraphServiceClient}
     * @param item               Represents the {@link OutlookEventNotification}
     * @return {@link Message}
     */
    public Message getMessage(final GraphServiceClient<Request> graphServiceClient, final OutlookEventNotification item) {
        return graphServiceClient.users(item.getUserId()).messages(item.getMessageId()).buildRequest().select(getHeaders()).get();
    }

    /**
     * <p>
     * Fetches the email headers
     * </p>
     *
     * @return The email headers
     */
    private String getHeaders() {
        return String.join(", ", EmailHeaderFields.ID, OutlookHeaderFields.FROM, EmailHeaderFields.TO_RECIPIENTS,
                EmailHeaderFields.BCC_RECIPIENTS, EmailHeaderFields.CC_RECIPIENTS, EmailHeaderFields.SUBJECT,
                EmailHeaderFields.BODY, EmailHeaderFields.SENDER, EmailHeaderFields.IS_READ,
                EmailHeaderFields.IS_DRAFT, EmailHeaderFields.HAS_ATTACHMENTS, EmailHeaderFields.SENT_DATE_TIME,
                EmailHeaderFields.RECEIVED_DATE_TIME, EmailHeaderFields.CONVERSATION_ID, EmailHeaderFields.PARENT_FOLDER_ID,
                EmailHeaderFields.INTERNET_MESSAGE_ID, EmailHeaderFields.INTERNET_MESSAGE_HEADERS);
    }

    /**
     * <p>
     * Fetches the 'From' email address by requesting only the internetMessageHeaders.
     * </p>
     *
     * @param graphServiceClient Represents the {@link GraphServiceClient}
     * @param messageId          Represents the message ID
     * @return The extracted email address or null if not found
     */
    public String getFromAddressFromHeaders(final GraphServiceClient<Request> graphServiceClient, final String messageId) {
        try {
            return extractFromAddressFromHeaders(graphServiceClient.me().messages(messageId)
                    .buildRequest().select(EmailHeaderFields.INTERNET_MESSAGE_HEADERS).get());
        } catch (final Exception exception) {
            LOGGER.debug("Graph Email Builder: Failed to retrieve from address from headers for message ID {}. Error: {}", messageId, exception.getMessage());
        }

        return null;
    }

    /**
     * <p>
     * Extracts the 'From' email address from the internet message headers.
     * Tries 'From' header first, then falls back to 'Return-Path'.
     * </p>
     *
     * @param message The Message object containing internetMessageHeaders
     * @return The extracted email address or null if not found
     */
    private String extractFromAddressFromHeaders(final Message message) {

        if (Objects.isNull(message) || Objects.isNull(message.internetMessageHeaders)) {
            return null;
        }
        String returnPath = null;

        for (final InternetMessageHeader header : message.internetMessageHeaders) {

            if (Objects.isNull(header.name) || Objects.isNull(header.value)) {
                continue;
            }

            if (OutlookHeaderFields.FROM.equalsIgnoreCase(header.name)) {
                final String fromAddress = extractEmailFromHeaderValue(header.value);

                if (Objects.nonNull(fromAddress)) {
                    return fromAddress;
                }
            } else if (Objects.isNull(returnPath) && EmailHeaderFields.RETURN_PATH.equalsIgnoreCase(header.name)) {
                returnPath = extractEmailFromHeaderValue(header.value);
            }
        }

        return returnPath;
    }

    /**
     * <p>
     * Extracts email address from a header value.
     * Handles formats like:
     * - "user@example.com"
     * - "John Doe &lt;user@example.com&gt;"
     * - "&lt;user@example.com&gt;"
     * </p>
     *
     * @param headerValue Represents the header value
     * @return The extracted email address or null
     */
    private String extractEmailFromHeaderValue(final String headerValue) {

        if (Objects.isNull(headerValue) || headerValue.isEmpty()) {
            return null;
        }
        final int startIdx = headerValue.indexOf(LESS_THAN);
        final int endIdx = headerValue.indexOf(GREATER_THAN);

        if (startIdx != NumericalConstant.NEGATIVE_ONE && endIdx > startIdx) {
            return headerValue.substring(startIdx + NumericalConstant.ONE, endIdx).trim();
        }

        if (headerValue.contains(AT_SYMBOL)) {
            return headerValue.trim();
        }

        return null;
    }
}