package com.tlc.crm.core.internal.event.contact.publisher;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.ContactSyncManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.ContactSyncSettings;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.internal.event.CoreEvent;
import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.crm.notification.app.Event;
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
import java.util.stream.Collectors;

/**
 * <p>
 * Removes the contact data from the contact provider.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public final class ContactDataRemover implements PostEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactDataRemover.class);

    private final ContactSyncManager contactSyncManager;
    private final QueueService queueService;

    private ContactDataRemover() {
        contactSyncManager = ContactSyncManager.getInstance();
        queueService = QueueServiceProvider.get();
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
        return CoreEvent.CONTACT_DELETE_EVENT;
    }

    private static class Instance {
        private static final ContactDataRemover INSTANCE = new ContactDataRemover();
    }

    /**
     * <p>
     * Gets the {@link ContactDataRemover}.
     * </p>
     *
     * @return {@link ContactDataRemover}
     */
    public static ContactDataRemover getInstance() {
        return ContactDataRemover.Instance.INSTANCE;
    }

    /**
     * <p>
     * Initiates the delete post process action for contact
     * </p>
     *
     * @param eventData Represents {@link EventData}.
     */
    @Override
    public void processPostEvent(final EventData eventData) {
        LOGGER.debug("ContactDataRemover listened to post event");
        final Collection<? extends TlcModel> models = eventData.getValues();

        if (!models.isEmpty()) {
            models.stream().findFirst().ifPresent(tlcModel -> {

                if (tlcModel.getClass().isAssignableFrom(Contact.class)) {
                    final Contact contact = (Contact) tlcModel;

                    if (Action.DELETE == eventData.getAction() && Objects.nonNull(contact.getExternalPersonId())) {
                        final Long orgId = eventData.getOrgId();
                        final Long userId = contact.getSalesOwner().id();

                        if (contactSyncManager.isSyncEnabled(orgId, userId)) {
                            final ContactSyncSettings.Config syncConfig = contactSyncManager.getSyncConfig(orgId, userId);

                            if (SyncMode.isEquals(SyncMode.TWO_WAY, syncConfig.getSyncWay())) {
                                final JsonObject jsonObject = Json.object();
                                final Collection<Contact> contacts = (Collection<Contact>) models;
                                final SubscriptionTopic contactDeleteEventTopic = CoreQueueTopics.getContactDeleteEventTopic(QueueModel.Type.KAFKA);
                                final KafkaProducerConfig contactDeleteEventProducerConfig = new KafkaProducerConfig(contactDeleteEventTopic);

                                jsonObject.put(ContactFields.ORG_ID, orgId);
                                jsonObject.put(ContactFields.USER_ID, userId);
                                jsonObject.put(ContactFields.EXTERNAL_PERSON_IDS, contacts.stream().map(Contact::getExternalPersonId).collect(Collectors.toSet()));

                                try (final Producer<byte[]> contactDeleteEventProducer = queueService.createProducer(contactDeleteEventProducerConfig)) {
                                    contactDeleteEventProducer.send(new StreamMessage<>(String.valueOf(orgId), contactDeleteEventTopic, jsonObject.getBytes()));
                                }
                            }
                        }
                    }
                }
            });
        }
    }
}