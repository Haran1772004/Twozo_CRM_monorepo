package com.tlc.crm.email.core.processor;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.SecureUserTaskProcessor;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.notification.email.api.model.email.EntityDraftEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.bulk.api.model.BulkMail;
import com.tlc.crm.core.notification.email.bulk.api.model.MailInfo;
import com.tlc.crm.core.notification.email.bulk.api.model.builder.BulkMailBuilder;
import com.tlc.crm.core.notification.email.bulk.fields.BulkMailFields;
import com.tlc.crm.core.notification.email.bulk.service.BulkMailService;
import com.tlc.crm.core.notification.email.bulk.status.BulkMailErrorCodes;
import com.tlc.crm.core.notification.email.internal.OutboundEmailService;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;

import com.tlc.crm.core.notification.email.tracking.fields.EmailTrackingFields;
import com.tlc.crm.email.core.fields.EmailLinkerFields;
import com.tlc.crm.email.core.manager.BulkEmailLinkerManager;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;
import com.tlc.crm.email.notification.model.MessageDispatchType;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.OutboundEmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics.MetricsType.NO_PRIMARY_EMAIL;
import static com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics.MetricsType.SENT;
import static com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics.MetricsType.UNSUBSCRIBED;

/**
 * Processes the bulk email linker related actions.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see SecureUserTaskProcessor
 */
public final class BulkEmailLinkerProcessor implements SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkEmailLinkerProcessor.class);

    private static final int DEFAULT_LIMIT_FOR_SEND = 100;
    private static final int DEFAULT_LIMIT_FOR_CONVERSATION = 100;
    private static final int DEFAULT_LIMIT = 100;

    private final BulkEmailLinkerManager bulkEmailLinkerManager;
    private final BulkMailBuilder bulkMailBuilder;
    private final BulkMailService bulkMailService;
    private final OutboundEmailService outboundEmailService;
    private final CoreMailManager coreMailManager;
    private final ContactManager contactManager;

    private BulkEmailLinkerProcessor() {
        this.bulkEmailLinkerManager = BulkEmailLinkerManager.getInstance();
        this.bulkMailBuilder = BulkMailBuilder.getInstance();
        this.bulkMailService = BulkMailService.getInstance();
        this.outboundEmailService = OutboundEmailService.getInstance();
        this.coreMailManager = CoreMailManager.getInstance();
        this.contactManager = ContactManager.getInstance();
    }

    private static class Instance {
        private static final BulkEmailLinkerProcessor INSTANCE = new BulkEmailLinkerProcessor();
    }

    public static BulkEmailLinkerProcessor getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Processes the {@link Thread} based on provided details.
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param thread     Represents the {@link Thread}
     * @param jsonObject Represents the state JSON data
     */
    public void process(final long orgId, final long userId, final Thread thread, final JsonObject jsonObject) {
        final Optional<Email> composeEmail = thread.getComposeEmail();
        final JsonObject data = jsonObject.getJsonObject(EmailLinkerFields.DATA);

        if (composeEmail.isPresent()) {
            final Email email = composeEmail.get();
            final Email.MessageCategory messageCategory = email.getMessageCategory();

            if (Email.MessageCategory.DRAFT == messageCategory) {
                final DraftEmail draftEmail = email.get();

                secureProcess(orgId, userId, () -> {
                    final BulkMail bulkMail = constructBulkMail(orgId, userId, data);
                    final Collection<MailInfo> mailInfo = bulkMail.getMailInfo();

                    for (final MailInfo info : mailInfo) {
                        final EntityOutboundEmail entityOutboundEmail = info.getOutboundEmail();

                        entityOutboundEmail.setSender(email.getSender());
                        entityOutboundEmail.setSyncedState(true);
                        entityOutboundEmail.setId(email.id());
                        entityOutboundEmail.setMessageCategory(Email.MessageCategory.OUTBOUND);
                        entityOutboundEmail.setMetaData(email.getMetaData());
                        entityOutboundEmail.setOutboundType(OutboundEmail.OutboundType.SEND);
                        entityOutboundEmail.setEmailTrackingDetails(getEmailTrackingDetails(jsonObject));
                    }

                    if (Objects.isNull(bulkMail.id())) {
                        bulkMail.setId(jsonObject.getLong(EmailLinkerFields.BULK_MAIL_ID));
                    }
                    bulkMail.setThreadId(thread.id());

                    switch (draftEmail.getDraftAction()) {
                        case CREATE -> bulkEmailLinkerManager.create(bulkMail);
                        case UPDATE, SEND -> bulkEmailLinkerManager.update(bulkMail);
                    }
                });
            } else {
                final OutboundEmail outboundEmail = email.get();

                outboundEmail.setOutboundType(OutboundEmail.OutboundType.SEND);

                secureProcess(orgId, userId, () -> {
                    final BulkMail bulkMail = constructBulkMail(orgId, userId, data);
                    final Collection<MailInfo> mailInfo = bulkMail.getMailInfo();

                    for (final MailInfo info : mailInfo) {
                        final EntityOutboundEmail entityOutboundEmail = info.getOutboundEmail();

                        entityOutboundEmail.setSender(email.getSender());
                        entityOutboundEmail.setSyncedState(true);
                        entityOutboundEmail.setId(email.id());
                        entityOutboundEmail.setMessageCategory(Email.MessageCategory.OUTBOUND);
                        entityOutboundEmail.setMetaData(email.getMetaData());
                        entityOutboundEmail.setOutboundType(OutboundEmail.OutboundType.SEND);
                        entityOutboundEmail.setEmailTrackingDetails(getEmailTrackingDetails(jsonObject));
                    }
                    bulkMail.setThreadId(thread.id());
                    bulkMail.setId(data.getLong(EmailLinkerFields.BULK_MAIL_ID));
                    bulkEmailLinkerManager.create(bulkMail);
                });
            }
        }
    }

    /**
     * <p>
     * Extracts and constructs a collection of {@link EmailTrackingDetail} objects from the provided {@link JsonObject}.
     * </p>
     *
     * @param data represents the {@link JsonObject} containing email tracking details
     * @return a {@link Collection} of {@link EmailTrackingDetail} objects extracted from the JSON data
     */
    private Collection<EmailTrackingDetail> getEmailTrackingDetails(final JsonObject data) {
        final Collection<EmailTrackingDetail> emailTrackingDetails = new HashSet<>();
        final String emailTrackingName = EmailTrackingFields.EMAIL_TRACKING.getName();

        if (data.containsKey(emailTrackingName)) {
            final JsonArray emailTrackingDetailsArray = data.getJsonArray(emailTrackingName);

            for (int index = 0; index < emailTrackingDetailsArray.size(); index++) {
                final JsonObject emailTrackingDetailJson = emailTrackingDetailsArray.getJsonObject(index);

                if (Objects.nonNull(emailTrackingDetailJson)) {
                    final EmailTrackingDetail emailTrackingDetail = new EmailTrackingDetail();
                    final String trackerIdKey = EmailTrackingFields.TRACKER_ID.getName();
                    final String trackerTypeKey = EmailTrackingFields.TRACKER_TYPE.getName();

                    if (emailTrackingDetailJson.containsKey(trackerIdKey)) {
                        emailTrackingDetail.setTrackingId(emailTrackingDetailJson.getString(trackerIdKey));
                    }

                    if (emailTrackingDetailJson.containsKey(trackerTypeKey)) {
                        emailTrackingDetail.setType(EmailTrackingDetail.Type.getType(emailTrackingDetailJson.getShort(trackerTypeKey)));
                    }

                    emailTrackingDetails.add(emailTrackingDetail);
                }
            }
        }

        return emailTrackingDetails;
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

        categorizeMailsByStatus(fetchUnsubscribedEmails(orgId, userId, bulkMail), bulkMail);

        return bulkMail;
    }

    /**
     * Categorizes the provided {@link Collection} of {@link EmailAddress} into different groups based on various criteria,
     * including personalized mails, non-personalized mails, skipped mails, and non-primary emails.
     *
     * @param unsubscribedEmailAddresses Represents the {@link Collection} of unsubscribed {@link EmailAddress}
     * @param bulkMail                   Represents the {@link BulkMail}
     */
    private void categorizeMailsByStatus(final Collection<EmailAddress> unsubscribedEmailAddresses, final BulkMail bulkMail) {
        final Collection<MailInfo> mailInfo = bulkMail.getMailInfo();
        final int availableSlot = (int) (DEFAULT_LIMIT_FOR_SEND -
                bulkMailService.getSubscribedRecipientsCountOnDay(bulkMail.orgId(), bulkMail.getSenderId()));
        final Collection<MailInfo> draftedMails = mailInfo.stream().skip(availableSlot).toList();
        final Collection<MailInfo> subscribedMailInfo = new HashSet<>();

        if (!draftedMails.isEmpty()) {

            for (final MailInfo draftedMail : draftedMails) {
                final EntityDraftEmail draftEmail = (EntityDraftEmail) draftedMail.getOutboundEmail();

                draftEmail.setDraftAction(DraftEmail.DraftAction.CREATE);
                draftedMail.setStatus(MailInfo.Status.DRAFTED);
                draftedMail.setMetricsTypes(Set.of(SENT));
                subscribedMailInfo.add(draftedMail);
            }
        }

        if (availableSlot > 0) {
            setMailStatus(mailInfo, draftedMails, subscribedMailInfo, unsubscribedEmailAddresses);
        }
        bulkMail.setSubscribedMailInfo(subscribedMailInfo);
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
        final BulkMail bulkMail = new BulkMail();

        bulkMail.setOrgId(orgId);
        bulkMail.setSenderId(userId);
        bulkMail.setMailInfo(buildMails(orgId, userId, data));
        bulkMail.setSource(Source.valueOf(data.getString(BulkMailFields.SOURCE.getName()).toUpperCase()));
        bulkMail.setFromAddress(new EmailAddress(bulkMail.getMailInfo().stream().map(MailInfo::getOutboundEmail)
                .map(mail -> mail.getSender().getEmailAddress()).findFirst().orElse(null)));

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
     * @param jsonObject Represents the state data
     */
    private void setEntityEmailProperties(final long orgId, final MailInfo mail, final JsonObject jsonObject) {
        final EntityOutboundEmail outboundEmail = mail.getOutboundEmail();
        final Optional<FromAddressInfo> optionalFromAddressInfo = coreMailManager.getFromAddressInfo(orgId, jsonObject.getLong(BulkMailFields.FROM_ADDRESS.getName()));

        if (optionalFromAddressInfo.isEmpty()) {
            throw ErrorCode.get(BulkMailErrorCodes.INVALID_FROM_ADDRESS, "i18n.notifications.from.address.invalid");
        } else {
            outboundEmail.setSender(optionalFromAddressInfo.get());
            outboundEmail.setSyncedState(true);
        }
        setRecipients(mail);
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
     * Sets the {@link MailInfo.Status} for a collection of {@link MailInfo}, categorized by skipped, subscribed, unsubscribed and non-primary mails.
     *
     * @param mailInfo                   Represents the {@link Collection} of {@link MailInfo}
     * @param skippedMails               Represents the skipped mails
     * @param subscribedMailInfo         Represents the subscribed {@link MailInfo}
     * @param unsubscribedEmailAddresses Represents the {@link Collection} of unsubscribed {@link EmailAddress}
     */
    private void setMailStatus(final Collection<MailInfo> mailInfo, final Collection<MailInfo> skippedMails,
                               final Collection<MailInfo> subscribedMailInfo, final Collection<EmailAddress> unsubscribedEmailAddresses) {
        for (final MailInfo mail : mailInfo) {

            if (skippedMails.contains(mail)) {
                continue;
            }

            if (Email.MessageCategory.DRAFT == mail.getOutboundEmail().getMessageCategory()) {
                final EntityDraftEmail draftEmail = (EntityDraftEmail) mail.getOutboundEmail();

                draftEmail.setDraftAction(DraftEmail.DraftAction.CREATE);
                mail.setStatus(MailInfo.Status.DRAFTED);
                mail.setMetricsTypes(Set.of(SENT));

                subscribedMailInfo.add(mail);
            } else if (isUnsubscribedMail(mail, unsubscribedEmailAddresses)) {
                mail.setStatus(MailInfo.Status.UNSUBSCRIBED);
                mail.setMetricsTypes(Set.of(SENT, UNSUBSCRIBED));
            } else if (Objects.nonNull(mail.getOutboundEmail().getEmailRecipients().toRecipients())) {
                mail.setStatus(MailInfo.Status.SUBSCRIBED);
                subscribedMailInfo.add(mail);
                mail.setMetricsTypes(Set.of(SENT));
            } else {
                mail.setStatus(MailInfo.Status.NO_EMAIL);
                mail.setMetricsTypes(Set.of(SENT, NO_PRIMARY_EMAIL));
            }
        }
    }

    /**
     * Checks whether a provided unsubscribed {@link EmailAddress} from {@link MailInfo} which
     * matches the address against a collection of unsubscribed email addresses.
     *
     * @param mailInfo                   Represents the {@link MailInfo}
     * @param unsubscribedEmailAddresses Represents the {@link Collection} of unsubscribed {@link EmailAddress}
     * @return true, when the mail is unsubscribed
     */
    private boolean isUnsubscribedMail(final MailInfo mailInfo, final Collection<EmailAddress> unsubscribedEmailAddresses) {
        final Collection<EmailAddress> receivers = mailInfo.getOutboundEmail().getEmailRecipients().toRecipients();

        return Objects.nonNull(receivers) && receivers.stream().map(EmailAddress::getEmailAddress)
                .anyMatch(emailId -> unsubscribedEmailAddresses.stream().filter(Objects::nonNull).anyMatch(email -> emailId.equals(email.getEmailAddress())));
    }
}
