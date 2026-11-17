package com.tlc.crm.core.notification.email.api.model.converter;

import com.tlc.attachment.model.profilePicture.DisplayPicture;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.model.email.EmailVariant;
import com.tlc.crm.core.notification.email.api.model.email.EntityDraftEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityInboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;

import com.tlc.crm.core.notification.email.api.model.email.metrics.OutboundMetrics;
import com.tlc.crm.core.notification.email.bulk.api.model.MailInfo;
import com.tlc.crm.core.notification.email.bulk.fields.BulkMailFields;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.core.notification.email.role.permission.controller.EmailLimitController;
import com.tlc.crm.core.notification.email.role.permission.controller.request.EmailLimitControllerRequest;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.security.permission.model.LimitActionType;
import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.type.OutboundEmail;
import com.tlc.email.common.service.EmailSanitizeService;
import com.tlc.file.client.converter.AttachmentConverter;
import com.tlc.file.common.models.Attachment;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.EntityThread;

import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;

import com.tlc.crm.email.notification.email.api.model.settings.EmailSettings;
import com.tlc.crm.email.notification.email.api.model.settings.EmailLinkerSettings;
import com.tlc.crm.email.notification.email.api.model.settings.EmailTrackerSettings;
import com.tlc.crm.email.notification.email.api.model.address.UserBlockedEmailAddress;
import com.tlc.crm.email.notification.email.api.model.settings.EmailSignature;

import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.Email.MessageCategory;
import com.tlc.email.common.model.type.DraftEmail.DraftAction;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import static com.tlc.email.common.model.address.AddressType.BCC;
import static com.tlc.email.common.model.address.AddressType.CC;
import static com.tlc.email.common.model.address.AddressType.TO;
import static com.tlc.email.common.model.type.Email.MessageCategory.INBOUND;
import static com.tlc.email.common.model.type.Email.MessageCategory.OUTBOUND;
import static com.tlc.email.common.model.type.Email.MessageCategory.DRAFT;
import static com.tlc.email.common.model.type.Email.MessageCategory.SCHEDULED;

/**
 * <p>
 * Provides different ways to convert the data from database into {@link EntityThread}
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailConverter extends EntityConverter<EntityThread> {

    private final AttachmentConverter attachmentConverter;
    private final ContactConverter contactConverter;
    private final CrmUserConverter crmUserConverter;
    private final EmailSanitizeService emailSanitizeService;
    private final CoreMailManager coreMailManager;

    private EmailConverter() {
        attachmentConverter = AttachmentConverter.getInstance();
        contactConverter = ContactConverter.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        emailSanitizeService = EmailSanitizeService.getInstance();
        coreMailManager = CoreMailManager.getInstance();
    }

    private static class Instance {
        private static final EmailConverter INSTANCE = new EmailConverter();
    }

    /**
     * Gets the {@link EmailConverter} Instance
     *
     * @return {@link EmailConverter}
     */
    public static EmailConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts json attributes data into {@link EntityThread}
     * </p>
     *
     * @param converterConfig Converter configuration settings
     * @param jsonObject      Holds a mail attributes
     * @return {@link EntityThread}
     */
    @Override
    public EntityThread convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return constructEmail(converterConfig, jsonObject);
    }

    /**
     * <p>
     * Converts json attributes into {@link EntityThread}
     * </p>
     *
     * @param converterConfig Converter configuration settings
     * @param jsonObject      Represents the json object
     * @return {@link Thread}
     */
    private EntityThread constructEmail(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final EntityThread thread = new EntityThread();
        final String idKey = CoreEmailFields.ID.getName();
        final Long orgId = converterConfig.orgId();
        final String draftActionKey = CoreEmailFields.DRAFT_ACTION.getName();
        final EmailLimitController limitController = new EmailLimitController();
        final EmailLimitControllerRequest limitCheckerRequest = new EmailLimitControllerRequest(LimitActionType.INDIVIDUAL_EMAIL);
        final Optional<FromAddressInfo> optionalFromAddressInfo = getFromAddressInfo(orgId, jsonObject);
        final EntityEmail entityEmail;

        try {
            if (!limitController.canProceed(limitCheckerRequest)) {
                final EntityDraftEmail draftEmail = new EntityDraftEmail();

                draftEmail.setDraftAction(jsonObject.containsKey(draftActionKey) ? DraftAction.UPDATE : DraftAction.CREATE);
                entityEmail = draftEmail;
            } else if (jsonObject.containsKey(CoreEmailFields.SCHEDULED_TIME.getName())) {
                final EntityScheduledEmail scheduledMail = new EntityScheduledEmail();

                scheduledMail.setScheduledTime(jsonObject.getLong(CoreEmailFields.SCHEDULED_TIME.getName()));
                entityEmail = scheduledMail;
            } else if (jsonObject.containsKey(draftActionKey)) {
                entityEmail = getEmail(jsonObject, DraftAction.get(jsonObject.getShort(draftActionKey)), optionalFromAddressInfo);
            } else {
                entityEmail = new EntityOutboundEmail();
            }
        } finally {
            limitController.clearLimitHistory(LimitActionType.INDIVIDUAL_EMAIL);
        }

        if (jsonObject.containsKey(idKey)) {
            final Long id = jsonObject.getLong(idKey);

            entityEmail.setConversationId(id);
            entityEmail.setParentConversationId(id);
        }
        thread.setEntityEmails(List.of(entityEmail));
        setJsonDataToModel(converterConfig, thread, jsonObject);

        return thread;
    }

    /**
     * <p>
     * Determines and constructs the appropriate {@link EntityEmail} based on
     * draft action and from-address information.
     * </p>
     *
     * @param jsonObject      Represents the JSON object
     * @param draftAction     Action to be performed on the draft
     * @param fromAddressInfo Optional from-address information
     * @return {@link EntityEmail}
     */
    private EntityEmail getEmail(final JsonObject jsonObject, final DraftAction draftAction, final Optional<FromAddressInfo> fromAddressInfo) {
        if (fromAddressInfo.isPresent() && !fromAddressInfo.get().getType().isSyncType() && draftAction.isSend()) {
            jsonObject.remove(CoreEmailFields.DRAFT_ACTION.getName());

            return new EntityOutboundEmail();
        }

        return getEntityDraftEmail(draftAction);
    }

    /**
     * <p>
     * Creates and initializes a {@link EntityDraftEmail} with the given draft action.
     * </p>
     *
     * @param draftAction Action to be set on the draft email
     * @return {@link EntityDraftEmail}
     */
    private EntityDraftEmail getEntityDraftEmail(final DraftAction draftAction) {
        final EntityDraftEmail draftEmail = new EntityDraftEmail();

        draftEmail.setDraftAction(draftAction);

        return draftEmail;
    }

    /**
     * <p>
     * Retrieves {@link FromAddressInfo} from the JSON object if present.
     * </p>
     *
     * @param orgId      Organization identifier
     * @param jsonObject Represents the JSON object
     * @return {@link Optional} containing {@link FromAddressInfo} if available
     */
    private Optional<FromAddressInfo> getFromAddressInfo(final Long orgId, final JsonObject jsonObject) {
        if (jsonObject.containsKey(CoreEmailFields.FROM_ADDRESS.getName())) {
            return coreMailManager.getFromAddressInfo(orgId, jsonObject.getLong(CoreEmailFields.FROM_ADDRESS.getName()));
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Sets JSON data into {@link EntityEmail}
     * </p>
     *
     * @param converterConfig Converter configuration settings
     * @param thread          An entity that holds thread attributes
     * @param jsonObject      Holds the field values of a mail
     */
    private void setJsonDataToModel(final ConverterConfig converterConfig, final EntityThread thread, final JsonObject jsonObject) {
        final EntityOutboundEmail mail = thread.getComposedEmail();

        if (Objects.nonNull(mail)) {
            final Long orgId = converterConfig.orgId();
            final EmailRecipients recipients = new EmailRecipients();
            final String entityIdKey = CoreEmailFields.ENTITY_ID.getName();
            final String sourceKey = CoreEmailFields.SOURCE.getName();
            final String draftActionKey = CoreEmailFields.DRAFT_ACTION.getName();
            final String templateKey = CoreEmailFields.TEMPLATE_ID.getName();
            final String conversationIdKey = CoreEmailFields.CONVERSATION_ID.getName();

            if (jsonObject.containsKey(conversationIdKey)) {
                mail.setConversationId(jsonObject.getLong(conversationIdKey));
            }
            mail.setSubject(jsonObject.optString(CoreEmailFields.SUBJECT.getName(), mail.getSubject()));
            mail.setContent(jsonObject.optString(CoreEmailFields.MESSAGE.getName(), ""));

            if (jsonObject.containsKey(draftActionKey)) {

                if (!mail.isScheduled()) {
                    final EntityDraftEmail draftEmail = (EntityDraftEmail) mail;
                    final DraftAction draftAction = draftEmail.getDraftAction();

                    if (Objects.isNull(draftEmail.getDraftAction())) {
                        draftEmail.setDraftAction(DraftAction.get(jsonObject.getShort(draftActionKey)));
                    }

                    if (Objects.equals(DraftAction.CREATE, draftEmail.getDraftAction())) {

                        if (jsonObject.containsKey(CoreEmailFields.OUTBOUND_TYPE.getName())) {
                            draftEmail.setDraftEnabled(jsonObject.optBoolean(CoreEmailFields.IS_DRAFT_ENABLED.getName()));
                        }
                    }
                    draftEmail.setMessageCategory(Objects.equals(DraftAction.SEND, draftAction) ? OUTBOUND : DRAFT);
                }
            } else {
                mail.setMessageCategory(OUTBOUND);
            }

            if (jsonObject.containsKey(sourceKey)) {
                thread.setSource(Source.valueOf(jsonObject.getString(sourceKey).toUpperCase()));
            }

            if (jsonObject.containsKey(entityIdKey)) {
                thread.setEntityId(jsonObject.getLong(entityIdKey));
            }

            if (jsonObject.containsKey(templateKey)) {
                mail.setTemplateId(jsonObject.getLong(templateKey));
            }
            setRecipientJsonToModel(jsonObject, mail, recipients);

            setDesignatedFieldsForMail(mail, jsonObject);

            if (jsonObject.containsKey(EmailFields.OUTBOUND_TYPE)) {
                mail.setOutboundType(OutboundEmail.OutboundType.get(jsonObject.getShort(EmailFields.OUTBOUND_TYPE)));
            }

            mail.setOrgId(orgId);
            mail.setUserId(converterConfig.ownerId());
            mail.setActionTime(Instant.now().getEpochSecond());
            mail.setUnsubscribed(jsonObject.optBoolean(CoreEmailFields.IS_UNSUBSCRIBED.getName()));
            thread.setOrgId(orgId);
        }
    }

    /**
     * <p>
     * Sets Json data into {@link ContactRecipients}
     * </p>
     *
     * @param jsonObject Holds the field values of mail
     * @param mail       A model that stores a mail attributes
     * @param recipients Represents the recipient
     */
    private void setRecipientJsonToModel(final JsonObject jsonObject, final EntityEmail mail, final EmailRecipients recipients) {
        final ContactRecipients contactRecipients = new ContactRecipients();
        final String toAddressKey = CoreEmailFields.TO_ADDRESS.getName();
        final String bccAddressKey = CoreEmailFields.BCC_ADDRESS.getName();
        final String ccAddressKey = CoreEmailFields.CC_ADDRESS.getName();
        final Map<AddressType, Collection<EmailAddress>> recipientsMap = new HashMap<>();

        if (jsonObject.containsKey(toAddressKey)) {
            recipientsMap.put(AddressType.TO, constructRecipientEmails(jsonObject.getJsonObject(toAddressKey), contactRecipients, TO));
        }

        if (jsonObject.containsKey(bccAddressKey)) {
            recipientsMap.put(AddressType.BCC, constructRecipientEmails(jsonObject.getJsonObject(bccAddressKey), contactRecipients, BCC));
        }

        if (jsonObject.containsKey(ccAddressKey)) {
            recipientsMap.put(AddressType.CC, constructRecipientEmails(jsonObject.getJsonObject(ccAddressKey), contactRecipients, CC));
        }

        recipients.set(recipientsMap);
        mail.setRecipients(List.of(recipients));
        mail.setContactRecipients(contactRecipients);
    }

    /**
     * <p>
     * Gets all the recipient addresses from the json object
     * </p>
     *
     * @param recipientAddressJson Holds the field values of mail recipient
     * @param contactRecipients    A model to which the contact recipient attributes will be set
     * @param recipientType        Represents the recipient type TO, BCC, CC
     * @return {@link EmailAddress}
     */
    private Collection<EmailAddress> constructRecipientEmails(final JsonObject recipientAddressJson, final ContactRecipients contactRecipients,
                                                              final AddressType recipientType) {
        final String contactEmailKey = CoreEmailFields.CONTACTS_EMAILS.getName();
        final String emailsKey = CoreEmailFields.EMAILS.getName();
        final Collection<String> addresses = new HashSet<>();

        if (recipientAddressJson.containsKey(contactEmailKey)) {
            final JsonArray jsonArray = recipientAddressJson.getJsonArray(contactEmailKey);

            for (int index = 0; index < jsonArray.size(); index++) {
                final Long contactEmailId = jsonArray.getLong(index);

                contactRecipients.addContact(recipientType, contactEmailId);
            }
        }

        if (recipientAddressJson.containsKey(emailsKey)) {
            final JsonArray emails = recipientAddressJson.getJsonArray(emailsKey);

            for (int index = 0; index < emails.size(); index++) {
                addresses.add(emails.getString(index).trim());
            }
        }
        return addresses.stream().map(EmailAddress::new).toList();
    }

    /**
     * <p>
     * Converts {@link EntityThread} into {@link JsonObject}
     * </p>
     *
     * @param thread A model that holds the attributes of thread
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final EntityThread thread) {
        final Collection<EntityEmail> mails = thread.getEntityEmails();
        final JsonObject threadJson = Json.object();
        final Long entityId = thread.getEntityId();

        if (Objects.nonNull(entityId)) {
            threadJson.put(CoreEmailFields.DEAL_ID.getName(), entityId);
        }
        threadJson.put(CoreEmailFields.THREAD_ID.getName(), thread.getCrmId());
        threadJson.put(CoreEmailFields.EMAILS.getName(), convertMailsToJsonArray(mails));

        return threadJson;
    }

    /**
     * <p>
     * Gets the recipients as {@link JsonArray}
     * </p>
     *
     * @param addressType       Represents the address type
     * @param recipients        Represents the recipients
     * @param contactRecipients Represents the contact recipients
     * @return the {@link JsonArray}
     */
    private JsonArray getRecipients(final AddressType addressType, final EmailRecipients recipients,
                                    final ContactRecipients contactRecipients) {
        final Collection<EmailAddress> emailAddresses = fetchRecipientEmailAddresses(addressType, recipients);
        final Collection<String> recipientAddresses = new HashSet<>();

        if (Objects.nonNull(emailAddresses) && !emailAddresses.isEmpty()) {
            emailAddresses.forEach(emailAddress -> recipientAddresses.add(emailAddress.getEmailAddress()));
        }

        if (Objects.nonNull(contactRecipients)) {
            final Collection<EmailAddress> contactAddresses = contactRecipients.getEmailAddresses().get(addressType);

            if (Objects.nonNull(contactAddresses)) {
                contactAddresses.forEach(emailAddress -> recipientAddresses.add(emailAddress.getEmailAddress()));
            }
        }

        return Json.array(recipientAddresses);
    }

    /**
     * <p>
     * Fetches the email addresses based on the given address type
     * </p>
     *
     * @param addressType Represents the {@link AddressType}
     * @param recipients  Represents the recipients
     * @return the collection of address
     */
    private Collection<EmailAddress> fetchRecipientEmailAddresses(final AddressType addressType, final EmailRecipients recipients) {
        return switch (addressType) {
            case FROM -> throw ErrorCode.get(EmailErrorCodes.INVALID_RECIPIENT);
            case TO -> recipients.toRecipients();
            case BCC -> recipients.bccRecipients();
            case CC -> recipients.ccRecipients();
        };
    }

    /**
     * <p>
     * Converts {@link EntityEmail} into {@link JsonArray}
     * </p>
     *
     * @param mails Represents the collection of {@link EntityEmail}
     * @return {@link JsonArray}
     */
    public JsonArray convertMailsToJsonArray(final Collection<EntityEmail> mails) {
        final JsonArray mailArray = Json.array();

        for (final EntityEmail mail : mails) {
            final JsonObject jsonObject = Json.object();
            final EmailAddress fromAddress = mail.getSender();
            final Contact contact = mail.getContact();
            final Collection<Attachment> attachments = mail.getEntityAttachments();
            final OutboundMetrics outboundMetrics = mail.getOutboundMetrics();
            final JsonObject fromJson = Json.object();
            final MessageCategory messageCategory = mail.getMessageCategory();

            loadEmailDataToJson(jsonObject, mail);
            jsonObject.put(CoreEmailFields.ID.getName(), mail.getCrmId());

            if (Objects.nonNull(contact)) {
                final DisplayPicture thumbnail = contact.getThumbnail();
                final Long id = contact.id();
                final Email primaryEmail = contact.getPrimaryEmail();

                fromJson.put(CoreEmailFields.CONTACT_NAME.getName(), contact.getFullName());
                fromJson.put(CoreEmailFields.CONTACT_ID.getName(), id);
                fromJson.put(CoreEmailFields.EMAIL.getName(), fromAddress.getEmailAddress());

                if (Objects.nonNull(primaryEmail)) {
                    fromJson.put(CoreEmailFields.EMAIL_ID.getName(), primaryEmail.id());
                }

                if (Objects.nonNull(thumbnail)) {
                    fromJson.put(CoreEmailFields.THUMBNAIL.getName(), contactConverter.convertThumbnailToJson(id, Contact.ENTITY_TYPE, thumbnail));
                }

            } else if (Objects.nonNull(fromAddress)) {
                final String emailId = fromAddress.getEmailAddress();

                fromJson.put(CoreEmailFields.EMAIL.getName(), emailId);
                fromJson.put(CoreEmailFields.NAME.getName(), EmailAddress.extractNameFromEmail(emailId));
                fromJson.put(CoreEmailFields.ID.getName(), fromAddress instanceof FromAddressInfo fromAddressInfo ?
                        fromAddressInfo.getCrmEmailId() : null);
            }

            if (Objects.nonNull(attachments)) {
                final JsonArray attachmentArray = Json.array();

                attachments.forEach(attachment -> attachmentArray.put(attachmentConverter.convert(attachment)));
                jsonObject.put(CoreEmailFields.ATTACHMENTS.getName(), attachmentArray);
            }

            if (Objects.nonNull(outboundMetrics)) {
                jsonObject.put(CoreEmailFields.OPENED.getName(), outboundMetrics.getOpenedCount());
                jsonObject.put(CoreEmailFields.CLICKED.getName(), outboundMetrics.getClickedCount());
            }

            if (INBOUND == messageCategory) {
                final EntityInboundEmail inboundEmail = (EntityInboundEmail) mail;

                jsonObject.put(CoreEmailFields.IS_READ.getName(), inboundEmail.isRead());
            } else if (messageCategory.isSendCategory() && !mail.getEmailVariant().isSmartBcc()) {
                final EntityOutboundEmail outboundEmail = (EntityOutboundEmail) mail;
                final Collection<EmailTrackingDetail> emailTrackingDetails = outboundEmail.getEmailTrackingDetails();

                if (Objects.nonNull(emailTrackingDetails) && !emailTrackingDetails.isEmpty()) {
                    jsonObject.put(CoreEmailFields.TRACKING.getName(), convertTrackingDetailsIntoJson(emailTrackingDetails));
                }
                jsonObject.put(CoreEmailFields.IS_BOUNCED.getName(), outboundEmail.isBounced());
            }

            jsonObject.put(CoreEmailFields.IS_ARCHIVED.getName(), mail.isArchived());
            jsonObject.put(CoreEmailFields.IS_ACTIVE.getName(), mail.isActive());
            jsonObject.put(CoreEmailFields.IS_DRAFT.getName(), mail.isDraft());
            jsonObject.put(CoreEmailFields.IS_STARRED.getName(), mail.isStarred());
            jsonObject.put(CoreEmailFields.MESSAGE_CATEGORY.getName(), messageCategory.name());
            jsonObject.put(CoreEmailFields.IS_SYNCED.getName(), mail.isSynced());
            jsonObject.put(CoreEmailFields.CONVERSATION_ID.getName(), mail.getConversationId());
            jsonObject.put(CoreEmailFields.FROM.getName(), fromJson);
            jsonObject.put(CoreEmailFields.VARIANT.getName(), mail.getEmailVariant().getType().name());

            if (SCHEDULED == messageCategory) {
                final EntityScheduledEmail scheduledMail = (EntityScheduledEmail) mail;

                jsonObject.put(CoreEmailFields.TIME_STAMP.getName(), scheduledMail.getScheduledTime());
            }

            mailArray.put(jsonObject);
        }

        return mailArray;
    }

    /**
     * <p>
     * Converts the data into {@link EmailSignature} model
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @param data   Represents the {@link JsonObject}
     * @return {@link EmailSignature}
     */
    public EmailSignature convertEmailSignature(final long orgId, final long userId, final JsonObject data) {
        final EmailSignature emailSignature = new EmailSignature();

        emailSignature.setOrgId(orgId);
        emailSignature.setUserId(userId);
        emailSignature.setEmailSignature(data.getString(CoreEmailFields.EMAIL_SIGNATURE.getName()).trim());

        return emailSignature;
    }

    /**
     * Loads the {@link EntityEmail} to {@link JsonObject}.
     *
     * @param jsonObject  Represents the request {@link JsonObject} data
     * @param entityEmail Represents the {@link EntityEmail}
     */
    private void loadEmailDataToJson(final JsonObject jsonObject, final EntityEmail entityEmail) {
        final EmailRecipients recipients = entityEmail.getEmailRecipients();
        final String message = entityEmail.getContent();
        final String subject = entityEmail.getSubject();
        final Long date = entityEmail.getMetaData().getDate();
        final JsonObject recipientJson = Json.object();
        final ContactRecipients contactRecipients = entityEmail.getContactRecipients();
        final JsonArray toRecipients = getRecipients(TO, recipients, contactRecipients);
        final JsonArray bccRecipients = getRecipients(BCC, recipients, contactRecipients);
        final JsonArray ccRecipients = getRecipients(CC, recipients, contactRecipients);

        if (toRecipients.size() > 0) {
            recipientJson.put(CoreEmailFields.TO.getName(), toRecipients);
        }

        if (bccRecipients.size() > 0) {
            recipientJson.put(CoreEmailFields.BCC.getName(), bccRecipients);
        }

        if (ccRecipients.size() > 0) {
            recipientJson.put(CoreEmailFields.CC.getName(), ccRecipients);
        }
        jsonObject.put(CoreEmailFields.RECIPIENTS.getName(), recipientJson);

        if (Objects.nonNull(message)) {
            jsonObject.put(CoreEmailFields.MESSAGE_PREVIEW.getName(), emailSanitizeService.trimContent(message));
            jsonObject.put(CoreEmailFields.MESSAGE.getName(), message);
        }

        if (Objects.nonNull(subject)) {
            jsonObject.put(CoreEmailFields.SUBJECT.getName(), subject);
        }

        if (Objects.nonNull(date)) {
            jsonObject.put(CoreEmailFields.TIME_STAMP.getName(), date);
        }
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link EmailSettings}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @param data   Represents the {@link JsonObject}
     * @return {@link EmailSettings}
     */
    public EmailSettings convertEmailSettings(final Long orgId, final Long userId, final JsonObject data) {
        final EmailSettings emailSettings = new EmailSettings();
        final EmailSignature emailSignature = new EmailSignature();
        final String emailSignatureKey = CoreEmailFields.EMAIL_SIGNATURE.getName();
        final String blockedAddressKey = CoreEmailFields.BLOCKED_ADDRESS.getName();
        final String emailTrackerSettingsKey = CoreEmailFields.EMAIL_TRACKER_SETTINGS.getName();
        final String emailLinkerSettingsKey = CoreEmailFields.EMAIL_LINKER_SETTINGS.getName();
        final Collection<UserBlockedEmailAddress> addresses = new HashSet<>();

        if (data.containsKey(emailSignatureKey)) {
            emailSignature.setOrgId(orgId);
            emailSignature.setUserId(userId);
            emailSignature.setEmailSignature(data.getString(emailSignatureKey).trim());

            emailSettings.setEmailSignature(emailSignature);
        }

        if (data.containsKey(blockedAddressKey)) {
            final JsonArray blockedAddressArray = data.getJsonArray(blockedAddressKey);

            blockedAddressArray.forEach(blockedAddress -> addresses.add(new UserBlockedEmailAddress((String) blockedAddress)));
            emailSettings.setUserBlockedEmailAddresses(addresses);
        }

        if (data.containsKey(emailTrackerSettingsKey)) {
            emailSettings.setEmailTrackerSettings(new EmailTrackerSettings(data.getJsonObject(emailTrackerSettingsKey)));
        }

        if (data.containsKey(emailLinkerSettingsKey)) {
            emailSettings.setEmailLinkerSettings(new EmailLinkerSettings(data.getJsonObject(emailLinkerSettingsKey)));
        }
        emailSettings.setUserId(userId);
        emailSettings.setOrgId(orgId);

        return emailSettings;
    }

    /**
     * <p>
     * Sets the designated fields for an email based on the provided {@link EntityEmail} and {@link JsonObject}.
     * </p>
     *
     * @param mail       Represents the {@link EntityEmail}
     * @param jsonObject Represents the {@link JsonObject} request
     */
    public void setDesignatedFieldsForMail(final EntityOutboundEmail mail, final JsonObject jsonObject) {
        final String contactLinkingKey = CoreEmailFields.IS_CONTACT_LINKING_ENABLED.getName();
        final String dealLinkingKey = CoreEmailFields.IS_DEAL_LINKING_ENABLED.getName();
        final String isEmailTrackerEnabledKey = CoreEmailFields.IS_EMAIL_TRACKER_ENABLED.getName();
        final String isLinkTrackerEnabledKey = CoreEmailFields.IS_LINK_TRACKER_ENABLED.getName();
        final EmailLinkerSettings emailLinkerSettings = new EmailLinkerSettings();
        final EmailTrackerSettings emailTrackerSettings = new EmailTrackerSettings();
        final String attachmentIdsKey = CoreEmailFields.ATTACHMENT_IDS.getName();

        if (jsonObject.containsKey(attachmentIdsKey)) {
            final JsonArray attachmentArray = jsonObject.getJsonArray(attachmentIdsKey);
            final Collection<Attachment> attachments = new ArrayList<>();

            for (int index = 0; index < attachmentArray.size(); index++) {
                final Attachment emailAttachment = new Attachment();

                emailAttachment.setId(attachmentArray.getLong(index));
                attachments.add(emailAttachment);
            }
            mail.setEntityAttachments(attachments);
        }

        if (jsonObject.containsKey(contactLinkingKey)) {
            emailLinkerSettings.setContactLinkingEnabled(jsonObject.getBoolean(contactLinkingKey));
        }

        if (jsonObject.containsKey(dealLinkingKey)) {
            emailLinkerSettings.setDealLinkingEnabled(jsonObject.getBoolean(dealLinkingKey));
        }

        if (jsonObject.containsKey(isEmailTrackerEnabledKey)) {
            emailTrackerSettings.setEmailTrackerEnabled(jsonObject.getBoolean(isEmailTrackerEnabledKey));
        }

        if (jsonObject.containsKey(isLinkTrackerEnabledKey)) {
            emailTrackerSettings.setLinkTrackerEnabled(jsonObject.getBoolean(isLinkTrackerEnabledKey));
        }

        if (Objects.nonNull(mail.getTemplateId())) {
            emailTrackerSettings.setLinkTrackerEnabled(true);
            emailTrackerSettings.setEmailTrackerEnabled(true);
        }

        mail.setEmailTrackerSettings(emailTrackerSettings);
        mail.setEmailLinkerSettings(emailLinkerSettings);
        mail.setHasUnsubscribeLink(jsonObject.optBoolean(CoreEmailFields.HAS_UNSUBSCRIBED_LINK.getName(), false));
    }

    /**
     * <p>
     * Converts the recipients to the {@link JsonObject}
     * </p>
     *
     * @param emailRecipients   Represents the {@link EmailRecipients} specific to conversation
     * @param contactRecipients Represents the {@link ContactRecipients} specific to conversation
     * @return {@link JsonObject} with recipients
     */
    public JsonObject convertRecipientsToJson(final Map<Long, EmailRecipients> emailRecipients, final Map<Long, ContactRecipients> contactRecipients) {
        final JsonObject recipientsJson = Json.object();
        final JsonArray toAddressArray = Json.array();
        final Collection<String> emailAddresses = new HashSet<>();

        for (final Map.Entry<Long, ContactRecipients> contactRecipientsEntry : contactRecipients.entrySet()) {
            final Long conversationId = contactRecipientsEntry.getKey();
            final ContactRecipients mailRecipients = contactRecipientsEntry.getValue();
            final Contact contact = Objects.nonNull(mailRecipients) ? mailRecipients.getFirstToRecipient() : null;

            if (Objects.nonNull(contact)) {
                final JsonObject recipient = Json.object();

                recipient.put(CoreEmailFields.ID.getName(), contact.id());
                recipient.put(CoreEmailFields.NAME.getName(), contact.getFullName());
                recipient.put(CoreEmailFields.CONVERSATION_ID.getName(), conversationId);
                final com.tlc.crm.entity.models.type.Email primaryEmail = contact.getPrimaryEmail();

                if (Objects.nonNull(primaryEmail)) {
                    final String value = primaryEmail.getValue();

                    recipient.put(CoreEmailFields.MAIL_ID.getName(), value);
                    recipient.put(CoreEmailFields.CONTACT_EMAIL_ID.getName(), primaryEmail.id());
                    emailAddresses.add(value);
                }
                toAddressArray.put(recipient);
            }
        }

        for (final Map.Entry<Long, EmailRecipients> emailRecipientsEntry : emailRecipients.entrySet()) {
            final Long conversationId = emailRecipientsEntry.getKey();
            final EmailRecipients recipients = emailRecipientsEntry.getValue();
            final EmailAddress emailAddress = Objects.nonNull(recipients) ? recipients.getFirstToRecipient() : null;

            if (Objects.nonNull(emailAddress)) {
                final String email = emailAddress.getEmailAddress();

                if (!emailAddresses.contains(email)) {
                    final JsonObject recipient = Json.object();

                    recipient.put(CoreEmailFields.NAME.getName(), EmailAddress.extractNameFromEmail(email));
                    recipient.put(CoreEmailFields.MAIL_ID.getName(), email);
                    recipient.put(CoreEmailFields.CONVERSATION_ID.getName(), conversationId);

                    toAddressArray.put(recipient);
                }
            }
        }
        recipientsJson.put(CoreEmailFields.TO.getName(), toAddressArray);

        return recipientsJson;
    }

    /**
     * <p>
     * Converts {@link EntityEmail} into {@link JsonArray}
     * </p>
     *
     * @param mailInfo Represents the collection of {@link MailInfo}
     * @return {@link JsonArray}
     */
    public JsonArray convertMailInfoToJsonArray(final Collection<MailInfo> mailInfo) {
        final JsonArray mailArray = Json.array();

        for (final MailInfo mail : mailInfo) {
            final JsonObject mailJson = Json.object();
            final EntityOutboundEmail outboundEmail = mail.getOutboundEmail();
            final ContactRecipients contactRecipients = outboundEmail.getContactRecipients();
            final Contact contact = Objects.nonNull(contactRecipients) ? contactRecipients.getFirstToRecipient() : null;
            final EmailRecipients emailRecipients = outboundEmail.getEmailRecipients();
            String email = null;

            if (Objects.nonNull(emailRecipients)) {
                final EmailAddress emailAddress = emailRecipients.getFirstToRecipient();

                if (Objects.nonNull(emailAddress)) {
                    email = emailAddress.getEmailAddress();
                }
            }

            mailJson.put(BulkMailFields.NAME.getName(), Objects.nonNull(contact) ? contact.getFullName()
                    : Objects.nonNull(email) ? EmailAddress.extractNameFromEmail(email) : null);
            mailJson.put(CoreEmailFields.MAIL_ID.getName(), email);
            mailJson.put(BulkMailFields.TEMPLATE_ID.getName(), outboundEmail.getTemplateId());
            setMailInfoIntoJson(mail, mailJson);

            mailArray.put(mailJson);
        }

        return mailArray;
    }

    /**
     * Sets the {@link MailInfo} into {@link JsonObject}.
     *
     * @param mailInfo   Represents the {@link MailInfo}
     * @param jsonObject Represents the {@link JsonObject}
     */
    private void setMailInfoIntoJson(final MailInfo mailInfo, final JsonObject jsonObject) {
        final EntityOutboundEmail outboundEmail = mailInfo.getOutboundEmail();
        final JsonObject editJson = Json.object();

        editJson.put(CoreEmailFields.SUBJECT.getName(), outboundEmail.getSubject());
        editJson.put(CoreEmailFields.MESSAGE.getName(), outboundEmail.getContent());

        jsonObject.put(BulkMailFields.EDIT.getName(), editJson);
    }

    /**
     * <p>
     * Loads the recipient json based on the given contact details with respect to emails.
     * </p>
     *
     * @param contactMap    Holds the contact with respective recipient email
     * @param recipientJson Represents the recipient {@link JsonObject}
     */
    public void loadRecipientJson(final Map<String, Contact> contactMap, final JsonObject recipientJson) {
        final Collection<String> recipientTypes = Set.of(TO.getType(), CC.getType(), BCC.getType());

        for (final String recipientType : recipientTypes) {

            if (recipientJson.containsKey(recipientType)) {
                final JsonArray addressArray = Json.array();
                final JsonArray recipientEmails = recipientJson.getJsonArray(recipientType);

                for (int index = 0; index < recipientEmails.size(); index++) {
                    final String recipientEmail = recipientEmails.getString(index);
                    final Contact contact = contactMap.get(recipientEmail);

                    if (Objects.isNull(contact)) {
                        addressArray.put(getRecipientDetailsToJson(null, null, recipientEmail, null));
                    } else {
                        final Email primaryEmail = contact.getPrimaryEmail();

                        if (Objects.nonNull(primaryEmail)) {
                            addressArray.put(getRecipientDetailsToJson(contact.id(), contact.getFullName(), primaryEmail.getValue(), primaryEmail.id()));
                        }
                    }
                }
                recipientJson.put(recipientType, addressArray);
            }
        }
    }

    /**
     * <p>
     * Gets the recipients details into {@link JsonObject}.
     * </p>
     *
     * @param id      Represents the id of the recipient.
     * @param name    Represents the name of the recipient.
     * @param email   Represents the email of the recipient.
     * @param emailId Represents the contact email id
     * @return A json object holding recipient details.
     */
    private JsonObject getRecipientDetailsToJson(final Long id, final String name, final String email, final Long emailId) {
        final JsonObject recipientJson = Json.object();

        if (Objects.isNull(id)) {
            recipientJson.put(CoreEmailFields.NAME.getName(), email.split("@")[0]);
            recipientJson.put(CoreEmailFields.MAIL_ID.getName(), email);
        } else {
            recipientJson.put(CoreEmailFields.ID.getName(), id);
            recipientJson.put(CoreEmailFields.NAME.getName(), name);
            recipientJson.put(CoreEmailFields.MAIL_ID.getName(), email);
            recipientJson.put(CoreEmailFields.CONTACT_EMAIL_ID.getName(), emailId);
        }

        return recipientJson;
    }

    /**
     * <p>
     * Converts the single {@link EntityEmail} conversation to {@link JsonObject}
     * </p>
     *
     * @param entityEmail Represents the {@link EntityEmail} which holds the recipients and email data
     * @return {@link JsonObject} of mail
     */
    public JsonObject convert(final EntityEmail entityEmail) {
        final JsonObject jsonObject = Json.object();
        final Collection<Attachment> attachments = entityEmail.getEntityAttachments();
        final String message = entityEmail.getContent();
        final String subject = entityEmail.getSubject();
        final JsonObject recipientJson = Json.object();
        final JsonArray toAddressArray = Json.array();
        final JsonArray bccAddressArray = Json.array();
        final JsonArray ccAddressArray = Json.array();
        final EmailRecipients emailRecipients = entityEmail.getEmailRecipients();
        final ContactRecipients recipients = entityEmail.getContactRecipients();
        final Collection<Contact> contacts = new HashSet<>();
        final Collection<EmailAddress> addresses = new HashSet<>();
        final CrmUser user = entityEmail.getUser();
        final MessageCategory messageCategory = entityEmail.getMessageCategory();

        if (Objects.nonNull(recipients)) {
            final Map<AddressType, Collection<Contact>> contactRecipients = recipients.getContacts();

            if (Objects.nonNull(contactRecipients)) {

                for (final Map.Entry<AddressType, Collection<Contact>> recipient : contactRecipients.entrySet()) {
                    final AddressType addressType = recipient.getKey();
                    final Collection<Contact> contactValues = recipient.getValue();

                    switch (addressType) {
                        case TO -> addContactRecipientInfoJson(contactValues, toAddressArray);
                        case BCC -> addContactRecipientInfoJson(contactValues, bccAddressArray);
                        case CC -> addContactRecipientInfoJson(contactValues, ccAddressArray);
                    }
                }
                Optional.ofNullable(contactRecipients.get(AddressType.FROM)).ifPresent(contacts::addAll);
            }
        }
        final Map<AddressType, Collection<EmailAddress>> addressTypeCollectionMap = emailRecipients.get();

        if (Objects.nonNull(addressTypeCollectionMap)) {
            for (final Map.Entry<AddressType, Collection<EmailAddress>> recipient : addressTypeCollectionMap.entrySet()) {
                final AddressType addressType = recipient.getKey();
                final Collection<EmailAddress> recipientsInfo = recipient.getValue();

                switch (addressType) {
                    case TO -> addEmailRecipientInfo(recipientsInfo, toAddressArray);
                    case BCC -> addEmailRecipientInfo(recipientsInfo, bccAddressArray);
                    case CC -> addEmailRecipientInfo(recipientsInfo, ccAddressArray);
                }
            }
            Optional.ofNullable(addressTypeCollectionMap.get(AddressType.FROM)).ifPresent(addresses::addAll);
        }
        final Contact fromContact = contacts.isEmpty() ? null : contacts.iterator().next();
        final EmailAddress fromAddress = addresses.isEmpty() ? null : addresses.iterator().next();

        final JsonObject fromJson = getFromJson(fromContact, fromAddress, messageCategory);

        if (!Objects.equals(INBOUND, messageCategory) && Objects.nonNull(user)) {
            final DisplayPicture thumbnail = user.getThumbnail();

            if (Objects.nonNull(thumbnail)) {
                fromJson.put(CoreEmailFields.THUMBNAIL.getName(), crmUserConverter.fetchProfileJson(thumbnail, entityEmail.orgId(), entityEmail.getUserId()));
            }
            fromJson.put(CoreEmailFields.NAME.getName(), user.getFullName());
        }

        jsonObject.put(CoreEmailFields.FROM.getName(), fromJson);

        if (toAddressArray.size() > 0) {
            recipientJson.put(CoreEmailFields.TO.getName(), toAddressArray);
        }

        if (bccAddressArray.size() > 0) {
            recipientJson.put(CoreEmailFields.BCC.getName(), bccAddressArray);
        }

        if (ccAddressArray.size() > 0) {
            recipientJson.put(CoreEmailFields.CC.getName(), ccAddressArray);
        }

        jsonObject.put(CoreEmailFields.RECIPIENTS.getName(), recipientJson).put(CoreEmailFields.ID.getName(), entityEmail.id());

        if (SCHEDULED == messageCategory) {
            final EntityScheduledEmail scheduledMail = (EntityScheduledEmail) entityEmail;

            jsonObject.put(CoreEmailFields.SCHEDULED_TIME.getName(), scheduledMail.getScheduledTime());
        } else {
            jsonObject.put(CoreEmailFields.TIME_STAMP.getName(), entityEmail.getMetaData().getDate());
        }
        final EmailVariant emailVariant = entityEmail.getEmailVariant();

        if (OUTBOUND == messageCategory && !emailVariant.isSmartBcc()) {
            final Collection<EmailTrackingDetail> emailTrackingDetails = ((EntityOutboundEmail) entityEmail).getEmailTrackingDetails();

            if (Objects.nonNull(emailTrackingDetails) && !emailTrackingDetails.isEmpty()) {
                jsonObject.put(CoreEmailFields.TRACKING.getName(), convertTrackingDetailsIntoJson(emailTrackingDetails));
            }
        }

        if (Objects.nonNull(attachments)) {
            final JsonArray attachmentArray = Json.array();

            attachments.forEach(attachment -> attachmentArray.put(attachmentConverter.convert(attachment)));
            jsonObject.put(CoreEmailFields.ATTACHMENTS.getName(), attachmentArray);
        }

        if (Objects.nonNull(message)) {
            jsonObject.put(CoreEmailFields.MESSAGE.getName(), message);
        }

        if (Objects.nonNull(subject)) {
            jsonObject.put(CoreEmailFields.SUBJECT.getName(), subject);
        }
        jsonObject.put(CoreEmailFields.VARIANT.getName(), emailVariant.getType().name());
        jsonObject.put(CoreEmailFields.MESSAGE_CATEGORY.getName(), messageCategory.name());

        return jsonObject;
    }

    /**
     * <p>
     * Gets the {@link JsonObject} contains of from recipients related data's
     * </p>
     *
     * @param contact         Represents the {@link Contact}
     * @param emailAddress    Represents the {@link EmailAddress}
     * @param messageCategory Represents the {@link MessageCategory}
     * @return {@link JsonObject} contains from address data's
     */
    private JsonObject getFromJson(final Contact contact, final EmailAddress emailAddress, final MessageCategory messageCategory) {
        final JsonObject jsonObject = Json.object();

        if (Objects.nonNull(contact)) {
            final Email primaryEmail = contact.getPrimaryEmail();
            final String primaryEmailAddress = primaryEmail.getValue();

            if (MessageCategory.INBOUND == messageCategory) {
                final DisplayPicture thumbnail = contact.getThumbnail();
                final Long contactId = contact.id();

                if (Objects.nonNull(thumbnail)) {
                    jsonObject.put(CoreEmailFields.THUMBNAIL.getName(), contactConverter.convertThumbnailToJson(contactId, Contact.ENTITY_TYPE, thumbnail));
                }

                jsonObject.put(CoreEmailFields.CONTACT_NAME.getName(), contact.getFullName());
                jsonObject.put(CoreEmailFields.CONTACT_ID.getName(), contactId);
                jsonObject.put(CoreEmailFields.EMAIL_ID.getName(), primaryEmail.id());
                jsonObject.put(CoreEmailFields.EMAIL.getName(), primaryEmailAddress);
            } else {
                jsonObject.put(CoreEmailFields.EMAIL.getName(), primaryEmailAddress);
            }
        } else if (Objects.nonNull(emailAddress)) {
            final String address = emailAddress.getEmailAddress();

            jsonObject.put(CoreEmailFields.NAME.getName(), EmailAddress.extractNameFromEmail(address));
            jsonObject.put(CoreEmailFields.ID.getName(), emailAddress.id());
            jsonObject.put(CoreEmailFields.EMAIL.getName(), address);
        }

        return jsonObject;
    }

    /**
     * <p>
     * Adds the {@link Collection} of {@link Contact} details into {@link JsonArray}
     * </p>
     *
     * @param recipientsInfo Represents the {@link Collection} of {@link Contact}
     * @param jsonArray      Represents the {@link JsonArray}
     */

    private void addContactRecipientInfoJson(final Collection<Contact> recipientsInfo, final JsonArray jsonArray) {
        final String name = CoreEmailFields.NAME.getName();
        final String contactIdKey = CoreEmailFields.CONTACT_ID.getName();
        final String contactEmailId = CoreEmailFields.CONTACT_EMAIL_ID.getName();
        final String mailId = CoreEmailFields.MAIL_ID.getName();
        final String thumbnailKey = CoreEmailFields.THUMBNAIL.getName();

        for (final Contact contact : recipientsInfo) {
            final JsonObject jsonObject = Json.object();
            final Email primaryEmail = contact.getPrimaryEmail();
            final Long contactId = contact.id();
            final DisplayPicture thumbnail = contact.getThumbnail();

            jsonObject.put(contactIdKey, contactId);
            jsonObject.put(name, contact.getFullName());
            jsonObject.put(contactEmailId, primaryEmail.id());
            jsonObject.put(mailId, primaryEmail.getValue());

            if (Objects.nonNull(thumbnail)) {
                jsonObject.put(thumbnailKey, contactConverter.convertThumbnailToJson(contactId, Contact.ENTITY_TYPE, thumbnail));
            }

            jsonArray.put(jsonObject);
        }
    }

    /**
     * <p>
     * Adds the {@link Collection} of {@link EmailAddress} details into {@link JsonArray}
     * </p>
     *
     * @param recipientsInfo Represents the {@link Collection} of {@link EmailAddress}
     * @param jsonArray      Represents the {@link JsonArray}
     */
    private void addEmailRecipientInfo(final Collection<EmailAddress> recipientsInfo, final JsonArray jsonArray) {
        final String name = CoreEmailFields.NAME.getName();
        final String mailId = CoreEmailFields.MAIL_ID.getName();
        final String id = CoreEmailFields.ID.getName();

        for (final EmailAddress emailAddress : recipientsInfo) {
            final JsonObject jsonObject = Json.object();
            final String address = emailAddress.getEmailAddress();

            jsonObject.put(name, EmailAddress.extractNameFromEmail(address));
            jsonObject.put(mailId, address);
            jsonObject.put(id, emailAddress.id());

            jsonArray.put(jsonObject);
        }
    }

    /**
     * <p>
     * Converts {@link Collection} of {@link EmailTrackingDetail} into {@link JsonObject}
     * </p>
     *
     * @param emailTrackingDetails Represents the {@link Collection} of {@link EmailTrackingDetail}
     * @return {@link JsonObject} contains of tracking details
     */
    private JsonObject convertTrackingDetailsIntoJson(final Collection<EmailTrackingDetail> emailTrackingDetails) {
        final JsonObject trackingJson = Json.object();
        final String timeStamp = CoreEmailFields.TIME_STAMP.getName();
        final String count = CoreEmailFields.COUNT.getName();

        for (final EmailTrackingDetail emailTrackingDetail : emailTrackingDetails) {
            final JsonObject jsonObject = Json.object();
            final String trackingType = Objects.equals(EmailTrackingDetail.Type.CLICKED, emailTrackingDetail.getType()) ? CoreEmailFields.CLICKED.getName() : CoreEmailFields.OPENED.getName();

            jsonObject.put(timeStamp, emailTrackingDetail.getTime());
            jsonObject.put(count, emailTrackingDetail.getCount());

            trackingJson.put(trackingType, jsonObject);
        }

        if (trackingJson.containsKey(CoreEmailFields.CLICKED.getName())) {
            trackingJson.put(CoreEmailFields.IS_CLICKED.getName(), true);
        } else if (trackingJson.containsKey(CoreEmailFields.OPENED.getName())) {
            trackingJson.put(CoreEmailFields.IS_OPENED.getName(), true);
        }

        return trackingJson;
    }

    /**
     * <p>
     * Processes the {@link Email}
     * </p>
     *
     * @param id    Represents the email id
     * @param value Represents the email value
     * @return {@link Email}
     */
    public Email processEmail(final Long id, final String value) {
        final Email email = new Email();

        email.setId(id);
        email.setValue(value);

        return email;
    }
}