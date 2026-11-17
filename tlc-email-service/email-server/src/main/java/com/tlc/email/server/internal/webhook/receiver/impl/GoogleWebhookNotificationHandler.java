package com.tlc.email.server.internal.webhook.receiver.impl;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.service.EmailTrackingService;
import com.tlc.email.server.fields.WebhookReceiverFields;
import com.tlc.email.server.internal.manager.ReferenceManager;
import com.tlc.email.server.internal.manager.ThreadManager;
import com.tlc.email.server.internal.provider.EmailTokenHandler;
import com.tlc.email.server.internal.provider.gmail.GmailClient;
import com.tlc.email.server.internal.provider.gmail.model.GmailFormat;
import com.tlc.email.server.internal.provider.gmail.model.History;
import com.tlc.email.server.internal.provider.gmail.model.MessageLabelInfo;
import com.tlc.email.server.internal.provider.gmail.service.GmailService;
import com.tlc.email.server.internal.provider.gmail.model.Label.Name;
import com.tlc.email.common.model.type.Email.MessageCategory;
import com.tlc.email.server.internal.webhook.receiver.AbstractWebhookNotificationHandler;
import com.tlc.email.server.internal.webhook.receiver.EmailWebhookReceiver;
import com.tlc.email.server.internal.webhook.receiver.provider.google.GoogleEmailWebhookReceiver;
import com.tlc.email.server.model.SyncSetting;

import java.math.BigInteger;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Responsible for processing incoming webhook events from Google (Gmail).
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public class GoogleWebhookNotificationHandler extends AbstractWebhookNotificationHandler {

    private final EmailTokenHandler emailTokenHandler;
    private final ReferenceManager referenceManager;
    private final ThreadManager threadManager;
    private final GmailService gmailService;
    private final EmailTrackingService emailTrackingService;

    private GoogleWebhookNotificationHandler() {
        this.emailTokenHandler = EmailTokenHandler.getInstance();
        this.referenceManager = ReferenceManager.getInstance();
        this.threadManager = ThreadManager.getInstance();
        this.gmailService = GmailService.getInstance();
        this.emailTrackingService = EmailTrackingService.getInstance();
    }

    private static class Instance {
        private static final GoogleWebhookNotificationHandler INSTANCE = new GoogleWebhookNotificationHandler();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link GoogleWebhookNotificationHandler}.
     * </p>
     *
     * @return instance of GoogleWebhookNotificationHandler
     */
    public static GoogleWebhookNotificationHandler getInstance() {
        return GoogleWebhookNotificationHandler.Instance.INSTANCE;
    }

    /**
     * <p>
     * Extracts {@link Email} objects from the webhook payload.
     * </p>
     *
     * @param payload the request payload
     * @return collection of Email objects
     */
    @Override
    protected Collection<Email> getEmails(final JsonObject payload) {
        if (payload.containsKey(WebhookReceiverFields.MESSAGE)) {
            final JsonObject messageJson = payload.getJsonObject(WebhookReceiverFields.MESSAGE);

            if (messageJson.containsKey(WebhookReceiverFields.DATA)) {
                final Object data = messageJson.get(WebhookReceiverFields.DATA);

                if (data instanceof final String stringData) {
                    final JsonObject decodedData = decodeData(stringData);

                    messageJson.put(WebhookReceiverFields.EMAIL_ADDRESS, decodedData.getString(WebhookReceiverFields.EMAIL_ADDRESS));
                    messageJson.put(WebhookReceiverFields.HISTORY_ID, decodedData.get(WebhookReceiverFields.HISTORY_ID));
                }
            }
            final Object historyId = messageJson.get(WebhookReceiverFields.HISTORY_ID);
            final BigInteger latestHistoryId = (historyId instanceof BigInteger id)
                    ? id
                    : BigInteger.valueOf(((Number) historyId).longValue());

            return buildEmail(latestHistoryId, messageJson.getString(WebhookReceiverFields.EMAIL_ADDRESS));
        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * Extracts {@link SyncAddressInfo} from the webhook payload.
     * </p>
     *
     * @param payload the request payload
     * @return SyncAddressInfo containing the email address
     */
    @Override
    protected Optional<SyncAddressInfo> getSyncAddressInfo(final JsonObject payload) {
        return Optional.of(new SyncAddressInfo(payload.getJsonObject(WebhookReceiverFields.MESSAGE).getString(WebhookReceiverFields.EMAIL_ADDRESS)));
    }

    /**
     * <p>
     * Builds the email changes from the Gmail history since the last processed history ID.
     * Processes additions, label modifications and deletions, then updates the modified threads
     * and the stored history ID of the receiver.
     * </p>
     *
     * @param latestHistoryId the latest history ID received in the notification
     * @param emailAddress    the email address for which the notification was received
     * @return a collection of {@link Email} objects to be synced
     */
    private Collection<Email> buildEmail(final BigInteger latestHistoryId, final String emailAddress) {
        final Optional<EmailWebhookReceiver> optionalEmailWebhookReceiver = webhookReceiverManager.get(emailAddress);
        final Collection<Email> emailCollection = new HashSet<>();

        optionalEmailWebhookReceiver.ifPresent(emailWebhookReceiver -> {
            final GoogleEmailWebhookReceiver googleReceiver = (GoogleEmailWebhookReceiver) emailWebhookReceiver;
            final BigInteger lastHistoryId = googleReceiver.getHistoryId();

            if (!Objects.equals(latestHistoryId, lastHistoryId)) {
                final BigInteger startHistoryId = Optional.ofNullable(lastHistoryId).orElse(latestHistoryId);
                final String accessToken = emailTokenHandler.getAccessToken(new SyncAddressInfo(emailAddress));
                final GmailClient gmailClient = new GmailClient.Builder().setAccessToken(accessToken).build();
                final String currentUser = gmailService.getCurrentUser();

                final Collection<History> histories = gmailClient.getMessageHistories(startHistoryId, currentUser);
                final Collection<Thread> modifiedThreads = new HashSet<>();
                final Collection<Long> messageIdsToBeDeleted = new HashSet<>();
                final Map<String, Email> pendingEmailsByInternalId = new HashMap<>();

                for (final History history : histories) {
                    buildEmail(history, emailAddress, currentUser, gmailClient, messageIdsToBeDeleted, modifiedThreads,
                            pendingEmailsByInternalId, emailCollection);
                }
                handleModifiedMails(new SyncAddressInfo(emailAddress), modifiedThreads, messageIdsToBeDeleted);
                googleReceiver.setHistoryId(latestHistoryId);
                webhookReceiverManager.update(googleReceiver);
            }
        });

        return emailCollection;
    }

    /**
     * <p>
     * Processes a single Gmail history record by resolving its added, label-modified and deleted
     * messages using their internal message IDs, and collects the resulting changes.
     * </p>
     *
     * @param history                   the Gmail history record to process
     * @param emailAddress              the email address associated with the user
     * @param userId                    the Gmail user ID
     * @param gmailClient               the Gmail client used to fetch label and message data
     * @param messageIdsToBeDeleted     collection to which the IDs of emails to be deleted are added
     * @param modifiedThreads           collection to which modified threads are added
     * @param pendingEmailsByInternalId map of emails collected so far in this sync, keyed by internal message ID
     * @param emailsToSync              collection to which emails to be synced are added
     */
    private void buildEmail(final History history, final String emailAddress,
                            final String userId, final GmailClient gmailClient,
                            final Collection<Long> messageIdsToBeDeleted, final Collection<Thread> modifiedThreads,
                            final Map<String, Email> pendingEmailsByInternalId, final Collection<Email> emailsToSync) {
        final Collection<String> addedMessages = history.getAddedMessages();
        final Collection<String> labelModifiedMessages = history.getLabelModifiedMessages();
        final Collection<String> deletedMessages = history.getDeletedMessages();

        final Collection<String> internalMessageIds = Stream.of(addedMessages, labelModifiedMessages, deletedMessages)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        if (internalMessageIds.isEmpty()) {
            return;
        }
        final SyncAddressInfo syncAddressInfo = new SyncAddressInfo(emailAddress);
        final Collection<Email> newlyFetched = new HashSet<>();

        syncSettingsManager.getId(emailAddress).ifPresent(syncedEmailId -> {
            final Optional<SyncSetting> optionalSyncSetting = syncSettingsManager.get(syncedEmailId);

            optionalSyncSetting.ifPresent(syncSetting -> {
                final Long emailId = syncSetting.getEmailAddress().id();

                for (final String internalMessageId : internalMessageIds) {
                    final boolean isDeleted = containsId(deletedMessages, internalMessageId);
                    final boolean isAdded = containsId(addedMessages, internalMessageId);
                    final boolean isLabelModified = containsId(labelModifiedMessages, internalMessageId);

                    if (isDeleted) {
                        handleDeletedMessage(emailId, internalMessageId, pendingEmailsByInternalId, emailsToSync,
                                messageIdsToBeDeleted, modifiedThreads);
                        continue;
                    }

                    if (isAdded) {
                        handleAddedMessage(userId, emailId, internalMessageId, gmailClient, pendingEmailsByInternalId,
                                emailsToSync, newlyFetched);
                    }

                    if (isLabelModified) {
                        handleLabelModifiedMessage(userId, emailId, internalMessageId, gmailClient, pendingEmailsByInternalId,
                                modifiedThreads);
                    }
                }
            });
            loadSenderCredentials(syncAddressInfo, newlyFetched);
            emailsToSync.addAll(newlyFetched);
        });
    }

    /**
     * <p>
     * Checks whether the given collection of message IDs contains the given internal message ID.
     * </p>
     *
     * @param messageIds        the collection of message IDs, may be {@code null}
     * @param internalMessageId the internal message ID to look for
     * @return {@code true} if the collection is non-null and contains the ID, {@code false} otherwise
     */
    private boolean containsId(final Collection<String> messageIds, final String internalMessageId) {
        return Objects.nonNull(messageIds) && messageIds.contains(internalMessageId);
    }

    /**
     * <p>
     * Handles a message that was deleted in Gmail.
     * </p>
     *
     * @param emailId                   the synced email ID
     * @param internalMessageId         the Gmail internal message ID
     * @param pendingEmailsByInternalId map of pending emails keyed by internal message ID
     * @param emailsToSync              collection of emails to be synced
     * @param messageIdsToBeDeleted     collection to which the IDs of emails to be deleted are added
     * @param modifiedThreads           collection to which modified threads are added
     */
    private void handleDeletedMessage(final Long emailId, final String internalMessageId,
                                      final Map<String, Email> pendingEmailsByInternalId, final Collection<Email> emailsToSync,
                                      final Collection<Long> messageIdsToBeDeleted, final Collection<Thread> modifiedThreads) {
        final Email pendingEmail = pendingEmailsByInternalId.remove(internalMessageId);

        if (Objects.nonNull(pendingEmail)) {
            emailsToSync.remove(pendingEmail);

            return;
        }

        resolveStoredEmail(emailId, internalMessageId).ifPresent(resolved -> {
            final Email email = resolved.email();
            final MessageCategory messageCategory = email.getMessageCategory();
            final boolean isDraft = Objects.nonNull(messageCategory) && messageCategory.isDraft()
                    && emailTrackingService.getTrackingId(email.getContent()).isEmpty();

            if (isDraft) {
                messageIdsToBeDeleted.add(email.id());
            } else {
                email.setDeleted(true);
                resolved.thread().ifPresent(modifiedThreads::add);
            }
        });
    }

    /**
     * <p>
     * Handles a message that was added in Gmail.
     * </p>
     *
     * @param userId                    the Gmail user ID
     * @param emailId                   the synced email ID
     * @param internalMessageId         the Gmail internal message ID
     * @param gmailClient               the Gmail client used to fetch message data
     * @param pendingEmailsByInternalId map of pending emails keyed by internal message ID
     * @param emailsToSync              collection of emails already queued to be synced
     * @param newlyFetched              collection to which newly registered emails are added
     */
    private void handleAddedMessage(final String userId, final Long emailId, final String internalMessageId,
                                    final GmailClient gmailClient, final Map<String, Email> pendingEmailsByInternalId,
                                    final Collection<Email> emailsToSync, final Collection<Email> newlyFetched) {
        if (pendingEmailsByInternalId.containsKey(internalMessageId) || emailsToSync.stream()
                .anyMatch(email -> internalMessageIdMatches(email, internalMessageId))) {
            return;
        }
        final Optional<ResolvedWebhookEmail> storedEmail = resolveStoredEmail(emailId, internalMessageId);

        if (storedEmail.isPresent()) {
            final Email email = storedEmail.get().email();
            final MessageCategory messageCategory = email.getMessageCategory();
            final boolean isDraft = Objects.nonNull(messageCategory) && messageCategory.isDraft()
                    && emailTrackingService.getTrackingId(email.getContent()).isEmpty();

            if (isDraft) {
                fetchAndSanitizeEmail(userId, emailId, internalMessageId, gmailClient).ifPresent(fetched -> {
                    registerPendingEmail(internalMessageId, fetched, pendingEmailsByInternalId, newlyFetched);
                });
            } else {
                registerPendingEmail(internalMessageId, email, pendingEmailsByInternalId, newlyFetched);
            }

            return;
        }
        fetchAndSanitizeEmail(userId, emailId, internalMessageId, gmailClient).ifPresent(fetched ->
                registerPendingEmail(internalMessageId, fetched, pendingEmailsByInternalId, newlyFetched));
    }

    /**
     * <p>
     * Handles a message whose labels were modified in Gmail.
     * </p>
     *
     * <p>
     * If the message is pending in the current sync, its labels are applied to the pending email.
     * Otherwise, the labels are applied to the stored email and its thread is marked as modified.
     * </p>
     *
     * @param userId                    the Gmail user ID
     * @param emailId                   the synced email ID
     * @param internalMessageId         the Gmail internal message ID
     * @param gmailClient               the Gmail client used to fetch label data
     * @param pendingEmailsByInternalId map of pending emails keyed by internal message ID
     * @param modifiedThreads           collection to which modified threads are added
     */
    private void handleLabelModifiedMessage(final String userId, final Long emailId, final String internalMessageId,
                                            final GmailClient gmailClient, final Map<String, Email> pendingEmailsByInternalId,
                                            final Collection<Thread> modifiedThreads) {
        final Email pendingEmail = pendingEmailsByInternalId.get(internalMessageId);

        if (Objects.nonNull(pendingEmail)) {
            loadLabels(pendingEmail, gmailClient.getMessageLabelData(userId, internalMessageId));

            return;
        }
        resolveStoredEmail(emailId, internalMessageId).ifPresent(resolved -> {
            loadLabels(resolved.email(), gmailClient.getMessageLabelData(userId, internalMessageId));

            resolved.thread().ifPresent(modifiedThreads::add);
        });
    }

    /**
     * <p>
     * Registers an email as pending for the given internal message ID and adds it to the target collection.
     * </p>
     *
     * @param internalMessageId         the Gmail internal message ID
     * @param email                     the email to register
     * @param pendingEmailsByInternalId map of pending emails keyed by internal message ID
     * @param target                    collection to which the email is added
     */
    private void registerPendingEmail(final String internalMessageId, final Email email,
                                      final Map<String, Email> pendingEmailsByInternalId, final Collection<Email> target) {
        pendingEmailsByInternalId.put(internalMessageId, email);
        target.add(email);
    }

    /**
     * <p>
     * Checks whether the internal message ID in the email's metadata matches the given ID.
     * </p>
     *
     * @param email             the email to check, may be {@code null}
     * @param internalMessageId the internal message ID to compare against
     * @return {@code true} if the email and its metadata are non-null and the IDs match, {@code false} otherwise
     */
    private boolean internalMessageIdMatches(final Email email, final String internalMessageId) {
        if (Objects.isNull(email) || Objects.isNull(email.getMetaData())) {
            return false;
        }

        return Objects.equals(email.getMetaData().getInternalMessageId(), internalMessageId);
    }

    /**
     * <p>
     * Resolves the stored email, along with its thread, for the given internal message ID.
     * </p>
     *
     * @param emailId           the synced email ID
     * @param internalMessageId the Gmail internal message ID
     * @return an {@link Optional} containing the {@link ResolvedWebhookEmail}, or empty if the message
     * or its thread cannot be found
     */
    private Optional<ResolvedWebhookEmail> resolveStoredEmail(final Long emailId, final String internalMessageId) {
        final Optional<String> optionalMessageId = referenceManager.getMessageIdByInternalId(emailId, internalMessageId);

        if (optionalMessageId.isEmpty()) {
            return Optional.empty();
        }
        final Optional<Thread> optionalThread = threadManager.getThreadsByEmailId(emailId, true, List.of(optionalMessageId.get()))
                .stream()
                .findFirst();

        return optionalThread.flatMap(thread -> thread.getComposeEmail().map(email -> new ResolvedWebhookEmail(email, Optional.of(thread))));
    }

    /**
     * <p>
     * Holds a stored email together with the thread it belongs to.
     * </p>
     *
     * @param email  the stored email
     * @param thread the thread containing the email, if available
     */
    private record ResolvedWebhookEmail(Email email, Optional<Thread> thread) {
    }

    /**
     * <p>
     * Fetches an email (either a draft or a regular message) using the provided Gmail client,
     * sanitizes its content and subject, and attempts to load the tracking ID for metadata association.
     * </p>
     *
     * @param userId            The Gmail user ID
     * @param emailId           The internal synced email ID used for cross-referencing
     * @param internalMessageId The Gmail internal message ID
     * @param gmailClient       The Gmail client used to fetch email data
     * @return An {@link Optional} containing the fetched {@link Email}, if available
     */
    private Optional<Email> fetchAndSanitizeEmail(final String userId, final Long emailId,
                                                  final String internalMessageId, final GmailClient gmailClient) {
        final MessageLabelInfo labelInfo = gmailClient.getMessageLabelData(userId, internalMessageId);
        final Collection<String> internalMessagedIds = List.of(internalMessageId);
        final Optional<Email> optionalEmail = labelInfo.isDraft()
                ? gmailClient.getDraftMails(userId, internalMessagedIds).stream().findFirst()
                : gmailClient.getMails(userId, internalMessagedIds, GmailFormat.FULL).stream().findFirst();

        optionalEmail.ifPresent(email -> loadTrackingId(emailId, email));

        return optionalEmail;
    }

    /**
     * <p>
     * Extracts the tracking ID from the email content, if present, and resolves the original
     * message ID associated with it. Updates the email's metadata with the resolved message ID
     * and tracking ID.
     * </p>
     *
     * @param emailId The synced internal email ID
     * @param email   The email object whose metadata should be updated
     */
    private void loadTrackingId(final Long emailId, final Email email) {
        final Optional<String> optionalTrackingId = emailTrackingService.getTrackingId(email.getContent());

        optionalTrackingId.ifPresent(trackingId -> {
            referenceManager.getMessageIdByTrackingId(emailId, trackingId)
                    .ifPresent(messageId -> {
                        final EmailMetaData metaData = email.getMetaData();

                        metaData.setMessageId(messageId);
                        metaData.setTrackingId(trackingId);
                    });
        });
    }

    /**
     * <p>
     * Updates label states (e.g., read/unread, starred, deleted, archived) for a given email
     * based on Gmail-provided label information.
     * </p>
     *
     * @param email     The email object whose state needs updating
     * @param labelInfo Gmail label metadata
     */
    private void loadLabels(final Email email, final MessageLabelInfo labelInfo) {
        final Collection<Name> labels = labelInfo.getLabelList();

        if (Objects.nonNull(labels)) {
            if (labels.contains(Name.UNREAD) && email.isRead()) {
                email.setRead(false);
            } else if (!labels.contains(Name.UNREAD) && !email.isRead()) {
                email.setRead(true);
            }
            final MessageCategory category = email.getMessageCategory();

            if (labels.contains(Name.INBOX) && category == MessageCategory.INBOUND) {
                if (email.isArchived()) {
                    email.setArchived(false);
                }
                if (!email.isActive()) {
                    email.isActive(true);
                }
            } else if (!labels.contains(Name.INBOX) && category == MessageCategory.INBOUND) {
                if (labels.contains(Name.SPAM)) {
                    // TODO: Handle spam logic if required
                } else if (labels.contains(Name.TRASH)) {
                    email.isActive(false);
                } else {
                    email.setArchived(true);
                }
            }

            if (labels.contains(Name.SENT) && category == MessageCategory.OUTBOUND) {
                if (email.isArchived()) {
                    email.setArchived(false);
                }
                email.isActive(!labels.contains(Name.TRASH));
            } else if (!labels.contains(Name.SENT) && category == MessageCategory.OUTBOUND) {
                if (labels.contains(Name.SPAM)) {
                    // TODO: Handle spam logic if required
                } else if (labels.contains(Name.TRASH)) {
                    email.isActive(false);
                } else {
                    email.setArchived(true);
                }
            }

            if (labels.contains(Name.STARRED) && !email.isStarred()) {
                email.setStarred(true);
            } else if (!labels.contains(Name.STARRED) && email.isStarred()) {
                email.setStarred(false);
            }
        }
    }

    /**
     * <p>
     * Decodes a base64-encoded string into a {@link JsonObject}.
     * </p>
     *
     * @param encodedData the base64-encoded payload
     * @return a decoded {@link JsonObject}
     */
    private JsonObject decodeData(final String encodedData) {
        return Json.object(Base64.getDecoder().decode(encodedData));
    }

    /**
     * <p>
     * Sets sender and reply-to information for outbound emails that are newly retrieved.
     * </p>
     *
     * @param syncAddressInfo The sync configuration for the email address
     * @param emails          Collection of emails to update
     */
    private void loadSenderCredentials(final SyncAddressInfo syncAddressInfo, final Collection<Email> emails) {
        for (final Email email : emails) {
            final EmailMetaData metaData = email.getMetaData();

            if (Email.MessageCategory.INBOUND != email.getMessageCategory()) {
                email.setSender(syncAddressInfo);
            }
            final String inReplyTo = metaData.getInReplyTo();

            if (Objects.isNull(inReplyTo) || inReplyTo.isEmpty()) {
                metaData.setReplyToAddress(syncAddressInfo.getEmailAddress());
            }
        }
    }
}