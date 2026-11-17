package com.tlc.email.server.internal.webhook.receiver.impl;

import com.microsoft.graph.models.Attachment;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.requests.AttachmentCollectionPage;
import com.microsoft.graph.requests.AttachmentCollectionRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;

import com.microsoft.graph.requests.MessageRequestBuilder;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.parser.html.HtmlDocument;
import com.tlc.commons.parser.html.HtmlElement;
import com.tlc.commons.parser.html.HtmlElements;
import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailAttachment;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.type.Email;

import com.tlc.email.server.fields.WebhookReceiverFields;
import com.tlc.email.server.internal.field.EmailHeaderFields;
import com.tlc.email.server.internal.field.OutlookHeaderFields;
import com.tlc.email.server.internal.manager.ThreadManager;
import com.tlc.email.server.internal.provider.EmailTokenHandler;
import com.tlc.email.server.internal.provider.outlook.EmailGraphMessage;
import com.tlc.email.server.internal.provider.outlook.EmailGraphMessageConverter;
import com.tlc.email.server.internal.provider.outlook.GraphClient;
import com.tlc.email.server.internal.provider.outlook.GraphEmailBuilder;
import com.tlc.email.server.internal.status.EmailWebhookErrorCodes;
import com.tlc.email.server.internal.webhook.receiver.AbstractWebhookNotificationHandler;
import com.tlc.email.server.internal.webhook.receiver.manager.EmailWebHookReceiverManager;
import com.tlc.email.server.internal.webhook.receiver.provider.outlook.converter.OutlookEventConverter;
import com.tlc.email.server.internal.webhook.receiver.provider.outlook.model.EncryptedContent;
import com.tlc.email.server.internal.webhook.receiver.provider.outlook.model.OutlookEvent;
import com.tlc.email.server.internal.webhook.receiver.provider.outlook.model.OutlookEventNotification;
import com.tlc.email.server.internal.webhook.receiver.provider.outlook.model.OutlookEventNotification.ChangeType;
import com.tlc.email.server.internal.webhook.receiver.provider.util.OutlookWebhookPayloadDecryptor;
import com.tlc.email.server.service.WebhookNotificationHandler;
import okhttp3.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Responsible for processing incoming outlook webhook events.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see WebhookNotificationHandler
 */
public class OutlookWebhookNotificationHandler extends AbstractWebhookNotificationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutlookWebhookNotificationHandler.class);

    private final EmailWebHookReceiverManager emailWebHookReceiverManager;
    private final OutlookEventConverter outlookEventConverter;
    private final EmailGraphMessageConverter emailGraphMessageConverter;
    private final GraphEmailBuilder graphEmailBuilder;
    private final EmailTokenHandler emailTokenHandler;
    private final OutlookWebhookPayloadDecryptor decrypt;
    private final ThreadManager threadManager;
    private final HtmlParser htmlParser;

    private OutlookWebhookNotificationHandler() {
        this.emailWebHookReceiverManager = EmailWebHookReceiverManager.get();
        this.outlookEventConverter = OutlookEventConverter.getInstance();
        this.emailGraphMessageConverter = EmailGraphMessageConverter.getInstance();
        this.decrypt = OutlookWebhookPayloadDecryptor.getInstance();
        this.emailTokenHandler = EmailTokenHandler.getInstance();
        this.graphEmailBuilder = GraphEmailBuilder.getInstance();
        this.threadManager = ThreadManager.getInstance();
        this.htmlParser = HtmlParser.getInstance();
    }

    /**
     * <p>
     * Holds the single instance of {@link OutlookWebhookNotificationHandler}.
     * </p>
     */
    private static class Instance {
        private static final OutlookWebhookNotificationHandler INSTANCE = new OutlookWebhookNotificationHandler();
    }

    /**
     * Gets the instance of {@link OutlookWebhookNotificationHandler}
     *
     * @return {@link OutlookWebhookNotificationHandler}
     */
    public static OutlookWebhookNotificationHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param payload Represents the request payload
     * @return {@link Email}
     */
    @Override
    protected Collection<Email> getEmails(final JsonObject payload) {
        return getProcessedEmails(payload);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SyncAddressInfo}
     */
    @Override
    protected Optional<SyncAddressInfo> getSyncAddressInfo(final JsonObject payload) {

        if (payload.containsKey(WebhookReceiverFields.VALUE)) {
            final JsonArray jsonArray = payload.getJsonArray(WebhookReceiverFields.VALUE);
            final JsonObject notificationJson = jsonArray.getJsonObject(0);
            final String subscriptionId = notificationJson.getString(WebhookReceiverFields.SUBSCRIPTION_ID);

            return emailWebHookReceiverManager.getSyncAddressInfo(subscriptionId);
        }

        return Optional.empty();
    }

    /**
     * Processes the incoming event payload received from the webhook.
     *
     * @param eventPayload The JSON object containing the raw event data from the email provider.
     * @return A collection of parsed and constructed Email derived from the event payload.
     */
    public Collection<Email> getProcessedEmails(final JsonObject eventPayload) {
        final Collection<Email> emails = new ArrayList<>();

        try {
            final OutlookEvent outlookEvent = outlookEventConverter.convert(eventPayload);

            for (final OutlookEventNotification notification : outlookEvent.getValue()) {
                final Collection<Thread> modifiedThreads = new HashSet<>();
                final Optional<SyncAddressInfo> optionalSubscription =
                        emailWebHookReceiverManager.getSyncAddressInfo(notification.getSubscriptionId());

                if (optionalSubscription.isEmpty()) {
                    LOGGER.debug("Webhook Receiver: Received notification for unknown subscription ID: {}", notification.getSubscriptionId());
                    continue;
                }
                final SyncAddressInfo syncAddressInfo = optionalSubscription.get();
                final String accessToken = emailTokenHandler.getAccessToken(syncAddressInfo);
                final GraphServiceClient<Request> graphServiceClient = GraphClient.build(accessToken);
                final Optional<Long> optionalSyncedEmailId = syncSettingsManager.getId(syncAddressInfo.getEmailAddress());

                optionalSyncedEmailId.ifPresent(syncedEmailId -> {
                    final ChangeType changeType = notification.getChangeType();

                    if (changeType.isCreated()) {
                        processMessage(
                                syncAddressInfo.id(),
                                graphServiceClient,
                                notification, syncAddressInfo,
                                accessToken, modifiedThreads,
                                Collections.emptyList(), changeType
                        ).ifPresent(emails::add);
                    } else {
                        processModifiedMessage(
                                syncAddressInfo.id(), graphServiceClient, syncAddressInfo, accessToken,
                                notification, modifiedThreads, changeType
                        ).ifPresent(emails::add);
                    }

                    if (!modifiedThreads.isEmpty()) {
                        handleModifiedMails(syncAddressInfo, modifiedThreads);
                    }
                });
            }
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Webhook Receiver: Failed to process event payload, message: {}, trace: {}", throwable.getMessage(), throwable);
        }

        return emails;
    }

    /**
     * <p>
     * Handles inline images in the email content by replacing "cid:" references with base64-encoded data URLs.
     * </p>
     *
     * @param graphServiceClient The Graph API client to fetch attachments.
     * @param email              The email whose content is to be processed for inline images.
     */
    public void handleInlineImages(final GraphServiceClient<Request> graphServiceClient, final Email email) {
        final Map<String, Collection<EmailAttachment>> attachmentGroups =
                emailGraphMessageConverter.convertAttachments(getAllAttachments(graphServiceClient, "me", email.getMetaData().getExternalReference()));
        final Collection<EmailAttachment> inlineAttachments = attachmentGroups.get("inline");

        if (Objects.nonNull(inlineAttachments) && !inlineAttachments.isEmpty()) {
            final HtmlDocument htmlDocument = htmlParser.parse(email.getContent());
            handleInlineAttachments(htmlDocument, inlineAttachments);
            email.setContent(htmlDocument.html());
        }
    }

    /**
     * <p>
     * Processes a deleted or updated message event using its internal message ID and updates related threads.
     * </p>
     *
     * @param emailId                  Represents the sync settings id.
     * @param graphServiceClient       The Graph API client.
     * @param syncAddressInfo          Represents the sync address info.
     * @param accessToken              The access token.
     * @param outlookEventNotification The event notification from Outlook.
     * @param modifiedThreads          The collection of threads to update.
     * @param changeType               Represents the {@link ChangeType}
     * @return {@link Optional} containing new {@link Email} if created, otherwise empty.
     */
    private Optional<Email> processModifiedMessage(final Long emailId, final GraphServiceClient<Request> graphServiceClient,
                                                   final SyncAddressInfo syncAddressInfo, final String accessToken,
                                                   final OutlookEventNotification outlookEventNotification,
                                                   final Collection<Thread> modifiedThreads, final ChangeType changeType) {
        final String alternateId = outlookEventNotification.getMessageId();
        final Optional<String> optionalMessageId = referenceManager.getMessageIdByAlternateId(emailId, alternateId);
        final Collection<Thread> existingThreads = getExistingThreads(emailId, optionalMessageId);

        if (changeType.isDeleted()) {
            processDeletedMessages(existingThreads, modifiedThreads);

            return Optional.empty();
        }

        return processMessage(
                emailId, graphServiceClient, outlookEventNotification, syncAddressInfo, accessToken, modifiedThreads, existingThreads, changeType
        );
    }

    /**
     * <p>
     * Marks emails within the provided threads as deleted and collects the modified threads.
     * </p>
     *
     * @param existingThreads The threads containing the emails to be marked as deleted.
     * @param modifiedThreads The collection to which modified threads will be added.
     */
    private void processDeletedMessages(final Collection<Thread> existingThreads, final Collection<Thread> modifiedThreads) {
        existingThreads.forEach(thread -> thread.getComposeEmail().ifPresent(email -> {
            email.setDeleted(true);
            modifiedThreads.add(thread);
        }));
    }

    /**
     * <p>
     * Processes a created or updated message by extracting the message, checking if it already exists,
     * and either creating or updating the email and thread accordingly.
     * </p>
     *
     * @param emailId            Represents the sync setting id.
     * @param graphServiceClient The Graph API client.
     * @param item               The event notification.
     * @param syncAddressInfo    The address info associated with the event.
     * @param accessToken        The Graph API access token.
     * @param modifiedThreads    The collection of threads that may be updated.
     * @param existingThreads    Represents the existing threads.
     * @param changeType         Represents the {@link ChangeType}
     * @return The newly created or updated {@link Email}, or null if already exists and updated.
     */
    private Optional<Email> processMessage(final Long emailId, final GraphServiceClient<Request> graphServiceClient,
                                           final OutlookEventNotification item, final SyncAddressInfo syncAddressInfo,
                                           final String accessToken, final Collection<Thread> modifiedThreads,
                                           final Collection<Thread> existingThreads, final ChangeType changeType) {
        final String internalMessageId = item.getMessageId();
        final Email email = extractEmail(internalMessageId, item.getEncryptedContent(), syncAddressInfo, accessToken);
        final EmailMetaData metaData = email.getMetaData();
        final Optional<String> optionalFolderName = getFolder(graphServiceClient, metaData.getParentFolderId());
        final Collection<Thread> threads = new ArrayList<>(existingThreads);

        if (optionalFolderName.isEmpty()) {
            return Optional.empty();
        }
        final String folderName = optionalFolderName.get();
        final Collection<String> supportedFolders = List.of(
                OutlookHeaderFields.INBOX, OutlookHeaderFields.SENT_ITEMS,
                OutlookHeaderFields.ARCHIVE, OutlookHeaderFields.DRAFTS
        );

        if (threads.isEmpty()) {
            final Message existingMessage = getMessageBasedOnFolder(folderName, internalMessageId, graphServiceClient);
            final Optional<String> optionalMessageId = Optional.ofNullable(existingMessage)
                    .map(message -> message.internetMessageId);

            threads.addAll(getExistingThreads(emailId, optionalMessageId));
        }

        if (threads.isEmpty()) {
            if (supportedFolders.contains(folderName)) {
                return buildNewEmail(graphServiceClient, item, syncAddressInfo, accessToken, email);
            } else {
                return Optional.empty();
            }
        }

        if ((changeType.isCreated() && !threads.isEmpty()) ||
                OutlookHeaderFields.DRAFTS.equals(folderName) || isDraft(threads.stream().findFirst().get())) {
            return buildNewEmail(graphServiceClient, item, syncAddressInfo, accessToken, email);
        } else {
            updateExistingEmails(threads, email, internalMessageId, folderName, modifiedThreads);
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Checks if the given {@link Thread} contains a composed {@link Email} marked as a draft.
     * </p>
     *
     * @param thread The {@link Thread} to check.
     * @return {@code true} if the thread's composed email is a draft; {@code false} otherwise.
     */
    private boolean isDraft(final Thread thread) {
        return thread.getComposeEmail()
                .map(email -> email.getMessageCategory() == Email.MessageCategory.DRAFT)
                .orElseGet(() -> false);
    }

    /**
     * <p>
     * Creates a new {@link Email} by retrieving the full message and its attachments from the Graph API.
     * </p>
     *
     * @param graphServiceClient The Graph API client.
     * @param item               The event notification.
     * @param syncAddressInfo    The address info.
     * @param accessToken        The Graph API access token.
     * @param email              Represents the email.
     * @return a fully constructed {@link Email}
     */
    private Optional<Email> buildNewEmail(final GraphServiceClient<Request> graphServiceClient,
                                          final OutlookEventNotification item,
                                          final SyncAddressInfo syncAddressInfo,
                                          final String accessToken,
                                          final Email email) {
        final EmailMetaData metaData = email.getMetaData();
        final Message message = graphEmailBuilder.getMessage(graphServiceClient, item);

        if (Objects.isNull(message)) {
            return Optional.empty();
        }
        final EmailGraphMessage emailGraphMessage = emailGraphMessageConverter.convert(message);

        if (Objects.isNull(emailGraphMessage.getFrom())) {
            final Email.MessageCategory messageCategory = email.getMessageCategory();
            final boolean isCategoryNonNull = Objects.nonNull(messageCategory);

            if (isCategoryNonNull && messageCategory.isSendCategory()) {
                emailGraphMessage.setFrom(syncAddressInfo);
            } else {
                final String fromAddress = graphEmailBuilder.getFromAddressFromHeaders(graphServiceClient, item.getMessageId());

                if (Objects.nonNull(fromAddress)) {
                    emailGraphMessage.setFrom(new SyncAddressInfo(fromAddress));
                } else {
                    LOGGER.error("Email Outlook: Failed to retrieve the sender's email address from the message headers for message ID: {}, message category: {}",
                            metaData.getMessageId(), isCategoryNonNull ? messageCategory.name() : "unknown");
                    throw ErrorCode.get(EmailErrorCodes.INVALID_FROM_ADDRESS);
                }
            }
        }
        final HtmlDocument htmlDocument = htmlParser.parse(emailGraphMessage.getBody());

        final Map<String, Collection<EmailAttachment>> attachmentGroups =
                emailGraphMessageConverter.convertAttachments(getAllAttachments(graphServiceClient, item.getUserId(), item.getMessageId()));
        final Collection<EmailAttachment> inlineAttachments = attachmentGroups.get("inline");

        if (Objects.nonNull(inlineAttachments) && !inlineAttachments.isEmpty()) {
            handleInlineAttachments(htmlDocument, inlineAttachments);
        }
        final Collection<EmailAttachment> regularAttachments = attachmentGroups.get("regular");

        if (Objects.nonNull(regularAttachments) && !regularAttachments.isEmpty()) {
            emailGraphMessage.setEmailAttachments(regularAttachments);
        }
        emailGraphMessage.setBody(htmlDocument.html());

        final Email newEmail = graphEmailBuilder.buildEmail(emailGraphMessage, accessToken, syncAddressInfo);

        newEmail.getMetaData().setParentFolderId(metaData.getParentFolderId());

        return Optional.of(newEmail);
    }

    /**
     * <p>
     * Fetches all attachments (including inline and regular) for a given message
     * from Microsoft Graph by handling pagination.
     * </p>
     *
     * @param graphServiceClient the authenticated Microsoft Graph client
     * @param userId             the Outlook user ID
     * @param messageId          the ID of the email message
     * @return a collection of all {@link Attachment} objects for the message
     */
    private Collection<Attachment> getAllAttachments(GraphServiceClient<Request> graphServiceClient,
                                                     String userId, String messageId) {
        final Collection<Attachment> allAttachments = new ArrayList<>();

        AttachmentCollectionPage currentPage = graphServiceClient
                .me()
                .messages(messageId)
                .attachments()
                .buildRequest()
                .get();

        while (Objects.nonNull(currentPage)) {
            allAttachments.addAll(currentPage.getCurrentPage());
            final AttachmentCollectionRequestBuilder nextPage = currentPage.getNextPage();

            if (Objects.nonNull(nextPage)) {
                currentPage = nextPage.buildRequest().get();
            } else {
                currentPage = null;
            }
        }

        return allAttachments;
    }

    /**
     * <p>
     * Replaces inline image `cid:` references in the email's HTML body
     * with base64-encoded data URLs, so the images render properly
     * without needing external resources.
     * </p>
     *
     * @param htmlDocument      the parsed HTML document of the email body
     * @param inlineAttachments the collection of inline attachments to embed
     */
    private void handleInlineAttachments(final HtmlDocument htmlDocument, final Collection<EmailAttachment> inlineAttachments) {
        final HtmlElements htmlElements = htmlDocument.select("img[src^=cid:]");
        final Map<String, List<HtmlElement>> groupedElements = htmlElements.getElements().stream()
                .collect(Collectors.groupingBy(
                        element -> extractContentIdFromSrc(element.attribute("src")),
                        Collectors.toList()
                ));

        for (final EmailAttachment inline : inlineAttachments) {
            final String contentId = stripBracketsFromContentId(inline.getInternalId());
            final List<HtmlElement> elements = groupedElements.get(contentId);

            if (Objects.nonNull(elements)) {
                for (final HtmlElement element : elements) {

                    try (final InputStream is = inline.getInputStream()) {
                        final byte[] bytes = is.readAllBytes();
                        final String base64 = Base64.getEncoder().encodeToString(bytes);
                        final String newSrc = "data:" + inline.getMediaType() + ";base64," + base64;

                        element.attribute("src", newSrc);
                    } catch (IOException e) {
                        LOGGER.error("Failed to process inline image: {}", inline.getFileName(), e);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Extracts the content ID from a "cid:" reference in an image source attribute.
     * </p>
     *
     * @param src the value of the HTML img "src" attribute
     * @return the extracted content ID, or null if not a cid reference
     */
    private String extractContentIdFromSrc(String src) {
        if (Objects.nonNull(src) && src.startsWith("cid:")) {
            return src.substring(4);
        }
        return null;
    }

    /**
     * <p>
     * Removes surrounding angle brackets from a content ID string.
     * </p>
     *
     * @param contentId the content ID possibly containing angle brackets
     * @return the cleaned content ID without angle brackets
     */
    private String stripBracketsFromContentId(String contentId) {
        if (Objects.isNull(contentId)) {
            return null;
        }
        return contentId.replace("<", "").replace(">", "");
    }

    /**
     * <p>
     * Updates existing emails within threads if any properties (e.g., read, archived) have changed.
     * </p>
     *
     * @param existingThreads   The threads containing existing emails.
     * @param email             The new email containing updated information.
     * @param internalMessageId The internal ID of the message.
     * @param folderName        The folder name (INBOX, ARCHIVE, etc.).
     * @param modifiedThreads   The collection to record any threads that were modified.
     */
    private void updateExistingEmails(final Collection<Thread> existingThreads, final Email email,
                                      final String internalMessageId, final String folderName,
                                      final Collection<Thread> modifiedThreads) {
        loadLabels(folderName, email);
        final EmailMetaData emailMetaData = email.getMetaData();

        for (final Thread thread : existingThreads) {
            final Optional<Email> composeEmail = thread.getComposeEmail();

            if (composeEmail.isPresent()) {
                final Email existingEmail = composeEmail.get();
                final EmailMetaData existingMetaData = existingEmail.getMetaData();

                updateIfModified(existingEmail::isRead, existingEmail::setRead, email.isRead());
                updateIfModified(existingEmail::isArchived, existingEmail::setArchived, email.isArchived());
                updateIfModified(existingEmail::isActive, existingEmail::isActive, email.isActive());
                updateIfModified(existingMetaData::getExternalReference, existingMetaData::setExternalReference, internalMessageId);
                updateIfModified(existingMetaData::getParentFolderId, existingMetaData::setParentFolderId, emailMetaData.getParentFolderId());

                modifiedThreads.add(thread);
            }
        }
    }

    /**
     * <p>
     * Helper method to update a property if the new value is different from the current value.
     * </p>
     *
     * @param getter   The current value getter.
     * @param setter   The setter to apply if value changes.
     * @param newValue The new value to apply.
     */
    private <T> void updateIfModified(final Supplier<T> getter, final Consumer<T> setter, final T newValue) {
        if (Objects.equals(getter.get(), newValue)) {
            return;
        }
        setter.accept(newValue);
    }

    /**
     * <p>
     * Retrieves existing threads using the given optional internet message ID.
     * </p>
     *
     * @param emailId           Represents the sync setting id.
     * @param optionalMessageId The optional message ID (usually internetMessageId).
     * @return collection of threads matching the message ID.
     */
    private Collection<Thread> getExistingThreads(final Long emailId, final Optional<String> optionalMessageId) {
        return optionalMessageId
                .map(messageId -> threadManager.getThreadsByEmailId(emailId, true, List.of(messageId)))
                .orElseGet(Collections::emptyList);
    }

    /**
     * <p>
     * Resolves the folder display name using the parent folder ID from Graph.
     * </p>
     *
     * @param graphServiceClient The Graph API client.
     * @param parentFolderId     The folder ID.
     * @return {@link Optional} folder name.
     */
    private Optional<String> getFolder(final GraphServiceClient<Request> graphServiceClient, final String parentFolderId) {
        return Optional.ofNullable(graphServiceClient.me().mailFolders(parentFolderId).buildRequest().get()).map(mailFolder -> mailFolder.displayName);
    }

    /**
     * <p>
     * Extracts the email from the encrypted content.
     * </p>
     *
     * @param messageId        Represents the message ID.
     * @param encryptedContent Represents the {@link EncryptedContent}.
     * @param syncAddressInfo  Represents the sync address info.
     * @param accessToken      Represents the access token.
     * @return {@link Email} The extracted email.
     */
    private Email extractEmail(final String messageId, final EncryptedContent encryptedContent,
                               final SyncAddressInfo syncAddressInfo, final String accessToken) {
        try {
            final String decryptedPayload = decrypt.decrypt(encryptedContent);
            final Email email = convertToEmail(decryptedPayload, syncAddressInfo, accessToken);

            email.getMetaData().setExternalReference(messageId);
            email.isActive(true);

            return email;
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Webhook Receiver: Failed to decrypt content, message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailWebhookErrorCodes.FAILED_TO_DECRYPT_CONTENT, throwable);
        }
    }

    /**
     * <p>
     * Converts the decrypted payload to {@link Email}.
     * </p>
     *
     * @param decryptedPayload Represents the decrypted payload.
     * @param syncAddressInfo  Represents the sync address info.
     * @param accessToken      Represents the access token.
     * @return {@link Email} The converted email.
     */
    private Email convertToEmail(final String decryptedPayload, final SyncAddressInfo syncAddressInfo, final String accessToken) {
        final EmailGraphMessage emailGraphMessage = emailGraphMessageConverter.convert(Json.object(decryptedPayload));

        return graphEmailBuilder.buildEmail(emailGraphMessage, accessToken, syncAddressInfo);
    }

    /**
     * <p>
     * Updates email properties such as archived/active based on folder name.
     * </p>
     *
     * @param folderName The folder the email is currently in.
     * @param email      The email object to update.
     */
    private void loadLabels(final String folderName, final Email email) {
        switch (folderName) {
            case OutlookHeaderFields.INBOX, OutlookHeaderFields.SENT_ITEMS -> {
                email.isActive(true);
                email.setArchived(false);
                email.setDeleted(false);
            }
            case OutlookHeaderFields.ARCHIVE -> {
                email.setArchived(true);
            }
            case OutlookHeaderFields.DELETED_ITEMS -> {
                email.isActive(false);
            }
        }
    }

    /**
     * <p>
     * Fetches a message using Graph API based on its folder and ID.
     * </p>
     *
     * @param folderName         Name of the folder where the message resides
     * @param messageId          The message ID
     * @param graphServiceClient The Graph API client
     * @return {@link Message} from Graph API, or null if not found
     */
    private Message getMessageBasedOnFolder(final String folderName, final String messageId,
                                            final GraphServiceClient<Request> graphServiceClient) {
        final MessageRequestBuilder requestBuilder;

        if (Objects.equals(OutlookHeaderFields.DELETED_ITEMS, folderName)) {
            requestBuilder = graphServiceClient
                    .me()
                    .mailFolders(EmailHeaderFields.DELETED_ITEMS)
                    .messages(messageId);
        } else {
            requestBuilder = graphServiceClient
                    .me()
                    .messages(messageId);
        }

        return requestBuilder
                .buildRequest()
                .select(EmailHeaderFields.INTERNET_MESSAGE_ID)
                .get();
    }
}