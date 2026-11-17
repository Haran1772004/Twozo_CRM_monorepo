package com.tlc.email.server.internal.provider.outlook;

import com.microsoft.graph.models.Attachment;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.InternetMessageHeader;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;

import com.tlc.attachment.io.Extension;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.parser.html.HtmlDocument;
import com.tlc.commons.parser.html.HtmlElement;
import com.tlc.commons.parser.html.HtmlElements;
import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.email.common.converter.Converter;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailAttachment;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.model.type.Email;

import com.tlc.email.server.fields.EmailFields;
import com.tlc.email.server.internal.field.EmailHeaderFields;
import com.tlc.email.server.internal.field.OutlookHeaderFields;

import com.tlc.notification.fields.NotificationKeys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Responsible for converting email-related messages into a graph-based structure.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class EmailGraphMessageConverter implements Converter<JsonObject, EmailGraphMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailGraphMessageConverter.class);

    private static final String EMPTY_CONTENT = "";
    private static final short CONTENT_ID_PREFIX_LENGTH = 4;

    private final HtmlParser htmlParser;

    private EmailGraphMessageConverter() {
        this.htmlParser = HtmlParser.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link EmailGraphMessageConverter}.
     * </p>
     */
    private static class EmailGraphMessageInstance {
        private static final EmailGraphMessageConverter INSTANCE = new EmailGraphMessageConverter();
    }

    public static EmailGraphMessageConverter getInstance() {
        return EmailGraphMessageInstance.INSTANCE;
    }

    /**
     * <p>
     * Converts the request json to {@link EmailGraphMessage}.
     * </p>
     *
     * @param graphMessageJson Represents the graph message json
     * @return {@link EmailGraphMessage}
     */
    @Override
    public EmailGraphMessage convert(final JsonObject graphMessageJson) {
        final EmailGraphMessage message = new EmailGraphMessage();

        message.setId(graphMessageJson.containsKey(EmailHeaderFields.ID) ? graphMessageJson.getString(EmailHeaderFields.ID) : null);
        message.setSentDateTime(graphMessageJson.containsKey(EmailHeaderFields.SENT_DATE_TIME) ? Instant.parse(graphMessageJson.getString(EmailHeaderFields.SENT_DATE_TIME)).toEpochMilli() : null);
        message.setReceivedDateTime(graphMessageJson.containsKey(EmailHeaderFields.RECEIVED_DATE_TIME) ? Instant.parse(graphMessageJson.getString(EmailHeaderFields.RECEIVED_DATE_TIME)).toEpochMilli() : null);
        message.setSubject(graphMessageJson.optString(EmailHeaderFields.SUBJECT, null));
        message.setBodyPreview(graphMessageJson.optString(EmailHeaderFields.BODY_PREVIEW, null));
        message.setRead(graphMessageJson.optBoolean(EmailHeaderFields.IS_READ));
        message.setDraft(graphMessageJson.optBoolean(EmailHeaderFields.IS_DRAFT));
        message.setInternetMessageId(graphMessageJson.containsKey(EmailHeaderFields.INTERNET_MESSAGE_ID) ? graphMessageJson.getString(EmailHeaderFields.INTERNET_MESSAGE_ID) : null);
        message.setConversationId(graphMessageJson.containsKey(EmailHeaderFields.CONVERSATION_ID) ? graphMessageJson.getString(EmailHeaderFields.CONVERSATION_ID) : null);
        message.setParentFolderId(graphMessageJson.containsKey(EmailHeaderFields.PARENT_FOLDER_ID) ? graphMessageJson.getString(EmailHeaderFields.PARENT_FOLDER_ID) : null);
        message.setHasAttachments(graphMessageJson.containsKey(EmailHeaderFields.HAS_ATTACHMENTS) && graphMessageJson.getBoolean(EmailHeaderFields.HAS_ATTACHMENTS));

        if (graphMessageJson.containsKey(EmailHeaderFields.BODY)) {
            final JsonObject bodyJson = graphMessageJson.getJsonObject(EmailHeaderFields.BODY);
            final Collection<EmailAttachment> attachments = buildEmailAttachments(graphMessageJson, bodyJson);

            message.setBody(bodyJson.optString(EmailHeaderFields.CONTENT, null));
            message.setEmailAttachments(attachments);
        }

        if (graphMessageJson.containsKey(EmailHeaderFields.INTERNET_MESSAGE_HEADERS)) {
            final JsonArray internetMessageHeaders = graphMessageJson.getJsonArray(EmailHeaderFields.INTERNET_MESSAGE_HEADERS);
            final Map<String, String> headers = new HashMap<>();
            final int headerSize = internetMessageHeaders.size();

            for (int headerIndex = 0; headerIndex < headerSize; headerIndex++) {
                final JsonObject internetMessageHeader = internetMessageHeaders.getJsonObject(headerIndex);
                final String headerName = internetMessageHeader.getString(EmailHeaderFields.NAME);
                final String value = internetMessageHeader.getString(EmailHeaderFields.VALUE);

                if (Objects.nonNull(headerName) && Objects.nonNull(value)) {
                    headers.put(headerName, value);
                }
            }

            message.setInternetMessageHeaders(headers);
        }

        if (graphMessageJson.containsKey(EmailHeaderFields.REPLY_TO)) {
            final JsonArray replyTo = graphMessageJson.getJsonArray(EmailHeaderFields.REPLY_TO);

            if (Objects.nonNull(replyTo) && !replyTo.toList().isEmpty()) {
                final JsonObject firstReplyToJson = replyTo.getJsonObject(0);
                final JsonObject emailAddress = firstReplyToJson.getJsonObject(EmailHeaderFields.EMAIL_ADDRESS);

                message.setReplyTo(new EmailAddress(emailAddress.getString(EmailHeaderFields.ADDRESS)));
            }
        }

        if (graphMessageJson.containsKey(OutlookHeaderFields.FROM)) {
            final JsonObject sender = graphMessageJson.getJsonObject(OutlookHeaderFields.FROM);
            final JsonObject emailAddress = sender.getJsonObject(EmailHeaderFields.EMAIL_ADDRESS);

            if (emailAddress.containsKey(EmailHeaderFields.ADDRESS)) {
                message.setFrom(new SyncAddressInfo(emailAddress.getString(EmailHeaderFields.ADDRESS)));
            }
        }

        message.setToRecipients(emailAddresses(EmailHeaderFields.TO_RECIPIENTS, graphMessageJson));
        message.setCcRecipients(emailAddresses(EmailHeaderFields.CC_RECIPIENTS, graphMessageJson));
        message.setBccRecipients(emailAddresses(EmailHeaderFields.BCC_RECIPIENTS, graphMessageJson));

        return message;
    }

    /**
     * <p>
     * Constructs email message json for the given {@link EmailGraphMessage}, folder id, folder name
     * </p>
     *
     * @param graphMessage Represents the {@link EmailGraphMessage}
     * @param folderId     Represents the folder id
     * @param folderName   Represents the  folder name
     * @return The  {@link JsonObject} containing the email message details
     */
    public JsonObject convertToJson(final EmailGraphMessage graphMessage, final String folderId, final FolderType folderName) {
        final JsonObject jsonObject = Json.object();
        final Map<String, String> internetMessageHeaders = graphMessage.getInternetMessageHeaders();
        final EmailAddress replyToAddress = graphMessage.getReplyTo();

        if (Objects.nonNull(replyToAddress)) {
            jsonObject.put(EmailFields.REPLY_TO_ADDRESS, replyToAddress.getEmailAddress());
        }

        if (!internetMessageHeaders.isEmpty()) {
            final String inReplyTo = internetMessageHeaders.get(EmailHeaderFields.IN_REPLY_TO);
            final String references = internetMessageHeaders.get(EmailHeaderFields.REFERENCES);

            jsonObject.put(EmailFields.IS_INITIAL_MESSAGE, Objects.isNull(inReplyTo) && Objects.isNull(references));
            jsonObject.put(EmailFields.IN_REPLY_TO, inReplyTo);
            jsonObject.put(EmailFields.REFERENCES, references);
            jsonObject.put(EmailFields.RETURN_PATH, internetMessageHeaders.get(EmailHeaderFields.RETURN_PATH));
        }

        jsonObject.put(EmailFields.SENT_DATE_TIME, graphMessage.getSentDateTime());
        jsonObject.put(EmailFields.RECEIVED_DATE_TIME, graphMessage.getReceivedDateTime());
        jsonObject.put(EmailFields.CONVERSATION_ID, graphMessage.getConversationId());
        jsonObject.put(EmailFields.CONTENT, graphMessage.getBody());
        jsonObject.put(EmailFields.SUBJECT, graphMessage.getSubject());
        jsonObject.put(EmailFields.IS_READ, graphMessage.isRead());
        jsonObject.put(EmailFields.IS_DRAFT, graphMessage.isDraft());
        jsonObject.put(EmailFields.MESSAGE_ID, graphMessage.getInternetMessageId());
        jsonObject.put(EmailFields.ID, graphMessage.getId());
        jsonObject.put(EmailFields.PARENT_FOLDER_ID, folderId);
        jsonObject.put(EmailFields.MESSAGE_CATEGORY, Email.MessageCategory.getMessageCategory(folderName).getId());
        jsonObject.put(EmailFields.RECIPIENTS, getRecipientsJson(graphMessage));
        final EmailAddress fromAddress = graphMessage.getFrom();

        if (Objects.nonNull(fromAddress)) {
            jsonObject.put(OutlookHeaderFields.FROM, fromAddress.getEmailAddress());
        }

        return jsonObject;
    }

    /**
     * <p>
     * Constructs a {@link JsonObject} containing the recipients of the email
     * </p>
     * <p>
     *
     * @param graphMessage Represents the {@link EmailGraphMessage}
     * @return The {@link JsonObject} containing the recipients of the email
     */
    private JsonObject getRecipientsJson(final EmailGraphMessage graphMessage) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(EmailFields.TO_RECIPIENTS, graphMessage.getToRecipients());
        jsonObject.put(EmailFields.CC_RECIPIENTS, graphMessage.getCcRecipients());
        jsonObject.put(EmailFields.BCC_RECIPIENTS, graphMessage.getBccRecipients());

        return jsonObject;
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link EmailAddress} the request json to {@link EmailGraphMessage}.
     * </p>
     *
     * @param recipientsKey Represents the recipient key
     * @param recipientJson Represents the {@link JsonObject} of recipient
     * @return {@link EmailGraphMessage}
     */
    private Collection<EmailAddress> emailAddresses(final String recipientsKey, final JsonObject recipientJson) {
        final Collection<EmailAddress> toAddresses = new ArrayList<>();

        if (recipientJson.containsKey(recipientsKey)) {
            final JsonArray recipientsArray = recipientJson.getJsonArray(recipientsKey);
            final int recipientsSize = recipientsArray.size();

            for (int recipientIndex = 0; recipientIndex < recipientsSize; recipientIndex++) {
                final JsonObject jsonObject = recipientsArray.getJsonObject(recipientIndex);
                final JsonObject emailAddress = jsonObject.getJsonObject(EmailFields.EMAIL_ADDRESS);

                if (emailAddress.containsKey(EmailFields.ADDRESS)) {
                    toAddresses.add(new EmailAddress(emailAddress.getString(EmailFields.ADDRESS)));
                }
            }
        }

        return toAddresses;
    }

    /**
     * <p>
     * Converts the request json to {@link EmailGraphMessage}.
     * </p>
     *
     * @param message Represents the graph message json
     * @return {@link EmailGraphMessage}
     */
    public EmailGraphMessage convert(final Message message) {
        final EmailGraphMessage emailGraphMessage = new EmailGraphMessage();
        final OffsetDateTime sentDateTime = message.sentDateTime;
        final OffsetDateTime receivedDateTime = message.receivedDateTime;

        emailGraphMessage.setId(message.id);
        emailGraphMessage.setSentDateTime(Objects.nonNull(sentDateTime) ? sentDateTime.toInstant().toEpochMilli() : null);
        emailGraphMessage.setReceivedDateTime(Objects.nonNull(receivedDateTime) ? receivedDateTime.toInstant().toEpochMilli() : null);
        emailGraphMessage.setSubject(message.subject);
        emailGraphMessage.setBodyPreview(message.bodyPreview);
        emailGraphMessage.setRead(Boolean.TRUE.equals(message.isRead));
        emailGraphMessage.setInternetMessageId(message.internetMessageId);
        emailGraphMessage.setConversationId(message.conversationId);
        emailGraphMessage.setParentFolderId(message.parentFolderId);

        final ItemBody body = message.body;
        final Collection<InternetMessageHeader> internetMessageHeaders = message.internetMessageHeaders;
        final Collection<Recipient> replyTo = message.replyTo;
        final Recipient from = message.from;

        if (Objects.nonNull(body)) {
            emailGraphMessage.setBody(body.content);
        }

        if (Objects.nonNull(internetMessageHeaders)) {
            final Map<String, String> headers = new HashMap<>();

            for (final InternetMessageHeader internetMessageHeader : internetMessageHeaders) {
                headers.put(internetMessageHeader.name, internetMessageHeader.value);
            }

            emailGraphMessage.setInternetMessageHeaders(headers);
        }

        if (Objects.nonNull(replyTo)) {
            final Optional<Recipient> OptionalReplyTo = replyTo.stream().findFirst();

            if (OptionalReplyTo.isPresent()) {
                final Recipient recipient = OptionalReplyTo.get();
                final com.microsoft.graph.models.EmailAddress emailAddress = recipient.emailAddress;

                if (Objects.nonNull(emailAddress)) {
                    emailGraphMessage.setReplyTo(new EmailAddress(emailAddress.address));
                }
            }
        }

        if (Objects.nonNull(from)) {
            final com.microsoft.graph.models.EmailAddress fromAddress = from.emailAddress;

            if (Objects.nonNull(fromAddress)) {
                emailGraphMessage.setFrom(new SyncAddressInfo(fromAddress.address));
            }
        }

        emailGraphMessage.setToRecipients(emailAddresses(message.toRecipients));
        emailGraphMessage.setCcRecipients(emailAddresses(message.ccRecipients));
        emailGraphMessage.setBccRecipients(emailAddresses(message.bccRecipients));

        return emailGraphMessage;
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link EmailAddress} the request json to {@link EmailGraphMessage}.
     * </p>
     *
     * @param recipients Represents the {@link Collection} of {@link Recipient}
     * @return {@link EmailGraphMessage}
     */
    private Collection<EmailAddress> emailAddresses(final Collection<Recipient> recipients) {
        final Collection<EmailAddress> addresses = new ArrayList<>();

        if (Objects.nonNull(recipients) && !recipients.isEmpty()) {

            for (final Recipient recipient : recipients) {
                final com.microsoft.graph.models.EmailAddress emailAddress = recipient.emailAddress;

                if (Objects.nonNull(emailAddress)) {
                    addresses.add(new EmailAddress(emailAddress.address));
                }
            }
        }

        return addresses;
    }

    /**
     * <p>
     * Builds a collection of email attachments from a JSON object.
     * </p>
     *
     * @param messageJson The JSON object containing the message details, including attachments.
     * @return A collection of {@link EmailAttachment} objects corresponding to the attachments.
     */
    private Collection<EmailAttachment> buildEmailAttachments(final JsonObject messageJson, final JsonObject bodyJson) {
        final Collection<EmailAttachment> emailAttachments = new ArrayList<>();
        final String attachmentsKey = NotificationKeys.ATTACHMENTS.getKey();

        if (messageJson.containsKey(attachmentsKey)) {
            final String isInlineKey = NotificationKeys.IS_INLINE.getKey();
            final String contentJson = bodyJson.getString(EmailHeaderFields.CONTENT);
            final String content = Objects.isNull(contentJson) ? EMPTY_CONTENT : contentJson;
            final JsonArray attachmentsJson = messageJson.getJsonArray(attachmentsKey);
            final HtmlDocument htmlDocument = htmlParser.parse(content);
            final HtmlElements htmlElements = getContentIdElements(htmlDocument);

            final Map<String, List<HtmlElement>> groupedContentIdElements =
                    content.isEmpty() ? Collections.emptyMap() : groupContentIdElements(htmlElements);
            final int attachmentJsonSize = attachmentsJson.size();

            for (int offset = 0; offset < attachmentJsonSize; offset++) {
                final JsonObject attachmentJson = attachmentsJson.getJsonObject(offset);
                final boolean isInline = attachmentJson.optBoolean(isInlineKey, false);

                if (isInline) {
                    processInlineElements(groupedContentIdElements, attachmentJson);
                } else {
                    final EmailAttachment emailAttachment = buildEmailAttachment(attachmentJson);

                    if (Objects.nonNull(emailAttachment)) {
                        emailAttachments.add(emailAttachment);
                    }
                }
            }
            bodyJson.put(EmailHeaderFields.CONTENT, htmlDocument.html());

            messageJson.remove(attachmentsKey);
        }

        return emailAttachments;
    }

    /**
     * Processes inline elements by updating their `src` attribute with base64 data.
     *
     * @param groupedInlineElements Map of Content ID's to collections of {@link HtmlElement}.
     * @param attachmentJson        JSON object representing the attachment.
     */
    private void processInlineElements(final Map<String, List<HtmlElement>> groupedInlineElements,
                                       final JsonObject attachmentJson) {
        final String contentBytesKey = NotificationKeys.CONTENT_BYTES.getKey();
        final String contentIdKey = EmailHeaderFields.CONTENT_ID;

        if (attachmentJson.containsKey(contentBytesKey)) {
            final String contentBytes = attachmentJson.getString(contentBytesKey);
            final String contentId = attachmentJson.getString(contentIdKey);

            if (groupedInlineElements.containsKey(contentId)) {
                final Collection<HtmlElement> htmlElements = groupedInlineElements.get(contentId);
                final String newSrcValue = String.format("data: %s; base64, %s", attachmentJson.getString(NotificationKeys.MEDIA_CONTENT_TYPE.getKey()), contentBytes);

                for (final HtmlElement htmlElement : htmlElements) {
                    htmlElement.attribute(EmailHeaderFields.SRC, newSrcValue);
                }
            }
        }
    }

    /**
     * Retrieves all inline elements with `src` attributes starting with `cid:`.
     *
     * @param content The {@link HtmlDocument} containing the HTML content.
     * @return The {@link HtmlElements} representing inline elements with `cid` references.
     */
    private HtmlElements getContentIdElements(final HtmlDocument content) {
        return content.select("img[src^=cid:]");
    }

    /**
     * Groups `HtmlElement` objects by their `cid` values.
     *
     * @param htmlElements The {@link HtmlElements} containing elements with `cid` references.
     * @return A map where the key is the `cid` and the value is a collection of {@link HtmlElement}.
     */
    private Map<String, List<HtmlElement>> groupContentIdElements(final HtmlElements htmlElements) {
        return htmlElements.getElements().stream()
                .collect(Collectors.groupingBy(
                        element -> extractContentIdFromSrc(element.attribute(EmailHeaderFields.SRC)),
                        Collectors.toList()
                ));
    }

    /**
     * Extracts the `cid` from the `src` attribute of an element.
     *
     * @param src The `src` attribute value.
     * @return The `cid` value, or null if the `src` does not contain a valid `cid`.
     */
    private String extractContentIdFromSrc(final String src) {

        if (Objects.nonNull(src) && src.startsWith("cid:")) {
            return src.substring(CONTENT_ID_PREFIX_LENGTH);
        }

        return null;
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
            } else {
                LOGGER.error("Email Sync: Attachment retrieval failed due to unsupported extension. Provider: Outlook, File name:{}", fileName);
            }
        }

        return null;
    }

    /**
     * <p>
     * Converts a collection of Microsoft Graph API {@link Attachment} objects
     * into a categorized map of {@link EmailAttachment} objects.
     * </p>
     *
     * @param attachments the collection of Graph API attachments retrieved from a message
     * @return a map containing two keys: "inline" and "regular", each mapping to a collection of {@link EmailAttachment} objects
     */
    public Map<String, Collection<EmailAttachment>> convertAttachments(final Collection<Attachment> attachments) {
        final Map<String, Collection<EmailAttachment>> attachmentMap = new HashMap<>();
        final Collection<EmailAttachment> inlineAttachments = new ArrayList<>();
        final Collection<EmailAttachment> normalAttachments = new ArrayList<>();

        for (final Attachment attachment : attachments) {
            if (attachment instanceof FileAttachment fileAttachment) {
                final EmailAttachment emailAttachment = new EmailAttachment();
                final byte[] contentBytes = fileAttachment.contentBytes;

                emailAttachment.setFileName(fileAttachment.name);
                emailAttachment.setMediaType(fileAttachment.contentType);
                emailAttachment.setFileSize(fileAttachment.size == null ? 0 : fileAttachment.size.longValue());
                emailAttachment.setExtension(getExtensionFromFile(fileAttachment.name));
                emailAttachment.setInternalId(fileAttachment.contentId);

                if (Objects.nonNull(contentBytes)) {
                    emailAttachment.setInputStream(new ByteArrayInputStream(contentBytes));
                }

                if (Boolean.TRUE.equals(fileAttachment.isInline)) {
                    inlineAttachments.add(emailAttachment);
                } else {
                    normalAttachments.add(emailAttachment);
                }
            }
        }
        attachmentMap.put("inline", inlineAttachments);
        attachmentMap.put("regular", normalAttachments);

        return attachmentMap;
    }

    /**
     * Gets the extension from file.
     *
     * @param fileName Represents the file name
     * @return file extension
     */
    private String getExtensionFromFile(final String fileName) {

        if (Objects.nonNull(fileName) && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.') + 1);
        }

        return "";
    }
}