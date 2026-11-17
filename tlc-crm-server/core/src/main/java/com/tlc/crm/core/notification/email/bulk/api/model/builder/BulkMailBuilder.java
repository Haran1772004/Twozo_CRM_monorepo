package com.tlc.crm.core.notification.email.bulk.api.model.builder;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.LoggerUtil;

import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.model.email.EntityDraftEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;
import com.tlc.crm.core.notification.email.api.model.converter.EmailConverter;
import com.tlc.crm.core.notification.email.bulk.service.BulkMailService;
import com.tlc.crm.core.notification.email.bulk.status.BulkMailErrorCodes;
import com.tlc.crm.core.notification.email.bulk.fields.BulkMailFields;
import com.tlc.crm.core.notification.email.template.api.extractor.MailPlaceHolderExtractor;
import com.tlc.crm.core.notification.email.role.permission.controller.EmailLimitController;
import com.tlc.crm.core.notification.email.role.permission.controller.request.EmailLimitControllerRequest;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.core.notification.email.bulk.api.model.BulkMail;
import com.tlc.crm.core.notification.email.bulk.api.model.MailInfo;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;

import com.tlc.crm.security.permission.model.LimitActionType;

import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.DraftEmail.DraftAction;

import com.tlc.file.common.models.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.tlc.email.common.model.type.Email.MessageCategory.DRAFT;
import static com.tlc.email.common.model.type.Email.MessageCategory.SCHEDULED;

/**
 * Builds and configures the {@link BulkMail} and provides the methods and utilities for assembling
 * and formatting bulk email content, recipients, and other attributes.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class BulkMailBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkMailBuilder.class);

    private final BulkMailService bulkMailService;
    private final EmailConverter emailConverter;
    private final CoreMailManager coreMailManager;
    private final MailPlaceHolderExtractor mailPlaceHolderExtractor;

    private BulkMailBuilder() {
        this.bulkMailService = BulkMailService.getInstance();
        this.emailConverter = EmailConverter.getInstance();
        this.coreMailManager = CoreMailManager.getInstance();
        this.mailPlaceHolderExtractor = MailPlaceHolderExtractor.getInstance();
    }

    private static class Instance {
        private static final BulkMailBuilder INSTANCE = new BulkMailBuilder();
    }

    /**
     * Gets instance of {@link BulkMailBuilder}.
     *
     * @return {@link BulkMailBuilder} instance
     */
    public static BulkMailBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Builds the {@link Collection} of {@link MailInfo} from the given {@link JsonObject}.
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents a user id
     * @param jsonObject Represents the {@link JsonObject}
     * @return {@link Collection} of {@link MailInfo}
     */
    public Collection<MailInfo> build(final long orgId, final long userId, final JsonObject jsonObject) {
        final String emailsKey = BulkMailFields.EMAILS.getName();
        final Source source = Source.valueOf(jsonObject.getString(BulkMailFields.SOURCE.getName()).toUpperCase());

        if (!(Source.CONTACT.equals(source) || Source.DEAL.equals(source))) {
            throw ErrorCode.get(BulkMailErrorCodes.SOURCE_NOT_SUPPORTED);
        }
        final Collection<MailInfo> mailInfo = new ArrayList<>();

        if (jsonObject.containsKey(emailsKey)) {

            try {
                buildMailInfoFromEmails(orgId, userId, mailInfo, jsonObject);
            } catch (final Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, "Email: Failed to build the bulk MailInfo");
            } finally {
                new EmailLimitController().clearLimitHistory(LimitActionType.BULK_EMAIL);
            }
        }

        return mailInfo;
    }

    /**
     * Builds the {@link MailInfo} for the given emails in the {@link JsonObject} and adds to the provided {@link Collection}.
     *
     * @param orgId         Represents an organization id
     * @param userId        Represents a user id
     * @param mailInfo      Represents the {@link Collection} to store the constructed {@link MailInfo}
     * @param jsonObject    Represents the request {@link JsonObject}
     */
    private void buildMailInfoFromEmails(final long orgId, final long userId, final Collection<MailInfo> mailInfo,
                                         final JsonObject jsonObject) {
        final JsonArray emailsArray = jsonObject.getJsonArray(BulkMailFields.EMAILS.getName());
        final String draftActionKey = BulkMailFields.DRAFT_ACTION.getName();
        final boolean isDraftUpdate = jsonObject.containsKey(draftActionKey) && !Objects.equals(DraftAction.CREATE,
                DraftAction.get(jsonObject.getShort(draftActionKey)));
        final int emailsSize = emailsArray.size();

        for (int index = 0; index < emailsSize; index++) {
            final JsonObject emailJson = emailsArray.getJsonObject(index);
            final JsonObject toAddressJson = emailJson.getJsonObject(BulkMailFields.TO_ADDRESS.getName());

            if (toAddressJson.containsKey(BulkMailFields.CONTACT.getName())) {
                buildContactMailInfo(orgId, userId, mailInfo, jsonObject, emailJson, toAddressJson, isDraftUpdate);
            } else if (toAddressJson.containsKey(BulkMailFields.EMAIL.getName())) {
                buildEmailMailInfo(orgId, userId, mailInfo, jsonObject, emailJson, toAddressJson, isDraftUpdate);
            }
        }
    }

    /**
     * Builds the {@link MailInfo} for the given contact email in the {@link JsonObject} and adds to the provided {@link Collection}.
     *
     * @param orgId         Represents an organization id
     * @param userId        Represents a user id
     * @param mailInfo      Represents the {@link Collection} to store the constructed {@link MailInfo}
     * @param jsonObject    Represents the request {@link JsonObject}
     * @param emailJson     Represents the {@link JsonObject} of email data
     * @param toAddressJson Represents the {@link JsonObject} of toAddress
     * @param isDraftUpdate Represents whether it's a draft update or not
     */
    private void buildContactMailInfo(final long orgId, final long userId, final Collection<MailInfo> mailInfo, final JsonObject jsonObject,
                                      final JsonObject emailJson, final JsonObject toAddressJson, final boolean isDraftUpdate) {
        final JsonObject contactJson = toAddressJson.getJsonObject(BulkMailFields.CONTACT.getName());
        final String idKey = BulkMailFields.ID.getName();
        final String emailIdKey = BulkMailFields.EMAIL_ID.getName();
        final String conversationIdKey = BulkMailFields.CONVERSATION_ID.getName();

        final Long contactId = contactJson.getLong(idKey);
        final Long contactEmailId = contactJson.containsKey(emailIdKey) ? contactJson.getLong(emailIdKey) : null;
        Long conversationId = null;

        if (isDraftUpdate && emailJson.containsKey(conversationIdKey)) {
            conversationId = emailJson.getLong(conversationIdKey);
        }
        mailInfo.add(buildContactMailInfo(orgId, userId, contactId, contactEmailId, conversationId, jsonObject, emailJson));
    }

    /**
     * Builds the {@link MailInfo} for the given contact email in the {@link JsonObject}.
     *
     * @param orgId            Represents an organization id
     * @param userId           Represents a user id
     * @param contactId        Represents the contact ID
     * @param contactEmailId   Represents the contact email id
     * @param conversationId   Represents the conversation ID
     * @param jsonObject       Represents the request {@link JsonObject}
     * @param emailJson        Represents the {@link JsonObject} of email data
     * @return Constructed {@link MailInfo}
     */
    private MailInfo buildContactMailInfo(final long orgId, final long userId, final Long contactId, final Long contactEmailId,
                                          final Long conversationId, final JsonObject jsonObject, final JsonObject emailJson) {
        final MailInfo mailInfo = new MailInfo();
        final EntityOutboundEmail outboundEmail = constructOutboundEmail(orgId, userId, jsonObject);
        final ContactRecipients contactRecipients = new ContactRecipients();
        final String dealIdKey = BulkMailFields.DEAL_ID.getName();

        if (emailJson.containsKey(dealIdKey)) {
            outboundEmail.setEntityId(emailJson.getLong(dealIdKey));
        } else {
            outboundEmail.setEntityId(contactId);
        }

        if (Objects.nonNull(conversationId)) {
            outboundEmail.setConversationId(conversationId);
        }

        if (outboundEmail.hasPlaceHolders()) {
            bulkMailService.fillPlaceHolderValues(orgId, outboundEmail, contactEmailId, contactId);
        }

        if (Objects.nonNull(contactEmailId)) {
            contactRecipients.addContact(AddressType.TO, contactEmailId);
        }
        outboundEmail.setContactRecipients(contactRecipients);
        mailInfo.setOutboundEmail(outboundEmail);
        setNotificationDetailsToMail(orgId, mailInfo, jsonObject, contactEmailId, null, contactId);

        if (emailJson.containsKey(BulkMailFields.IS_PERSONALISED.getName())) {
            setEmailPersonalization(emailJson, outboundEmail);
            mailInfo.setPersonalized(true);
        }
        return mailInfo;
    }

    /**
     * Builds the {@link MailInfo} for the given email address in the {@link JsonObject} and adds to the provided {@link Collection}.
     *
     * @param orgId         Represents an organization id
     * @param userId        Represents a user id
     * @param mailInfo      Represents the {@link Collection} to store the constructed {@link MailInfo}
     * @param jsonObject    Represents the request {@link JsonObject}
     * @param emailJson     Represents the {@link JsonObject} of email data
     * @param toAddressJson Represents the {@link JsonObject} of toAddress
     * @param isDraftUpdate Represents whether it's a draft update or not
     */
    private void buildEmailMailInfo(final long orgId, final long userId, final Collection<MailInfo> mailInfo,
                                final JsonObject jsonObject, final JsonObject emailJson, final JsonObject toAddressJson, final boolean isDraftUpdate) {
        final JsonObject emailAddressJson = toAddressJson.getJsonObject(BulkMailFields.EMAIL.getName());
        final String emailAddress = emailAddressJson.getString(BulkMailFields.ID.getName());
        final String conversationIdKey = BulkMailFields.CONVERSATION_ID.getName();

        final MailInfo mail = new MailInfo();
        final EntityOutboundEmail outboundEmail = constructOutboundEmail(orgId, userId, jsonObject);

        if (isDraftUpdate && emailJson.containsKey(conversationIdKey)) {
            outboundEmail.setConversationId(emailJson.getLong(conversationIdKey));
        }
        mail.setOutboundEmail(outboundEmail);
        setNotificationDetailsToMail(orgId, mail, jsonObject, null, emailAddress, null);
        mailInfo.add(mail);
    }

    /**
     * Sets the email personalization to the {@link EntityOutboundEmail} based on the provided email data in the {@link JsonObject}.
     *
     * @param emailJson    Represents the {@link JsonObject} of email data
     * @param outboundEmail Represents the {@link EntityOutboundEmail} to set the personalization
     */
    private void setEmailPersonalization(final JsonObject emailJson, final EntityOutboundEmail outboundEmail) {
        final String messageKey = BulkMailFields.MESSAGE.getName();
        final String subjectKey = BulkMailFields.SUBJECT.getName();
        final String fromAddressKey = BulkMailFields.FROM_ADDRESS.getName();
        final String templateIdKey = BulkMailFields.TEMPLATE_ID.getName();
        final String contactKey = BulkMailFields.CONTACT.getName();
        final String contactEmailIdKey = BulkMailFields.EMAIL_ID.getName();
        final String attachmentIdsKey = BulkMailFields.ATTACHMENT_IDS.getName();

        final JsonObject contactJson = emailJson.getJsonObject(BulkMailFields.TO_ADDRESS.getName()).getJsonObject(contactKey);
        final Long contactId = contactJson.getLong(BulkMailFields.ID.getName());
        final Long contactEmailId = contactJson.containsKey(contactEmailIdKey) ? contactJson.getLong(contactEmailIdKey) : null;

        if (emailJson.containsKey(messageKey)) {
            outboundEmail.setContent(emailJson.getString(messageKey));
        }

        if (emailJson.containsKey(subjectKey)) {
            outboundEmail.setSubject(emailJson.getString(subjectKey));
        }

        if (emailJson.containsKey(templateIdKey)) {
            outboundEmail.setTemplateId(emailJson.getLong(templateIdKey));
        }

        if (outboundEmail.hasPlaceHolders()) {
            bulkMailService.fillPlaceHolderValues(outboundEmail.orgId(), outboundEmail, contactEmailId, contactId);
        }

        if (emailJson.containsKey(attachmentIdsKey)) {
            final JsonArray attachmentArray = emailJson.getJsonArray(attachmentIdsKey);
            final Collection<Attachment> attachments = new ArrayList<>();

            for (int index = 0; index < attachmentArray.size(); index++) {
                final Attachment emailAttachment = new Attachment();

                emailAttachment.setId(attachmentArray.getLong(index));
                attachments.add(emailAttachment);
            }
            outboundEmail.setEntityAttachments(attachments);
        }

        if (emailJson.containsKey(fromAddressKey)) {
            outboundEmail.setSender(coreMailManager.getFromAddressInfo(outboundEmail.orgId(), emailJson.getLong(fromAddressKey)).orElse(null));
        }
    }

    /**
     * Sets the email related details to the {@link MailInfo}.
     *
     * @param orgId          Represents an organization id
     * @param mailInfo       Represents the {@link MailInfo}
     * @param jsonObject     Represents the {@link JsonObject}
     * @param contactEmailId Represents the contact email id
     * @param emailAddress   Represents the {@link EmailAddress}
     * @param contactId      Represents the contact ID
     */
    private void setNotificationDetailsToMail(final Long orgId, final MailInfo mailInfo, final JsonObject jsonObject,
                                              final Long contactEmailId, final String emailAddress, final Long contactId) {
        final EntityOutboundEmail outboundEmail = mailInfo.getOutboundEmail();
        final String draftActionKey = BulkMailFields.DRAFT_ACTION.getName();
        final String templateIdKey = BulkMailFields.TEMPLATE_ID.getName();
        final EmailRecipients recipients = new EmailRecipients();
        final String subject = jsonObject.optString(BulkMailFields.SUBJECT.getName(), outboundEmail.getSubject());
        final String content = jsonObject.optString(BulkMailFields.MESSAGE.getName(), outboundEmail.getContent());

        if (Objects.nonNull(emailAddress)) {
            recipients.addRecipients(AddressType.TO, List.of(new EmailAddress(emailAddress)));
            outboundEmail.setRecipients(List.of(recipients));
        } else {
            outboundEmail.setRecipients(List.of());
        }
        outboundEmail.setContent(content);
        outboundEmail.setSubject(subject);

        if (jsonObject.containsKey(templateIdKey)) {
            outboundEmail.setTemplateId(jsonObject.getLong(templateIdKey));
        }

        if ((mailPlaceHolderExtractor.hasPlaceHolders(subject) || mailPlaceHolderExtractor.hasPlaceHolders(content))) {
            bulkMailService.fillPlaceHolderValues(orgId, outboundEmail, contactEmailId, contactId);
        }

        if (jsonObject.containsKey(draftActionKey)) {
            final EntityDraftEmail draftEmail = (EntityDraftEmail) mailInfo.getOutboundEmail();

            draftEmail.setDraftAction(DraftAction.get(jsonObject.getShort(draftActionKey)));
        }
        coreMailManager.getFromAddressInfo(orgId, jsonObject.getLong(BulkMailFields.FROM_ADDRESS.getName())).ifPresent(outboundEmail::setSender);
        emailConverter.setDesignatedFieldsForMail(outboundEmail, jsonObject);
        mailInfo.setOrgId(orgId);
        outboundEmail.setActionTime(Instant.now().getEpochSecond());
        outboundEmail.setHasUnsubscribeLink(jsonObject.optBoolean(BulkMailFields.HAS_UNSUBSCRIBED_LINK.getName(), false));
    }

    /**
     * <p>
     * Constructs the outbound email for an given request {@link JsonObject} data.
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param userId             Represents the user id
     * @param jsonObject         Represents the request {@link JsonObject} data
     * @return {@link EntityOutboundEmail}
     */
    private EntityOutboundEmail constructOutboundEmail(final long orgId, final long userId, final JsonObject jsonObject) {
        final String scheduledTimeKey = BulkMailFields.SCHEDULED_TIME.getName();
        final String draftActionKey = BulkMailFields.DRAFT_ACTION.getName();
        final EmailLimitController limitController = new EmailLimitController();
        final EmailLimitControllerRequest limitCheckerRequest = new EmailLimitControllerRequest(LimitActionType.BULK_EMAIL);
        final EntityOutboundEmail outboundEmail;

        if (jsonObject.containsKey(draftActionKey) || !limitController.canProceed(limitCheckerRequest)) {
            final EntityDraftEmail entityDraftEmail = new EntityDraftEmail();
            final short draftActionId = jsonObject.optShort(draftActionKey, DraftAction.CREATE.getId());
            final DraftAction draftAction = DraftAction.get(draftActionId);

            entityDraftEmail.setDraftAction(draftAction);
            entityDraftEmail.setMessageCategory(DRAFT);
            outboundEmail = entityDraftEmail;
        } else if (jsonObject.containsKey(scheduledTimeKey)) {
            final EntityScheduledEmail scheduledMail = new EntityScheduledEmail();

            scheduledMail.setScheduledTime(jsonObject.getLong(scheduledTimeKey));
            scheduledMail.setMessageCategory(SCHEDULED);
            outboundEmail = scheduledMail;
        } else {
            outboundEmail = new EntityOutboundEmail();
        }

        outboundEmail.setOrgId(orgId);
        outboundEmail.setUserId(userId);

        return outboundEmail;
    }
}
