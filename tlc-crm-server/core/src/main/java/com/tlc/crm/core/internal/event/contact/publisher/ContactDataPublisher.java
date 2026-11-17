package com.tlc.crm.core.internal.event.contact.publisher;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactSyncManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.ContactSyncSettings;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.internal.event.CoreEvent;
import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.crm.entity.models.displayPicture.SourceDisplayPicture;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.notification.app.Event;
import com.tlc.l10n.temporal.resolver.DateUtil;
import com.tlc.crm.common.sync.SyncMode;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.QueueService;
import com.tlc.queue.service.provider.QueueServiceProvider;
import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Publishes the contact data to the provider based on the action chosen.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class ContactDataPublisher implements PostEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactDataPublisher.class);

    private final QueueService queueService;
    private final CompanyManager companyManager;
    private final ContactSyncManager contactSyncManager;
    private final DateUtil dateUtil;

    private ContactDataPublisher() {
        queueService = QueueServiceProvider.get();
        companyManager = CompanyManager.getInstance();
        contactSyncManager = ContactSyncManager.getInstance();
        dateUtil = DateUtil.getInstance();
    }

    /**
     * <p>
     * Provides the name of the event.
     * </p>
     *
     * @return {@link Event}.
     */
    @Override
    public Event event() {
        return CoreEvent.CONTACT_PUBLISH_EVENT;
    }

    private static class Instance {
        private static final ContactDataPublisher INSTANCE = new ContactDataPublisher();
    }

    /**
     * <p>
     * Gets the {@link ContactDataPublisher}.
     * </p>
     *
     * @return {@link ContactDataPublisher}
     */
    public static ContactDataPublisher getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Initiates the post process action which is to decide whether to publish the contact to provider.
     * </p>
     *
     * @param eventData Represents {@link EventData}.
     */
    @Override
    public void processPostEvent(final EventData eventData) {
        LOGGER.debug("ContactDataPublisher listened to the post event");
        final Collection<? extends TlcModel> models = eventData.getValues();

        if (!models.isEmpty()) {
            models.stream().findFirst().ifPresent(tlcModel -> {

                if (tlcModel.getClass().isAssignableFrom(Contact.class)) {
                    final Contact contact = (Contact) tlcModel;

                    if (Objects.isNull(contact.getExternalPersonId())) {
                        final Long orgId = eventData.getOrgId();
                        final Long userId = contact.getSalesOwner().id();

                        if (contactSyncManager.isSyncEnabled(orgId, userId)) {
                            final ContactSyncSettings.Config syncConfig = contactSyncManager.getSyncConfig(orgId, userId);

                            if (SyncMode.isEquals(SyncMode.TWO_WAY, syncConfig.getSyncWay())) {
                                publish(orgId, userId, eventData);
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * <p>
     * Publishes contact to the provider.
     * </p>
     *
     * @param eventData Represents {@link EventData}.
     */
    private void publish(final Long orgId, final Long userId, final EventData eventData) {
        final String orgIdAsString = String.valueOf(orgId);
        final JsonObject jsonObject = Json.object();
        final String contactsKey = ContactFields.CONTACTS;
        final Collection<? extends TlcModel> tlcModels = eventData.getValues();
        final Collection<Contact> contacts = (Collection<Contact>) tlcModels;

        jsonObject.put(ContactFields.ORG_ID, orgId);
        jsonObject.put(ContactFields.USER_ID, userId);
        jsonObject.put(contactsKey, buildContacts(orgId, userId, contacts));
        final byte[] bytes = jsonObject.getBytes();

        switch (eventData.getAction()) {
            case CREATE -> {
                final SubscriptionTopic contactCreateEventTopic = CoreQueueTopics.getContactCreateEventTopic(QueueModel.Type.KAFKA);
                final KafkaProducerConfig contactCreateEventProducerConfig = new KafkaProducerConfig(contactCreateEventTopic);

                try (final Producer<byte[]> contactCreateEventProducer = queueService.createProducer(contactCreateEventProducerConfig)) {
                    contactCreateEventProducer.send(new StreamMessage<>(orgIdAsString, contactCreateEventTopic, bytes));
                }
            }
            case UPDATE -> {
                final SubscriptionTopic contactUpdateEventTopic = CoreQueueTopics.getContactUpdateEventTopic(QueueModel.Type.KAFKA);
                final KafkaProducerConfig contactUpdateEventProducerConfig = new KafkaProducerConfig(contactUpdateEventTopic);

                try (final Producer<byte[]> contactUpdateEventProducer = queueService.createProducer(contactUpdateEventProducerConfig)) {
                    contactUpdateEventProducer.send(new StreamMessage<>(orgIdAsString, contactUpdateEventTopic, bytes));
                }
            }
        }
    }

    /**
     * <p>
     * Constructs and publish the contacts based on the given action
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param contacts Represents the collection of {@link Contact}
     */
    private JsonArray buildContacts(final long orgId, final long userId, final Collection<Contact> contacts) {
        final String idKey = ContactFields.ID;
        final String emailsKey = ContactFields.EMAILS;
        final String phonesKey = ContactFields.PHONES;
        final String companyKey = ContactFields.COMPANY;
        final String addressLine1Key = ContactFields.ADDRESS_LINE1;
        final String addressLine2Key = ContactFields.ADDRESS_LINE2;
        final String stateKey = ContactFields.STATE;
        final String cityKey = ContactFields.CITY;
        final String countryKey = ContactFields.COUNTRY;
        final String zipcodeKey = ContactFields.ZIPCODE;
        final String addressKey = ContactFields.ADDRESS;
        final String firstNameKey = ContactFields.FIRST_NAME;
        final String lastNameKey = ContactFields.LAST_NAME;
        final String designationKey = ContactFields.DESIGNATION;
        final String birthdayKey = ContactFields.BIRTHDAY;
        final String orgIdKey = ContactFields.ORG_ID;
        final String userIdKey = ContactFields.USER_ID;
        final String sourceKey = ContactFields.SOURCE;
        final String originalSizeKey = ContactFields.ORIGINAL_SIZE;
        final String originalExtensionIdKey = ContactFields.ORIGINAL_EXTENSION_ID;
        final String thumbnailSizeKey = ContactFields.THUMBNAIL_SIZE;
        final String displayPictureKey = ContactFields.DISPLAY_PICTURE;
        final JsonArray contactsArray = Json.array();

        for (final Contact contact : contacts) {
            final JsonObject contactJson = Json.object();
            final Collection<Email> emails = contact.getEmails();
            final Collection<Phone> phones = contact.getPhones();
            final Company company = contact.getCompany();
            final Address address = contact.getAddress();
            final SourceDisplayPicture sourceDisplayPicture = contact.getDisplayPicture();

            if (Objects.nonNull(emails) && !emails.isEmpty()) {
                contactJson.put(emailsKey, emails.stream().map(Email::getValue).toList());
            }

            if (Objects.nonNull(phones) && !phones.isEmpty()) {
                contactJson.put(phonesKey, phones.stream().map(Phone::getNumber).toList());
            }

            if (Objects.nonNull(company)) {
                final String name = company.getName();

                contactJson.put(companyKey, Objects.isNull(name) ? companyManager.partialGet(orgId, company.id()).getName() : name);
            }

            if (Objects.nonNull(address)) {
                final JsonObject addressJson = Json.object();

                addressJson.put(addressLine1Key, address.getAddressLine1());
                addressJson.put(addressLine2Key, address.getAddressLine2());
                addressJson.put(cityKey, address.getCity());
                addressJson.put(stateKey, address.getState());
                addressJson.put(countryKey, address.getCountry());
                addressJson.put(zipcodeKey, address.getZipcode());
                contactJson.put(addressKey, addressJson);
            }

            if (Objects.nonNull(sourceDisplayPicture)) {
                final JsonObject displayPictureJson = Json.object();
                final DisplayPicture originalDisplayPicture = (DisplayPicture) sourceDisplayPicture.getOriginalPicture();
                final DisplayPicture thumbnail = (DisplayPicture) sourceDisplayPicture.getThumbnail();

                displayPictureJson.put(sourceKey, sourceDisplayPicture.getSource());
                displayPictureJson.put(userIdKey, contact.getSalesOwner().id());
                displayPictureJson.put(originalSizeKey, originalDisplayPicture.size());
                displayPictureJson.put(originalExtensionIdKey, originalDisplayPicture.extension().getId());
                displayPictureJson.put(thumbnailSizeKey, thumbnail.size());
                contactJson.put(displayPictureKey, displayPictureJson);
            }
            contactJson.put(idKey, contact.id());
            contactJson.put(firstNameKey, contact.getFirstName());
            contactJson.put(lastNameKey, contact.getLastName());
            contactJson.put(designationKey, contact.getDesignation());
            contactJson.put(birthdayKey, dateUtil.getTimeStamp(contact.getDateOfBirth()));
            contactJson.put(orgIdKey, orgId);
            contactJson.put(userIdKey, userId);
            contactsArray.put(contactJson);
        }
        return contactsArray;
    }
}