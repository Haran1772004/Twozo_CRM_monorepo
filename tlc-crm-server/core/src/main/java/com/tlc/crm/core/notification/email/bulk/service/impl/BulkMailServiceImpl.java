package com.tlc.crm.core.notification.email.bulk.service.impl;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.notification.email.api.model.email.EntityDraftEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;
import com.tlc.crm.core.notification.email.tracking.fields.EmailTrackingFields;
import com.tlc.crm.email.notification.email.EmailDispatcher;
import com.tlc.crm.email.notification.email.api.manager.EmailReferenceManager;

import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;
import com.tlc.email.common.model.EmailAttachment;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.DraftEmail.DraftAction;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.type.Email;

import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.notification.email.bulk.api.model.MailInfo;
import com.tlc.crm.core.notification.email.bulk.api.model.BulkMail;
import com.tlc.crm.core.notification.email.bulk.api.model.MailRecipientInfo;
import com.tlc.crm.core.notification.email.bulk.service.BulkMailService;
import com.tlc.crm.core.notification.email.internal.OutboundEmailService;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.bulk.api.manager.BulkMailManager;
import com.tlc.crm.core.notification.email.bulk.fields.BulkMailFields;
import com.tlc.crm.core.notification.email.bulk.status.BulkMailErrorCodes;
import com.tlc.crm.core.notification.email.template.api.TemplateManager;

import com.tlc.crm.email.notification.email.api.model.MailResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;

import static com.tlc.email.common.model.type.Email.MessageCategory.SCHEDULED;

/**
 * Provides implementations for bulk mail-related functionalities and services pertaining to {@link BulkMail}.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see BulkMailService
 */
public class BulkMailServiceImpl implements BulkMailService {

    private final DealManager dealManager;
    private final ContactManager contactManager;
    private final BulkMailManager bulkMailManager;
    private final EmailDispatcher emailDispatcher;
    private final TemplateManager templateManager;
    private final EmailReferenceManager emailReferenceManager;
    private final ModelValidator modelValidator;

    private BulkMailServiceImpl() {
        this.dealManager = DealManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.bulkMailManager = BulkMailManager.getInstance();
        this.emailDispatcher = EmailDispatcher.get();
        this.templateManager = TemplateManager.getInstance();
        this.emailReferenceManager = EmailReferenceManager.getInstance();
        this.modelValidator = ValidatorServiceProvider.get().modelValidator();
    }

    private static class Instance {
        private static final BulkMailServiceImpl INSTANCE = new BulkMailServiceImpl();
    }

    /**
     * Gets instance of {@link BulkMailServiceImpl}.
     *
     * @return {@link BulkMailServiceImpl} instance
     */
    public static BulkMailServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     *  Groups the primary emails based on the provided entities within a specific organization.
     *
     * @param orgId          Represents an organization id
     * @param contactIds     Represents the {@link Collection} of entity id
     * @param entityEmailIds Represents the {@link Collection} of entity email id
     * @param bulkMailId     Represents the bulk mail ID
     * @return {@link Collection} of primary email mapped with the respective entity id
     */
    @Override
    public Map<Long, Collection<Long>> groupPrimaryEmailsByContactIds(final Long orgId, final Collection<Long> contactIds,
                                                                      final Collection<Long> entityEmailIds, final Long bulkMailId) {
        final Map<Long, Collection<Long>> entityEmails = new HashMap<>();

        if (contactIds.isEmpty()) {
            return entityEmails;
        }
        entityEmails.putAll(contactManager.groupPrimaryEmailsByContacts(orgId, contactIds));

        if (Objects.isNull(entityEmailIds) || entityEmailIds.isEmpty()) {

            for (final Long contactId : contactIds) {
                entityEmails.put(contactId, null);
            }
        } else {
            final Collection<Long> primaryEmailIds = new ArrayList<>();

            for (final Long contactId : contactIds) {
                final Collection<Long> emailIds = entityEmails.get(contactId);

                if (Objects.nonNull(emailIds) && !emailIds.isEmpty()) {
                    final Long contactEmailId = entityEmailIds.stream().findFirst().orElseGet(() -> null);

                    if (Objects.nonNull(bulkMailId) && Objects.nonNull(contactEmailId) && emailIds.contains(contactEmailId)) {
                        emailIds.clear();
                        emailIds.add(contactEmailId);
                    }
                    primaryEmailIds.addAll(emailIds);
                } else {
                    entityEmails.put(contactId, null);
                }
            }
            entityEmailIds.removeAll(primaryEmailIds);
        }

        return entityEmails;
    }

    /**
     * Groups the participants contacts based on the provided deals ids within a specific organization.
     *
     * @param orgId          Represents an organization id
     * @param dealIds        Represents the {@link Collection} of deal id
     * @param entityEmailIds Represents the {@link Map} which grouped the primary emails of entities
     * @param bulkMailId     Represents the {@link Long} bulk mail ID
     * @return A {@link Map} of deal ids and their corresponding participant contacts
     */
    @Override
    public Map<Long, Collection<Contact>> groupContactsByDealIds(final Long orgId, final Collection<Long> dealIds,
                                                                 final Collection<Long> entityEmailIds, final Long bulkMailId) {
        final Map<Long, Collection<Contact>> participantContacts = new HashMap<>();

        if (dealIds.isEmpty()) {
            return participantContacts;
        }
        participantContacts.putAll(dealManager.groupParticipantsContactsByDeals(orgId, dealIds));

        if (Objects.isNull(entityEmailIds) || entityEmailIds.isEmpty()) {

            if (Objects.nonNull(bulkMailId)) {

                for (final Long dealId : dealIds) {
                    participantContacts.put(dealId, null);
                }
            }

        } else {
            final Collection<Long> primaryEmailIds = new ArrayList<>();

            for (final Long dealId : dealIds) {
                final Collection<Contact> contacts = participantContacts.get(dealId);

                if (Objects.nonNull(contacts) && !contacts.isEmpty()) {
                    final Long emailId = entityEmailIds.stream().findFirst().orElseGet(() -> null);

                    Contact emailContact = null;

                    for (final Contact dealContact : contacts) {
                        final com.tlc.crm.entity.models.type.Email primaryEmail = dealContact.getPrimaryEmail();

                        if (Objects.nonNull(primaryEmail)) {
                            final Long primaryEmailId = primaryEmail.id();

                            if (Objects.nonNull(primaryEmailId)) {
                                primaryEmailIds.add(primaryEmailId);

                                if (Objects.nonNull(bulkMailId) && Objects.nonNull(emailId) && Objects.equals(emailId, primaryEmailId)) {
                                    emailContact = dealContact;
                                    break;
                                }
                            }
                        }
                    }

                    if (Objects.nonNull(emailContact)) {
                        contacts.clear();
                        contacts.add(emailContact);
                    }
                } else {
                    participantContacts.put(dealId, null);
                }
            }
            entityEmailIds.removeAll(primaryEmailIds);
        }

        return participantContacts;
    }

    /**
     * {@inheritDoc}
     *
     * @param bulkMail    Represents the {@link BulkMail}
     * @param requestData Represents the JSON which contains request data
     * @return {@link MailResponse}
     */
    @Override
    public MailResponse bulkDraft(final BulkMail bulkMail, final JsonObject requestData) {
        final Long orgId = bulkMail.orgId();
        final Collection<MailInfo> bulkMailMessages = bulkMail.getMailInfo();
        final Collection<EntityOutboundEmail> entityOutboundEmails = bulkMail.getMailInfo().stream().map(MailInfo::getOutboundEmail).toList();
        final DraftAction draftAction = entityOutboundEmails.stream().map(mail -> {
                    final EntityDraftEmail entityDraftEmail = (EntityDraftEmail) mail;

                    return entityDraftEmail.getDraftAction();
                })
                .findFirst().orElseGet(() -> null);

        if (Objects.nonNull(draftAction) && !bulkMailMessages.isEmpty()) {
            final Collection<MailInfo> mailInfo = bulkMail.getMailInfo();
            final Collection<ErrorDescriptor> errors = modelValidator.validate(mailInfo, Group.Create.class);

            if (errors.isEmpty()) {

                return switch (draftAction) {
                    case CREATE -> {
                        bulkMailManager.create(bulkMail);

                        yield new MailResponse(Collections.emptyMap());
                    }
                    case UPDATE -> {
                        bulkMailManager.update(orgId, List.of(bulkMail));

                        yield new MailResponse(Collections.emptyMap());
                    }
                    case SEND -> {
                        bulkMail.setHasDrafts(false);

                        if (!mailInfo.isEmpty()) {
                            sendMail(mailInfo.stream().map(MailInfo::getOutboundEmail).toList(), requestData, bulkMail);
                        }

                        yield new MailResponse(Collections.emptyMap());
                    }
                };
            } else {
                throw ErrorCode.get(BulkMailErrorCodes.DRAFT_ACTION_FAILED);
            }
        } else {
            throw ErrorCode.get(BulkMailErrorCodes.DRAFT_ACTION_FAILED);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param bulkMail Represents the {@link BulkMail}
     * @return {@link MailResponse}
     */
    @Override
    public MailResponse bulkSend(final BulkMail bulkMail, final JsonObject requestData) {
        final Map<String, Object> response = new HashMap<>();
        final Collection<MailInfo> mailInfo = bulkMail.getMailInfo();
        final Collection<ErrorDescriptor> errors = modelValidator.validate(mailInfo, Group.Create.class);

        if (errors.isEmpty()) {
            final Collection<MailInfo> mails = bulkMail.hasDrafts() ? mailInfo : bulkMail.getSubscribedMailInfo();

            final Collection<MailInfo> filteredMails = mailInfo.stream()
                    .filter(mail -> !mails.contains(mail)).toList();

            if (!filteredMails.isEmpty()) {
                bulkMail.setMailInfo(filteredMails);

                bulkMailManager.create(bulkMail);
            }

            if (!mails.isEmpty()) {
                final Collection<MailInfo> draftMailInfos = mails.stream()
                        .filter(mail -> Objects.equals(mail.getStatus(), MailInfo.Status.DRAFTED)).toList();

                if (!draftMailInfos.isEmpty()) {
                    final BulkMail draftBulkMail = new BulkMail();

                    draftBulkMail.setMailInfo(draftMailInfos);
                    draftBulkMail.setOrgId(bulkMail.orgId());
                    draftBulkMail.setSource(bulkMail.getSource());
                    draftBulkMail.setFromAddress(bulkMail.getFromAddress());
                    draftBulkMail.setSenderId(bulkMail.getSenderId());
                    draftBulkMail.setSubscribedMailInfo(draftMailInfos);
                    draftBulkMail.setHasDrafts(true);

                    bulkDraft(draftBulkMail, requestData);

                    mails.removeAll(draftMailInfos);
                    bulkMail.setMailInfo(mails);
                    bulkMail.setSubscribedMailInfo(mails);
                }
                final Collection<EntityOutboundEmail> entityOutboundEmails = mails.stream().map(MailInfo::getOutboundEmail).toList();
                final EntityOutboundEmail outboundEmail = entityOutboundEmails.stream().findFirst().orElseGet(() -> null);

                if (Objects.nonNull(outboundEmail) && Email.MessageCategory.SCHEDULED == outboundEmail.getMessageCategory()) {
                    final EntityScheduledEmail scheduledEmail = (EntityScheduledEmail) outboundEmail;

                    scheduledEmail.setSender(outboundEmail.getSender());
                    bulkMailManager.create(bulkMail);
                } else {
                    sendMail(entityOutboundEmails, requestData, bulkMail);
                }
            }
            response.put(BulkMailFields.CREATED_RECORDS.getName(), bulkMail.id());
        }
        response.put(BulkMailFields.ACTION.getName(), OutboundEmailService.Action.CREATE.name());
        response.put(BulkMailFields.VALIDATION_ERRORS.getName(), errors);

        return new MailResponse(response);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @return the count of total recipients
     */
    @Override
    public long getSubscribedRecipientsCountOnDay(final long orgId, final long userId) {
        return bulkMailManager.getSubscribedRecipientsCountOnDay(orgId, userId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents a user id
     * @param bulkMailIds Represents the {@link Collection} of bulk mail id
     */
    @Override
    public void deleteBulkMails(final long orgId, final long userId, final Collection<Long> bulkMailIds) {
        bulkMailManager.delete(orgId, userId, bulkMailIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents a user id
     * @param bulkMailId Represents the bulk mail id
     * @return {@link BulkMail}
     */
    @Override
    public BulkMail getBulkMail(final long orgId, final long userId, final long bulkMailId) {
        return bulkMailManager.getBulkMail(orgId, userId, bulkMailId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param bulkMailIds Represents the {@link Set} of bulk mail id
     * @return true, when the corresponding {@link BulkMail} is exists
     */
    @Override
    public boolean exists(final long orgId, final Set<Long> bulkMailIds) {
        return bulkMailManager.exists(orgId, bulkMailIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param bulkMailId Represents the bulk mail id
     * @return the total recipients count
     */
    @Override
    public Long getBulkMailRecipientsCount(final long orgId, final long bulkMailId) {
        return bulkMailManager.getRecipientsCount(orgId, bulkMailId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param bulkMailId Represents the bulk mail id
     * @param start      Represents the lower bound of recipients to be fetched
     * @param limit      Represents the upper bound of recipients to be fetched
     * @return {@link Collection} of {@link MailRecipientInfo}
     */
    @Override
    public Collection<MailRecipientInfo> getBulkMailRecipients(final long orgId, final long bulkMailId,
                                                               final int start, final int limit) {
        return bulkMailManager.getBulkMailRecipients(orgId, bulkMailId, start, limit);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @return {@link EntityEmail}
     */
    @Override
    public EntityEmail getEntityEmail(final long orgId, final long conversationId) {
        return bulkMailManager.getEntityEmail(orgId, conversationId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param outboundEmail  Represents the {@link EntityOutboundEmail}
     * @param contactEmailId Represents the contact email id
     * @param contactId      Represents the contact ID
     */
    @Override
    public void fillPlaceHolderValues(final long orgId, final EntityOutboundEmail outboundEmail, final Long contactEmailId, final Long contactId) {
        templateManager.fillPlaceHolderValues(orgId, outboundEmail, contactEmailId, contactId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param bulkMailIds Represents the {@link Set} of bulk mail ids
     * @return {@link Collection} of drafted conversation ids
     */
    @Override
    public Collection<Long> getDraftedConversationIds(final long orgId, final long userId, final Set<Long> bulkMailIds) {
        return bulkMailManager.getDraftedConversationIds(orgId, userId, bulkMailIds);
    }

    /**
     * Dispatches emails for the given {@link Collection} of {@link MailInfo}.
     *
     * @param outboundEmails Represents the {@link Collection} of {@link EntityOutboundEmail}
     * @param requestData    Represents the json which contains request data
     * @param bulkMail       Represents the {@link BulkMail}
     */
    private void sendMail(final Collection<EntityOutboundEmail> outboundEmails, final JsonObject requestData, final BulkMail bulkMail) {

        if (!outboundEmails.isEmpty()) {
            final Long id = bulkMail.id();
            final Long bulkMailId = Objects.isNull(id) ? bulkMailManager.fetchBulkMailId(bulkMail) : id;

            for (final EntityOutboundEmail mail : outboundEmails) {
                final JsonObject mailPayload = buildMailPayload(mail, requestData, bulkMailId);
                final Email.MessageCategory messageCategory = mail.getMessageCategory();

                mailPayload.put(BulkMailFields.TYPE.getName(), requestData.get(BulkMailFields.TYPE.getName()));
                mailPayload.put(BulkMailFields.ORG_ID.getName(), requestData.get(BulkMailFields.ORG_ID.getName()));
                mailPayload.put(BulkMailFields.USER_ID.getName(), requestData.get(BulkMailFields.USER_ID.getName()));
                mailPayload.put(BulkMailFields.IS_BULK.getName(), requestData.get(BulkMailFields.IS_BULK.getName()));

                loadTrackingJson(mail.getEmailTrackingDetails(), mailPayload);
                initiateEmailSend(mail, messageCategory, mailPayload);
            }
        }
    }

    /**
     * <p>
     * Builds the bulk mail payload for a specific outbound email.
     * </p>
     *
     * @param mail          Represents the {@link EntityOutboundEmail}
     * @param requestData   Represents the json which contains request data
     * @param bulkMailId    Represents the ID of the Bulk mail
     * @return the formatted bulk mail payload wrapped in a DATA object
     */
    private JsonObject buildMailPayload(final EntityOutboundEmail mail, final JsonObject requestData, final Long bulkMailId) {
        final JsonObject originalData = requestData.getJsonObject(BulkMailFields.DATA.getName());
        final JsonObject mailPayload = Json.object()
                .put(BulkMailFields.BULK_MAIL_ID.getName(), bulkMailId)
                .put(BulkMailFields.FROM_ADDRESS.getName(), originalData.get(BulkMailFields.FROM_ADDRESS.getName()))
                .put(BulkMailFields.SOURCE.getName(), originalData.get(BulkMailFields.SOURCE.getName()))
                .put(BulkMailFields.IS_EMAIL_TRACKER_ENABLED.getName(), originalData.get(BulkMailFields.IS_EMAIL_TRACKER_ENABLED.getName()))
                .put(BulkMailFields.IS_LINK_TRACKER_ENABLED.getName(), originalData.get(BulkMailFields.IS_LINK_TRACKER_ENABLED.getName()))
                .put(BulkMailFields.IS_CONTACT_LINKING_ENABLED.getName(), originalData.get(BulkMailFields.IS_CONTACT_LINKING_ENABLED.getName()))
                .put(BulkMailFields.IS_DEAL_LINKING_ENABLED.getName(), originalData.get(BulkMailFields.IS_DEAL_LINKING_ENABLED.getName()))
                .put(BulkMailFields.SUBJECT.getName(), mail.getSubject())
                .put(BulkMailFields.MESSAGE.getName(), mail.getContent());

        if (originalData.containsKey(BulkMailFields.TEMPLATE_ID.getName())) {
            mailPayload.put(BulkMailFields.TEMPLATE_ID.getName(), originalData.get(BulkMailFields.TEMPLATE_ID.getName()));
        }

        if (originalData.containsKey(BulkMailFields.ATTACHMENT_IDS.getName())) {
            mailPayload.put(BulkMailFields.ATTACHMENT_IDS.getName(), originalData.get(BulkMailFields.ATTACHMENT_IDS.getName()));
        }
        final JsonObject matchedEmailEntry = extractMatchingEmailJson(mail, originalData);

        if (Objects.nonNull(matchedEmailEntry)) {
            mailPayload.put(BulkMailFields.EMAILS.getName(), Json.array().put(matchedEmailEntry));
        }

        return Json.object().put(BulkMailFields.DATA.getName(), mailPayload);
    }

   /**
     * <p>
     * Finds the matching email JSON object for the given outbound email from the original request data.
     * </p>
     *
     * @param mail      Represents the {@link EntityOutboundEmail}
     * @param dataJson  Represents the original request data as a {@link JsonObject}
     * @return the matching email JSON object if found; otherwise, null
     */
    private JsonObject extractMatchingEmailJson(final EntityOutboundEmail mail, final JsonObject dataJson) {
        final String emailsKey = BulkMailFields.EMAILS.getName();

        if (!dataJson.containsKey(emailsKey)) {
            return null;
        }

        final JsonArray emailsArray = dataJson.getJsonArray(emailsKey);
        final String toAddressKey = BulkMailFields.TO_ADDRESS.getName();
        final String contactKey = BulkMailFields.CONTACT.getName();
        final String emailKey = BulkMailFields.EMAIL.getName();
        final String idKey = BulkMailFields.ID.getName();
        final String emailIdKey = BulkMailFields.EMAIL_ID.getName();
        final ContactRecipients contactRecipients = mail.getContactRecipients();
        final boolean isContactMail = Objects.nonNull(contactRecipients) && Objects.nonNull(contactRecipients.getContacts());

        for (int index = 0; index < emailsArray.size(); index++) {
            final JsonObject emailEntry = emailsArray.getJsonObject(index);

            if (emailEntry.containsKey(toAddressKey)) {
                final JsonObject toAddressJson = emailEntry.getJsonObject(toAddressKey);

                if (isContactMail && toAddressJson.containsKey(contactKey)) {
                    final Collection<Long> contactEmailIds = contactRecipients.getEmailIds();

                    if (contactEmailIds.isEmpty()) {
                        continue;
                    }

                    final Long expectedEmailId = contactEmailIds.iterator().next();
                    final JsonObject contactJson = toAddressJson.getJsonObject(contactKey);

                    if (contactJson.containsKey(emailIdKey) && Objects.equals(expectedEmailId, contactJson.getLong(emailIdKey))) {
                        return emailEntry;
                    }
                } else if (!isContactMail && toAddressJson.containsKey(emailKey)) {
                    final String resolvedAddress = mail.getRecipients().stream()
                            .findFirst().flatMap(recipient -> recipient.toRecipients().stream().findFirst())
                            .map(EmailAddress::getEmailAddress).orElse(null);

                    if (Objects.nonNull(resolvedAddress)) {
                        final JsonObject emailJson = toAddressJson.getJsonObject(emailKey);

                        if (Objects.equals(resolvedAddress, emailJson.getString(idKey))) {
                            return emailEntry;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * <p>
     * Sets the entity ID of the specific email in the JSON Object
     * </p>
     *
     * @param emailTrackingDetails Represents the email tracking details
     * @param data                 Represents the {@link JsonObject}
     */
    private void loadTrackingJson(final Collection<EmailTrackingDetail> emailTrackingDetails, final JsonObject data) {
        if (Objects.isNull(emailTrackingDetails) || emailTrackingDetails.isEmpty()) {
            return;
        }
        final JsonArray emailTrackingDetailsArray = Json.array();

        for (final EmailTrackingDetail emailTrackingDetail : emailTrackingDetails) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(EmailTrackingFields.TRACKER_ID.getName(), emailTrackingDetail.getTrackingId());
            jsonObject.put(EmailTrackingFields.TRACKER_TYPE.getName(), emailTrackingDetail.getType().id);

            emailTrackingDetailsArray.put(jsonObject);
        }

        data.put(EmailTrackingFields.EMAIL_TRACKING.getName(), emailTrackingDetailsArray);
    }

    /**
     * <p>
     * Sets the entity ID of the specific email in the Json Object
     * </p>
     *
     * @param entityIdsSize Represents the size of the entity ID array
     * @param jsonData      Represents the {@link JsonObject}
     * @param entityId      Represents the entity ID of the recipient
     * @param entityIds     Represents a {@link Collection} of entity IDs
     */
    private void setEntityIdInJson(final int entityIdsSize, final JsonObject jsonData,
                                   final Long entityId, final JsonArray entityIds) {
        final String entityIdsKey = BulkMailFields.ENTITY_IDS.getName();

        if (entityIdsSize == 0) {
            jsonData.put(entityIdsKey, Collections.emptyList());
        } else {
            boolean isEntityIdFound = false;

            for (int index = 0; index < entityIdsSize; index++) {

                if (Objects.equals(entityId, entityIds.getLong(index))) {
                    jsonData.put(entityIdsKey, Json.array().put(entityId));
                    isEntityIdFound = true;
                    break;
                }
            }

            if (!isEntityIdFound) {
                jsonData.put(entityIdsKey, Collections.emptyList());
            }
        }
    }


    /**
     * <p>
     * Sends the email to the provider.
     * </p>
     *
     * @param entityEmail Represents the {@link EntityEmail}
     * @param requestData Represents the {@link JsonObject}
     */
    private void sendEmail(final EntityEmail entityEmail, final JsonObject requestData) {
        final Email email = entityEmail.getEmail();

        email.setMetaData(entityEmail.getMetaData());
        email.setSender(entityEmail.getSender());
        email.setSubject(entityEmail.getSubject());
        email.setContent(entityEmail.getContent());
        email.setRecipients(entityEmail.getRecipients());
        email.setAttachments(getEmailAttachments(entityEmail));
        email.setFiles(entityEmail.getFiles());
        email.setId(entityEmail.id());

        emailDispatcher.send(email, requestData);
    }

    /**
     * <p>
     * Fetches the attachments associated with an email.
     * </p>
     *
     * @param entityEmail Represents the {@link EntityEmail}
     * @return {@link Collection} of {@link EmailAttachment}
     */
    private Collection<EmailAttachment> getEmailAttachments(final EntityEmail entityEmail) {
        final Collection<Long> attachmentIds = entityEmail.getEntityAttachmentIds();
        final Collection<EmailAttachment> emailAttachments = new HashSet<>();

        if (!attachmentIds.isEmpty()) {

            for (final Long attachmentId : attachmentIds) {
                final EmailAttachment emailAttachment = new EmailAttachment();

                emailAttachment.setId(attachmentId);
                emailAttachments.add(emailAttachment);
            }

            entityEmail.setAttachments(emailAttachments);
        }
        return emailAttachments;
    }

    /**
     * Initializes and sends an email based on message category.
     *
     * @param mail            Represents the {@link EntityOutboundEmail}
     * @param messageCategory Represents the {@link Email.MessageCategory}
     * @param jsonObject      Represents the {@link JsonObject}
     */
    private void initiateEmailSend(final EntityOutboundEmail mail, final Email.MessageCategory messageCategory, final JsonObject jsonObject) {
        final EmailMetaData data = mail.getMetaData();
        final EmailMetaData metaData = Objects.isNull(data) ? new EmailMetaData() : data;

        if (Objects.isNull(metaData.getReferences()) && Objects.isNull(metaData.getInReplyTo())) {
            metaData.setInitialMessage(true);
        }

        mail.setMetaData(metaData);

        final EntityScheduledEmail scheduledMail = messageCategory == SCHEDULED
                ? (EntityScheduledEmail) mail : null;

        if (Objects.nonNull(scheduledMail)) {
            scheduledMail.setSender(mail.getSender());
        } else {

            if (Email.MessageCategory.DRAFT == messageCategory) {
                final EntityDraftEmail draftEmail = (EntityDraftEmail) mail;

                draftEmail.setDraftAction(DraftAction.CREATE);
            } else {
                sendEmail(mail, jsonObject);
            }
        }
    }
}