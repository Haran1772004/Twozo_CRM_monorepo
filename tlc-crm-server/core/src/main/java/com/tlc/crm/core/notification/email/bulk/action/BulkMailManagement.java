package com.tlc.crm.core.notification.email.bulk.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.iam.service.OrganizationService;

import com.tlc.crm.core.api.contact.model.Contact;

import com.tlc.crm.core.notification.email.api.manager.EntityMailManager;
import com.tlc.crm.core.notification.email.api.manager.SubscriptionTrackerManager;
import com.tlc.crm.core.notification.email.api.model.converter.EmailConverter;
import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.annotation.authorizer.EmailAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.LimitAuthorizer;
import com.tlc.crm.security.permission.model.LimitActionType;

import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.bulk.api.manager.BulkMailMetricsManager;
import com.tlc.crm.core.notification.email.bulk.api.model.BulkMail;
import com.tlc.crm.core.notification.email.bulk.api.model.MailInfo;
import com.tlc.crm.core.notification.email.bulk.api.model.MailInfo.Status;
import com.tlc.crm.core.notification.email.bulk.api.model.MailRecipientInfo;
import com.tlc.crm.core.notification.email.bulk.api.model.builder.BulkMailBuilder;
import com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics;
import com.tlc.crm.core.notification.email.bulk.api.model.metrics.MetricsInsight;
import com.tlc.crm.core.notification.email.bulk.fields.BulkMailFields;
import com.tlc.crm.core.notification.email.bulk.service.BulkMailService;
import com.tlc.crm.core.notification.email.bulk.status.BulkMailErrorCodes;
import com.tlc.crm.core.notification.email.internal.OutboundEmailService;
import com.tlc.crm.core.notification.email.template.api.extractor.MailPlaceHolderExtractor;
import com.tlc.crm.core.notification.email.template.api.model.placeholder.MailPlaceHolder;
import com.tlc.crm.core.notification.email.tracking.service.EmailTrackingService;
import com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics.MetricsType;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.email.notification.email.EmailSyncService;
import com.tlc.crm.email.notification.email.api.model.MailResponse;
import com.tlc.crm.email.notification.email.api.model.settings.EmailTrackerSettings;
import com.tlc.crm.email.notification.model.MessageDispatchType;
import com.tlc.crm.email.notification.model.UserSyncAddressInfo;

import com.tlc.crm.core.notification.email.template.service.MailPlaceHolderResolver;
import com.tlc.file.client.converter.AttachmentConverter;
import com.tlc.file.client.service.AttachmentService;

import com.tlc.file.common.models.Attachment;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;

import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.DraftEmail.DraftAction;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.config.EmailConfiguration;
import com.tlc.email.common.model.type.OutboundEmail;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;

import com.tlc.notification.model.template.PlaceHolder;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics.MetricsType.NO_PRIMARY_EMAIL;
import static com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics.MetricsType.SENT;
import static com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics.MetricsType.UNSUBSCRIBED;

/**
 * Manages {@link BulkMail} related web actions for sending, retrieving the bulk mails.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see AuthorizationController
 */
@LimitAuthorizer(types = LimitActionType.BULK_EMAIL, apiTypes = "bulkSend")
@EmailAuthorizer(subCategories = {SubCategory.Type.VIEW_EMAIL_CONVERSATION, SubCategory.Type.BULK_EMAIL_LIMIT})
@WebAction(path = "/notifications/bulk/email")
public class BulkMailManagement extends AuthorizationController {

    private static final int DEFAULT_RECIPIENT_START_VALUE = 1;
    private static final int DEFAULT_RECIPIENT_LIMIT = 7;
    private static final int DEFAULT_LIMIT_FOR_CONVERSATION = 100;
    private static final int DEFAULT_LIMIT = 100;

    private final BulkMailBuilder bulkMailBuilder;
    private final BulkMailService bulkMailService;
    private final OutboundEmailService outboundEmailService;
    private final CoreMailManager mailManager;
    private final ContactManager contactManager;
    private final BulkMailMetricsManager bulkMailMetricsManager;
    private final EmailTrackingService emailTrackingService;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final MailPlaceHolderResolver placeHolderResolver;
    private final MailPlaceHolderExtractor placeHolderExtractor;
    private final EmailSyncService emailSyncService;
    private final AttachmentService attachmentService;
    private final AttachmentConverter attachmentConverter;
    private final EmailConverter emailConverter;
    private final EmailManager emailManager;
    private final EntityMailManager entityMailManager;
    private final SubscriptionTrackerManager subscriptionTrackerManager;

    public BulkMailManagement() {
        this.bulkMailBuilder = BulkMailBuilder.getInstance();
        this.bulkMailService = BulkMailService.getInstance();
        this.outboundEmailService = OutboundEmailService.getInstance();
        this.mailManager = CoreMailManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.bulkMailMetricsManager = BulkMailMetricsManager.getInstance();
        this.emailTrackingService = EmailTrackingService.getInstance();
        this.userService = UserService.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.placeHolderResolver = MailPlaceHolderResolver.getInstance();
        this.placeHolderExtractor = MailPlaceHolderExtractor.getInstance();
        this.emailSyncService = EmailSyncService.getInstance();
        this.attachmentService = AttachmentService.getInstance();
        this.attachmentConverter = AttachmentConverter.getInstance();
        this.emailConverter = EmailConverter.getInstance();
        this.emailManager = EmailManager.getInstance();
        this.entityMailManager = EntityMailManager.getInstance();
        this.subscriptionTrackerManager = SubscriptionTrackerManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Represents the {@link CrmRequest} data
     * @param crmResponse Represents the {@link CrmResponse} to be sent
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(BulkMailFields.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(BulkMailFields.DATA.getName());
        final Long orgId = request.orgId();
        final Long userId = request.userId();

        switch (type) {
            case "bulkDraft" -> {
                setDataToBulkMailRequest(orgId, userId, requestJson);
                bulkDraft(orgId, userId, data, crmResponse, requestJson);
            }
            case "bulkSend" -> {
                setDataToBulkMailRequest(orgId, userId, requestJson);
                bulkSend(orgId, userId, data, crmResponse, requestJson);
            }
            case "totalBulkMailUsage" -> crmResponse.put(BulkMailFields.TOTAL_BULK_MAIL_USAGE.getName(),
                    bulkMailService.getSubscribedRecipientsCountOnDay(orgId, userId));
            case "bulkMailDelete" -> deleteBulkMail(orgId, userId, data, crmResponse);
            case "getBulkMail" -> crmResponse.put(BulkMailFields.BULK_MAIL.getName(), getBulkMail(orgId, userId, data));
            case "getRecipients" -> crmResponse.put(BulkMailFields.RECIPIENTS.getName(),
                    getRecipientsByBulkMail(orgId, data, crmResponse));
            case "getEmailData" -> crmResponse.put(BulkMailFields.EMAIL.getName(),
                    getEmailData(orgId, data.getLong(BulkMailFields.ID.getName())));
            case "getMetrics" -> crmResponse.put(BulkMailFields.METRICS.getName(),
                    getBulkMailMetrics(orgId, userId, data));
            case "metricsInsights" -> crmResponse.put(BulkMailFields.METRICS.getName(),
                    getMetricsInsights(orgId, userId, data, crmResponse));
        }
    }

    /**
     * <p>
     * Sets the needed data to the bulk mail request json
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param userId      Represents the user id
     * @param requestJson Represents the request json
     */
    private void setDataToBulkMailRequest(final long orgId, final long userId, final JsonObject requestJson) {
        requestJson.put(BulkMailFields.ORG_ID.getName(), orgId);
        requestJson.put(BulkMailFields.USER_ID.getName(), userId);
        requestJson.put(BulkMailFields.IS_BULK.getName(), true);
    }

    /**
     * Creates draft for {@link BulkMail} and handles the creating, updating, and sending of emails
     * as specified in the provided {@link CrmRequest} for a given organization and user.
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents a user id
     * @param data        Represents the request {@link JsonObject}
     * @param crmResponse Represents the {@link CrmResponse} to be sent
     * @param requestData Represents the JSON which contains request data
     */
    private void bulkDraft(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse, final JsonObject requestData) {
        final BulkMail bulkMail = constructBulkMail(orgId, userId, data);
        final String draftActionKey = BulkMailFields.DRAFT_ACTION.getName();

        for (final MailInfo mailInfo : bulkMail.getMailInfo()) {
            final EntityOutboundEmail outboundEmail = mailInfo.getOutboundEmail();
            final Collection<Long> mailAttachmentIds = outboundEmail.getEntityAttachmentIds();

            if (Objects.nonNull(mailAttachmentIds) && !mailAttachmentIds.isEmpty()) {
                outboundEmail.setFiles(attachmentService.getFiles(organizationService.getIamOrgId(orgId), mailAttachmentIds));
            }
        }
        final short draftAction = data.getShort(draftActionKey);
        final DraftAction mailDraftAction = DraftAction.get(draftAction);

        if (bulkMail.getMailInfo().stream().map(MailInfo::getOutboundEmail).anyMatch(EntityOutboundEmail::hasPlaceHolders) && !mailDraftAction.isCreateOrUpdate()) {
            resolveBulkMailToDeliver(bulkMail, crmResponse, true, requestData);
        } else {
            setResponse(bulkMailService.bulkDraft(bulkMail, requestData), crmResponse);
        }
    }

    /**
     * Sends the {@link BulkMail} for the provided {@link CrmRequest} and given organization and user.
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents a user id
     * @param data        Represents the request {@link JsonObject}
     * @param crmResponse Represents the {@link CrmResponse} to be sent
     * @param requestData Represents the json which contains request data
     */
    private void bulkSend(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse,
                          final JsonObject requestData) {
        final String emailsKey = BulkMailFields.EMAILS.getName();

        if (!data.containsKey(BulkMailFields.IGNORE_UNSUBSCRIBED.getName()) && data.containsKey(emailsKey)
                && hasUnsubscribedEmailAddress(orgId, data.getJsonArray(emailsKey))) {
            throw ErrorCode.get(EmailErrorCodes.UNSUBSCRIBED_EMAIL_ADDRESS_FOUND);
        }
        final BulkMail bulkMail = constructBulkMail(orgId, userId, data);
        final Collection<MailInfo> bulkMailInfo = bulkMail.getMailInfo();

        for (final MailInfo mailInfo : bulkMailInfo) {
            final EntityOutboundEmail outboundEmail = mailInfo.getOutboundEmail();
            final EmailTrackerSettings emailTrackerSettings = outboundEmail.getEmailTrackerSettings();
            final Collection<Long> mailAttachmentIds = outboundEmail.getEntityAttachmentIds();

            if (!mailAttachmentIds.isEmpty()) {
                outboundEmail.setFiles(attachmentService.getFiles(organizationService.getIamOrgId(orgId), mailAttachmentIds));
            }
            outboundEmail.setOutboundType(OutboundEmail.OutboundType.SEND);

            if (emailTrackerSettings.isEmailTrackerEnabled() || emailTrackerSettings.isLinkTrackerEnabled() || outboundEmail.hasUnsubscribeLink()) {
                emailTrackingService.setTrackingDetails(outboundEmail, requestData);
            }
        }

        if (bulkMailInfo.stream().map(MailInfo::getOutboundEmail).anyMatch(EntityOutboundEmail::hasPlaceHolders)) {
            resolveBulkMailToDeliver(bulkMail, crmResponse, false, requestData);
        } else {
            setResponse(bulkMailService.bulkSend(bulkMail, requestData), crmResponse);
        }
    }

    /**
     * <p>
     * Checks for the presence of unsubscribed email addresses in the provided list of emails for a given organization.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param emailsArray Represents the {@link JsonArray} of emails to be checked for unsubscribed email addresses
     * @return true if there are unsubscribed email addresses, false otherwise
     */
    private boolean hasUnsubscribedEmailAddress(final long orgId, final JsonArray emailsArray) {
        final String toAddressKey = BulkMailFields.TO_ADDRESS.getName();
        final String contactKey = BulkMailFields.CONTACT.getName();
        final String emailKey = BulkMailFields.EMAIL.getName();
        final String emailIdKey = BulkMailFields.EMAIL_ID.getName();
        final String idKey = BulkMailFields.ID.getName();
        final int emailsArraySize = emailsArray.size();
        final Collection<String> emailAddresses = new HashSet<>();
        final Collection<Long> contactEmailIds = new HashSet<>();

        for (int index = 0; index < emailsArraySize; index++) {
            final JsonObject emailJson = emailsArray.getJsonObject(index);

            if (!emailJson.containsKey(toAddressKey)) {
                continue;
            }
            final JsonObject toAddressJson = emailJson.getJsonObject(toAddressKey);

            if (toAddressJson.containsKey(emailKey)) {
                emailAddresses.add(toAddressJson.getJsonObject(emailKey).getString(idKey));
            } else if (toAddressJson.containsKey(contactKey)) {
                final JsonObject contactJson = toAddressJson.getJsonObject(contactKey);

                if (contactJson.containsKey(emailIdKey)) {
                    contactEmailIds.add(contactJson.getLong(emailIdKey));
                }
            }
        }
        emailAddresses.addAll(emailManager.get(orgId, contactEmailIds).stream()
                .map(com.tlc.crm.entity.models.type.Email::getValue).collect(Collectors.toUnmodifiableSet()));
        isValidRecipientList(orgId, contactEmailIds);

        return (!contactEmailIds.isEmpty() && entityMailManager.hasUnsubscribedContact(orgId, contactEmailIds))
                || outboundEmailService.hasUnsubscribedEmailAddress(orgId, emailAddresses);
    }

    /**
     * <p>
     * Validates the given recipients
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param contactEmails Represents the contact email ids
     */
    private void isValidRecipientList(final Long orgId, final Collection<Long> contactEmails) {
        if (Objects.nonNull(contactEmails) && !contactEmails.isEmpty()) {

            if (!contactManager.isContactEmailIdsExists(orgId, contactEmails)) {
                throw ErrorCode.get(EmailErrorCodes.CONTACT_EMAIL_NOT_FOUND, "i18n.notifications.mail.contact.email.not.found");
            }
        }
    }

    /**
     * Constructs and retrieves a {@link BulkMail} using the provided organization id, user id, and request {@link JsonObject}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @param data   represents the {@link JsonObject} data
     * @return {@link BulkMail}
     */
    private BulkMail constructBulkMail(final long orgId, final long userId, final JsonObject data) {
        final BulkMail bulkMail = constructBulkMailFromJson(orgId, userId, data);

        categorizeMailsByStatus(orgId, bulkMail);

        return bulkMail;
    }

    /**
     * Fetches a {@link Collection} of unsubscribed {@link EmailAddress} for a given organization and user.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a userId
     * @return the {@link Collection} of unsubscribed mail
     */
    private Collection<EmailAddress> fetchUnsubscribedEmails(final long orgId, final long userId, final BulkMail bulkMail) {
        final Collection<String> emailAddresses = new HashSet<>();
        final Collection<Long> contactEmailIds = new ArrayList<>();

        for (final MailInfo mailInfo : bulkMail.getMailInfo()) {
            final EntityOutboundEmail outboundEmail = mailInfo.getOutboundEmail();
            final ContactRecipients contactRecipients = outboundEmail.getContactRecipients();

            if (Objects.nonNull(contactRecipients)) {
                final Collection<Long> toContacts = contactRecipients.toEmailIds();

                if (Objects.nonNull(toContacts)) {
                    contactEmailIds.addAll(toContacts);
                }
            }
            final Collection<EmailAddress> receivers = outboundEmail.getEmailRecipients().toRecipients();

            if (Objects.nonNull(receivers)) {
                emailAddresses.addAll(receivers.stream().map(EmailAddress::getEmailAddress).collect(Collectors.toUnmodifiableSet()));
            }
        }

        return outboundEmailService.fetchUnsubscribedMailAddresses(orgId, userId, contactEmailIds, emailAddresses);
    }

    /**
     * Constructs a {@link BulkMail} from the provided {@link JsonObject} data, considering the specified organization and user.
     *
     * @param orgId  Represents the organization id
     * @param userId Represents a userId
     * @param data   Represents the request {@link JsonObject} data
     * @return {@link BulkMail} with data populated from the {@link JsonObject}
     */
    private BulkMail constructBulkMailFromJson(final long orgId, final long userId, final JsonObject data) {
        final Optional<FromAddressInfo> fromAddress = mailManager.getFromAddressInfo(orgId, data.getLong(BulkMailFields.FROM_ADDRESS.getName()));
        final BulkMail bulkMail = new BulkMail();

        bulkMail.setOrgId(orgId);
        bulkMail.setSenderId(userId);
        bulkMail.setMailInfo(buildMails(orgId, userId, data));
        bulkMail.setSource(Source.valueOf(data.getString(BulkMailFields.SOURCE.getName()).toUpperCase()));

        final Optional<String> senderAddress = bulkMail.getMailInfo().stream().filter(mailInfo -> !mailInfo.isPersonalized())
                .map(MailInfo::getOutboundEmail).map(mail -> mail.getSender().getEmailAddress()).findFirst()
                .or(() -> fromAddress.map(EmailAddress::getEmailAddress));
        senderAddress.map(EmailAddress::new).ifPresent(bulkMail::setFromAddress);

        if (data.containsKey(BulkMailFields.DRAFT_ACTION.getName())) {
            bulkMail.setHasDrafts(true);
            final String draftIdKey = BulkMailFields.DRAFT_ID.getName();

            if (data.containsKey(draftIdKey)) {
                bulkMail.setId(data.getLong(draftIdKey));
            }
        }

        if (data.containsKey(BulkMailFields.SCHEDULED_TIME.getName())) {
            bulkMail.setHasScheduledConversation(true);
        }

        return bulkMail;
    }

    /**
     * Categorizes the provided {@link Collection} of {@link EmailAddress} into different groups based on various criteria,
     * including personalized mails, non-personalized mails, skipped mails, and non-primary emails.
     *
     * @param orgId    Represents the orgId.
     * @param bulkMail Represents the {@link BulkMail}
     */
    private void categorizeMailsByStatus(final Long orgId, final BulkMail bulkMail) {
        final Collection<MailInfo> mailInfoList = bulkMail.getMailInfo();
        final Collection<MailInfo> subscribedMailInfo = new HashSet<>();

        if (!mailInfoList.isEmpty()) {

            for (final MailInfo mailInfo : mailInfoList) {
                final EntityOutboundEmail entityOutboundEmail = mailInfo.getOutboundEmail();
                final ContactRecipients contactRecipients = entityOutboundEmail.getContactRecipients();
                final EmailRecipients emailRecipients = entityOutboundEmail.getEmailRecipients();
                final Collection<Long> contactEmailIds = Objects.isNull(contactRecipients) ? Collections.emptyList() : contactRecipients.getEmailIds();
                final Collection<String> emailAddresses = Objects.isNull(emailRecipients) ? Collections.emptyList() : emailRecipients.getAllEmailAddresses();

                if (Email.MessageCategory.DRAFT == entityOutboundEmail.getMessageCategory()) {
                    mailInfo.setStatus(Status.DRAFTED);
                    mailInfo.setMetricsTypes(Set.of(SENT));
                    subscribedMailInfo.add(mailInfo);
                } else if (subscriptionTrackerManager.hasUnsubscribedRecipients(orgId, contactEmailIds, emailAddresses)) {
                    mailInfo.setStatus(Status.UNSUBSCRIBED);
                    mailInfo.setMetricsTypes(Set.of(SENT, UNSUBSCRIBED));
                } else if (!entityOutboundEmail.getEmailRecipients().toRecipients().isEmpty()) {
                    mailInfo.setStatus(Status.SUBSCRIBED);
                    subscribedMailInfo.add(mailInfo);
                    mailInfo.setMetricsTypes(Set.of(SENT));
                } else {
                    mailInfo.setStatus(Status.NO_EMAIL);
                    mailInfo.setMetricsTypes(Set.of(SENT, NO_PRIMARY_EMAIL));
                }
            }
        }
        bulkMail.setSubscribedMailInfo(subscribedMailInfo);
    }

    /**
     * Builds the {@link Collection} of {@link MailInfo} using the given organization id, user id, and input data.
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents a user id
     * @param jsonObject Represents the request {@link JsonObject}
     * @return the {@link Collection} of {@link MailInfo}
     */
    private Collection<MailInfo> buildMails(final long orgId, final long userId, final JsonObject jsonObject) {
        final Collection<MailInfo> mailInfo = bulkMailBuilder.build(orgId, userId, jsonObject);

        if (mailInfo.size() > DEFAULT_LIMIT) {
            throw ErrorCode.get(EmailErrorCodes.RECIPIENT_LIMIT_EXCEEDED, "i18n.notifications.mail.recipient.limit.exceeded");
        }

        if (!mailInfo.isEmpty()) {

            for (final MailInfo mail : mailInfo) {
                final EntityOutboundEmail outboundEmail = mail.getOutboundEmail();

                mail.setSenderId(userId);
                outboundEmail.setMessageDispatchType(MessageDispatchType.BULK);

                setEntityEmailProperties(orgId, mail, jsonObject);
            }
        }

        return mailInfo;
    }

    /**
     * Configures the {@link EntityEmail} properties of the specified {@link EntityEmail} based on the provided organization and user.
     *
     * @param orgId      Represents an organization id
     * @param mail       Represents the {@link EntityEmail}
     * @param jsonObject Represents the JSON object.
     */
    private void setEntityEmailProperties(final long orgId, final MailInfo mail, final JsonObject jsonObject) {
        final EntityOutboundEmail outboundEmail = mail.getOutboundEmail();
        final Optional<FromAddressInfo> optionalFromAddressInfo = mailManager.getFromAddressInfo(orgId, jsonObject.getLong(BulkMailFields.FROM_ADDRESS.getName()));

        if (optionalFromAddressInfo.isEmpty()) {
            throw ErrorCode.get(BulkMailErrorCodes.INVALID_FROM_ADDRESS, "i18n.notifications.from.address.invalid");
        } else {

            if (Objects.nonNull(outboundEmail.getSender())) {
                outboundEmail.setSender(optionalFromAddressInfo.get());
            }
            outboundEmail.setSyncedState(true);
        }
        setRecipients(mail);
    }

    /**
     * Deletes the {@link BulkMail} for the corresponding given request {@link JsonObject}.
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents a user id
     * @param data     Represents the {@link JsonObject} data
     * @param response Represents the {@link CrmResponse} to be sent
     */
    private void deleteBulkMail(final long orgId, final long userId, final JsonObject data, final CrmResponse response) {
        final Set<Long> bulkMailIds = data.getJsonArray(BulkMailFields.IDS.getName()).toList().stream()
                .map(id -> (Long) id).collect(Collectors.toUnmodifiableSet());

        try {
            if (bulkMailService.exists(orgId, bulkMailIds)) {
                final Collection<Long> draftedConversationIds = bulkMailService.getDraftedConversationIds(orgId, userId, bulkMailIds);

                if (Objects.nonNull(draftedConversationIds) && !draftedConversationIds.isEmpty()) {
                    outboundEmailService.deleteDraftMessages(orgId, userId, draftedConversationIds, getConfiguration(orgId, userId));
                }
                bulkMailService.deleteBulkMails(orgId, userId, bulkMailIds);

                response.put(BulkMailFields.MESSAGE.getName(), response.resolveI18nString("i18n.notifications.bulk.mail.delete.success"));
            } else {
                throw ErrorCode.get(BulkMailErrorCodes.BULK_MAIL_NOT_FOUND, "i18n.notifications.bulk.mail.not.found");
            }
        } catch (ErrorCode errorCode) {
            throw ErrorCode.get(errorCode.getProvider(), response.resolveI18nString("i18n.notifications.bulk.mail.delete.failed"));
        }
    }

    /**
     * <p>
     * Gets the {@link EmailConfiguration} for the given respective user.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return {@link EmailConfiguration}
     */
    private EmailConfiguration getConfiguration(final long orgId, final long userId) {
        final EmailReceiverConfiguration configuration = new EmailReceiverConfiguration();
        final Pair<Long, UserSyncAddressInfo> senderInfo = emailSyncService.getSenderInfo(orgId, userId);

        configuration.setSyncEmailAddress(senderInfo.getValue());

        return configuration;
    }

    /**
     * Gets the {@link BulkMail} for the specified organization and user.
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents a user id
     * @param jsonObject Represents the request {@link JsonObject}
     */
    private JsonObject getBulkMail(final long orgId, final long userId, final JsonObject jsonObject) {
        final String idKey = BulkMailFields.ID.getName();
        final long bulkMailId = jsonObject.getLong(idKey);
        final JsonObject bulkMailJson = Json.object();

        if (bulkMailService.exists(orgId, Set.of(bulkMailId))) {
            final BulkMail bulkMail = bulkMailService.getBulkMail(orgId, userId, bulkMailId);

            if (Objects.nonNull(bulkMail)) {
                final Collection<MailInfo> mailInfo = bulkMail.getMailInfo();

                if (!mailInfo.isEmpty()) {
                    final Collection<MailInfo> personalisedMailInfo = new HashSet<>();
                    final Map<Long, EmailRecipients> emailRecipients = new HashMap<>();
                    final Map<Long, ContactRecipients> contactRecipients = new HashMap<>();
                    final EntityOutboundEmail nonPersonalisedEmail = mailInfo.stream().filter(mail -> !mail.isPersonalized())
                            .map(MailInfo::getOutboundEmail).findFirst().orElseGet(() -> null);
                    EntityOutboundEmail outboundEmail = null;
                    Email.MessageCategory messageCategory = null;

                    for (final MailInfo mail : mailInfo) {
                        final EntityOutboundEmail email = mail.getOutboundEmail();

                        if (Objects.isNull(outboundEmail)) {
                            outboundEmail = email;
                        }

                        if (Objects.isNull(messageCategory)) {
                            messageCategory = mail.getMessageCategory();
                        }

                        if (mail.isPersonalized()) {
                            personalisedMailInfo.add(mail);
                        }

                        if (Objects.nonNull(email)) {
                            final Long conversationId = email.getConversationId();

                            emailRecipients.put(conversationId, email.getEmailRecipients());
                            contactRecipients.put(conversationId, email.getContactRecipients());
                        }
                    }

                    if (Objects.nonNull(outboundEmail)) {
                        final Collection<Attachment> attachments = outboundEmail.getEntityAttachments();
                        final EmailAddress fromAddress = bulkMail.getFromAddress();

                        bulkMailJson.put(idKey, bulkMail.id());
                        bulkMailJson.put(BulkMailFields.RECIPIENTS.getName(), emailConverter.convertRecipientsToJson(emailRecipients, contactRecipients));

                        if (Objects.nonNull(nonPersonalisedEmail)) {
                            final Long templateId = nonPersonalisedEmail.getTemplateId();

                            bulkMailJson.put(BulkMailFields.SUBJECT.getName(), nonPersonalisedEmail.getSubject());
                            bulkMailJson.put(BulkMailFields.MESSAGE.getName(), nonPersonalisedEmail.getContent());
                            bulkMailJson.put(BulkMailFields.TEMPLATE_ID.getName(), templateId);
                        }

                        if (Objects.nonNull(attachments)) {
                            final JsonArray attachmentArray = Json.array();

                            attachments.forEach(attachment -> attachmentArray.put(attachmentConverter.convert(attachment)));
                            bulkMailJson.put(CoreEmailFields.ATTACHMENTS.getName(), attachmentArray);
                        }

                        if (!personalisedMailInfo.isEmpty()) {
                            bulkMailJson.put(BulkMailFields.PERSONALIZE.getName(), emailConverter.convertMailInfoToJsonArray(personalisedMailInfo));
                        }

                        if (Objects.nonNull(fromAddress)) {
                            bulkMailJson.put(BulkMailFields.FROM.getName(), fromAddress.getEmailAddress());
                        }

                        if (messageCategory == Email.MessageCategory.SCHEDULED) {
                            bulkMailJson.put(BulkMailFields.TIMESTAMP.getName(), ((EntityScheduledEmail) outboundEmail).getScheduledTime());
                        }
                        bulkMailJson.put(BulkMailFields.MESSAGE_CATEGORY.getName(), messageCategory);
                    }
                }
            }
        } else {
            throw ErrorCode.get(BulkMailErrorCodes.BULK_MAIL_NOT_FOUND, "i18n.notifications.bulk.mail.not.found");
        }

        return bulkMailJson;
    }

    /**
     * Retrieves a {@link JsonArray} of recipients corresponding to a given {@link BulkMail} id, based on the specified organization id and input data.
     *
     * @param orgId      Represents an organization id
     * @param jsonObject Represents the {@link JsonObject} request data
     * @param response   Represents the {@link CrmResponse}
     * @return {@link JsonArray} of {@link MailRecipientInfo}
     */
    private JsonArray getRecipientsByBulkMail(final long orgId, final JsonObject jsonObject, final CrmResponse response) {
        final long bulkMailId = jsonObject.getLong(BulkMailFields.ID.getName());
        final int start = Math.max(jsonObject.optInt(BulkMailFields.START.getName(), DEFAULT_RECIPIENT_START_VALUE), DEFAULT_RECIPIENT_START_VALUE);
        final int limit = Math.min(jsonObject.optInt(BulkMailFields.LIMIT.getName(), DEFAULT_RECIPIENT_LIMIT), DEFAULT_RECIPIENT_LIMIT);
        final long totalRecords = bulkMailService.getBulkMailRecipientsCount(orgId, bulkMailId);
        final Collection<MailRecipientInfo> recipients = 0 < totalRecords && start <= totalRecords ?
                bulkMailService.getBulkMailRecipients(orgId, bulkMailId, start, limit) : null;
        final JsonArray recipientArray = Json.array();

        if (Objects.nonNull(recipients) && !recipients.isEmpty()) {
            final String contactIdKey = BulkMailFields.CONTACT_ID.getName();
            final String nameKey = BulkMailFields.NAME.getName();
            final String contactEmailIdKey = BulkMailFields.CONTACT_EMAIL_ID.getName();
            final String isPersonalizedKey = BulkMailFields.IS_PERSONALIZED.getName();
            final String statusKey = BulkMailFields.STATUS.getName();
            final String conversationIdKey = BulkMailFields.CONVERSATION_ID.getName();
            final String emailKey = BulkMailFields.EMAIL.getName();

            for (final MailRecipientInfo recipientInfo : recipients) {
                final JsonObject recipientJson = Json.object();

                recipientJson.put(contactIdKey, recipientInfo.contactId());
                recipientJson.put(nameKey, recipientInfo.name());
                recipientJson.put(contactEmailIdKey, recipientInfo.contactEmailId());

                if (recipientInfo.isPersonalized()) {
                    recipientJson.put(isPersonalizedKey, true);
                }
                final Status status = recipientInfo.status();

                if (Objects.nonNull(status)) {
                    recipientJson.put(statusKey, status.name().toLowerCase());
                }
                recipientJson.put(conversationIdKey, recipientInfo.conversationId());
                recipientJson.put(emailKey, recipientInfo.emailAddress().getEmailAddress());
                recipientArray.put(recipientJson);
            }
            response.put(BulkMailFields.TOTAL_RECIPIENTS.getName(), totalRecords);
            response.put(BulkMailFields.HAS_MORE.getName(), (start + limit) <= totalRecords);
        }

        return recipientArray;
    }

    /**
     * Gets the email message and content based on the given organization id
     *
     * @param orgId Represents an organization id
     * @param id    Represents the conversation id
     * @return {@link JsonObject} of {@link EntityEmail}
     */
    private JsonObject getEmailData(final long orgId, final long id) {
        final EntityEmail entityEmail = bulkMailService.getEntityEmail(orgId, id);
        final JsonObject json = Json.object();

        json.put(BulkMailFields.SUBJECT.getName(), entityEmail.getSubject());
        json.put(BulkMailFields.MESSAGE.getName(), entityEmail.getContent());

        return json;
    }

    /**
     * Sets the {@link MailResponse} for send related apis
     *
     * @param mailResponse Represents the {@link MailResponse}
     * @param crmResponse  Represents the {@link CrmResponse}
     */
    private void setResponse(final MailResponse mailResponse, final CrmResponse crmResponse) {
        final Collection<ErrorDescriptor> errors = (Collection<ErrorDescriptor>) mailResponse.map().get(BulkMailFields.VALIDATION_ERRORS.getName());

        if (Objects.nonNull(errors) && !errors.isEmpty()) {
            final ErrorCode errorCode = ErrorCode.get(BulkMailErrorCodes.CREATE_ACTION_FAILED, "i18n.notifications.bulk.mail.create.action.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * Sets the recipients for both contact related emails and unknown emails
     *
     * @param mail Represents the {@link MailInfo}
     */
    private void setRecipients(final MailInfo mail) {
        final EntityOutboundEmail outboundEmail = mail.getOutboundEmail();
        final Long orgId = outboundEmail.orgId();
        final ContactRecipients contactRecipients = outboundEmail.getContactRecipients();

        if (Objects.nonNull(contactRecipients)) {
            final Collection<Long> contactEmailIds = contactRecipients.getEmailIds();
            final Map<Long, String> contactEmails = contactEmailIds.isEmpty() ? Collections.emptyMap()
                    : contactManager.getContactEmailIds(orgId, contactEmailIds);
            final EmailRecipients recipients = new EmailRecipients();

            if (!contactEmails.isEmpty()) {
                recipients.addRecipients(AddressType.TO, getEmailRecipients(contactRecipients, contactEmails));
            }

            outboundEmail.setRecipients(List.of(recipients));
        }
    }

    /**
     * Gets the {@link Collection} of {@link EmailAddress} email recipients.
     *
     * @param contactRecipients Refers to the contact recipients
     * @param contactEmails     Represents the contact relate recipient ids
     * @return the collections of {@link EmailAddress} mapped in recipients
     */
    private Collection<EmailAddress> getEmailRecipients(final ContactRecipients contactRecipients, final Map<Long, String> contactEmails) {
        final Collection<Long> recipientIds = contactRecipients.toEmailIds();
        final Collection<String> recipients = new HashSet<>();
        final Collection<Contact> toRecipientsContact = Optional.ofNullable(contactRecipients.getContacts())
                .map(contacts -> contacts.get(AddressType.TO)).orElseGet(() -> null);

        if (Objects.nonNull(contactEmails)) {

            if (Objects.nonNull(recipientIds) && !recipientIds.isEmpty()) {
                Optional.ofNullable(toRecipientsContact).ifPresent(contacts -> contacts.forEach(contact ->
                        Optional.ofNullable(contact.getPrimaryEmail()).ifPresent(primaryEmail ->
                                primaryEmail.setValue(contactEmails.get(primaryEmail.id())))));
                recipients.addAll(contactEmails.values());
            }

            if (DEFAULT_LIMIT_FOR_CONVERSATION < recipients.size()) {
                throw ErrorCode.get(BulkMailErrorCodes.LIMIT_EXCEEDED, "i18n.notifications.bulk.mail.recipients.size.exceeded");
            }
        }

        return recipients.stream().map(EmailAddress::new).toList();
    }

    /**
     * Gets the {@link JsonObject} of bulk mail {@link Metrics} for the given organization and user.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param data   Represents the request {@link JsonObject} data
     * @return {@link JsonObject} of {@link Metrics}
     */
    private JsonObject getBulkMailMetrics(final long orgId, final long userId, final JsonObject data) {
        final long bulkMailId = data.getLong(BulkMailFields.ID.getName());

        if (bulkMailMetricsManager.exists(orgId, userId, bulkMailId)) {
            final Metrics metrics = bulkMailMetricsManager.getBulkMailMetrics(orgId, userId, bulkMailId);
            final JsonObject jsonObject = Json.object();

            jsonObject.put(BulkMailFields.ID.getName(), metrics.id());
            jsonObject.put(BulkMailFields.ORG_ID.getName(), metrics.orgId());
            jsonObject.put(BulkMailFields.SENDER_ID.getName(), metrics.getSenderId());
            jsonObject.put(BulkMailFields.SENT.getName(), metrics.getSentCount());
            jsonObject.put(BulkMailFields.OPENED.getName(), metrics.getOpenedCount());
            jsonObject.put(BulkMailFields.CLICKED.getName(), metrics.getClickedCount());
            jsonObject.put(BulkMailFields.REPLIED.getName(), metrics.getRepliedCount());
            jsonObject.put(BulkMailFields.OPENED_NOT_CLICKED.getName(), metrics.getOpenedNotClickedCount());
            jsonObject.put(BulkMailFields.OPENED_NOT_REPLIED.getName(), metrics.getOpenedNotRepliedCount());
            jsonObject.put(BulkMailFields.NOT_SENT.getName(), metrics.getNotSentCount());
            jsonObject.put(BulkMailFields.NOT_OPENED.getName(), metrics.getNotOpenedCount());

            return jsonObject;
        } else {
            throw ErrorCode.get(BulkMailErrorCodes.BULK_MAIL_METRICS_NOT_FOUND, "i18n.notifications.bulk.mail.metrics.not.found");
        }
    }

    /**
     * Gets the {@link JsonArray} of bulk mail {@link MetricsInsight} based on given request data and organization.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param data   Represents the request {@link JsonObject} data
     * @return {@link JsonArray} of {@link MetricsInsight}
     */
    private JsonArray getMetricsInsights(final long orgId, final long userId, final JsonObject data, final CrmResponse response) {
        final long bulkMailId = data.getLong(BulkMailFields.ID.getName());

        if (bulkMailMetricsManager.exists(orgId, userId, bulkMailId)) {
            final MetricsType type = MetricsType.getMetricType(data.getString(BulkMailFields.TYPE.getName()));
            final Collection<MetricsInsight> metricsInsights = bulkMailMetricsManager.getMetricsInsights(orgId, userId, bulkMailId, type);
            final JsonArray metricsArray = Json.array();
            final String valueKey = BulkMailFields.VALUE.getName();
            final String statusKey = BulkMailFields.STATUS.getName();

            for (final MetricsInsight metricsInsight : metricsInsights) {
                final JsonObject jsonObject = Json.object();
                final List<String> resolvedNames = metricsInsight.types().stream()
                        .map(metricsType -> response.resolveI18nString(metricsType.getName())).toList();
                final String status = IntStream.range(0, resolvedNames.size())
                        .mapToObj(index -> {
                            final String name = resolvedNames.get(index);

                            return index == 0 ? name : name.toLowerCase();
                        }).collect(Collectors.joining(", "));

                jsonObject.put(valueKey, metricsInsight.data());
                jsonObject.put(statusKey, status);

                metricsArray.put(jsonObject);
            }

            return metricsArray;
        } else {
            throw ErrorCode.get(BulkMailErrorCodes.BULK_MAIL_METRICS_NOT_FOUND, "i18n.notifications.bulk.mail.metrics.not.found");
        }
    }

    /**
     * <p>
     * Handles the bulk mail to proceed with fetching owner details or to fill the
     * placeholders with appropriate data before dispatching the mail
     * </p>
     *
     * @param bulkMail    Represents the {@link BulkMail}
     * @param response    Represents the {@link CrmResponse}
     * @param isDraft     A flag to indicate the processed bulk mail is draft or for send action
     * @param requestData Represents the json which contains request data
     */
    private void resolveBulkMailToDeliver(final BulkMail bulkMail, final CrmResponse response, final boolean isDraft,
                                          final JsonObject requestData) {
        if ((bulkMail.getMailInfo().stream().filter(Objects::nonNull).map(MailInfo::getOutboundEmail).flatMap(mail -> Objects.isNull(mail.getMailPlaceHolders())
                ? Stream.empty() : mail.getMailPlaceHolders().stream()).anyMatch(MailPlaceHolder::isOwnerField))) {
            fetchOwnersAndFillPlaceHolders(response, bulkMail, isDraft, requestData);
        } else {
            formatAndDispatchBulkMail(bulkMail, null, isDraft, response, requestData);
        }
    }

    /**
     * <p>
     * Fetches the owners and fills the place holders with its corresponding data.
     * </p>
     *
     * @param response    Represents the {@link CrmResponse}
     * @param bulkMail    Represents the {@link BulkMail}
     * @param isDraft     A flag to indicate whether mail is created for draft or send action
     * @param requestData Represents the json which contains request data
     */
    private void fetchOwnersAndFillPlaceHolders(final CrmResponse response, final BulkMail bulkMail, final boolean isDraft,
                                                final JsonObject requestData) {
        final Collection<Long> iamUserIds = getUserIds(bulkMail);

        if (!iamUserIds.isEmpty()) {
            response.convertIntoAsync();
            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    try {
                        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                        formatAndDispatchBulkMail(bulkMail, userMap, isDraft, response, requestData);
                    } catch (Throwable throwable) {
                        sendError(response, null, throwable);
                    } finally {
                        response.send();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(response, null, ErrorCode.get(EmailErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                }
            };
            userService.fetchUsers(organizationService.getIamOrgId(bulkMail.orgId()), iamUserIds, usersFetchCallBack);
        } else {
            formatAndDispatchBulkMail(bulkMail, null, isDraft, response, requestData);
        }
    }

    /**
     * <p>
     * Fetches the user ids from bulk mail to process with callback.
     * </p>
     *
     * @param bulkMail Represents the {@link BulkMail}
     * @return {@link Collection} of user ids
     */
    private Collection<Long> getUserIds(final BulkMail bulkMail) {
        final Collection<Long> iamUserIds = new HashSet<>();

        for (final MailInfo mailInfo : bulkMail.getMailInfo()) {

            if (Objects.nonNull(mailInfo)) {
                final EntityOutboundEmail outboundEmail = mailInfo.getOutboundEmail();

                if (outboundEmail.hasPlaceHolders()) {
                    final Collection<MailPlaceHolder> placeHolders = outboundEmail.getMailPlaceHolders();

                    if (Objects.nonNull(placeHolders) && !placeHolders.isEmpty()) {

                        for (final MailPlaceHolder placeHolder : placeHolders) {

                            if (Objects.nonNull(placeHolder) && placeHolder.isOwnerField()) {
                                final Long userId = placeHolder.getLong();

                                if (Objects.nonNull(userId)) {
                                    iamUserIds.add(userId);
                                }
                            }
                        }
                    }
                }
            }
        }

        return iamUserIds;
    }

    /**
     * <p>
     * Process the bulk mail attributes, formatting with appropriate placeholders and dispatches it based
     * on draft or send action
     * </p>
     *
     * @param bulkMail    Represents the {@link BulkMail}
     * @param users       Represents the {@link Collection} of users
     * @param isDraft     A flag to indicate the processed bulk mail is draft or for send action
     * @param response    Represents the {@link CrmResponse}
     * @param requestData Represents the json which contains request data
     */
    private void formatAndDispatchBulkMail(final BulkMail bulkMail, final Map<Long, User> users, final boolean isDraft,
                                           final CrmResponse response, final JsonObject requestData) {

        for (final MailInfo mailInfo : bulkMail.getMailInfo()) {
            final EntityOutboundEmail outboundEmail = mailInfo.getOutboundEmail();

            if (outboundEmail.hasPlaceHolders()) {

                if (Objects.nonNull(users) && !users.isEmpty()) {
                    outboundEmail.setPlaceHolders(placeHolderExtractor.resolvePlaceHoldersWithUser(outboundEmail.getMailPlaceHolders(), users));
                }
                final Collection<PlaceHolder> placeHolders = outboundEmail.placeHolders();
                final String subject = outboundEmail.getSubject();
                final String content = outboundEmail.getContent();

                if (outboundEmail.isSubjectHasPlaceHolders() && Objects.nonNull(subject)) {
                    outboundEmail.setSubject(placeHolderResolver.resolve(subject,
                            placeHolderExtractor.getSubjectPlaceHolders(placeHolders)));
                }

                if (outboundEmail.isContentHasPlaceHolders() && Objects.nonNull(content)) {
                    outboundEmail.setContent(placeHolderResolver.resolve(content,
                            placeHolderExtractor.getContentPlaceHolders(placeHolders)));
                }
            }
        }

        if (isDraft) {
            final MailResponse mailResponse = bulkMailService.bulkDraft(bulkMail, requestData);

            if (Objects.nonNull(mailResponse)) {
                setResponse(mailResponse, response);
            }
        } else {
            setResponse(bulkMailService.bulkSend(bulkMail, requestData), response);
        }
    }
}