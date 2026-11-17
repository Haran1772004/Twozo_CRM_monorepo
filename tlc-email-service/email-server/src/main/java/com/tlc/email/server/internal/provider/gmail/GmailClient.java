package com.tlc.email.server.internal.provider.gmail;

import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.HistoryLabelAdded;
import com.google.api.services.gmail.model.HistoryLabelRemoved;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.HistoryMessageDeleted;
import com.google.api.services.gmail.model.ListDraftsResponse;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailAttachment;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.server.internal.field.EmailHeaderFields;
import com.tlc.email.server.internal.field.GmailHeaderFields;
import com.tlc.email.server.internal.provider.MailBoxClient;
import com.tlc.email.server.internal.provider.gmail.converter.GmailDraftMessageConverter;
import com.tlc.email.server.internal.provider.gmail.converter.GmailMessageConverter;
import com.tlc.email.server.internal.provider.gmail.converter.GmailRawMessageConverter;
import com.tlc.email.server.internal.provider.gmail.model.GmailFormat;
import com.tlc.email.server.internal.provider.gmail.model.History;
import com.tlc.email.server.internal.provider.gmail.model.Label;
import com.tlc.email.server.internal.provider.gmail.model.Label.Name;
import com.tlc.email.server.internal.provider.gmail.model.ListRequest;
import com.tlc.email.server.internal.provider.gmail.model.ListResponse;
import com.tlc.email.server.internal.provider.gmail.model.MessageLabelInfo;
import com.tlc.email.server.internal.provider.gmail.model.MessageModificationRequest;
import com.tlc.email.server.internal.provider.gmail.model.WatchRequest;
import com.tlc.email.server.internal.provider.gmail.model.WatchResponse;
import com.tlc.email.server.internal.provider.gmail.service.GmailMessageContentExtractor;
import com.tlc.email.server.internal.provider.gmail.service.GmailService;
import com.google.api.services.gmail.Gmail.Users;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * A comprehensive Gmail API client wrapper that provides high-level operations
 * for email management including sending, drafts, labels, and batch operations.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class GmailClient implements MailBoxClient {

    public static final Logger LOGGER = LoggerFactory.getLogger(GmailClient.class);

    private static final String RFC_822_MSG_ID = "rfc822msgid:";
    private static final String CONTENT_ID = "Content-ID";
    private static final String CID_URL_PREFIX = "cid:";
    private static final String DATA_URL_PREFIX = "data:";
    private static final String BASE64_DELIMITER = ";base64,";
    private static final String IMAGE_MIME_PREFIX = "image/";
    private static final String ANGLE_BRACKETS_REGEX = "^<|>$";
    private static final String HYPHEN_SYMBOL = "-";
    private static final String UNDERSCORE_SYMBOL = "_";
    private static final String PLUS_SYMBOL = "+";
    private static final String SLASH_SYMBOL = "/";
    private static final String EMPTY_STRING = "";

    private final Gmail gmailClient;
    private final GmailMessageConverter emailConverter;
    private final GmailDraftMessageConverter gmailDraftMessageConverter;
    private final GmailRawMessageConverter gmailRawMessageConverter;
    private final GmailMessageContentExtractor gmailMessageContentExtractor;

    private GmailClient(final Gmail gmail) {
        this.gmailClient = gmail;
        this.emailConverter = GmailMessageConverter.getInstance();
        this.gmailDraftMessageConverter = GmailDraftMessageConverter.getInstance();
        this.gmailRawMessageConverter = GmailRawMessageConverter.getInstance();
        this.gmailMessageContentExtractor = GmailMessageContentExtractor.getInstance();
    }

    /**
     * <p>
     * Sends an email message immediately via Gmail API.
     * </p>
     *
     * @param userId The Gmail user ID.
     * @param email  The Email object containing all message data to send.
     */
    public void send(final String userId, final Email email) {
        final Message message = getMessage(userId, email);

        try {
            retrieveAndLoadMessage(
                    userId,
                    gmailClient.users().messages().send(userId, message).execute(),
                    email
            );
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Send action failed, message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_SEND_FAILED);
        }
    }

    /**
     * <p>
     * Modifies message properties such as labels, read status, or importance.
     * </p>
     *
     * @param userId                     The Gmail user ID.
     * @param messageModificationRequest The request containing message ID and modifications to apply.
     */
    public void modify(final String userId, final MessageModificationRequest messageModificationRequest) {
        try {
            gmailClient.users().messages().modify(
                    userId,
                    messageModificationRequest.getMessageId(),
                    getModifiedRequest(messageModificationRequest)
            ).execute();
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Modify action failed, message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.MODIFY_ACTION_FAILED, throwable);
        }
    }

    /**
     * <p>
     * Converts the internal MessageModificationRequest model to the Gmail API MessageModificationRequest.
     * </p>
     *
     * @param messageModificationRequest the internal MessageModificationRequest instance
     * @return the Gmail API MessageModificationRequest with labels to add and remove set
     */
    private ModifyMessageRequest getModifiedRequest(final MessageModificationRequest messageModificationRequest) {
        return new ModifyMessageRequest()
                .setAddLabelIds(messageModificationRequest.getAddLabelIds())
                .setRemoveLabelIds(messageModificationRequest.getRemoveLabelIds());
    }

    /**
     * <p>
     * Permanently deletes a Gmail message.
     * This operation cannot be undone - the message is completely removed.
     * </p>
     *
     * @param userId  The Gmail user ID.
     * @param draftId The unique identifier of the message to delete.
     */
    public void deleteDraft(final String userId, final String draftId) {
        try {
            gmailClient.users().drafts().delete(userId, draftId).execute();
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Draft ,message delete action failed, message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_DELETE_FAILED, throwable);
        }
    }

    /**
     * <p>
     * Permanently deletes a Gmail message.
     * </p>
     *
     * @param userId    the Gmail user ID.
     * @param messageId the unique identifier of the message to delete
     * @throws RuntimeException if the delete operation fails
     */
    public void deleteMessage(final String userId, final String messageId) {
        try {
            gmailClient.users().messages().delete(userId, messageId).execute();
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Message delete action failed, message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_DELETE_FAILED, throwable);
        }
    }

    /**
     * <p>
     * Moves a message to the moveToTrash (soft delete).
     * Unlike delete, trashed messages can be recovered and are automatically
     * deleted after 30 days in Gmail.
     * </p>
     *
     * @param userId    The Gmail user ID.
     * @param messageId The unique identifier of the message to moveToTrash
     */
    public void moveToTrash(final String userId, final String messageId) {
        try {
            gmailClient.users().messages().trash(userId, messageId).execute();
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Trash action failed, message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_TRASH_FAILED, throwable);
        }
    }

    /**
     * <p>
     * Creates a new draft message in Gmail.
     * </p>
     *
     * @param userId The Gmail user ID.
     * @param email  The Email object to create as a draft.
     */
    public void createDraft(final String userId, final Email email) {
        final Draft draft = getDraftMessage(userId, email);

        try {
            retrieveAndLoadDraftMessage(
                    userId,
                    gmailClient.users().drafts().create(userId, draft).execute(),
                    email
            );
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Draft create action failed, message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.DRAFT_CREATE_FAILED, throwable);
        }
    }

    /**
     * <p>
     * Updates an existing draft message with new content.
     * </p>
     *
     * @param userId The Gmail user ID.
     * @param email  The Email object containing updated draft content and metadata with draft ID.
     */
    public void updateDraft(final String userId, final Email email) {
        try {
            final EmailMetaData emailMetaData = email.getMetaData();
            final Draft draft = getDraftMessage(userId, email);

            retrieveAndLoadDraftMessage(
                    userId,
                    gmailClient.users().drafts().update(userId, emailMetaData.getInternalDraftId(), draft).execute(),
                    email
            );
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Draft update action failed, message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.DRAFT_UPDATE_FAILED, throwable);
        }
    }

    /**
     * <p>
     * Sends a draft message.
     * </p>
     *
     * @param userId The Gmail user ID.
     * @param email  The Email object representing the draft to send.
     */
    public void sendDraft(final String userId, final Email email) {
        final Draft draft = getDraftMessage(userId, email);

        try {
            retrieveAndLoadMessage(
                    userId,
                    gmailClient.users().drafts().send(userId, draft).execute(),
                    email
            );
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Draft send action failed, message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.DRAFT_SEND_FAILED, throwable);
        }
    }

    /**
     * <p>
     * Checks whether a given draft id exists for the user.
     * </p>
     *
     * @param userId  The Gmail user ID.
     * @param draftId The draft id to validate.
     * @return {@code true} if the draft exists, {@code false} otherwise.
     */
    public boolean isValid(final String userId, final String draftId) {
        if (Objects.isNull(draftId)) {
            return false;
        }

        try {
            gmailClient.users().drafts().get(userId, draftId).execute();

            return true;
        } catch (Throwable throwable) {
            LOGGER.debug("Gmail: Draft existence check failed for id {}: {}", draftId, throwable.getMessage());
            return false;
        }
    }

    /**
     * <p>
     * Retrieves a paginated list of email messages or drafts from Gmail based on the provided request parameters.
     * </p>
     *
     * @param listRequest The request object containing parameters such as user ID, query, label, pagination, and folder type.
     * @return A {@link ListResponse} containing the list of emails or drafts and the next page token for pagination.
     */
    public ListResponse getListResponse(final ListRequest listRequest) {
        final String userId = listRequest.getUserId();
        final String query = listRequest.getQuery();
        final String labelId = listRequest.getLabelId();
        final Long max = listRequest.getMaxResults();
        final String pageToken = listRequest.getPageToken();
        final Collection<Email> messages;
        final String nextPageToken;
        final Users users = gmailClient.users();

        try {
            if (FolderType.DRAFT == listRequest.getFolderType()) {
                final ListDraftsResponse draftsResponse = users.drafts()
                        .list(userId)
                        .setQ(query)
                        .setMaxResults(max)
                        .setPageToken(pageToken)
                        .execute();

                messages = getBatchDraftMessage(userId, draftsResponse.getDrafts());
                nextPageToken = draftsResponse.getNextPageToken();
            } else {
                final ListMessagesResponse messagesResponse = users.messages()
                        .list(userId)
                        .setQ(query)
                        .setLabelIds(Collections.singletonList(labelId))
                        .setMaxResults(max)
                        .setPageToken(pageToken)
                        .execute();

                messages = getBatchMessages(userId, messagesResponse.getMessages());
                nextPageToken = messagesResponse.getNextPageToken();
            }
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Failed to fetch list response, message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_RETRIEVE_FAILED);
        }

        return ListResponse.builder()
                .nextPageToken(nextPageToken)
                .messages(messages)
                .build();
    }

    /**
     * <p>
     * Retrieves all labels for the authenticated user.
     * </p>
     *
     * @param userId     The Gmail user ID (typically "me" for authenticated user).
     * @param labelNames Refers the label name.
     * @return Collection of all labels available to the user
     */
    public List<Label> getLabels(final String userId, final Collection<Name> labelNames) {
        try {
            return getLabels(gmailClient.users().labels().list(userId).execute().getLabels(), labelNames);
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Label retrieve action failed, message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_RETRIEVE_FAILED, throwable);
        }
    }

    /**
     * <p>
     * Retrieves the messages by using the rfc id.
     * </p>
     *
     * @param userId       Refers the user id.
     * @param rfcMessageId Refers the exact message id.
     */
    public Optional<Message> getMessageByRfcMessageId(final String userId, final String rfcMessageId) {
        if (Objects.isNull(rfcMessageId)) {
            return Optional.empty();
        }

        try {
            final ListMessagesResponse response = gmailClient.users().messages()
                    .list(userId)
                    .setQ(RFC_822_MSG_ID + rfcMessageId)
                    .setIncludeSpamTrash(true)
                    .execute();
            final List<Message> messages = response.getMessages();

            if (Objects.nonNull(messages) && !messages.isEmpty()) {
                return Optional.ofNullable(fetchMessageById(userId, messages.get(0).getId()));
            }
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Failed to retrieve message by RFC Message-ID: {}, message: {}, trace: {}",
                    rfcMessageId, throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_RETRIEVE_FAILED, throwable);
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Retrieves Gmail messages in the specified format and converts them into {@link Email} objects.
     * </p>
     *
     * @param userId      The Gmail user ID
     * @param ids         Collection of Gmail message IDs to retrieve
     * @param gmailFormat The desired {@link GmailFormat} (e.g., FULL, META_DATA) for message retrieval
     * @return Collection of {@link Email} objects constructed from the retrieved messages
     */
    public Collection<Email> getMails(final String userId, final Collection<String> ids, final GmailFormat gmailFormat) {
        return buildEmail(userId, retrieveBatchMessages(userId, ids, gmailFormat, Collections.emptyList()).values());
    }

    /**
     * <p>
     * Retrieves Gmail draft messages and converts them into {@link Email} objects.
     * </p>
     *
     * @param userId The Gmail user ID
     * @param ids    Collection of internal Gmail message IDs used to retrieve associated draft messages
     * @return Collection of {@link Email} objects constructed from the retrieved draft messages
     */
    public Collection<Email> getDraftMails(final String userId, final Collection<String> ids) {
        return buildDraftEmail(userId, retrieveBatchDraftMessage(userId, getDraftIdsById(userId, ids).values()).values());
    }

    /**
     * <p>
     * Executes a batch request to fetch full Gmail messages for the given message IDs.
     * </p>
     *
     * @param userId         The Gmail user ID.
     * @param ids            Collection of Gmail message IDs to be retrieved.
     * @param gmailFormat    Represent the gmail format.
     * @param selectedFields Represents which fields are to be selected.
     * @return A map of message ID to fully populated {@link Message} objects.
     */
    public Map<String, Message> retrieveBatchMessages(final String userId, final Collection<String> ids,
                                                      final GmailFormat gmailFormat, final Collection<String> selectedFields) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            final Map<String, Message> batchResults = new ConcurrentHashMap<>();
            final BatchRequest batchRequest = gmailClient.batch();

            for (final String id : ids) {
                final Users.Messages.Get request = gmailClient.users().messages()
                        .get(userId, id)
                        .setFormat(gmailFormat.getName());

                if (Objects.nonNull(selectedFields) && !selectedFields.isEmpty()) {
                    request.setFields(String.join(",", selectedFields));
                }

                request.queue(batchRequest, createMessageCallback(batchResults, id));
            }
            batchRequest.execute();

            return batchResults;
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Batch message retrieve action failed, message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_BATCH_RETRIEVE_FAILED, throwable);
        }
    }

    /**
     * <p>
     * Retrieves the label information associated with a specific Gmail message.
     * </p>
     *
     * @param userId    The Gmail user ID
     * @param messageId The ID of the Gmail message to fetch label information for
     * @return {@link MessageLabelInfo} object containing the message ID and its supported labels
     */
    public MessageLabelInfo getMessageLabelData(final String userId, final String messageId) {
        final Optional<Message> optionalMessage = retrieveBatchMessages(userId, List.of(messageId), GmailFormat.META_DATA, Collections.emptyList())
                .values().stream().findFirst();
        final Collection<String> supportedLabels = Arrays.stream(Name.values())
                .map(Name::getValue)
                .collect(Collectors.toSet());
        final MessageLabelInfo.Builder messageLabelInfoBuilder = MessageLabelInfo.builder();

        optionalMessage.ifPresent(message -> {
            final Collection<String> labels = message.getLabelIds();
            final Collection<Name> labelCollection = labels.stream()
                    .filter(supportedLabels::contains)
                    .map(Name::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            messageLabelInfoBuilder
                    .messageId(messageId)
                    .labelList(labelCollection);
        });

        return messageLabelInfoBuilder.build();
    }

    /**
     * <p>
     * Creates a watch request for the user's Gmail account to receive notifications on the configured Pub/Sub topic.
     * </p>
     *
     * @param userId       The Gmail user ID for whom the watch request is created.
     * @param watchRequest Contains the Pub/Sub topic and label filters.
     * @return A {@link WatchResponse} containing the expiration time and history ID.
     */
    public WatchResponse watch(final String userId, final WatchRequest watchRequest) {
        final com.google.api.services.gmail.model.WatchRequest request = new com.google.api.services.gmail.model.WatchRequest()
                .setTopicName(watchRequest.getPubSubTopic())
                .setLabelFilterBehavior(watchRequest.getLabelFilterBehavior())
                .setLabelIds(watchRequest.getLabelIds());

        try {
            final com.google.api.services.gmail.model.WatchResponse watchResponse = this.gmailClient.users()
                    .watch(userId, request)
                    .execute();

            return WatchResponse.builder()
                    .expiration(watchResponse.getExpiration())
                    .historyId(watchResponse.getHistoryId())
                    .build();
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Watch request action create failed, message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.WATCH_REQUEST_CREATE_FAILED, throwable);
        }
    }

    /**
     * <p>
     * Stops Gmail watch notifications for the given user.
     * </p>
     *
     * @param userId the Gmail user ID for whom to stop the watch notifications
     */
    public void stopWatch(final String userId) {
        try {
            gmailClient.users().stop(userId).execute();
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Watch request stop failed : message: {}, trace: {}", throwable.getMessage(), throwable);
        }
    }

    /**
     * <p>
     * Retrieves the Gmail history from a specified starting history ID for a given user.
     * </p>
     *
     * @param historyId The starting history ID
     * @param userId    The Gmail user ID
     * @return list of {@link History} objects based on Gmail history
     */
    public Collection<History> getMessageHistories(final BigInteger historyId, final String userId) {
        final List<com.google.api.services.gmail.model.History> allHistory = new ArrayList<>();
        String pageToken = null;

        try {
            do {
                final ListHistoryResponse response = gmailClient.users().history()
                        .list(userId)
                        .setStartHistoryId(historyId)
                        .setHistoryTypes(History.Type.getValues())
                        .setPageToken(pageToken)
                        .setMaxResults(GmailService.getInstance().getHistoryBatchSize())
                        .execute();
                final List<com.google.api.services.gmail.model.History> historyList = response.getHistory();

                if (Objects.nonNull(historyList) && !historyList.isEmpty()) {
                    allHistory.addAll(historyList);
                }

                pageToken = response.getNextPageToken();
            } while (Objects.nonNull(pageToken));
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: History fetch action failed : message: {}, trace: {}", throwable.getMessage(), throwable);
        }

        return buildHistory(allHistory);
    }

    /**
     * <p>
     * Loads inline attachments for the given email by fetching the full message from Gmail using the RFC Message-ID.
     * </p>
     *
     * @param userId The Gmail user ID.
     * @param email  The Email object to load inline attachments into
     */
    public void loadInlineAttachments(final String userId, final Email email) {
        final String messageId = email.getMetaData().getMessageId();

        if (Objects.isNull(messageId)) {
            return;
        }

        try {
            final Optional<Message> message = getFullMessageByRfcMessageId(userId, messageId);

            message.ifPresent(messageValue -> loadAttachment(userId, email, messageValue));
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Inline Attachment load action failed : message: {}, trace: {}", throwable.getMessage(), throwable);
        }
    }

    /**
     * <p>
     * Retrieves the full Gmail message using the RFC Message-ID and returns it as an Optional.
     * </p>
     *
     * @param userId       The Gmail user ID.
     * @param rfcMessageId The RFC Message-ID to search for.
     * @return An Optional containing the full Gmail Message if found, or empty if not found or if the RFC Message-ID is null.
     */
    public Optional<Message> getFullMessageByRfcMessageId(final String userId, final String rfcMessageId) {
        if (Objects.isNull(rfcMessageId)) {
            return Optional.empty();
        }

        try {
            final ListMessagesResponse response = gmailClient.users().messages()
                    .list(userId)
                    .setQ(RFC_822_MSG_ID + rfcMessageId)
                    .setIncludeSpamTrash(true)
                    .execute();
            final List<Message> messages = response.getMessages();

            if (Objects.nonNull(messages) && !messages.isEmpty()) {
                return Optional.ofNullable(fetchFullMessageById(userId, messages.get(0).getId()));
            }
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Failed to retrieve message by RFC Message-ID: {}, message: {}, trace: {}",
                    rfcMessageId, throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_RETRIEVE_FAILED, throwable);
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Filters and converts Gmail API labels into application-specific label representations.
     * </p>
     *
     * @param labels         the Gmail API label objects to convert
     * @param nameCollection the collection of label names to match
     * @return a collection of matched application-specific labels
     */
    private List<Label> getLabels(final Collection<com.google.api.services.gmail.model.Label> labels,
                                  final Collection<Label.Name> nameCollection) {
        if (Objects.isNull(labels) || Objects.isNull(nameCollection) || nameCollection.isEmpty()) {
            return Collections.emptyList();
        }
        final Collection<String> expectedNames = nameCollection.stream()
                .map(name -> name.name().toLowerCase())
                .collect(Collectors.toSet());

        return labels.stream()
                .filter(gmailLabel -> expectedNames.contains(gmailLabel.getName().toLowerCase()))
                .map(gmailLabel -> Label.builder()
                        .id(gmailLabel.getId())
                        .name(Label.Name.valueOf(gmailLabel.getName()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * <p>
     * Constructs a Message object from an Email.
     * </p>
     *
     * @param userId The user ID.
     * @param email  The email data.
     * @return a Message populated with the fetched Gmail message ID and thread ID (if reply),and raw content converted from the email.
     */
    private Message getMessage(final String userId, final Email email) {
        final Message newMessage = new Message();
        final Optional<Message> optionalMessage = getMessageByRfcMessageId(userId, email.getMetaData().getMessageId());
        final EmailMetaData emailMetaData = email.getMetaData();

        optionalMessage.ifPresent(message -> {
            newMessage.setId(message.getId());

            if (Objects.nonNull(emailMetaData.getInReplyTo())) {
                newMessage.setThreadId(message.getThreadId());
            }
        });

        return newMessage.setRaw(gmailRawMessageConverter.convert(email));
    }

    /**
     * <p>
     * Creates a Draft object from an Email, linking it with the corresponding Gmail message.
     * </p>
     *
     * @param userId The Gmail user ID.
     * @param email  The email data.
     * @return a Draft populated with the draft ID and the message converted from the email.
     */
    private Draft getDraftMessage(final String userId, final Email email) {
        final Draft draft = new Draft();

        draft.setId(email.getMetaData().getInternalDraftId());
        draft.setMessage(getMessage(userId, email));

        return draft;
    }

    /**
     * <p>
     * Retrieves a freshly updated Gmail message (by ID) and loads its metadata into the given {@link Email}.
     * </p>
     *
     * @param userId  Gmail user ID.
     * @param message The Gmail API {@link Message} whose ID points to the full server copy.
     * @param email   The internal {@link Email} with server-side metadata.
     */
    private void retrieveAndLoadMessage(final String userId, final Message message, final Email email) {
        final String id = message.getId();
        final Message retrievedMessage = getMessage(userId, id);
        final Map<String, String> headers = gmailMessageContentExtractor.extractHeaders(retrievedMessage);
        final EmailMetaData emailMetaData = email.getMetaData();

        emailMetaData.setDate(message.getInternalDate());
        emailMetaData.setMessageId(headers.getOrDefault(GmailHeaderFields.SENT_MESSAGE_ID,
                headers.getOrDefault(GmailHeaderFields.DRAFT_MESSAGE_ID, headers.getOrDefault(GmailHeaderFields.MESSAGE_ID, null)
                )
        ));
        emailMetaData.setConversationThreadId(message.getThreadId());
        emailMetaData.setInternalMessageId(id);
        emailMetaData.setInternalDraftId(null);
    }

    /**
     * <p>
     * Retrieves a Gmail draft (including its underlying message) and loads its metadata into the given {@link Email}.
     * </p>
     *
     * @param userId Gmail user ID (<code>"me"</code> for the authenticated user)
     * @param draft  The Gmail API {@link Draft} object returned by the Drafts API
     * @param email  The internal {@link Email} to hydrate with draft metadata
     */
    private void retrieveAndLoadDraftMessage(final String userId, final Draft draft, final Email email) {
        retrieveAndLoadMessage(userId, draft.getMessage(), email);

        email.getMetaData().setInternalDraftId(draft.getId());
    }

    /**
     * <p>
     * Retrieves multiple messages in a single batch request for optimal performance.
     * </p>
     *
     * @param userId   Refers the user id.
     * @param messages Collection of Message objects.
     * @return Collection of Email objects with full details, or empty list if no messages.
     */
    private Collection<Email> getBatchMessages(final String userId, final Collection<Message> messages) {
        return Objects.isNull(messages) || messages.isEmpty() ? Collections.emptyList() :
                getMails(userId, messages.stream().map(Message::getId).collect(Collectors.toSet()), GmailFormat.FULL);
    }

    /**
     * <p>
     * Converts a collection of Gmail messages or drafts to Email domain objects, and loads attachments.
     * </p>
     *
     * @param userId    The Gmail user ID.
     * @param source    The source collection of Gmail items (Message or Draft).
     * @param converter The function to convert a Gmail item to an Email.
     * @param <T>       The type of Gmail object (Message or Draft).
     * @return collection of converted Email objects
     */
    private <T> Collection<Email> buildEmails(final String userId,
                                              final Collection<T> source,
                                              final Function<T, Email> converter) {
        final List<Email> emails = new ArrayList<>();

        for (final T item : source) {
            final Email email = converter.apply(item);
            final Message message;

            if (item instanceof Draft draft) {
                message = draft.getMessage();
            } else {
                message = (Message) item;
            }
            loadAttachment(userId, email, message);
            emails.add(email);
        }

        return emails;
    }

    /**
     * <p>
     * Builds a collection of Email objects from Gmail Message objects.
     * </p>
     *
     * @param userId   The Gmail user ID
     * @param messages Collection of Gmail Message objects
     * @return collection of Email objects with attachments loaded
     */
    private Collection<Email> buildEmail(final String userId, final Collection<Message> messages) {
        return buildEmails(userId, messages, emailConverter::convert);
    }

    /**
     * <p>
     * Builds a collection of Email objects from Gmail Draft objects.
     * </p>
     *
     * @param userId The Gmail user ID.
     * @param drafts Collection of Gmail Draft objects.
     * @return collection of Email objects with attachments loaded
     */
    private Collection<Email> buildDraftEmail(final String userId, final Collection<Draft> drafts) {
        return buildEmails(userId, drafts, gmailDraftMessageConverter::convert);
    }

    /**
     * <p>
     * Loads attachments for the provided Email by retrieving them from Gmail API and setting their data.
     * </p>
     *
     * @param userId  The Gmail user ID.
     * @param email   The Email object for which attachments are to be loaded.
     * @param message Collection of Gmail Message Objects
     */
    private void loadAttachment(final String userId, final Email email, final Message message) {
        final Collection<EmailAttachment> emailAttachments = email.getAttachments();
        final Map<String, String> cidToDataUri = new HashMap<>();

        try {
            if (Objects.nonNull(emailAttachments)) {
                for (final EmailAttachment emailAttachment : emailAttachments) {
                    final com.google.api.services.gmail.model.MessagePartBody attachmentPartBody = gmailClient.users().messages()
                            .attachments()
                            .get(userId, email.getMetaData().getInternalMessageId(), emailAttachment.getInternalId())
                            .execute();
                    final byte[] fileBytes = Base64.getUrlDecoder().decode(attachmentPartBody.getData());
                    final InputStream inputStream = new ByteArrayInputStream(fileBytes);

                    emailAttachment.setFileSize((long) fileBytes.length);
                    emailAttachment.setInputStream(inputStream);
                }
            }

            if (Objects.nonNull(message) && Objects.nonNull(message.getPayload())) {
                final List<MessagePart> inlineParts = new ArrayList<>();

                findAttachments(message.getPayload(), inlineParts, true);

                for (final MessagePart part : inlineParts) {
                    final List<MessagePartHeader> partHeaders = part.getHeaders();
                    final MessagePartBody partBody = part.getBody();
                    final String mimeType = part.getMimeType();
                    final String attachmentId = partBody.getAttachmentId();

                    if (Objects.isNull(partHeaders) || Objects.isNull(mimeType) || !mimeType.startsWith(IMAGE_MIME_PREFIX) || Objects.isNull(attachmentId)) {
                        continue;
                    }

                    partHeaders.stream()
                            .filter(header -> CONTENT_ID.equalsIgnoreCase(header.getName()))
                            .map(header -> header.getValue().replaceAll(ANGLE_BRACKETS_REGEX, EMPTY_STRING))
                            .findFirst()
                            .ifPresent(contentId -> {
                                try {
                                    final MessagePartBody inlineAttachmentBody = gmailClient.users().messages()
                                            .attachments()
                                            .get(userId, message.getId(), attachmentId)
                                            .execute();

                                    if (Objects.nonNull(inlineAttachmentBody) && Objects.nonNull(inlineAttachmentBody.getData())) {
                                        final String base64Data = inlineAttachmentBody.getData()
                                                .replace(HYPHEN_SYMBOL, PLUS_SYMBOL)
                                                .replace(UNDERSCORE_SYMBOL, SLASH_SYMBOL);

                                        cidToDataUri.put(contentId, DATA_URL_PREFIX + mimeType + BASE64_DELIMITER + base64Data);
                                    }
                                } catch (Throwable throwable) {
                                    LOGGER.error("Email Sync: Failed to fetch inline image. MessageId: {}, AttachmentId: {}",
                                            message.getId(), attachmentId, throwable);
                                }
                            });
                }

                if (!cidToDataUri.isEmpty()) {
                    String resolvedBody = email.getContent();

                    for (final Map.Entry<String, String> entry : cidToDataUri.entrySet()) {
                        resolvedBody = resolvedBody.replace(CID_URL_PREFIX + entry.getKey(), entry.getValue());
                    }
                    email.setContent(resolvedBody);
                }
            }
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Attachment content retrieve action failed, message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.ATTACHMENT_RETRIEVAL_FAILED, throwable);
        }
    }

    /**
     * <p>
     * Recursively finds attachments in the given {@link MessagePart} and collects them into the results list.
     * </p>
     *
     * @param part       The {@link MessagePart} to search for attachments
     * @param results    The {@link List} to collect found attachments
     * @param inlineOnly if true, only attachments with non-null attachment IDs are included
     */
    private void findAttachments(final MessagePart part, final List<MessagePart> results, final boolean inlineOnly) {
        if (Objects.nonNull(part)) {
            final String filename = part.getFilename();
            final String attachmentId = Optional.ofNullable(part.getBody())
                    .map(MessagePartBody::getAttachmentId)
                    .orElseGet((() -> null));
            final boolean hasFilename = Objects.nonNull(filename);
            final boolean includeAttachment = !inlineOnly || Objects.nonNull(attachmentId);

            if (hasFilename && includeAttachment) {
                results.add(part);
            }
            Optional.ofNullable(part.getParts())
                    .ifPresent(subParts -> subParts.forEach(subPart -> findAttachments(subPart, results, inlineOnly)));
        }
    }

    /**
     * <p>
     * Retrieves multiple drafts in a single batch request for optimal performance.
     * </p>
     *
     * @param userId Refers the userId.
     * @param drafts Collection of Draft objects.
     * @return Collection of Email objects with full details, or empty list if no drafts
     */
    private Collection<Email> getBatchDraftMessage(final String userId, final Collection<Draft> drafts) {
        return Objects.isNull(drafts) || drafts.isEmpty() ? Collections.emptyList() : buildDraftEmail(userId, retrieveBatchDraftMessage(userId, drafts.stream().map(Draft::getId).collect(Collectors.toSet())).values());
    }

    /**
     * <p>
     * Executes a batch request to fetch full draft messages for the given draft IDs.
     * </p>
     *
     * @param userId   The Gmail user ID.
     * @param draftIds Collection of Gmail draft IDs to be retrieved.
     * @return A map of message ID to fully populated {@link Message} objects.
     */
    private Map<String, Draft> retrieveBatchDraftMessage(final String userId, final Collection<String> draftIds) {
        if (Objects.isNull(draftIds) || draftIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            final Map<String, Draft> batchResults = new ConcurrentHashMap<>();
            final BatchRequest batchRequest = gmailClient.batch();

            for (final String draftId : draftIds) {
                gmailClient.users().drafts()
                        .get(userId, draftId)
                        .setFormat(GmailFormat.FULL.getName())
                        .queue(batchRequest, createDraftCallback(batchResults, draftId));
            }
            batchRequest.execute();

            return batchResults;
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Batch draft retrieve action failed, message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_BATCH_RETRIEVE_FAILED, throwable);
        }
    }

    /**
     * <p>
     * Creates a callback handler to process the result of a batched Gmail message retrieval.
     * </p>
     *
     * @param batchResults The map to store successfully retrieved {@link Message} objects, keyed by message ID.
     * @param messageId    The ID of the message being retrieved in the batch.
     * @return A {@link JsonBatchCallback} to handle success or failure of the message fetch operation.
     */
    private JsonBatchCallback<Message> createMessageCallback(final Map<String, Message> batchResults,
                                                             final String messageId) {
        return new JsonBatchCallback<>() {
            @Override
            public void onSuccess(final Message message, final HttpHeaders responseHeaders) {
                batchResults.put(messageId, message);
            }

            @Override
            public void onFailure(final GoogleJsonError exception, final HttpHeaders responseHeaders) {
                LOGGER.error("Batch message fetch failed for item: {} - {}", messageId, exception.getMessage());
            }
        };
    }

    /**
     * <p>
     * Creates a callback handler to process the result of a batched Gmail draft retrieval.
     * </p>
     *
     * @param batchResults The map to store successfully retrieved {@link Draft} objects, keyed by draft ID.
     * @param draftId      The ID of the draft being retrieved in the batch.
     * @return A {@link JsonBatchCallback} to handle success or failure of the draft fetch operation.
     */
    private JsonBatchCallback<Draft> createDraftCallback(final Map<String, Draft> batchResults,
                                                         final String draftId) {
        return new JsonBatchCallback<>() {
            @Override
            public void onSuccess(final Draft message, final HttpHeaders responseHeaders) {
                batchResults.put(draftId, message);
            }

            @Override
            public void onFailure(final GoogleJsonError exception, final HttpHeaders responseHeaders) {
                LOGGER.error("Batch draft fetch failed for item: {} - {}", draftId, exception.getMessage());
            }
        };
    }

    /**
     * <p>
     * Retrieves the messages by using the message id.
     * </p>
     *
     * @param userId    Refers the user id.
     * @param messageId Refers the  message id.
     */
    private Message getMessage(final String userId, final String messageId) {
        return fetchMessageById(userId, messageId);
    }

    /**
     * <p>
     * Fetch the messages by using the message id.
     * </p>
     *
     * @param userId    Refers the user id.
     * @param messageId Refers the  message id.
     */
    private Message fetchMessageById(final String userId, final String messageId) {
        try {
            return gmailClient.users().messages()
                    .get(userId, messageId)
                    .setFormat(GmailFormat.META_DATA.getName())
                    .setMetadataHeaders(Arrays.asList(
                            EmailHeaderFields.THREAD_ID,
                            GmailHeaderFields.SENT_MESSAGE_ID,
                            GmailHeaderFields.DRAFT_MESSAGE_ID,
                            GmailHeaderFields.MESSAGE_ID
                    ))
                    .execute();
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Failed to fetch message by ID: {}, message: {}, trace: {}",
                    messageId, throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_RETRIEVE_FAILED);
        }
    }

    /**
     * <p>
     * Fetches the full Gmail message (including payload) for the given message ID.
     * </p>
     *
     * @param userId    The Gmail user ID.
     * @param messageId The ID of the message to fetch.
     * @return The full {@link Message} object from Gmail API.
     */
    private Message fetchFullMessageById(final String userId, final String messageId) {
        try {
            return gmailClient.users().messages()
                    .get(userId, messageId)
                    .setFormat(GmailFormat.FULL.getName())
                    .execute();
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Failed to fetch full message by ID: {}, message: {}, trace: {}",
                    messageId, throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_RETRIEVE_FAILED);
        }
    }

    /**
     * <p>
     * Builds a collection of {@link History} objects from Gmail history records.
     * Tracks message additions, deletions, and label changes.
     * </p>
     *
     * @param historyList List of Gmail API {@link com.google.api.services.gmail.model.History} records
     * @return A collection of internal {@link History} instances capturing changes
     */
    private Collection<History> buildHistory(List<com.google.api.services.gmail.model.History> historyList) {
        return historyList.stream()
                .map(record -> {
                    final Collection<String> deletedMessages = new HashSet<>();
                    final Collection<String> addedMessages = new HashSet<>();
                    final Collection<String> labelModifiedMessages = new HashSet<>();

                    final Collection<HistoryMessageAdded> historyMessageAdded = record.getMessagesAdded();
                    final Collection<HistoryMessageDeleted> historyMessageDeleted = record.getMessagesDeleted();
                    final Collection<HistoryLabelAdded> historyLabelAdded = record.getLabelsAdded();
                    final Collection<HistoryLabelRemoved> historyLabelRemoved = record.getLabelsRemoved();

                    if (Objects.nonNull(historyMessageDeleted)) {
                        historyMessageDeleted.stream()
                                .map(HistoryMessageDeleted::getMessage)
                                .map(Message::getId)
                                .filter(Objects::nonNull)
                                .forEach(deletedMessages::add);
                    }

                    if (Objects.nonNull(historyMessageAdded)) {
                        historyMessageAdded.stream()
                                .map(HistoryMessageAdded::getMessage)
                                .map(Message::getId)
                                .filter(Objects::nonNull)
                                .forEach(addedMessages::add);
                    }

                    if (Objects.nonNull(historyLabelAdded)) {
                        historyLabelAdded.stream()
                                .map(HistoryLabelAdded::getMessage)
                                .map(Message::getId)
                                .filter(Objects::nonNull)
                                .forEach(labelModifiedMessages::add);
                    }

                    if (Objects.nonNull(historyLabelRemoved)) {
                        historyLabelRemoved.stream()
                                .map(HistoryLabelRemoved::getMessage)
                                .map(Message::getId)
                                .filter(Objects::nonNull)
                                .forEach(labelModifiedMessages::add);
                    }

                    return History.builder()
                            .id(record.getId())
                            .deletedMessages(deletedMessages)
                            .addedMessages(addedMessages)
                            .labelModifiedMessages(labelModifiedMessages)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * <p>
     * Retrieves a mapping of Gmail message IDs to their corresponding draft IDs for the specified user.
     * </p>
     *
     * @param user The Gmail user ID.
     * @param ids  A collection of internal Gmail message IDs for which draft IDs are to be fetched.
     * @return A {@link Map} where the key is the message ID and the value is the associated draft ID.
     */
    private Map<String, String> getDraftIdsById(final String user, final Collection<String> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, String> draftMessageIds = new HashMap<>();

        try {
            String nextPageToken = null;

            do {
                final ListDraftsResponse response = gmailClient.users().drafts()
                        .list(user)
                        .setPageToken(nextPageToken)
                        .execute();
                final List<Draft> drafts = response.getDrafts();

                if (Objects.nonNull(drafts)) {

                    for (final Draft draft : drafts) {
                        final Message message = draft.getMessage();

                        if (Objects.nonNull(message)) {
                            final String messageId = message.getId();

                            if (ids.contains(messageId)) {
                                draftMessageIds.put(messageId, draft.getId());
                            }
                        }
                    }
                }
                nextPageToken = response.getNextPageToken();
            } while (Objects.nonNull(nextPageToken));
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Gmail: Failed to retrieve drafts message: {}, stack: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_RETRIEVE_FAILED, throwable);
        }

        return draftMessageIds;
    }

    /**
     * <p>
     * Builder class for creating authenticated GmailClient instances.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public static class Builder {

        private String accessToken;
        private String refreshToken;
        private String clientId;
        private String clientSecret;

        private boolean requiredNonRefreshableClient = true;

        /**
         * <p>
         * Sets the OAuth2 access token for Gmail API authentication.
         * </p>
         *
         * @param accessToken the OAuth2 access token
         * @return Builder instance for method chaining
         */
        public Builder setAccessToken(final String accessToken) {
            this.accessToken = accessToken;

            return this;
        }

        /**
         * <p>
         * Sets the OAuth2 refreshable credentials (clientId, clientSecret, refreshToken).
         * Automatically enables refreshable mode.
         * </p>
         *
         * @param clientId     The OAuth2 client ID
         * @param clientSecret The OAuth2 client secret
         * @param refreshToken The OAuth2 refresh token
         * @return Builder instance.
         */
        public Builder setRefreshableCredentials(final String clientId, final String clientSecret, final String refreshToken) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.refreshToken = refreshToken;
            this.requiredNonRefreshableClient = false;

            return this;
        }

        /**
         * <p>
         * Builds and returns a configured GmailClient instance.
         * </p>
         *
         * @return Fully configured and authenticated GmailClient instance.
         */
        public GmailClient build() {
            try {
                final GoogleCredentials googleCredentials = requiredNonRefreshableClient ? getNonRefreshableCredential()
                        : getRefreshableCredential();
                final HttpRequestInitializer httpRequestInitializer = new HttpCredentialsAdapter(googleCredentials);

                return new GmailClient(
                        new Gmail.Builder(
                                GoogleNetHttpTransport.newTrustedTransport(),
                                GsonFactory.getDefaultInstance(),
                                httpRequestInitializer
                        )
                                .setApplicationName(GmailService.getInstance().getAppName())
                                .build()
                );
            } catch (Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, "Gmail: Client build action failed message: {}, trace: {}",
                        throwable.getMessage(), throwable);

                throw ErrorCode.get(EmailErrorCodes.CLIENT_INITIALIZATION_FAILED, throwable);
            }
        }

        /**
         * <p>
         * Constructs and returns a refreshable {@link GoogleCredentials} instance using the provided
         * client ID, client secret, and refresh token. This credential can automatically refresh
         * the access token when it expires.
         * </p>
         *
         * @return A {@link UserCredentials} object capable of refreshing access tokens.
         */
        private GoogleCredentials getRefreshableCredential() {
            return UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(refreshToken)
                    .build();
        }

        /**
         * <p>
         * Constructs and returns a non-refreshable {@link GoogleCredentials} instance using a fixed
         * access token. This is suitable for short-lived, manually obtained tokens that do not auto-refresh.
         * </p>
         *
         * @return A {@link GoogleCredentials} object with a static access token.
         */
        private GoogleCredentials getNonRefreshableCredential() {
            return GoogleCredentials.create(new AccessToken(accessToken, null));
        }
    }
}