package com.tlc.email.server.internal.provider.outlook;

import com.microsoft.graph.models.Attachment;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.MessageCreateForwardParameterSet;
import com.microsoft.graph.models.MessageCreateReplyAllParameterSet;
import com.microsoft.graph.models.MessageCreateReplyParameterSet;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.requests.AttachmentCollectionPage;
import com.microsoft.graph.requests.AttachmentCollectionRequestBuilder;
import com.microsoft.graph.requests.AttachmentCollectionResponse;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MessageRequestBuilder;
import com.microsoft.graph.requests.UserRequestBuilder;

import com.tlc.attachment.io.File;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.ConsentType;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.EmailInterval;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.model.config.TenantConfiguration;

import com.tlc.email.common.request.UpdateRequest;
import com.tlc.email.server.internal.field.EmailKeys;
import com.tlc.email.server.internal.field.OutlookHeaderFields;
import com.tlc.email.server.model.EmailProviderDataStore;
import com.tlc.email.server.model.OutlookDataStore;
import com.tlc.email.server.service.EmailProviderService;
import com.tlc.email.server.internal.field.EmailHeaderFields;
import com.tlc.email.server.internal.provider.EmailTokenHandler;

import com.tlc.notification.fields.NotificationKeys;
import com.tlc.notification.internal.status.NotificationErrorCodes;

import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.OutboundEmail;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.config.EmailConfiguration;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.model.type.Email.MessageCategory;
import com.tlc.email.common.model.type.Email.State;

import com.tlc.sync.service.oauth.fields.SyncServiceFields;

import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;

import okhttp3.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.tlc.email.server.internal.field.EmailKeys.ARCHIVE;
import static com.tlc.email.server.internal.field.EmailKeys.OUTLOOK_DRAFTS;
import static com.tlc.email.server.internal.field.EmailKeys.OUTLOOK_INBOX;
import static com.tlc.email.server.internal.field.EmailKeys.OUTLOOK_SENT_FOLDER;
import static com.tlc.email.server.internal.field.EmailKeys.OUTLOOK_SENT_ITEMS;
import static com.tlc.email.common.model.type.Email.MessageCategory.INBOUND;
import static com.tlc.email.common.model.type.Email.State.ACTIVE;
import static com.tlc.email.common.model.type.Email.State.UNARCHIVE;

/**
 * <p>
 * Provides the email service related to outlook.
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class OutlookService implements EmailProviderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutlookService.class);

    private static final String TO_RECIPIENTS = "toRecipients";
    private static final String CC_RECIPIENTS = "ccRecipients";
    private static final String BCC_RECIPIENTS = "bccRecipients";

    private static int LIMIT;

    private final EmailGraphMessageConverter emailGraphMessageConverter;
    private final GraphEmailBuilder graphEmailBuilder;

    private OutlookService() {
        final Configurations configurations = ConfHolder.getInstance().getProperties(EmailKeys.SYNC_SERVICE.getKey()).getNestedConfigs(SyncServiceFields.OUTLOOK);

        this.emailGraphMessageConverter = EmailGraphMessageConverter.getInstance();
        this.graphEmailBuilder = GraphEmailBuilder.getInstance();

        LIMIT = configurations.getInt(EmailKeys.MESSAGE_BATCH_SIZE.getKey());
    }

    private static class Instance {
        private static final OutlookService INSTANCE = new OutlookService();
    }

    /**
     * <p>
     * Gets the {@link OutlookService} Instance
     * </p>
     *
     * @return {@link OutlookService}
     */
    public static OutlookService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Builds a URL for retrieving messages from a specified mail folder
     * </p>
     *
     * @param accessToken Represents the OAuth access token used for authentication
     * @param dateFilter  A filter restrict messages based on date criteria
     * @return The URL for the messages
     */
    private String buildUrl(final String accessToken, final String dateFilter, final FolderType folderType) {
        final String folder = switch (folderType) {
            case INBOX -> OUTLOOK_INBOX.getKey();
            case SENT -> OUTLOOK_SENT_ITEMS.getKey();
            case DRAFT -> OUTLOOK_DRAFTS.getKey();
            case ARCHIVE -> ARCHIVE.getKey();
        };

        if (FolderType.ARCHIVE == folderType) {
            return getUserRequest(accessToken).mailFolders(folder).messages().buildRequest().top(LIMIT)
                    .expand(NotificationKeys.ATTACHMENTS.getKey()).select(getHeaders()).getRequestUrl().toString();
        }

        return getUserRequest(accessToken).mailFolders(folder).messages().buildRequest().filter(dateFilter)
                .top(LIMIT).expand(NotificationKeys.ATTACHMENTS.getKey()).select(getHeaders()).getRequestUrl().toString();
    }

    /**
     * <p>
     * Retrieves the emails from the provider for the provided details in email
     * </p>
     *
     * @param emailConfiguration Refers to the {@link EmailConfiguration}
     * @return {@link Collection} of {@link Email} messages
     */
    @Override
    public EmailProviderDataStore receive(final EmailConfiguration emailConfiguration) {
        final EmailTokenHandler tokenHandler = EmailTokenHandler.getInstance();
        final SyncAddressInfo emailAddress = emailConfiguration.getSyncEmailAddress();
        final String accessToken = tokenHandler.getAccessToken(emailAddress);
        final OutlookDataStore emailProviderDataStore = new OutlookDataStore(accessToken, emailConfiguration);

        try {
            if (emailAddress.getConsentType() == ConsentType.USER_GRANTED) {
                final EmailReceiverConfiguration receiverConfiguration = emailConfiguration.get();
                final FolderType folderType = receiverConfiguration.getFirstFolderType();

                Optional.ofNullable(folderType)
                        .map(folder -> receiverConfiguration.getEmailIntervals().get(folder))
                        .map(this::getDateFilter)
                        .map(dateFilter -> buildUrl(accessToken, dateFilter, folderType))
                        .ifPresent(url -> {
                            emailProviderDataStore.setUrl(url);
                            emailProviderDataStore.setFolderType(folderType);

                            loadEmailProviderData(emailProviderDataStore);
                        });
            } else {
                final String folder = getFolder(FolderType.INBOX);
                final GraphServiceClient<Request> graphClient = GraphClient.build(accessToken);
                final TenantConfiguration tenantConfiguration = emailConfiguration.get();
                final String url = graphClient.users(tenantConfiguration.getEmailUserId())
                        .mailFolders(folder).messages().buildRequest().select(getHeaders()).getRequestUrl().toString();

                if (Objects.nonNull(url)) {
                    final JsonArray messages = getValueAsArray(url, accessToken);
                    final Collection<Email> graphMessage = buildEmailGraphMessage(messages, accessToken, emailConfiguration, null);

                    emailProviderDataStore.setEmails(graphMessage);
                }
            }
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Email sync: Failed to retrieve emails from the provider. reason: {}, stack: {}", exception.getMessage(), exception);
            throw ErrorCode.get(EmailErrorCodes.EMAIL_RETRIEVE_FAILED, exception);
        }

        return emailProviderDataStore;
    }

    /**
     * <p>
     * Gets the folder name for the given {@link FolderType}
     * </p>
     *
     * @param folder Represents the {@link FolderType}
     * @return The folder name
     */
    private String getFolder(final FolderType folder) {
        return switch (folder) {
            case INBOX -> OUTLOOK_INBOX.getKey();
            case SENT -> OUTLOOK_SENT_FOLDER.getKey();
            case DRAFT -> OUTLOOK_DRAFTS.getKey();
            case ARCHIVE -> ARCHIVE.getKey();
        };
    }

    /**
     * <p>
     * Fetches data from a given URL using the provided access token for authorization
     * </p>
     *
     * @param uri         The URL to send the request to
     * @param accessToken The OAuth token used to authenticate the request
     * @return The {@link JsonObject} containing the response data
     */
    private JsonObject getData(final String uri, final String accessToken) {
        try {
            final Map<String, String> headers = new HashMap<>();

            headers.put(NotificationKeys.AUTHORIZATION.getKey(), String.format("%s%s", NotificationKeys.BEARER.getKey(), accessToken));
            headers.put(NotificationKeys.ACCEPT.getKey(), NotificationKeys.APPLICATION_JSON.getKey());
            headers.put(NotificationKeys.CONTENT_TYPE.getKey(), NotificationKeys.APPLICATION_JSON.getKey());

            return HttpUtil.makeRequest(new HttpRequest(new URL(uri), Collections.emptyMap(), HttpMethod.GET, headers));
        } catch (Exception exception) {
            LOGGER.debug("Email Outlook: The provided URL is either malformed or does not meet the required format. Reason: {}, Stack: ", exception.getMessage(), exception);
            throw ErrorCode.get(NotificationErrorCodes.INVALID_URL, exception);
        }
    }

    /**
     * <p>
     * Sends the email using smtp properties
     * </p>
     *
     * @param email Refers to the {@link Email} entity
     */
    @Override
    public void send(final Email email) {
        final SyncAddressInfo emailAddress = email.getSender();
        final String accessToken = EmailTokenHandler.getInstance().getAccessToken(email.getSender());
        final EmailRecipients emailRecipients = email.getEmailRecipients();
        final Collection<EmailAddress> toAddress = emailRecipients.toRecipients();
        final Collection<EmailAddress> bccAddress = emailRecipients.bccRecipients();
        final Collection<EmailAddress> ccAddress = emailRecipients.ccRecipients();

        final Message message = new Message();
        message.subject = email.getSubject();
        final ItemBody body = new ItemBody();
        body.contentType = BodyType.HTML;
        body.content = email.getContent();
        message.body = body;
        message.conversationId = email.getMetaData().getConversationThreadId();

        if (Objects.nonNull(toAddress)) {
            if (toAddress.isEmpty()) {
                message.additionalDataManager().put(TO_RECIPIENTS, new com.google.gson.JsonArray());
            } else {
                message.toRecipients = getRecipients(toAddress);
            }
        }

        if (Objects.nonNull(ccAddress)) {
            if (ccAddress.isEmpty()) {
                message.additionalDataManager().put(CC_RECIPIENTS, new com.google.gson.JsonArray());
            } else {
                message.ccRecipients = getRecipients(ccAddress);
            }
        }

        if (Objects.nonNull(bccAddress)) {
            if (bccAddress.isEmpty()) {
                message.additionalDataManager().put(BCC_RECIPIENTS, new com.google.gson.JsonArray());
            } else {
                message.bccRecipients = getRecipients(bccAddress);
            }
        }
        final Collection<File> files = email.getFiles();

        if (Objects.nonNull(files) && !files.isEmpty()) {
            final List<Attachment> attachments = new ArrayList<>();

            for (final File file : files) {

                try (final InputStream inputStream = new BufferedInputStream(file.inputStream())) {
                    final byte[] buffer = new byte[1024];
                    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    int readOffset;

                    while ((readOffset = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, readOffset);
                    }
                    final FileAttachment fileAttachment = new FileAttachment();

                    fileAttachment.name = file.name();
                    fileAttachment.contentType = file.mediaType();
                    fileAttachment.size = Math.toIntExact(file.size());
                    fileAttachment.contentBytes = byteArrayOutputStream.toByteArray();
                    fileAttachment.oDataType = "#microsoft.graph.fileAttachment";

                    attachments.add(fileAttachment);
                } catch (IOException exception) {
                    throw ErrorCode.get(EmailErrorCodes.FILE_UPLOAD_FAILED, exception);
                }
            }
            final AttachmentCollectionResponse attachmentCollectionResponse = new AttachmentCollectionResponse();

            attachmentCollectionResponse.value = attachments;
            message.attachments = new AttachmentCollectionPage(attachmentCollectionResponse, null);
        }
        final ConsentType consentType = emailAddress.getConsentType();

        if (Objects.requireNonNull(consentType) == ConsentType.USER_GRANTED) {
            sendUserMail(accessToken, email, message);
        } else {
            throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_SETTINGS);
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
        final SyncAddressInfo syncAddressInfo = updateRequest.getSyncEmailAddress();
        final String accessToken = EmailTokenHandler.getInstance().getAccessToken(syncAddressInfo);
        final State state = emailStateTransitionInfo.stream().map(EmailStateTransitionInfo::getNewState).findFirst().orElseGet(() -> null);

        if (Objects.nonNull(state)) {
            final String folderName = switch (state) {
                case ACTIVE, INACTIVE -> EmailKeys.DELETED_ITEMS.getKey();
                case ARCHIVE -> ARCHIVE.getKey();
                default -> null;
            };

            if (Objects.nonNull(folderName)) {
                final String destinationId = getFolderId(accessToken, folderName, null);

                updateGraphMessages(emailStateTransitionInfo, accessToken, destinationId);
            } else {
                updateGraphMessages(emailStateTransitionInfo, accessToken, null);
            }
            updateRequest.setState(state);
        }
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
        final OutlookDataStore providerDataStore = emailProviderDataStore.get();
        final String url = providerDataStore.getUrl();
        final String accessToken = providerDataStore.getAccessToken();
        final EmailConfiguration emailConfiguration = providerDataStore.getEmailReceiverConfiguration();
        final FolderType folderType = providerDataStore.getFolderType();
        final JsonObject jsonObject = getData(url, accessToken);
        final Collection<Email> emails = new HashSet<>();

        if (Objects.nonNull(jsonObject)) {
            final String valueKey = NotificationKeys.VALUE.getKey();
            final String nextLinkKey = NotificationKeys.NEXT_LINK.getKey();

            if (jsonObject.containsKey(valueKey)) {
                final JsonArray messages = jsonObject.getJsonArray(valueKey);

                emails.addAll(buildEmailGraphMessage(messages, accessToken, emailConfiguration, folderType));
            }

            providerDataStore.setAccessToken(accessToken);
            providerDataStore.setEmailReceiverConfiguration(emailConfiguration);
            providerDataStore.setFolderType(folderType);
            providerDataStore.setEmails(emails);

            if (jsonObject.containsKey(nextLinkKey)) {
                providerDataStore.setNextLink(true);
                providerDataStore.setUrl(jsonObject.getString(nextLinkKey));
            } else {
                providerDataStore.setNextLink(false);
            }
        } else {
            providerDataStore.setNextLink(false);
        }
    }

    /**
     * <p>
     * Fetches the folder id inorder to store in DB for further update action
     * </p>
     *
     * @param accessToken Represents the access token
     * @param folderName  Refers to the name of the folder
     * @return {@link String} folder id
     */
    private String getFolderId(final String accessToken, final String folderName, final EmailConfiguration configuration) {
        final String filterQuery = String.format("displayName eq '%s'", folderName);
        final String url;

        if (Objects.isNull(configuration)) {
            url = getUserRequest(accessToken).mailFolders().buildRequest().filter(filterQuery).getRequestUrl().toString();
        } else {
            final TenantConfiguration tenantConfiguration = configuration.get();

            url = GraphClient.build(accessToken).users(tenantConfiguration.getEmailUserId()).mailFolders()
                    .buildRequest().filter(filterQuery).getRequestUrl().toString();
        }
        final JsonArray mailFolders = getValueAsArray(url, accessToken);

        if (Objects.nonNull(mailFolders) && mailFolders.size() > 0) {
            return mailFolders.getJsonObject(0).getString(NotificationKeys.ID.getKey());
        }

        return null;
    }

    /**
     * <p>
     * Fetches the email headers
     * </p>
     *
     * @return The email headers
     */
    private String getHeaders() {
        return String.format("%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s",
                EmailHeaderFields.ID, OutlookHeaderFields.FROM, EmailHeaderFields.TO_RECIPIENTS,
                EmailHeaderFields.BCC_RECIPIENTS, EmailHeaderFields.CC_RECIPIENTS, EmailHeaderFields.SUBJECT,
                EmailHeaderFields.BODY, EmailHeaderFields.SENDER, EmailHeaderFields.IS_READ,
                EmailHeaderFields.IS_DRAFT, EmailHeaderFields.HAS_ATTACHMENTS, EmailHeaderFields.SENT_DATE_TIME,
                EmailHeaderFields.RECEIVED_DATE_TIME, EmailHeaderFields.CONVERSATION_ID, EmailHeaderFields.PARENT_FOLDER_ID,
                EmailHeaderFields.INTERNET_MESSAGE_ID, EmailHeaderFields.INTERNET_MESSAGE_HEADERS);
    }

    /**
     * <p>
     * Updates the message and move to respective folder based on the state chosen
     * </p>
     *
     * @param emailStateTransitionInfo has the state transition attributes
     * @param accessToken              Refers to the access token
     * @param folderId                 Refers the destination folder to be placed
     */
    private void updateGraphMessages(final Collection<EmailStateTransitionInfo> emailStateTransitionInfo, final String accessToken,
                                     final String folderId) {
        try {
            final String messageRequestUrl = getUserRequest(accessToken).messages().getRequestUrl();

            for (final EmailStateTransitionInfo stateTransitionInfo : emailStateTransitionInfo) {
                final State state = stateTransitionInfo.getNewState();
                final String destinationFolderId = state == ACTIVE || state == UNARCHIVE ?
                        stateTransitionInfo.getParentFolderId() : folderId;

                if (Objects.nonNull(destinationFolderId)) {
                    final URL url = new URL(String.format("%s%s%s%s", messageRequestUrl, "/", stateTransitionInfo.getOldMessageId(), "/move"));
                    final Map<String, String> headers = new HashMap<>();
                    final Map<String, String> requestParams = new HashMap<>();

                    headers.put(NotificationKeys.AUTHORIZATION.getKey(), String.format("%s%s", NotificationKeys.BEARER.getKey(), accessToken));
                    headers.put(NotificationKeys.ACCEPT.getKey(), NotificationKeys.APPLICATION_JSON.getKey());
                    headers.put(NotificationKeys.CONTENT_TYPE.getKey(), NotificationKeys.APPLICATION_JSON.getKey());
                    requestParams.put(NotificationKeys.REQUEST_BODY.getKey(), String.format("%s%s%s", "{\"destinationId\":\"", destinationFolderId, "\"}"));
                    final JsonObject response = HttpUtil.makeRequest(new HttpRequest(url, requestParams, HttpMethod.POST, headers));

                    stateTransitionInfo.setNewMessageId(response.getString(NotificationKeys.ID.getKey()));
                }
            }
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.MAIL_UPDATE_FAILED, exception);
        }
    }

    /**
     * <p>
     * Gets the user request for getting the endpoint url
     * </p>
     *
     * @param accessToken Represents the access token
     * @return {@link UserRequestBuilder}
     */
    private UserRequestBuilder getUserRequest(final String accessToken) {
        return GraphClient.build(accessToken).me();
    }

    /**
     * <p>
     * Gets the output for required request
     * </p>
     *
     * @param uri         Represents the uri
     * @param accessToken Represents the access token
     * @return {@link JsonArray}
     */
    private JsonArray getValueAsArray(final String uri, final String accessToken) {
        try {
            final Map<String, String> headers = new HashMap<>();
            final String applicationJsonKey = NotificationKeys.APPLICATION_JSON.getKey();

            headers.put(NotificationKeys.AUTHORIZATION.getKey(), String.format("%s%s", NotificationKeys.BEARER.getKey(), accessToken));
            headers.put(NotificationKeys.ACCEPT.getKey(), applicationJsonKey);
            headers.put(NotificationKeys.CONTENT_TYPE.getKey(), applicationJsonKey);
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(uri), Collections.emptyMap(), HttpMethod.GET, headers));

            return response.optJsonArray(NotificationKeys.VALUE.getKey());
        } catch (Exception exception) {
            LOGGER.debug("Email Outlook: The provided URL is either malformed or does not meet the required format. Reason: {}, Stack: ", exception.getMessage(), exception);
            throw ErrorCode.get(NotificationErrorCodes.INVALID_URL, exception);
        }
    }

    /**
     * <p>
     * Gets the email date filter
     * </p>
     *
     * @param interval Represents the  {@link EmailInterval}
     * @return the date filter
     */
    private String getDateFilter(final EmailInterval interval) {

        if (Objects.nonNull(interval)) {
            final OffsetDateTime startSyncDate = OffsetDateTime.ofInstant(Instant.ofEpochMilli(interval.startTime()), ZoneOffset.UTC);
            final OffsetDateTime endSyncDate = OffsetDateTime.ofInstant(Instant.ofEpochMilli(interval.endTime()), ZoneOffset.UTC);

            return String.format("sentDateTime le %s and sentDateTime ge %s", endSyncDate, startSyncDate);
        }

        return null;
    }

    /**
     * <p>
     * Builds the email graph message.
     * </p>
     *
     * @param messages      Represents the {@link JsonArray} of messages
     * @param accessToken   Refers to the access token
     * @param configuration Refers to the {@link EmailConfiguration}
     * @param folderType    Represents the {@link FolderType}
     */
    private Collection<Email> buildEmailGraphMessage(final JsonArray messages, final String accessToken, final EmailConfiguration configuration, final FolderType folderType) {
        final Collection<Email> emails = new ArrayList<>();
        final int messageSize = messages.size();
        final SyncAddressInfo syncAddressInfo = configuration.getSyncEmailAddress();
        final EmailConfiguration emailConfiguration = ConsentType.USER_GRANTED == syncAddressInfo.getConsentType() ? null : configuration;
        final boolean isFolderTypeNull = Objects.isNull(folderType);
        final Optional<String> optionalFolderId = isFolderTypeNull ? Optional.empty() :
                Optional.ofNullable(getFolderId(accessToken, getFolder(folderType), emailConfiguration));
        final boolean isArchive = FolderType.ARCHIVE == folderType;
        final String sentFolderId = !isFolderTypeNull && isArchive ?
                getFolderId(accessToken, OUTLOOK_SENT_FOLDER.getKey(), emailConfiguration) : null;

        for (int index = 0; index < messageSize; index++) {
            final JsonObject messageJson = messages.getJsonObject(index);

            try {
                final EmailGraphMessage emailGraphMessage = emailGraphMessageConverter.convert(messageJson);
                final String parentFolderId = emailGraphMessage.getParentFolderId();

                if (Objects.isNull(emailGraphMessage.getFrom())) {

                    if (!isFolderTypeNull && folderType.isSentOrDraft()
                        || (isArchive && (emailGraphMessage.isDraft() ||
                               (Objects.nonNull(parentFolderId) && Objects.equals(parentFolderId, sentFolderId))))) {
                        emailGraphMessage.setFrom(syncAddressInfo);
                    } else {
                        final String messageId = emailGraphMessage.getId();
                        final String fromAddress = graphEmailBuilder.getFromAddressFromHeaders(GraphClient.build(accessToken), messageId);

                        if (Objects.nonNull(fromAddress)) {
                            emailGraphMessage.setFrom(new SyncAddressInfo(fromAddress));
                        } else {
                            LOGGER.error("Email Outlook: Failed to retrieve the sender's email address from the message headers for message ID: {}, folderType: {}",
                                    messageId, isFolderTypeNull ? "unknown" : folderType.name());
                            throw ErrorCode.get(EmailErrorCodes.INVALID_FROM_ADDRESS);
                        }
                    }
                }
                final Email email = graphEmailBuilder.buildEmail(emailGraphMessage, accessToken, configuration);
                final EmailMetaData metaData = email.getMetaData();

                if (optionalFolderId.isPresent()) {

                    if (isArchive) {
                        final String senderAddress = email.getSender().getEmailAddress();
                        final String syncedEmailAddress = syncAddressInfo.getEmailAddress();

                        if (emailGraphMessage.isDraft()) {
                            metaData.setParentFolderId(parentFolderId);
                        } else if (Objects.equals(senderAddress, syncedEmailAddress)) {
                            metaData.setParentFolderId(sentFolderId);
                        } else {
                            final String inboundFolderId = getFolderId(accessToken, OUTLOOK_INBOX.getKey(), emailConfiguration);

                            metaData.setParentFolderId(inboundFolderId);
                        }
                    } else {
                        metaData.setParentFolderId(optionalFolderId.get());
                    }
                }

                if (Objects.nonNull(folderType)) {
                    final MessageCategory messageCategory = MessageCategory.getMessageCategory(folderType);

                    email.setMessageCategory(messageCategory);
                    metaData.setDate(INBOUND == messageCategory ? emailGraphMessage.getReceivedDateTime() : emailGraphMessage.getSentDateTime());
                }
                emails.add(email);
            } catch (final Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, "Email Outlook: Failed to retrieve emails from the provider. Reason: {}, Stack: ", throwable.getMessage());
                throw ErrorCode.get(EmailErrorCodes.EMAIL_RETRIEVE_FAILED, throwable);
            }
        }

        return emails;
    }

    /**
     * <p>
     * Performs the delete action for the provided email and message ids.
     * </p>
     *
     * @param emailStateTransitionInfo Represent the email sate transition info.
     * @param emailAddress             Refers the email address.
     */
    @Override
    public void delete(final EmailStateTransitionInfo emailStateTransitionInfo, final String emailAddress) {
        final String accessToken = EmailTokenHandler.getInstance().getAccessToken(emailAddress);
        final GraphServiceClient<Request> graphServiceClient = GraphServiceClient.builder().authenticationProvider(
                (token) -> CompletableFuture.completedFuture(accessToken)).buildClient();

        graphServiceClient.me().messages(emailStateTransitionInfo.getOldMessageId()).buildRequest().delete();
    }

    /**
     * <p>
     * Sends the mail with synced email credentials
     * </p>
     *
     * @param accessToken Represents the authorized token
     * @param email       Represents the {@link Email}
     * @param message     Represents the {@link Message}
     */
    private void sendUserMail(final String accessToken, final Email email, final Message message) {
        final GraphServiceClient<Request> graphClient = GraphClient.build(accessToken);

        if (email.isDraft()) {
            handleDraftEmail(graphClient, email, message, accessToken);
        } else {
            handleOutboundEmail(graphClient, email, message, accessToken);
        }
    }

    /**
     * Handles the draft email actions based on the specified draft action.
     * Delegates the appropriate action to the corresponding method.
     *
     * @param graphClient The GraphServiceClient used to interact with Microsoft Graph API.
     * @param email       The email containing metadata and draft action details.
     * @param message     The Message to be processed.
     * @param accessToken The access token for authenticating requests.
     */
    private void handleDraftEmail(final GraphServiceClient<Request> graphClient, final Email email, final Message message, final String accessToken) {
        final DraftEmail.DraftAction draftAction = ((DraftEmail) email).getDraftAction();
        final EmailMetaData metaData = email.getMetaData();

        switch (draftAction) {
            case CREATE -> createDraft(graphClient, message, metaData);
            case UPDATE -> updateDraft(graphClient, message, metaData);
            case SEND -> sendDraft(graphClient, message, metaData, accessToken);
            default -> throw ErrorCode.get(EmailErrorCodes.INVALID_DRAFT_ACTION);
        }
    }

    /**
     * Creates a draft email in the Outlook Drafts folder using the Microsoft Graph API.
     * Which sends a request to create a draft message and updates the provided
     * {@link EmailMetaData} object with the draft's details, such as the message ID,
     * parent folder ID, and external reference ID.
     *
     * @param graphClient the {@link GraphServiceClient} used to interact with the Microsoft Graph API
     * @param message     the {@link Message} containing the email content to be saved as a draft
     * @param metaData    the {@link EmailMetaData} to update with the draft email's metadata
     */
    private void createDraft(final GraphServiceClient<Request> graphClient, final Message message, final EmailMetaData metaData) {
        final Message draftMessage = graphClient.me().mailFolders(EmailKeys.OUTLOOK_DRAFTS.getKey())
                .messages().buildRequest().post(message);

        metaData.setConversationThreadId(draftMessage.conversationId);
        metaData.setMessageId(draftMessage.internetMessageId);
        metaData.setParentFolderId(draftMessage.parentFolderId);
        metaData.setExternalReference(draftMessage.id);
    }

    /**
     * Updates an existing draft email in the user's mailbox using the Microsoft Graph API.
     * Which sends a PATCH request to modify the draft email identified by the
     * external reference ID stored in the provided {@link EmailMetaData} object.
     *
     * @param graphClient the {@link GraphServiceClient} used to interact with the Microsoft Graph API
     * @param message     the {@link Message} containing the email content to be saved as a draft
     * @param metaData    the {@link EmailMetaData} to update with the draft email's metadata
     */
    private void updateDraft(final GraphServiceClient<Request> graphClient, final Message message, final EmailMetaData metaData) {
        graphClient.me().messages(metaData.getExternalReference()).buildRequest().patch(message);
    }

    /**
     * Sends a draft email and updates the metadata with the sent message's details.
     * Which uses the Microsoft Graph API to send an email draft, and then
     * retrieves the sent message from the Sent Items folder. The external reference
     * (message ID) and the parent folder ID are updated in the provided {@link EmailMetaData} instance.
     *
     * @param graphClient the {@link GraphServiceClient} used to interact with the Microsoft Graph API
     * @param message     the {@link Message} containing the email content to be saved as a draft
     * @param metaData    the {@link EmailMetaData} to update with the draft email's metadata
     * @param accessToken The access token for authenticating requests.
     */
    private void sendDraft(final GraphServiceClient<Request> graphClient, final Message message, final EmailMetaData metaData, final String accessToken) {
        final String externalRef = metaData.getExternalReference();

        if (Objects.nonNull(externalRef)) {
            try {
                graphClient.me().messages(externalRef).buildRequest().get();
                final MessageRequestBuilder messageRequestBuilder = graphClient.me().messages(externalRef);

                messageRequestBuilder.buildRequest().patch(message);
                messageRequestBuilder.send().buildRequest().post();
                final String url = graphClient.me().mailFolders(OUTLOOK_SENT_ITEMS.getKey()).messages().buildRequest().getRequestUrl().toString();

                if (Objects.nonNull(url)) {
                    final JsonArray messages = getValueAsArray(url, accessToken);

                    if (Objects.nonNull(messages) && messages.size() > 0) {
                        metaData.setExternalReference(messages.getJsonObject(0).getString(NotificationKeys.ID.getKey()));
                        metaData.setParentFolderId(getFolderId(accessToken, OUTLOOK_SENT_FOLDER.getKey(), null));
                    }
                }

                return;
            } catch (Exception exception) {
                LOGGER.debug("Outlook: draft external reference invalid or not found ({}). Falling back to fresh send. Reason: {}", externalRef, exception.getMessage());
            }
        }
        final Message dispatchedMessage = sendNewEmail(graphClient, message);

        if (Objects.isNull(dispatchedMessage.id)) {
            throw ErrorCode.get(EmailErrorCodes.EMAIL_SEND_FAILED);
        }

        metaData.setExternalReference(dispatchedMessage.id);
        metaData.setParentFolderId(getFolderId(accessToken, OUTLOOK_SENT_FOLDER.getKey(), null));
        metaData.setMessageId(dispatchedMessage.internetMessageId);
        metaData.setConversationThreadId(dispatchedMessage.conversationId);
        metaData.setDate(dispatchedMessage.sentDateTime.toInstant().toEpochMilli());
    }

    /**
     * Handles the sending of an outbound email, determining whether to send a new email
     * or a reply/forward based on the presence of an external reference (message ID).
     * Which checks if the provided {@link EmailMetaData} has an external reference
     * (message ID).
     *
     * @param graphClient The {@link GraphServiceClient} instance used to interact
     *                    with the Microsoft Graph API.
     * @param email       The {@link Email} that contains the email metadata and
     *                    the content of the email to be sent.
     * @param message     The {@link Message} that contains the email details,
     *                    such as subject and body.
     * @param accessToken The access token required for making API calls to Microsoft
     *                    Graph on behalf of the authenticated user.
     */
    private void handleOutboundEmail(final GraphServiceClient<Request> graphClient, final Email email, final Message message, final String accessToken) {
        final EmailMetaData metaData = email.getMetaData();
        final String messageId = metaData.getMessageId();
        final Message dispatchedMessage = Objects.isNull(messageId)
                ? sendNewEmail(graphClient, message)
                : sendReplyOrForward(graphClient, email, message, messageId, accessToken);
        final String dispatcherId = dispatchedMessage.id;
        final OffsetDateTime dispatchDateAndTime = dispatchedMessage.sentDateTime;

        if (Objects.isNull(dispatcherId) || Objects.isNull(dispatchDateAndTime)) {
            throw ErrorCode.get(EmailErrorCodes.EMAIL_SEND_FAILED);
        }
        graphClient.me().messages(dispatcherId).send().buildRequest().postAsync();
        final String internetMessageId = dispatchedMessage.internetMessageId;

        metaData.setExternalReference(fetchAlternateMessageId(graphClient, internetMessageId, accessToken));
        metaData.setParentFolderId(getFolderId(accessToken, OUTLOOK_SENT_FOLDER.getKey(), null));
        metaData.setMessageId(internetMessageId);
        metaData.setConversationThreadId(dispatchedMessage.conversationId);
        metaData.setDate(dispatchDateAndTime.toInstant().toEpochMilli());
    }

    /**
     * Sends a new email and updates the email metadata with the details of the sent message.
     * Which uses the Microsoft Graph API to send a new email message. Once the email
     * is sent, it retrieves the message ID, sent date, and other relevant details to update
     * the {@link EmailMetaData}. If the email cannot be sent successfully, it throws an error.
     *
     * @param graphClient The {@link GraphServiceClient} used to interact
     *                    with the Microsoft Graph API.
     * @param message     The {@link Message} containing the details of the email
     *                    to be sent (e.g., subject, body, recipient).
     * @return the {@link Message}
     */
    private Message sendNewEmail(final GraphServiceClient<Request> graphClient, final Message message) {
        return graphClient.me().mailFolders(OUTLOOK_SENT_ITEMS.getKey())
                .messages().buildRequest().post(message);
    }

    /**
     * Sends a reply, reply-all, or forwarded email based on the outbound type of the email.
     * Which handles sending a reply, reply-all, or forward of an existing email
     * using the Microsoft Graph API. The specific type of outbound email action is determined
     * by the {@link OutboundEmail} type (REPLY, REPLY_ALL, or FORWARD).
     *
     * @param graphClient The {@link GraphServiceClient} used to interact
     *                    with the Microsoft Graph API.
     * @param email       The {@link Email} containing metadata and message details.
     * @param message     The {@link Message} containing the content of the email
     *                    to be sent as a reply or forwarded message.
     * @param messageId   The ID of the original message being replied to or forwarded.
     * @param accessToken The access token required for making API calls to Microsoft
     *                    Graph on behalf of the authenticated user.
     * @return {@link Message}
     */
    private Message sendReplyOrForward(final GraphServiceClient<Request> graphClient, final Email email, final Message message, final String messageId, String accessToken) {
        final String alternateMessageId = fetchAlternateMessageId(graphClient, messageId, accessToken);

        if (Objects.isNull(alternateMessageId)) {
            throw ErrorCode.get(EmailErrorCodes.EMAIL_NOT_FOUND);
        }

        return switch (((OutboundEmail) email).getOutboundType()) {
            case REPLY -> graphClient.me().messages(alternateMessageId).createReply(
                    MessageCreateReplyParameterSet.newBuilder().withMessage(message).build()
            ).buildRequest().post();
            case REPLY_ALL -> graphClient.me().messages(alternateMessageId).createReplyAll(
                    MessageCreateReplyAllParameterSet.newBuilder().withMessage(message).build()
            ).buildRequest().post();
            case FORWARD -> graphClient.me().messages(alternateMessageId).createForward(
                    MessageCreateForwardParameterSet.newBuilder().withMessage(message).build()
            ).buildRequest().post();
            default -> throw ErrorCode.get(EmailErrorCodes.INVALID_OUTBOUND_TYPE);
        };
    }

    /**
     * <p>
     * Fetches the alternate message id for the provided internet message id
     * </p>
     *
     * @param graphClient Represents the authorized graph client
     * @param messageId   Represents the internet message id
     * @param accessToken Represents the access token
     * @return {@link String} having the alternate message id user for further mail process
     */
    private String fetchAlternateMessageId(final GraphServiceClient<Request> graphClient, final String messageId,
                                           final String accessToken) {
        try {
            final String encodedMessageId = URLEncoder.encode(String.format("%s%s%s", " eq '", messageId, "'"), StandardCharsets.UTF_8);
            final String url = String.format("%s%s%s", graphClient.me().messages().getRequestUrl(),
                    "?", encodedMessageId);
            final Map<String, String> headers = new HashMap<>();

            headers.put(NotificationKeys.AUTHORIZATION.getKey(), String.format("%s%s", NotificationKeys.BEARER.getKey(), accessToken));
            headers.put(NotificationKeys.ACCEPT.getKey(), NotificationKeys.APPLICATION_JSON.getKey());
            final JsonObject responseJson = HttpUtil.makeRequest(new HttpRequest(new URL(url), Collections.emptyMap(), HttpMethod.GET, headers));
            final JsonArray value = responseJson.optJsonArray(NotificationKeys.VALUE.getKey());

            if (Objects.nonNull(value)) {
                return value.getJsonObject(0).getString(NotificationKeys.ID.getKey());
            }
        } catch (Exception exception) {
            LOGGER.debug("Email Outlook: Failed to retrieve message ID from the provider. Reason: {}, Stack: ", exception.getMessage(), exception);
            throw ErrorCode.get(EmailErrorCodes.EMAIL_RETRIEVE_FAILED, exception);
        }

        return null;
    }

    /**
     * <p>
     * Gets the recipients for sending emails by means of outlook graph
     * </p>
     *
     * @param recipients Represents the collection of recipients
     */
    private List<Recipient> getRecipients(final Collection<EmailAddress> recipients) {
        final List<Recipient> recipientsList = new ArrayList<>();

        recipients.forEach(address -> {
            final com.microsoft.graph.models.EmailAddress emailAddress = new com.microsoft.graph.models.EmailAddress();

            for (final String email : address.getEmailAddress().split(",")) {
                final Recipient mailRecipients = new Recipient();

                if (!email.isBlank()) {
                    emailAddress.address = email.trim();
                    mailRecipients.emailAddress = emailAddress;

                    recipientsList.add(mailRecipients);
                }
            }
        });

        return recipientsList;
    }

    /**
     * <p>
     * Retrieves all attachments for the given Outlook message ID associated with
     * the specified synced email account.
     * </p>
     *
     * @param emailAddress Represents the synced email account information.
     * @param messageId    The Outlook message ID whose attachments must be retrieved.
     * @return A collection of {@link Attachment} objects, or an empty collection if the retrieval fails.
     */
    public Collection<Attachment> getAttachmentsByMessageId(final SyncAddressInfo emailAddress, final String messageId) {
        final String accessToken = EmailTokenHandler.getInstance().getAccessToken(emailAddress);

        return getAllAttachments(emailAddress, GraphClient.build(accessToken), messageId);
    }

    /**
     * <p>
     * Retrieves all attachments for a given Outlook message using the provided
     * {@link SyncAddressInfo}.
     * </p>
     *
     * @param syncAddressInfo Represents the synced email account information.
     * @param messageId       The Outlook message ID for which attachments must be retrieved.
     * @return A collection of {@link Attachment} objects for the specified message,
     * or an empty collection if retrieval fails.
     */
    private Collection<Attachment> getAllAttachments(final SyncAddressInfo syncAddressInfo, final GraphServiceClient<Request> graphServiceClient,
                                                     final String messageId) {
        final Collection<Attachment> allAttachments = new ArrayList<>();

        AttachmentCollectionPage currentPage = graphServiceClient
                .users(syncAddressInfo.getEmailAddress())
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
}