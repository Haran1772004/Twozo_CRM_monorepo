package com.tlc.crm.core.notification.email.internal.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.notification.email.api.manager.SubscriptionTrackerManager;
import com.tlc.crm.core.notification.email.api.model.email.EmailVariant;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.api.model.converter.EmailConverter;
import com.tlc.crm.core.notification.email.api.model.email.EntityDraftEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.core.notification.email.api.manager.OutboundEmailManager;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;
import com.tlc.crm.core.notification.email.internal.OutboundEmailService;
import com.tlc.crm.core.notification.email.tracking.service.EmailTrackingService;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;

import com.tlc.crm.email.notification.email.EmailDispatcher;
import com.tlc.crm.email.notification.email.api.manager.EmailReferenceManager;
import com.tlc.crm.email.notification.email.api.model.MailResponse;
import com.tlc.crm.email.notification.email.api.model.settings.EmailTrackerSettings;
import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;
import com.tlc.crm.email.notification.email.sync.internal.async.service.EmailSyncSettingsService;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.config.EmailConfiguration;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.EmailMetaData;

import com.tlc.email.common.model.type.OutboundEmail;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the outbound mail related services
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class OutboundEmailServiceImpl implements OutboundEmailService {

    private static final int DEFAULT_LIMIT_FOR_CONVERSATION = 50;

    private final ContactManager contactManager;
    private final EmailDispatcher emailDispatcher;
    private final OutboundEmailManager outboundEmailManager;
    private final DealManager dealManager;
    private final CoreMailManager mailManager;
    private final EmailTrackingService emailTrackingService;
    private final EmailConverter emailConverter;
    private final EmailReferenceManager emailReferenceManager;
    private final SubscriptionTrackerManager subscriptionTrackerManager;
    private final EmailSyncSettingsService emailSyncSettingsService;

    private OutboundEmailServiceImpl() {
        contactManager = ContactManager.getInstance();
        emailDispatcher = EmailDispatcher.get();
        outboundEmailManager = OutboundEmailManager.getInstance();
        dealManager = DealManager.getInstance();
        mailManager = CoreMailManager.getInstance();
        this.emailTrackingService = EmailTrackingService.getInstance();
        this.emailConverter = EmailConverter.getInstance();
        this.emailReferenceManager = EmailReferenceManager.getInstance();
        this.subscriptionTrackerManager = SubscriptionTrackerManager.getInstance();
        this.emailSyncSettingsService = EmailSyncSettingsService.getInstance();
    }

    private static class Instance {
        private static final OutboundEmailServiceImpl INSTANCE = new OutboundEmailServiceImpl();
    }

    public static OutboundEmailServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param mailIds  Represents the Collection of outbound ids
     * @param isActive Represents the flag for active
     * @return true if data is present
     */
    @Override
    public boolean exists(final Long orgId, final Collection<Long> mailIds, final boolean isActive) {
        return outboundEmailManager.exists(orgId, mailIds, isActive);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents an organization id
     * @param entityThread  Represents the {@link EntityThread}
     * @param configuration Represents the {@link EmailConfiguration}
     * @param requestData   Represents the json which contains request data
     */
    @Override
    public MailResponse draft(final Long orgId, final EntityThread entityThread, final EmailConfiguration configuration, final JsonObject requestData) {
        final Map<String, Object> response = new HashMap<>();
        final EntityOutboundEmail entityOutboundEmail = entityThread.getComposedEmail();

        if (Objects.nonNull(entityOutboundEmail)) {
            final Long conversationId = entityOutboundEmail.getConversationId();
            final boolean isScheduled = Boolean.TRUE.equals(entityOutboundEmail.isScheduled());

            if (isScheduled) {

                if (mailManager.isDraftedConversation(orgId, List.of(conversationId))) {
                    mailManager.setDraftedData(orgId, conversationId, entityOutboundEmail);

                    requestData.put(EmailFields.THREAD_ID.getName(), entityOutboundEmail.getThreadId());
                    if (0 == entityOutboundEmail.getEmailRecipients().size()) {
                        throw ErrorCode.get(EmailErrorCodes.RECIPIENT_NOT_FOUND, "i18n.notifications.mail.recipient.not.found");
                    }

                    if (!entityOutboundEmail.isUnsubscribed() && hasUnsubscribedRecipients(orgId, entityOutboundEmail)) {
                        return null;
                    }
                    final long userId = entityOutboundEmail.getUserId();
                    final Long messageId = emailReferenceManager.getMessageId(orgId, userId, conversationId);

                    final boolean isOutboundType = Objects.nonNull(entityOutboundEmail.getOutboundType()) && entityOutboundEmail.getOutboundType().isSendType();
                    final Long existingParentConversationId = isOutboundType ?
                            null : emailReferenceManager.getParentConversationId(orgId, userId, conversationId);
                    final Long parentConversationId = Objects.isNull(existingParentConversationId) && !isOutboundType
                            ? mailManager.getParentConversationId(orgId, conversationId) : existingParentConversationId;
                    final Long parentThreadId = mailManager.getThreadId(orgId, parentConversationId);

                    if (Objects.nonNull(messageId)) {
                        deleteDraftMessages(orgId, userId, List.of(conversationId), configuration);
                    }
                    discardDrafts(orgId, userId, List.of(mailManager.getThreadId(orgId, conversationId)));
                    final EntityScheduledEmail scheduledEmail = (EntityScheduledEmail) entityOutboundEmail;

                    scheduledEmail.setSender(entityOutboundEmail.getSender());

                    if (!mailManager.isConversationExists(orgId, parentConversationId, true)) {
                        entityOutboundEmail.setConversationId(null);
                        entityThread.setCrmId(null);
                    } else {
                        entityOutboundEmail.setConversationId(parentConversationId);
                        entityThread.setCrmId(parentThreadId);
                    }
                    outboundEmailManager.create(entityThread);

                    response.put(EmailFields.MESSAGE.getName(), "i18n.notifications.mail.schedule.success");

                    return new MailResponse(response);
                } else {
                    throw ErrorCode.get(EmailErrorCodes.INVALID_DRAFTED_CONVERSATION_ID, "i18n.notifications.mail.draft.conversation.id.invalid");
                }
            } else {
                final EntityDraftEmail entityDraftEmail = entityOutboundEmail.get();

                return switch (entityDraftEmail.getDraftAction()) {
                    case CREATE -> {
                        entityDraftEmail.setParentConversationId(entityDraftEmail.getConversationId());
                        yield send(orgId, entityThread, requestData);
                    }
                    case UPDATE -> {
                        final FromAddressInfo fromAddressInfo = entityDraftEmail.getSenderFromAddressInfo();

                        if (Objects.nonNull(fromAddressInfo)) {
                            send(orgId, entityThread, requestData);
                            yield new MailResponse(Collections.emptyMap());
                        } else if (mailManager.isDraftedConversation(orgId, List.of(conversationId)) && isValidRecipientList(orgId, List.of(entityDraftEmail))) {
                            final OutboundEmail.OutboundType outboundType = entityDraftEmail.getOutboundType();

                            mailManager.setDraftedData(orgId, conversationId, entityDraftEmail);
                            requestData.put(EmailFields.THREAD_ID.getName(), entityDraftEmail.getThreadId());

                            if (Objects.nonNull(outboundType)) {
                                final JsonObject dataJson = requestData.getJsonObject(EmailFields.DATA.getName());

                                dataJson.put(EmailFields.OUTBOUND_TYPE.getName(), outboundType.getId());
                            }

                            sendEmail(entityDraftEmail, requestData);

                            yield new MailResponse(Collections.emptyMap());
                        } else {
                            throw ErrorCode.get(EmailErrorCodes.INVALID_DRAFTED_CONVERSATION_ID, "i18n.notifications.mail.draft.conversation.id.invalid");
                        }
                    }
                    case SEND -> {
                        if (mailManager.isDraftedConversation(orgId, List.of(conversationId))) {
                            mailManager.setDraftedData(orgId, conversationId, entityDraftEmail);

                            requestData.put(EmailFields.THREAD_ID.getName(), entityDraftEmail.getThreadId());

                            if (0 == entityDraftEmail.getEmailRecipients().size()) {
                                throw ErrorCode.get(EmailErrorCodes.RECIPIENT_NOT_FOUND, "i18n.notifications.mail.recipient.not.found");
                            }

                            if (!entityDraftEmail.isUnsubscribed() && hasUnsubscribedRecipients(orgId, entityDraftEmail)) {
                                yield null;
                            }
                            sendEmail(entityDraftEmail, requestData);
                        } else {
                            sendEmail(entityDraftEmail, requestData);
                        }
                        yield new MailResponse(Collections.emptyMap());
                    }
                };
            }
        }

        return null;
    }

    /**
     * <p>
     * Compose the mail with headers and recipients from the chosen reply mail
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param thread An entity that holds the thread attributes
     * @param data   Contains the JSON object
     */
    @Override
    public void composeReply(final Long orgId, final EntityThread thread, final JsonObject data) {
        composeReply(orgId, thread, data, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param thread An entity that holds the thread attributes
     * @param data   Contains the Json object
     */
    @Override
    public void composeReply(final Long orgId, final EntityThread thread, final JsonObject data, final boolean isDraftSendAction) {
        final EntityEmail entityEmail = thread.getComposedEmail();

        if (Objects.nonNull(entityEmail)) {
            final Long conversationId = isDraftSendAction ?
                    entityEmail.getParentConversationId() : entityEmail.getConversationId();

            if (0 == entityEmail.getEmailRecipients().size() && !entityEmail.isDraft()) {
                throw ErrorCode.get(EmailErrorCodes.RECIPIENT_NOT_FOUND, "i18n.notifications.mail.recipient.not.found");
            }

            mailManager.composeReply(orgId, conversationId, thread, data);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param thread An entity that holds the thread attributes
     * @param data   Represents the json which contains request data
     */
    @Override
    public void composeForward(final Long orgId, final EntityThread thread, final JsonObject data) {
        final EntityEmail entityEmail = thread.getComposedEmail();

        if (Objects.nonNull(entityEmail)) {
            final Long conversationId = entityEmail.getConversationId();

            if (0 == entityEmail.getEmailRecipients().size() && !entityEmail.isDraft()) {
                throw ErrorCode.get(EmailErrorCodes.RECIPIENT_NOT_FOUND, "i18n.notifications.mail.recipient.not.found");
            }
            mailManager.composeReply(orgId, conversationId, thread, data);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param thread      Represents the thread
     * @param requestData Represents the json which contains request data
     * @return {@link MailResponse}
     */
    @Override
    public MailResponse send(final Long orgId, final EntityThread thread, final JsonObject requestData) {
        final Map<String, Object> response = new HashMap<>();
        final Collection<EntityEmail> entityEmails = thread.getEntityEmails();
        final Collection<ErrorDescriptor> errors = ValidatorServiceProvider.get().modelValidator().validate(entityEmails, Group.Create.class);

        if (errors.isEmpty() && isValidRecipientList(orgId, entityEmails)) {

            if (Objects.nonNull(thread.getSource())) {
                isValidDetails(orgId, List.of(thread));
            }

            for (final EntityEmail entityEmail : entityEmails) {
                final EmailMetaData emailMetaData = entityEmail.getMetaData();
                final EmailMetaData metaData = Objects.isNull(emailMetaData) ? new EmailMetaData() : emailMetaData;
                final FromAddressInfo fromAddressInfo = entityEmail.getSenderFromAddressInfo();

                if (Objects.isNull(metaData.getReferences()) && Objects.isNull(metaData.getInReplyTo())) {
                    metaData.setInitialMessage(true);
                }
                entityEmail.setMetaData(metaData);
                final Email.MessageCategory messageCategory = entityEmail.getMessageCategory();

                if (Objects.equals(messageCategory, Email.MessageCategory.SCHEDULED)) {
                    final EntityScheduledEmail scheduledEmail = (EntityScheduledEmail) entityEmail;

                    scheduledEmail.setSender(entityEmail.getSender());
                    outboundEmailManager.create(thread);
                } else if (Objects.nonNull(fromAddressInfo) && entityEmail instanceof EntityDraftEmail entityDraftEmail) {
                    final DraftEmail.DraftAction draftAction = entityDraftEmail.getDraftAction();

                    if (draftAction.isCreate()) {
                        outboundEmailManager.create(thread);
                    } else {
                        outboundEmailManager.update(orgId, List.of(thread));
                    }
                } else {
                    sendEmail(entityEmail, requestData);
                }
            }
        }
        response.put("validationErrors", errors);
        response.put("action", Action.CREATE.name());

        return new MailResponse(response);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents an user id
     * @param emailAddress Represents the email address
     */
    @Override
    public void unsubscribe(final long orgId, final long userId, final String emailAddress) {
        subscriptionTrackerManager.unsubscribe(orgId, emailAddress);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param mail  Represents the {@link EntityEmail}
     * @return true, if the mail recipient is unsubscribed
     */
    @Override
    public boolean hasUnsubscribedRecipients(final Long orgId, final EntityEmail mail) {
        final ContactRecipients contactRecipients = mail.getContactRecipients();

        return isValidRecipientList(orgId, List.of(mail)) &&
                subscriptionTrackerManager.hasUnsubscribedRecipients(orgId, Objects.isNull(contactRecipients) ?
                        Collections.emptyList() : contactRecipients.getEmailIds(), mail.getEmailRecipients().getAllEmailAddresses());
    }

    /**
     * <p>
     * Checks whether the recipients are unsubscribed or not
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param emailAddresses Represents the {@link Collection} of mail addresses
     * @return true, if the mail is unsubscribed
     */
    @Override
    public boolean hasUnsubscribedEmailAddress(final Long orgId, final Collection<String> emailAddresses) {
        return subscriptionTrackerManager.hasUnsubscribedEmailAddress(orgId, emailAddresses);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Refers the organization id.
     * @param userId        Refers the user id.
     * @param thread        Refers the {@link EntityThread} where the mail info is configured.
     * @param configuration Refers to the {@link EmailConfiguration}
     * @param data          Refers the state data in JSON
     */
    @Override
    public final void setEmailThreadDetails(final Long orgId, final Long userId, final EntityThread thread,
                                            final EmailConfiguration configuration, final JsonObject data) {
        final EntityEmail mail = thread.getComposedEmail();

        if (Objects.nonNull(mail)) {
            final EmailTrackerSettings emailTrackerSettings = mail.getEmailTrackerSettings();
            final Optional<FromAddressInfo> optionalFromAddressInfo = mailManager.getFromAddressInfo(orgId, data.getLong(CoreEmailFields.FROM_ADDRESS.getName()));

            if (optionalFromAddressInfo.isEmpty()) {
                throw ErrorCode.get(EmailErrorCodes.INVALID_FROM_ADDRESS, "i18n.notifications.from.address.invalid");
            } else {
                final FromAddressInfo fromAddressInfo = optionalFromAddressInfo.get();

                mail.setSender(fromAddressInfo);
                configuration.setSyncEmailAddress(fromAddressInfo);
                mail.setSyncedState(true);
                final EmailVariant emailVariant = EmailVariant.of(getVariant(fromAddressInfo.getType()));

                loadVariantInfo(orgId, userId, emailVariant, fromAddressInfo);
                mail.setVariant(emailVariant);
                final JsonObject jsonObject = Json.object();

                jsonObject.put(CoreEmailFields.TYPE.getName(), emailVariant.getValue());
                jsonObject.put(CoreEmailFields.ID.getName(), emailVariant.getId());
                data.put(CoreEmailFields.VARIANT.getName(), jsonObject);
            }
            setRecipients(mail);
            thread.setUserId(userId);

            if (emailTrackerSettings.isEmailTrackerEnabled() || emailTrackerSettings.isLinkTrackerEnabled() || mail.hasUnsubscribeLink()) {
                emailTrackingService.setTrackingDetails(mail, data);
            }
        }
    }

    /**
     * <p>
     * Loads and sets the variant information into the given {@link EmailVariant}
     * based on the {@link FromAddressInfo.Type} and the associated sender address.
     * </p>
     *
     * @param orgId           the organization ID used to resolve the sync settings
     * @param userId          the user ID used to resolve the sync settings
     * @param emailVariant    the {@link EmailVariant} into which the variant info is loaded
     * @param fromAddressInfo the {@link FromAddressInfo} containing the sender type and address
     */
    private void loadVariantInfo(final long orgId, final Long userId,
                                 final EmailVariant emailVariant, final FromAddressInfo fromAddressInfo) {
        switch (fromAddressInfo.getType()) {
            case SYNC_EMAIL -> {
                final Optional<EmailSyncSettings> optionalEmailSyncSettings = emailSyncSettingsService
                        .getSyncSetting(orgId, userId, fromAddressInfo.getEmailAddress());

                optionalEmailSyncSettings.ifPresent(emailSyncSettings -> emailVariant.setId(emailSyncSettings.id()));
            }
        }
    }

    /**
     * <p>
     * Determines the {@link EmailVariant} based on the given {@link FromAddressInfo.Type}.
     * Maps specific sender address types to their corresponding email variants.
     * </p>
     *
     * @param fromAddressType the type of the sender address
     * @return the corresponding {@link EmailVariant}
     */
    private EmailVariant.Type getVariant(final FromAddressInfo.Type fromAddressType) {
        return switch (fromAddressType) {
            case TEAM_INBOX -> EmailVariant.Type.TEAM_INBOX;
            case ORG_EMAIL -> EmailVariant.Type.ORG_MAIL;
            default -> EmailVariant.Type.SYNC_EMAIL;
        };
    }

    /**
     * <p>
     * Sets the recipients for both contact related emails and unknown emails
     * </p>
     *
     * @param mail Represents the {@link EntityEmail}
     */
    private void setRecipients(final EntityEmail mail) {
        final Long orgId = mail.orgId();
        final ContactRecipients recipients = mail.getContactRecipients();
        final EmailRecipients emailRecipients = mail.getEmailRecipients();
        final Collection<EmailAddress> receivers = emailRecipients.toRecipients();
        final Map<Long, String> contactRecipients;

        if (Objects.nonNull(recipients)) {
            final Collection<Long> contactEmailIds = recipients.getEmailIds();

            contactRecipients = contactEmailIds.isEmpty() ? Collections.emptyMap() : contactManager.getContactEmailIds(orgId, contactEmailIds);
        } else {
            contactRecipients = Collections.emptyMap();
        }

        final BiConsumer<AddressType, Collection<EmailAddress>> setRecipientByType = (addressType, addresses) -> {
            final Collection<EmailAddress> emailAddresses = contactRecipients.isEmpty() ? new HashSet<>()
                    : getEmailRecipients(orgId, recipients, addressType, contactRecipients);

            if (Objects.nonNull(addresses)) {
                emailAddresses.addAll(addresses);
            }

            if (DEFAULT_LIMIT_FOR_CONVERSATION < emailAddresses.size()) {
                throw ErrorCode.get(EmailErrorCodes.LIMIT_EXCEEDED, "i18n.notifications.mail.recipient.limit.exceeded");
            }

            emailRecipients.addRecipients(addressType, emailAddresses);
        };

        setRecipientByType.accept(AddressType.TO, receivers);
        setRecipientByType.accept(AddressType.BCC, emailRecipients.bccRecipients());
        setRecipientByType.accept(AddressType.CC, emailRecipients.ccRecipients());

        if (0 == emailRecipients.size() && !mail.isDraft()) {
            throw ErrorCode.get(EmailErrorCodes.RECIPIENT_NOT_FOUND, "i18n.notifications.mail.recipient.not.found");
        }

        if (Objects.nonNull(receivers) && receivers.size() > 1 && mail.hasPlaceHolders()) {
            throw ErrorCode.get(EmailErrorCodes.PLACEHOLDER_NOT_SUPPORTED_FOR_MULTIPLE_RECIPIENTS, "i18n.notifications.mail.placeholder.unsupported");
        }
    }

    /**
     * Gets the email recipients list
     *
     * @param orgId             Represents the organization id
     * @param recipients        Represents the recipient ids
     * @param contactRecipients Represents the contact relate recipient ids
     * @param addressType       Represents the {@link AddressType}
     * @return the collections of {@link EmailAddress} mapped in recipients
     */
    private Collection<EmailAddress> getEmailRecipients(final Long orgId, final ContactRecipients recipients, final AddressType addressType,
                                                        final Map<Long, String> contactRecipients) {
        final Collection<Contact> contacts = new HashSet<>();
        final Collection<EmailAddress> addresses = new HashSet<>();
        final Collection<Long> recipientIds = switch (addressType) {
            case TO -> recipients.toEmailIds();
            case CC -> recipients.ccEmailIds();
            case BCC -> recipients.bccEmailIds();
            default -> null;
        };

        if (Objects.nonNull(contactRecipients)) {

            if (Objects.nonNull(recipientIds) && !recipientIds.isEmpty()) {

                for (final Long id : recipientIds) {
                    final Contact contact = new Contact();
                    final String value = contactRecipients.get(id);

                    contact.setPrimaryEmail(emailConverter.processEmail(id, value));
                    contacts.add(contact);
                    addresses.add(new EmailAddress(id, value));
                }

                recipients.addContacts(addressType, contacts);
            }
        }

        return addresses;
    }

    /**
     * <p>
     * Checks for valid details using validator
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param threads Represents the collection of {@link Thread}
     */
    private void isValidDetails(final Long orgId, final Collection<EntityThread> threads) {
        final Map<Source, Long> entityMails = threads.stream().collect(Collectors.toMap(EntityThread::getSource,
                EntityThread::getEntityId));
        final Source dealSource = Source.DEAL;

        if (entityMails.containsKey(dealSource) && !dealManager.exists(orgId, entityMails.get(dealSource))) {
            throw ErrorCode.get(EmailErrorCodes.DEAL_NOT_FOUND, "i18n.notifications.mail.deal.not.found");
        }
    }

    /**
     * <p>
     * Validates the given recipients
     * </p>
     *
     * @param orgId Represents an organization id
     * @param mails Represents the Collections of {@link EntityEmail}
     * @return true if validate recipients
     */
    boolean isValidRecipientList(final Long orgId, final Collection<EntityEmail> mails) {
        if (Objects.nonNull(mails) && !mails.isEmpty()) {
            final Collection<ContactRecipients> contactRecipients = mails.stream().map(EntityEmail::getContactRecipients).filter(Objects::nonNull).toList();
            final Set<Long> recipientIds = new HashSet<>();

            contactRecipients.forEach(recipient -> recipientIds.addAll(recipient.getEmailIds()));

            if (!recipientIds.isEmpty() && !contactManager.isContactEmailIdsExists(orgId, recipientIds)) {
                throw ErrorCode.get(EmailErrorCodes.CONTACT_EMAIL_NOT_FOUND, "i18n.notifications.mail.contact.email.not.found");
            }
        }

        return true;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param userId        Represents a user id
     * @param contactIds    Represents the {@link Collection} of contacts
     * @param mailAddresses Represents the {@link Collection} of mail address
     * @return the {@link Collection} of unsubscribed {@link EmailAddress}
     */
    @Override
    public Collection<EmailAddress> fetchUnsubscribedMailAddresses(final long orgId, final long userId,
                                                                   final Collection<Long> contactIds, final Collection<String> mailAddresses) {
        return subscriptionTrackerManager.fetchUnsubscribedMailAddresses(orgId, userId, contactIds, mailAddresses);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the userId
     * @param conversationIds Represents the {@link Collection} of {@link String} of conversion ids
     * @param configuration   Represents the email configuration
     */
    public void deleteDraftMessages(final long orgId, final long userId, final Collection<Long> conversationIds,
                                    final EmailConfiguration configuration) {
        final Map<String, Collection<EmailStateTransitionInfo>> emailStateTransitionInfos = new HashMap<>();

        mailManager.fillStateTransitionDetailsByConversationIds(orgId, userId, conversationIds, null, emailStateTransitionInfos);
        emailDispatcher.delete(emailStateTransitionInfos, Email.MessageCategory.DRAFT);
    }

    /**
     * {@inheritDoc}
     *
     * @param trackingType Represents the {@link EmailTrackingDetail.Type}
     * @param trackingId   Represents the tracking id
     */
    @Override
    public void updateTrackingDetails(final EmailTrackingDetail.Type trackingType, final String trackingId) {
        subscriptionTrackerManager.updateTrackingDetails(trackingType, trackingId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents an organization id
     * @param userId    Represents the user id
     * @param threadIds Represents the thread ids
     */
    @Override
    public Collection<Long> discardDrafts(final long orgId, final Long userId, final Collection<Long> threadIds) {
        return outboundEmailManager.discardDrafts(orgId, userId, threadIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param trackingId Represents the tracking id
     */
    @Override
    public void updateUnsubscribeDetails(final String trackingId) {
        subscriptionTrackerManager.updateUnsubscribeDetails(trackingId);
    }

    /**
     * <p>
     * Sends the email to the provider.
     * </p>
     *
     * @param entityEmail Represents the {@link EntityEmail}
     * @param stateData   Represents the state data in json
     */
    private void sendEmail(final EntityEmail entityEmail, final JsonObject stateData) {
        final Email email = entityEmail.getEmail();

        email.setMetaData(entityEmail.getMetaData());
        email.setSender(entityEmail.getSender());
        email.setSubject(entityEmail.getSubject());
        email.setContent(entityEmail.getContent());
        email.setRecipients(entityEmail.getRecipients());
        email.setFiles(entityEmail.getFiles());
        email.setAttachments(entityEmail.getAttachments());
        email.setId(entityEmail.id());

        emailDispatcher.send(email, stateData);
    }

    /**
     * <p>
     * Gets the {@link OutboundEmail.OutboundType} for the given conversation id
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @return the {@link OutboundEmail.OutboundType}
     */
    public OutboundEmail.OutboundType getOutboundType(final long orgId, final Long conversationId) {
        return outboundEmailManager.getOutboundType(orgId, conversationId);
    }
}
