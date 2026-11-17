package com.tlc.crm.email.core.processor;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.notification.email.api.manager.schedule.EmailSchedulerManager;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.converter.EmailConverter;
import com.tlc.crm.core.notification.email.api.model.email.EmailVariant;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;

import com.tlc.crm.core.workflow.internal.WorkflowEmailPublisher;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.tracking.fields.EmailTrackingFields;
import com.tlc.crm.email.core.OutboundEmailLinkerService;
import com.tlc.crm.email.core.fields.EmailLinkerFields;

import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;

import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.OutboundEmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Processes the email linker related actions.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see SecureUserTaskProcessor
 */
public final class EmailLinkerProcessor implements SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailLinkerProcessor.class);

    private final EmailConverter emailConverter;
    private final OutboundEmailLinkerService outboundService;
    private final EmailSchedulerManager emailSchedulerManager;
    private final WorkflowEmailPublisher workflowEmailPublisher;

    private EmailLinkerProcessor() {
        this.emailConverter = EmailConverter.getInstance();
        this.outboundService = OutboundEmailLinkerService.getInstance();
        this.emailSchedulerManager = EmailSchedulerManager.getInstance();
        this.workflowEmailPublisher = WorkflowEmailPublisher.getInstance();
    }

    private static class Instance {
        private static final EmailLinkerProcessor INSTANCE = new EmailLinkerProcessor();
    }

    public static EmailLinkerProcessor getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Processes the {@link Thread} based on provided details.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param thread Represents the {@link Thread}
     * @param data   Represents the state json data
     */
    public void process(final long orgId, final long userId, final Thread thread, final JsonObject data, final JsonObject stateJson) {

        try {
            secureProcess(orgId, userId, () -> {
                final EntityThread entityThread = emailConverter.convert(new ConverterConfig(orgId, userId, Locale.getDefault(), false, data), data);
                final Collection<EmailTrackingDetail> emailTrackingDetailCollection = new HashSet<>();

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

                            emailTrackingDetailCollection.add(emailTrackingDetail);
                        }
                    }
                }

                entityThread.getEntityEmails().stream().findFirst().ifPresent(entityEmail -> {
                    final Optional<Email> composeEmail = thread.getComposeEmail();
                    final Email.MessageCategory messageCategory = entityEmail.getMessageCategory();

                    setRecipients(entityEmail);

                    if (composeEmail.isPresent()) {
                        final Email email = composeEmail.get();

                        entityEmail.setMetaData(email.getMetaData());
                        entityEmail.setId(email.id());
                        entityEmail.setSender(email.getSender());
                        entityEmail.setSyncedState(true);

                        if (data.containsKey(EmailLinkerFields.CONVERSATION_ID)) {
                            entityEmail.setConversationId(data.getLong(EmailLinkerFields.CONVERSATION_ID));
                        }

                        if (data.containsKey(EmailLinkerFields.THREAD_ID)) {
                            entityThread.setCrmId(data.getLong(EmailLinkerFields.THREAD_ID));
                        }

                        if (data.containsKey(EmailFields.VARIANT.getName())) {
                            final JsonObject variant = data.getJsonObject(EmailFields.VARIANT.getName());
                            final EmailVariant emailVariant = EmailVariant.fromValue(variant.getShort(EmailFields.TYPE.getName()));

                            if (variant.containsKey(EmailFields.ID.getName())) {
                                emailVariant.setId(variant.getLong(EmailFields.ID.getName()));
                            }
                            entityEmail.setVariant(emailVariant);
                        }

                        if (Objects.requireNonNull(messageCategory) == Email.MessageCategory.OUTBOUND) {
                            final EntityOutboundEmail entityOutboundEmail = entityEmail.get();
                            final OutboundEmail outboundEmail = email.get();

                            entityOutboundEmail.setOutboundType(outboundEmail.getOutboundType());
                            entityOutboundEmail.setEmailTrackingDetails(emailTrackingDetailCollection);
                            entityOutboundEmail.setScheduled(data.optBoolean(EmailFields.IS_SCHEDULED.getName()));
                        }
                    }

                    entityThread.setId(thread.id());
                    entityThread.setUserId(userId);

                    if (entityEmail instanceof EntityOutboundEmail entityOutboundEmail && entityOutboundEmail.isScheduled()) {
                        emailSchedulerManager.loadMailRowIntoContainer(entityOutboundEmail);
                    } else {
                        entityEmail.setUserId(userId);
                        final String threadUserIdKey = CoreEmailFields.THREAD_USER_ID.getName();

                        if (data.containsKey(threadUserIdKey)) {
                            entityThread.setUserId(data.getLong(threadUserIdKey));
                        }
                        outboundService.send(orgId, entityThread);

                        try {
                            workflowEmailPublisher.publish(orgId, userId, entityThread, stateJson, true);
                        } catch (Exception exception) {
                            LOGGER.error("Email Send: Failed to publish the email action, OrgId:{}, UserId:{}, Message: {}", orgId, userId, exception.getMessage(), exception);
                        }
                    }
                });
            });
        } catch (Exception e) {
            LOGGER.error("Email Send: Failed to load the mail into DB, OrgId:{}, UserId:{}, Message: {}", orgId, userId, e.getMessage(), e);
            workflowEmailPublisher.publish(orgId, userId, null, stateJson, false);

            throw ErrorCode.get(EmailErrorCodes.DB_LOAD_ACTION_FAILED, e);
        }
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

            contactRecipients = contactEmailIds.isEmpty() ? Collections.emptyMap() : ContactManager.getInstance().getContactEmailIds(orgId, contactEmailIds);
        } else {
            contactRecipients = Collections.emptyMap();
        }

        final BiConsumer<AddressType, Collection<EmailAddress>> setRecipientByType = (addressType, addresses) -> {
            final Collection<EmailAddress> emailAddresses = contactRecipients.isEmpty() ? new HashSet<>()
                    : getEmailRecipients(recipients, addressType, contactRecipients);

            if (Objects.nonNull(addresses)) {
                emailAddresses.addAll(addresses);
            }

            emailRecipients.addRecipients(addressType, emailAddresses);
        };

        setRecipientByType.accept(AddressType.TO, receivers);
        setRecipientByType.accept(AddressType.BCC, emailRecipients.bccRecipients());
        setRecipientByType.accept(AddressType.CC, emailRecipients.ccRecipients());
    }

    /**
     * Gets the email recipients list
     *
     * @param recipients        Represents the recipient ids
     * @param contactRecipients Represents the contact relate recipient ids
     * @param addressType       Represents the {@link AddressType}
     * @return the collections of {@link EmailAddress} mapped in recipients
     */
    private Collection<EmailAddress> getEmailRecipients(final ContactRecipients recipients, final AddressType addressType,
                                                        final Map<Long, String> contactRecipients) {
        final Collection<Contact> contacts = new HashSet<>();
        final Collection<EmailAddress> addresses = new HashSet<>();
        final Collection<Long> recipientIds = switch (addressType) {
            case TO -> recipients.toEmailIds();
            case CC -> recipients.ccEmailIds();
            case BCC -> recipients.bccEmailIds();
            default -> null;
        };

        if (Objects.nonNull(contactRecipients) && Objects.nonNull(recipientIds) && !recipientIds.isEmpty()) {

            for (final Long id : recipientIds) {
                final Contact contact = new Contact();
                final String value = contactRecipients.get(id);

                contact.setPrimaryEmail(emailConverter.processEmail(id, value));
                contacts.add(contact);
                addresses.add(new EmailAddress(id, value));
            }

            recipients.addContacts(addressType, contacts);
        }

        return addresses;
    }
}
