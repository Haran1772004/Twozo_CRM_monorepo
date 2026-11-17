package com.tlc.email.server.listener;

import com.tlc.attachment.storage.model.ObjectDownloadRequest;
import com.tlc.attachment.storage.model.ObjectDownloadResponse;
import com.tlc.attachment.storage.model.ObjectVersionDeleteRequest;
import com.tlc.commons.executor.Executor;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.StringUtil;
import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.ConsentType;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.ServiceType;
import com.tlc.email.common.model.StateData;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.model.config.TenantConfiguration;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.request.TenantSyncRequest;
import com.tlc.email.common.resolver.EmailServiceResolver;
import com.tlc.email.common.service.AutoForwardingEmailService;
import com.tlc.email.common.service.EmailAddOnService;
import com.tlc.email.common.service.TeamInboxService;
import com.tlc.email.server.fields.InboundEmailReceiverFields;
import com.tlc.email.server.internal.converter.EmailMimeMessageConverter;
import com.tlc.email.server.internal.storage.InboundEmailProcessingTracker;
import com.tlc.email.server.internal.storage.InboundEmailStorageService;
import com.tlc.email.server.internal.sync.AbstractSyncActionHandler;
import com.tlc.email.server.service.EmailMapper;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Listens for inbound email events from the message stream and processes the corresponding
 * email objects stored in the configured storage. Only processes emails whose mailbox address
 * env marker matches this instance's configured environment (or carries no env marker at all);
 * mismatched emails are skipped and left for the matching environment's listener to handle.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class InboundEmailActionListener extends AbstractSyncActionHandler implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InboundEmailActionListener.class);

    private static final String VERSION_ID = "versionId";
    private static final char AT_SYMBOL = '@';

    private final EmailMimeMessageConverter emailMimeMessageConverter;
    private final InboundEmailStorageService inboundEmailStorageService;
    private final InboundEmailProcessingTracker processingTracker;
    private final Executor executor;
    private final EmailAddOnService teamInboxService;
    private final AutoForwardingEmailService autoForwardingEmailService;
    private final String domain;

    /**
     * <p>
     * Initializes the inbound email listener and its required services.
     * </p>
     *
     * @param executor executor used for asynchronous email processing
     */
    public InboundEmailActionListener(final Executor executor) {
        this.emailMimeMessageConverter = EmailMimeMessageConverter.getInstance();
        this.inboundEmailStorageService = InboundEmailStorageService.getInstance();
        this.processingTracker = InboundEmailProcessingTracker.getInstance();
        this.executor = executor;
        this.teamInboxService = TeamInboxService.getInstance();
        this.autoForwardingEmailService = AutoForwardingEmailService.getInstance();
        this.domain = ConfHolder.getInstance()
                .getProperties(EmailFields.EMAIL)
                .getNestedConfigs(EmailFields.ADD_ON_SERVICES)
                .getConfig(EmailFields.ENV)
                .trim();
    }

    /**
     * <p>
     * Listens for an inbound email event, downloads the corresponding email object,
     * and processes the email asynchronously.
     * </p>
     *
     * @param streamMessage inbound email stream message
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        executor.execute(() -> {
            String objectId = null;

            try {
                final JsonObject requestParameters = Json.object(streamMessage.data());
                final JsonObject data = resolveData(requestParameters);

                final String versionId = data.optString(VERSION_ID, null);
                objectId = data.getString(InboundEmailReceiverFields.OBJECT_ID);
                final String bucketName = data.getString(InboundEmailReceiverFields.BUCKET_NAME);

                if (!inboundEmailStorageService.getBucketName().equals(bucketName)) {
                    LOGGER.warn("Inbound Email: Ignoring message for unexpected bucketName={}, expected={}",
                            bucketName, inboundEmailStorageService.getBucketName());
                    streamMessage.clean();
                    return;
                }

                if (!processingTracker.tryAcquire(objectId)) {
                    LOGGER.info("Inbound Email: Skipping duplicate in-flight objectId={}", objectId);
                    streamMessage.clean();
                    return;
                }

                final String processingObjectId = objectId;
                final ObjectDownloadRequest objectDownloadRequest = new ObjectDownloadRequest(versionId, processingObjectId);

                LOGGER.info("Inbound Email: Downloading object with versionId={} and objectId={}", versionId, processingObjectId);

                inboundEmailStorageService.getObjectStorageService()
                        .downloadObjectAsync(objectDownloadRequest)
                        .thenAccept(objectDownloadResponse -> executor.execute(() -> {
                            try {
                                processDownloadedEmail(processingObjectId, versionId, objectDownloadResponse);
                            } finally {
                                streamMessage.clean();
                            }
                        }))
                        .exceptionally(throwable -> {
                            LoggerUtil.logError(LOGGER, throwable,
                                    "Inbound Email: Download failed for objectId={}, message={}, trace=",
                                    processingObjectId, throwable.getMessage(), throwable);
                            processingTracker.release(processingObjectId);
                            streamMessage.clean();
                            return null;
                        });
            } catch (Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable,
                        "Email: Listener failed to process the email stream message. message={}, trace=",
                        throwable.getMessage(), throwable);

                if (Objects.nonNull(objectId)) {
                    processingTracker.release(objectId);
                }

                streamMessage.clean();
            }
        });
    }

    /**
     * <p>
     * Processes the downloaded email, resolves its destination service, synchronizes the email,
     * and deletes the processed email object. If the mailbox address carries an env marker that
     * does not match this instance's configured env, the email is skipped and the underlying
     * object is left untouched so the matching environment's listener can process it.
     * </p>
     *
     * @param objectId               identifier of the email object
     * @param versionId              version identifier of the email object
     * @param objectDownloadResponse downloaded email object response
     */
    private void processDownloadedEmail(final String objectId, final String versionId,
                                        final ObjectDownloadResponse objectDownloadResponse) {
        try {
            LOGGER.info("Inbound Email: Successfully downloaded raw email content for objectId={}", objectId);
            Email email = emailMimeMessageConverter.convert(objectDownloadResponse.inputStream());

            final String destinationAddress = email.getMetaData().getCurrentMailboxId();

            if (Objects.nonNull(destinationAddress)) {
                final String mailBoxEnvDomain = extractDomain(destinationAddress);

                if ((mailBoxEnvDomain == null && domain != null) || !isDomainMatch(mailBoxEnvDomain)) {
                    LOGGER.info("Inbound Email: Skipping objectId={} - mailbox env={} does not match configured env={}",
                            objectId, mailBoxEnvDomain, domain);
                    return;
                }

                final TenantConfiguration tenantConfiguration = new TenantConfiguration();
                final SyncAddressInfo emailAddress = new SyncAddressInfo();

                emailAddress.setConsentType(ConsentType.TENANT);
                final Optional<ServiceType> optionalServiceType = EmailServiceResolver.resolveServiceType(destinationAddress);

                if (optionalServiceType.isPresent()) {
                    final ServiceType serviceType = optionalServiceType.get();

                    switch (serviceType) {
                        case TEAM_INBOX -> {
                            final EmailAddress account = teamInboxService.getAccount();
                            final String currentMailBox = getEmailAddress(email);

                            emailAddress.setEmailAddress(Objects.isNull(currentMailBox) ? account.getEmailAddress() : currentMailBox);
                            email = EmailMapper.toInbound(email);
                            final EmailMetaData emailMetaData = email.getMetaData();

                            emailMetaData.setAutoForwardingAddress(
                                    autoForwardingEmailService.extractForwardingAddress(email.getSubject())
                            );

                            tenantConfiguration.setFolderTypes(List.of(FolderType.INBOX));
                            tenantConfiguration.setSyncEmailAddress(emailAddress);
                        }
                        case SMART_BCC -> {
                            emailAddress.setEmailAddress(email.getSender().getEmailAddress());
                            email = EmailMapper.toOutbound(email);

                            tenantConfiguration.setFolderTypes(List.of(FolderType.SENT));
                            tenantConfiguration.setSyncEmailAddress(emailAddress);
                        }
                    }

                    loadEmails(
                            serviceType, List.of(email), new StateData(), new TenantSyncRequest(tenantConfiguration)
                    );
                }
            }

            inboundEmailStorageService.getObjectStorageService()
                    .deleteObjectVersionAsync(new ObjectVersionDeleteRequest(objectId, versionId))
                    .join();

            LOGGER.info("Inbound Email: Deleted object after successful processing, objectId={}", objectId);
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable,
                    "Inbound Email: Processing failed for objectId={}, message={}, trace=",
                    objectId, throwable.getMessage(), throwable);
        } finally {
            processingTracker.release(objectId);
        }
    }

    /**
     * <p>
     * Determines whether the mailbox's env marker matches this instance's configured domain.
     * A mailbox address with no env marker (e.g. no {@code (env)} segment) is always treated
     * as a match, since it carries no environment restriction.
     * </p>
     *
     * @param mailboxEnv env marker parsed from the mailbox address, or {@code null} if absent
     * @return {@code true} if the email should be processed by this instance
     */
    private boolean isDomainMatch(final String mailboxEnv) {
        return mailboxEnv == null || mailboxEnv.equalsIgnoreCase(domain);
    }

    /**
     * <p>
     * Extracts the domain marker from a mailbox address of the form
     * {@code name-domain-marker@domain}, e.g. {@code yasar-twozo-net@twozo.net}
     * resolves to {@code twozo}.
     * </p>
     *
     * @param mailboxAddress the full mailbox address
     * @return the parsed domain key, or {@code null} if the address has no domain marker
     */
    private String extractDomain(final String mailboxAddress) {
        if (mailboxAddress == null) {
            return null;
        }
        final int atIndex = mailboxAddress.indexOf(AT_SYMBOL);
        final String localPart = atIndex > 0 ? mailboxAddress.substring(0, atIndex) : mailboxAddress;
        final int openIndex = localPart.indexOf(StringUtil.Delimiter.HYPHEN.getDelimiter());
        final int closeIndex = openIndex >= 0 ? localPart.indexOf(StringUtil.Delimiter.HYPHEN.getDelimiter(), openIndex + 1) : -1;

        if (openIndex < 0 || closeIndex < 0 || closeIndex <= openIndex + 1) {
            return null;
        }
        return localPart.substring(openIndex + 1, closeIndex).trim();
    }

    /**
     * <p>
     * Resolves the data object from the stream message parameters.
     * </p>
     *
     * @param requestParameters request parameters received from the stream message
     * @return resolved data object
     */
    private JsonObject resolveData(final JsonObject requestParameters) {
        if (requestParameters.containsKey(InboundEmailReceiverFields.DATA)) {
            return requestParameters.getJsonObject(InboundEmailReceiverFields.DATA);
        }

        return requestParameters;
    }

    /**
     * <p>
     * Resolves the email address associated with the current email.
     * Falls back to the auto-forwarding address when the sender identifier is unavailable.
     * </p>
     *
     * @param email email from which the address is resolved
     * @return resolved email address, or {@code null} if no address is available
     */
    private String getEmailAddress(final Email email) {
        final EmailMetaData emailMetaData = email.getMetaData();
        String account = null;

        if (Objects.nonNull(emailMetaData)) {
            account = emailMetaData.getSenderId();

            if (Objects.isNull(account)) {
                final String autoForwardedAddress = emailMetaData.getAutoForwardingAddress();

                if (Objects.nonNull(autoForwardedAddress) && !autoForwardedAddress.isEmpty() &&
                        autoForwardingEmailService.isValidForwardingSource(email.getSender().getEmailAddress())) {
                    account = autoForwardedAddress;
                }
            }
        }

        return account;
    }
}