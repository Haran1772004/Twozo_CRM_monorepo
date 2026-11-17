package com.tlc.crm.core.notification.email.api.model.builder;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.email.notification.email.sync.api.EmailSyncManager;
import com.tlc.crm.email.notification.model.UserSyncAddressInfo;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.model.EmailTimeline;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;

import com.tlc.email.common.model.address.AddressType;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.i18n.I18nKey;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.email.common.model.type.Email.MessageCategory;
import com.tlc.email.common.model.address.EmailAddress;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.tlc.email.common.model.type.Email.MessageCategory.INBOUND;
import static com.tlc.email.common.model.type.Email.MessageCategory.OUTBOUND;

/**
 * <p>
 * Provides the functionalities related to builds the {@link Timeline} for  email
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 * @see EmailTimelineBuilder
 */
public final class EmailTimelineBuilder {

    private final ContactManager contactManager;
    private final EmailSyncManager emailSyncManager;

    private static class Instance {
        private static final EmailTimelineBuilder INSTANCE = new EmailTimelineBuilder();
    }

    private EmailTimelineBuilder() {
        contactManager = ContactManager.getInstance();
        emailSyncManager = EmailSyncManager.getInstance();
    }

    /**
     * <p>
     * Gets instance of {@link EmailTimelineBuilder}
     * </p>
     *
     * @return {@link EmailTimelineBuilder} instance
     */
    public static EmailTimelineBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Builds timeline to hold email related details
     * </p>
     *
     * @param orgId          Represents an organization's id
     * @param userId         Represents a user id
     * @param entityEmail    Represents the entity email
     * @param conversationId Represents a conversation id
     * @return a {@link Timeline}
     */
    public Timeline buildTimeline(final long orgId, final Long userId, final EntityEmail entityEmail,
                                  final Long conversationId) {
        final Timeline timeline = new Timeline();
        final EmailTimeline emailTimeline = new EmailTimeline();
        final MessageCategory messageCategory = entityEmail.getMessageCategory();

        if (Objects.equals(INBOUND, messageCategory)) {
            timeline.setAction(TimelineActionFields.RECEIVED);
            timeline.setTimelineType(TimelineType.EMAIL_RECEIVED);
        } else {

            if (entityEmail instanceof EntityOutboundEmail outboundEmail) {
                switch (outboundEmail.getOutboundType()) {
                    case SEND, FORWARD -> {
                        timeline.setAction(TimelineActionFields.SENT);
                        timeline.setTimelineType(TimelineType.EMAIL_SENT);
                    }
                    case REPLY, REPLY_ALL -> {
                        timeline.setAction(TimelineActionFields.REPLIED);
                        timeline.setTimelineType(TimelineType.EMAIL_REPLIED);
                    }
                }
            } else if (entityEmail.getEmailVariant().isSmartBcc()) {
                timeline.setAction(TimelineActionFields.SENT);
                timeline.setTimelineType(TimelineType.EMAIL_SENT);
            }
        }

        timeline.setOwnerId(userId);
        timeline.setCategory(Timeline.Category.EMAILS);
        timeline.setOrgId(orgId);
        timeline.setAuditedTime(new DateTime(Instant.now().toEpochMilli()));
        emailTimeline.setId(conversationId);
        timeline.setEntityTimeline(emailTimeline);

        return timeline;
    }

    /**
     * <p>
     * Builds the {@link FieldTimeline}
     * </p>
     *
     * @param orgId             Represents an organization id
     * @param userId            Represents the user id
     * @param fromAddress       Represents the from address
     * @param contactEmailIds   Represents the contact email ids
     * @param receiverAddresses Represents the receiver addresses
     * @param messageCategory   Represents the {@link MessageCategory}
     * @return {@link FieldTimeline}
     */
    public FieldTimeline buildFieldTimeline(final long orgId, final Long userId, final EmailAddress fromAddress,
                                            final Collection<Long> contactEmailIds, final Collection<EmailAddress> receiverAddresses,
                                            final MessageCategory messageCategory, final ContactRecipients contactRecipients) {
        final Map<String, Contact> existingContactEmails = Objects.nonNull(contactEmailIds) && !contactEmailIds.isEmpty()
                ? contactManager.getContactEmails(orgId, contactEmailIds) : Collections.emptyMap();
        final Map<String, Contact> mailContactEmails = Objects.nonNull(contactRecipients) ? getContactEmails(contactRecipients)
                : Collections.emptyMap();
        final JsonObject jsonObject = Json.object();
        final JsonObject toJson = Json.object();
        final JsonArray addressArray = Json.array();
        final JsonArray contactArray = Json.array();
        final Optional<UserSyncAddressInfo> syncedAddressInfo = emailSyncManager.getSyncEmailAddress(orgId, userId);
        final String syncedEmailAddress = syncedAddressInfo.map(EmailAddress::getEmailAddress).orElseGet(() -> null);

        if (receiverAddresses.contains(fromAddress)) {
            receiverAddresses.remove(fromAddress);
        }

        if (messageCategory.isReceiveCategory() && existingContactEmails.containsKey(syncedEmailAddress)) {
            receiverAddresses.remove(new EmailAddress(syncedEmailAddress));
        }

        for (final EmailAddress emailAddress : receiverAddresses) {
            final String address = emailAddress.getEmailAddress();

            if (existingContactEmails.containsKey(address)) {
                contactArray.put(getContactJson(address, existingContactEmails.get(address)));
            } else if (mailContactEmails.containsKey(address)) {
                contactArray.put(getContactJson(address, mailContactEmails.get(address)));
            } else {
                addressArray.put(address);
            }
        }

        if (INBOUND == messageCategory) {
            final String from = fromAddress.getEmailAddress();

            if (Objects.nonNull(from)) {

                if (existingContactEmails.containsKey(from)) {
                    jsonObject.put(CoreEmailFields.FROM.getName(), getContactJson(from, existingContactEmails.get(from)));
                } else {
                    jsonObject.put(CoreEmailFields.FROM.getName(), from);
                }
            }
            jsonObject.put(CoreEmailFields.TYPE.getName(), INBOUND.name().toLowerCase());
            jsonObject.put(CoreEmailFields.SYNCED_ADDRESS.getName(), syncedEmailAddress);
        } else {
            jsonObject.put(CoreEmailFields.TYPE.getName(), OUTBOUND.name().toLowerCase());
        }

        toJson.put(CoreEmailFields.CONTACTS.getName(), contactArray);
        toJson.put(CoreEmailFields.EMAIL_ADDRESS.getName(), addressArray);
        jsonObject.put(CoreEmailFields.TO.getName(), toJson);

        return new FieldTimeline(null, jsonObject.toString(), new I18nKey("i18n.notifications.email.address"));
    }

    /**
     * <p>
     * Gets the {@link JsonObject} for contact
     * </p>
     *
     * @param email   Represents the email ID.
     * @param contact Represents the {@link Contact}
     * @return {@link JsonObject}
     */
    private JsonObject getContactJson(final String email, final Contact contact) {
        final JsonObject contactJson = Json.object();

        contactJson.put(CoreEmailFields.CONTACT_ID.getName(), contact.id());
        contactJson.put(CoreEmailFields.CONTACT_NAME.getName(), contact.getFullName());
        contactJson.put(CoreEmailFields.EMAIL_ADDRESS.getName(), email);

        return contactJson;
    }

    /**
     * <p>
     * Gets the {@link Map} of contact and its respective email address
     * </p>
     *
     * @param contactRecipients  Represents the {@link ContactRecipients}
     * @return the {@link Map} of contact and email
     */
    private Map<String, Contact> getContactEmails(final ContactRecipients contactRecipients) {
        final Map<AddressType, Collection<Contact>> recipients = contactRecipients.getContacts();
        final Collection<Contact> contacts = Objects.isNull(recipients) ? Collections.emptyList() : recipients.get(AddressType.TO);
        final Map<String, Contact> contactEmails = new HashMap<>();

        if (Objects.nonNull(contacts)) {

            for (final Contact contact : contacts) {
                final Email primaryEmail = contact.getPrimaryEmail();
                final String emailAddress = Objects.nonNull(primaryEmail) ? primaryEmail.getValue() : null;

                if (Objects.nonNull(emailAddress)) {
                    contactEmails.put(emailAddress, contact);
                }
            }
        }
        return contactEmails;
    }
}
