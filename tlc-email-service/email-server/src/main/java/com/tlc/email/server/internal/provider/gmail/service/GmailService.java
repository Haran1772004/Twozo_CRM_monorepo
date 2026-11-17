package com.tlc.email.server.internal.provider.gmail.service;

import com.google.api.services.gmail.model.Message;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.EmailConfiguration;
import com.tlc.email.common.model.config.EmailInterval;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.request.DeleteRequest;
import com.tlc.email.common.request.UpdateRequest;
import com.tlc.email.common.service.EmailTrackingService;
import com.tlc.email.server.internal.field.EmailKeys;
import com.tlc.email.server.internal.provider.gmail.model.ListResponse;
import com.tlc.email.server.internal.provider.gmail.model.Label;
import com.tlc.email.server.internal.provider.gmail.model.MessageModificationRequest;
import com.tlc.email.server.internal.provider.EmailTokenHandler;
import com.tlc.email.server.internal.provider.gmail.model.ListRequest;
import com.tlc.email.server.model.EmailProviderDataStore;
import com.tlc.email.server.internal.provider.gmail.GmailClient;
import com.tlc.email.server.model.GmailDataStore;
import com.tlc.email.server.service.EmailProviderService;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.email.server.internal.provider.gmail.model.Label.Name;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenReadRequest;
import com.tlc.sync.service.oauth.service.token.EmailSyncTokenService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.tlc.email.common.model.type.Email.MessageCategory.DRAFT;

/**
 * <p>
 * Provides the email service related to google.
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class GmailService implements EmailProviderService {

    private static final String CURRENT_USER = "me";

    private final String appName;
    private final int batchSize;
    private final long historyBatchSize;
    private final String pubSubTopic;
    private final String clientId;
    private final String clientSecret;

    private final EmailTokenHandler emailTokenHandler;
    private final EmailSyncTokenService emailSyncTokenService;
    private final EmailTrackingService emailTrackingService;

    private static class Instance {
        private static final GmailService INSTANCE = new GmailService();
    }

    private GmailService() {
        this.emailTokenHandler = EmailTokenHandler.getInstance();
        this.emailSyncTokenService = EmailSyncTokenService.getInstance();
        this.emailTrackingService = EmailTrackingService.getInstance();
        final Configurations configurations = ConfHolder.getInstance()
                .getProperties(EmailKeys.SYNC_SERVICE.getKey()).getNestedConfigs(SyncServiceFields.GMAIL);

        appName = configurations.getConfig(EmailKeys.APP_NAME.getKey());
        batchSize = configurations.getInt(EmailKeys.MESSAGE_BATCH_SIZE.getKey());
        historyBatchSize = configurations.getInt(EmailKeys.HISTORY_BATCH_SIZE.getKey());
        pubSubTopic = configurations.getConfig(EmailFields.PUB_SUB_TOPIC);
        clientId = configurations.getConfig(SyncServiceFields.APP_CLIENT_ID);
        clientSecret = configurations.getConfig(SyncServiceFields.APP_CLIENT_SECRET);
    }

    /**
     * <p>
     * Gets the {@link GmailService} Instance
     * </p>
     *
     * @return {@link GmailService}
     */
    public static GmailService getInstance() {
        return GmailService.Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the application name used for Gmail API client.
     * </p>
     *
     * @return the application name as a {@link String}
     */
    public String getAppName() {
        return appName;
    }

    /**
     * <p>
     * Gets the history batch size.
     * </p>
     *
     * @return the history batch long value.
     */
    public long getHistoryBatchSize() {
        return historyBatchSize;
    }

    /**
     * <p>
     * Gets the pub sub topic.
     * </p>
     *
     * @return the pub sub topic value.
     */
    public String getPubSubTopic() {
        return pubSubTopic;
    }

    /**
     * <p>
     * Gets the current user.
     * </p>
     *
     * @return The current user.
     */
    public String getCurrentUser() {
        return CURRENT_USER;
    }

    /**
     * {@inheritDoc}
     *
     * @param email An entity that holds the email attributes.
     */
    @Override
    public void send(final Email email) {
        final SyncAddressInfo emailAddress = email.getSender();
        final String accessToken = emailTokenHandler.getAccessToken(emailAddress);
        final GmailClient gmailClient = new GmailClient.Builder()
                .setAccessToken(accessToken)
                .build();

        if (email.getMessageCategory() == DRAFT && email instanceof DraftEmail draftEmail) {
            final DraftEmail.DraftAction draftAction = draftEmail.getDraftAction();

            switch (draftAction) {
                case CREATE -> {
                    emailTrackingService.populateTrackingId(email);
                    gmailClient.createDraft(CURRENT_USER, email);
                }
                case UPDATE -> {
                    emailTrackingService.populateTrackingId(email);
                    gmailClient.updateDraft(CURRENT_USER, email);
                }
                case SEND -> {
                    final EmailMetaData metaData = draftEmail.getMetaData();
                    final String internalDraftId = Objects.isNull(metaData) ? null : metaData.getInternalDraftId();

                    if (Objects.nonNull(internalDraftId) && gmailClient.isValid(CURRENT_USER, internalDraftId)) {
                        gmailClient.sendDraft(CURRENT_USER, email);
                    } else {
                        gmailClient.send(CURRENT_USER, email);
                    }
                }
            }
        } else {
            gmailClient.send(CURRENT_USER, email);
        }
    }

    /**
     * <p>
     * Updates the email to its respective state preferred provided in the input
     * </p>
     *
     * @param updateRequest            Refers to the {@link UpdateRequest}
     * @param emailStateTransitionInfo Have the attributes needed to change the state of an email
     */
    @Override
    public void update(final UpdateRequest updateRequest, final Collection<EmailStateTransitionInfo> emailStateTransitionInfo) {
        final String accessToken = emailTokenHandler.getAccessToken(updateRequest.getSyncEmailAddress());
        final GmailClient gmailClient = new GmailClient.Builder().setAccessToken(accessToken).build();
        final Email.State state = emailStateTransitionInfo.stream()
                .map(EmailStateTransitionInfo::getNewState)
                .findFirst()
                .orElseGet(() -> null);

        final Collection<String> inboundMessageIds = new ArrayList<>();
        final Collection<String> outboundMessageIds = new ArrayList<>();

        for (final EmailStateTransitionInfo currentMessageTransition : emailStateTransitionInfo) {
            final Email.MessageCategory messageCategory = currentMessageTransition.getMessageCategory();
            final String messageId = currentMessageTransition.getOldMessageId();

            switch (messageCategory) {
                case INBOUND -> inboundMessageIds.add(messageId);
                case OUTBOUND -> outboundMessageIds.add(messageId);
            }
        }
        updateRequest.setState(state);

        if (Objects.nonNull(state)) {
            switch (state) {
                case ACTIVE -> {
                    if (!inboundMessageIds.isEmpty()) {
                        restoreFromTrash(gmailClient, inboundMessageIds, List.of(Name.INBOX));
                    }

                    if (!outboundMessageIds.isEmpty()) {
                        restoreFromTrash(gmailClient, outboundMessageIds, Collections.emptyList());
                    }
                }
                case INACTIVE -> {
                    if (!inboundMessageIds.isEmpty()) {
                        moveToTrash(gmailClient, inboundMessageIds, state);
                    }

                    if (!outboundMessageIds.isEmpty()) {
                        moveToTrash(gmailClient, outboundMessageIds, state);
                    }
                }
                case ARCHIVE -> {
                    if (!inboundMessageIds.isEmpty()) {
                        archiveInboundMessages(gmailClient, inboundMessageIds);
                    }
                }
                case UNARCHIVE -> {
                    if (!inboundMessageIds.isEmpty()) {
                        unArchiveInboundMessages(gmailClient, inboundMessageIds);
                    }
                }
                default -> throw ErrorCode.get(EmailErrorCodes.FOLDER_NOT_FOUND);
            }
        }
    }

    /**
     * <p>
     * Performs the delete action for the provided email and message ids.
     * </p>
     *
     * @param emailStateTransitionInfo Represent the email sate transition info.
     * @param emailAddress          Refers the email address.
     */
    @Override
    public void delete(final EmailStateTransitionInfo emailStateTransitionInfo, final String emailAddress) {
        final String accessToken = emailTokenHandler.getAccessToken(emailAddress);
        final GmailClient gmailClient = new GmailClient.Builder()
                .setAccessToken(accessToken)
                .build();
        final String id = emailStateTransitionInfo.getOldMessageId();

        if (emailStateTransitionInfo.getMessageCategory().isDraft()) {
            gmailClient.deleteDraft(CURRENT_USER, id);
        } else {
            gmailClient.deleteMessage(CURRENT_USER, id);
        }
    }

    /**
     * <p>
     * Receives the notification from the sender.
     * </p>
     *
     * @param emailConfiguration An entity that holds the configuration attributes
     * @return {@link Object}
     */
    @Override
    public EmailProviderDataStore receive(final EmailConfiguration emailConfiguration) {
        final EmailReceiverConfiguration emailReceiverConfiguration = emailConfiguration.get();
        final SyncAddressInfo emailAddress = emailReceiverConfiguration.getSyncEmailAddress();
        final FolderType folderType = emailReceiverConfiguration.getFirstFolderType();
        final EmailInterval emailInterval = emailReceiverConfiguration.getEmailIntervals().get(folderType);
        final EmailTokenReadRequest accessTokenReadRequest = SyncTokenRequestInstanceProvider.getEmailTokenReadRequest(new EmailAddress(emailAddress.getEmailAddress()), TokenRequest.TokenType.REFRESH);
        final String refreshToken = emailSyncTokenService.getRefreshToken(accessTokenReadRequest);
        final GmailClient gmailClient = new GmailClient.Builder()
                .setRefreshableCredentials(clientId, clientSecret, refreshToken)
                .build();
        final GmailDataStore gmailDataStore = new GmailDataStore(emailReceiverConfiguration);

        gmailDataStore.setFilterQuery(buildGmailQuery(emailInterval));
        gmailDataStore.setBatchSize(batchSize);
        gmailDataStore.setGmailClient(gmailClient);
        gmailDataStore.setEmailReceiverConfiguration(emailReceiverConfiguration);
        gmailDataStore.setFolderType(folderType);

        loadEmailProviderData(gmailDataStore);

        return gmailDataStore;
    }

    /**
     * <p>
     * Loads and processes the given email provider data.
     * </p>
     *
     * @param emailProviderDataStore The email provider data to be loaded.
     */
    @Override
    public void loadEmailProviderData(final EmailProviderDataStore emailProviderDataStore) {
        final GmailDataStore gmailDataStore = emailProviderDataStore.get();
        final GmailClient gmailClient = gmailDataStore.getGmailClient();
        final SyncAddressInfo syncAddressInfo = gmailDataStore.getEmailReceiverConfiguration().getSyncEmailAddress();
        final String query = gmailDataStore.getFilterQuery();
        final long batchSize = gmailDataStore.getBatchSize();
        final FolderType folderType = emailProviderDataStore.getFolderType();

        if (!Objects.equals(FolderType.ARCHIVE, folderType)) {
            final ListRequest listRequest = ListRequest.builder()
                    .labelId(folderType.name())
                    .maxResults(batchSize)
                    .pageToken(gmailDataStore.getPageToken())
                    .userId(CURRENT_USER)
                    .query(query)
                    .folderType(folderType)
                    .build();
            final ListResponse gmailListResponse = gmailClient.getListResponse(listRequest);
            final Collection<Email> emails = gmailListResponse.getMessages();

            gmailDataStore.setPageToken(gmailListResponse.getNextPageToken());

            if (!emails.isEmpty()) {
                loadSenderCredentials(syncAddressInfo, emails);
                emailProviderDataStore.setEmails(emails);
            }
        }
    }

    /**
     * <p>
     * Builds a Gmail client for the specified email address using the access token.
     * </p>
     *
     * @param emailAddress The email address for which to build the Gmail client
     * @return A configured instance of {@link GmailClient}
     */
    public GmailClient buildGmailClient(final String emailAddress) {
        final String accessToken = emailTokenHandler.getAccessToken(new SyncAddressInfo(emailAddress));
        return new GmailClient.Builder()
                .setAccessToken(accessToken)
                .build();
    }

    /**
     * <p>
     * Loads inline images for the given email and email address.
     * </p>
     *
     * @param email       The email for which to load inline images
     * @param gmailClient The Gmail client used to fetch inline attachments
     */
    public void loadInlineImages(final Email email, final GmailClient gmailClient) {
        gmailClient.loadInlineAttachments(CURRENT_USER, email);
    }

    /**
     * <p>
     * Populates sender and reply-to address information for a collection of emails.
     * </p>
     *
     * @param syncAddressInfo The sender information to populate
     * @param emails          The emails to enrich with sender and reply-to data
     */
    private void loadSenderCredentials(final SyncAddressInfo syncAddressInfo, final Collection<Email> emails) {
        for (final Email email : emails) {
            final Email.MessageCategory messageCategory = email.getMessageCategory();
            final EmailMetaData emailMetaData = email.getMetaData();
            final String replyTo = email.getMetaData().getInReplyTo();

            if (Email.MessageCategory.INBOUND != messageCategory) {
                email.setSender(syncAddressInfo);
            }

            if (Objects.isNull(replyTo) || replyTo.isEmpty()) {
                emailMetaData.setReplyToAddress(syncAddressInfo.getEmailAddress());
            }
        }
    }

    /**
     * <p>
     * Builds the Gmail query string based on the email interval.
     * </p>
     *
     * @param emailInterval The email interval containing start and end times in seconds
     * @return A query string to filter emails in the given time range
     */
    public String buildGmailQuery(final EmailInterval emailInterval) {
        return String.format("after:%d before:%d", emailInterval.startTime() / 1000, emailInterval.endTime() / 1000);
    }

    /**
     * <p>
     * Retrieves the list of Gmail label IDs corresponding to the given label names.
     * </p>
     *
     * @param gmailClient the Gmail client used to retrieve label metadata
     * @param names       the collection of system label names (e.g., INBOX, TRASH)
     * @return a list of matching Gmail label IDs; returns an empty list if none are found or input is null/empty
     */
    private List<String> getLabel(final GmailClient gmailClient, final Collection<Name> names) {
        if (Objects.isNull(names) || names.isEmpty()) {
            return Collections.emptyList();
        }

        return getGmailLabelId(gmailClient, names);
    }

    /**
     * <p>
     * Retrieves the list of Gmail label IDs corresponding to the given label names.
     * </p>
     *
     * @param gmailClient       the Gmail client used to retrieve label metadata.
     * @param messageIds        Represent the message ids.
     * @param destinationLabels Represent the destination labels.
     */
    private void restoreFromTrash(final GmailClient gmailClient,
                                  final Collection<String> messageIds,
                                  final Collection<Name> destinationLabels) {
        final List<String> addLabels = getLabel(gmailClient, destinationLabels);
        final String trashLabel = getLabel(gmailClient, List.of(Name.TRASH)).get(0);

        for (final String rfcId : messageIds) {
            final Optional<String> optionalMessageId = gmailClient.getMessageByRfcMessageId(CURRENT_USER, rfcId)
                    .map(Message::getId);

            optionalMessageId.ifPresent(messageId -> {
                final MessageModificationRequest.Builder builder = MessageModificationRequest.builder()
                        .messageId(messageId)
                        .removeLabelIds(List.of(trashLabel));

                if (!addLabels.isEmpty()) {
                    builder.addLabelIds(addLabels);
                }

                gmailClient.modify(CURRENT_USER, builder.build());
            });
        }
    }

    /**
     * <p>
     * Archives inbound Gmail messages by removing them from the INBOX label.
     * </p>
     *
     * @param gmailClient the Gmail client used for label modification
     * @param messageIds  a collection of RFC message IDs to archive
     */
    private void archiveInboundMessages(final GmailClient gmailClient, final Collection<String> messageIds) {
        updateInboxLabel(gmailClient, messageIds, false);
    }

    /**
     * <p>
     * Un archives inbound Gmail messages by re-adding the INBOX label.
     * </p>
     *
     * @param gmailClient the Gmail client used for label modification
     * @param messageIds  a collection of RFC message IDs to un archive
     */
    private void unArchiveInboundMessages(final GmailClient gmailClient, final Collection<String> messageIds) {
        updateInboxLabel(gmailClient, messageIds, true);
    }

    /**
     * <p>
     * Adds or removes the INBOX label from the specified Gmail messages.
     * </p>
     *
     * @param gmailClient the Gmail client used for label updates
     * @param messageIds  the collection of RFC message IDs to update
     * @param addLabel    true to add the INBOX label (un archive), false to remove it (archive)
     */
    private void updateInboxLabel(final GmailClient gmailClient, final Collection<String> messageIds, final boolean addLabel) {
        final List<String> inboxLabel = getLabel(gmailClient, List.of(Name.INBOX));

        for (final String rfcId : messageIds) {
            final Optional<String> optionalMessageId = gmailClient.getMessageByRfcMessageId(CURRENT_USER, rfcId)
                    .map(Message::getId);

            optionalMessageId.ifPresent(messageId -> {
                final MessageModificationRequest.Builder builder = MessageModificationRequest.builder()
                        .messageId(messageId);

                if (addLabel) {
                    builder.addLabelIds(inboxLabel);
                } else {
                    builder.removeLabelIds(inboxLabel);
                }

                gmailClient.modify(CURRENT_USER, builder.build());
            });
        }
    }

    /**
     * <p>
     * Retrieves the Gmail label ID for the specified system label (e.g., INBOX, TRASH, SENT).
     * </p>
     *
     * @param gmailClient The Gmail client used to fetch labels
     * @param labelNames  Refers the label names
     * @return an Optional containing the label ID if found, or empty otherwise
     */
    private List<String> getGmailLabelId(final GmailClient gmailClient, Collection<Name> labelNames) {
        return gmailClient.getLabels(CURRENT_USER, labelNames).stream().map(Label::getName).map(Name::getValue).collect(Collectors.toList());
    }

    /**
     * <p>
     * Updates Gmail messages by applying actions based on the given email state.
     * </p>
     *
     * @param gmailClient The Gmail client used for message operations
     * @param messageIds  The collection of RFC message IDs to process
     * @param state       The target state (currently supports only {@code ACTIVE} for trashing)
     */
    private void moveToTrash(final GmailClient gmailClient, final Collection<String> messageIds, final Email.State state) {
        for (final String rfcId : messageIds) {
            final Optional<String> optionalMessageId = gmailClient.getMessageByRfcMessageId(CURRENT_USER, rfcId)
                    .map(Message::getId);

            optionalMessageId.ifPresent(messageId -> {
                try {
                    if (Objects.equals(state, Email.State.INACTIVE)) {
                        gmailClient.moveToTrash(CURRENT_USER, messageId);
                    }
                } catch (Throwable throwable) {
                    throw ErrorCode.get(EmailErrorCodes.EMAIL_TRASH_FAILED, throwable);
                }
            });
        }
    }
}