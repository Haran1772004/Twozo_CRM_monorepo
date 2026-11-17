package com.tlc.crm.core.internal.event.contact;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.core.api.contact.manager.ContactSyncManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.internal.contact.sync.ContactSyncService;
import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.notification.model.contact.Person;
import com.tlc.notification.model.email.EmailAddress;
import com.tlc.notification.service.contact.ContactService;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.consumer.ConsumerConfig;

import com.tlc.queue.consumer.BackgroundConsumer;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaConsumerConfig;
import com.tlc.queue.service.provider.QueueServiceProvider;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * A listener that is responsible to trigger and control the contact update consumer actions {@link ContactUpdateEventHandler}
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class ContactUpdateEventListener implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactUpdateEventListener.class);
    private static final String REQUEST_CONSUMER_NAME = "tlc-crm-core-contact-update-consumer";
    private static final String THREAD_POOL_NAME = "contact-update-pool";
    private static final String TASK_SCHEDULER_NAME = "contact-update";

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    public ContactUpdateEventListener() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the input of contact updation
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.task = new ContactUpdateTask(new ContactUpdationTaskController());
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, task);

            LOGGER.debug("Contact updation service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(task) && task.isRunning()) {
                task.stopTask();
            }
            LOGGER.debug("Contact updation service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible to update contact
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class ContactUpdateTask extends Task {

        private final BackgroundConsumer contactUpdateConsumer;

        private ContactUpdateTask(final TaskController controller) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(REQUEST_CONSUMER_NAME, false,
                    List.of(CoreQueueTopics.getContactUpdateEventTopic(QueueModel.Type.KAFKA)), new ContactUpdateEventHandler(),
                    new ConsumerConfig.PollConfig(10, 1000, 10));
            contactUpdateConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            contactUpdateConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            contactUpdateConsumer.stop();
            return true;
        }
    }

    /**
     * <p>
     * A controller which controls the {@link ContactUpdationTaskController}.
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class ContactUpdationTaskController extends TaskController {

        private ContactUpdationTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getNextInterval() {
            return -1;
        }
    }

    /**
     * <p>
     * An event handler that process the contact update requests and triggers the contact update action based on the request parameters
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class ContactUpdateEventHandler implements StreamListener<byte[]> {

        private static final Logger LOGGER = LoggerFactory.getLogger(ContactUpdateEventHandler.class);
        private final ContactSyncService contactSyncService;
        private final ContactSyncManager contactSyncManager;
        private final ContactService contactService;

        private ContactUpdateEventHandler() {
            contactSyncService = ContactSyncService.getInstance();
            contactSyncManager = ContactSyncManager.getInstance();
            contactService = ContactService.getInstance();
        }

        @Override
        public void listen(final StreamMessage<byte[]> streamMessage) {
            final JsonObject requestParameters = Json.object(streamMessage.data());
            final long orgId = requestParameters.getLong(ContactFields.ORG_ID);
            final long userId = requestParameters.getLong(ContactFields.USER_ID);
            final EmailAddress emailAddress = contactSyncService.getSyncedAddress(orgId, userId);

            if (Objects.nonNull(emailAddress)) {
                final Collection<Contact> contacts = contactSyncService.convertJsonToModels(requestParameters.getJsonArray(ContactFields.CONTACTS));

                for (final Contact contact : contacts) {
                    if (Objects.isNull(contact.getExternalPersonId())) {
                        final Long id = contact.id();
                        final Person person = contactSyncService.convertContactToPerson(contact, orgId);

                        person.setExternalPersonId(contactSyncService.getExternalPersonId(orgId, id));
                        contact.setOwnerId(userId);
                        contact.setExternalPersonId(contactService.updateContact(person, emailAddress));
                        final Long syncId = contactSyncManager.getSyncId(orgId, userId);

                        if (Objects.nonNull(syncId)) {
                            contactSyncManager.loadSyncedContactRowIntoContainer(contact, syncId, null);
                            LOGGER.debug("Contact updated successfully in provider, Contact id : {} ", id);
                        }
                    }
                }
            }
        }
    }
}