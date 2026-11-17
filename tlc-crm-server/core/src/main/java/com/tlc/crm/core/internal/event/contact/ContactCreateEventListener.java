package com.tlc.crm.core.internal.event.contact;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.core.api.contact.manager.ContactSyncManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.internal.contact.sync.ContactSyncService;

import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.notification.service.contact.ContactService;

import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.provider.ContactProvider;
import com.tlc.sync.service.oauth.service.provider.ProviderService;
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
 * A listener that is responsible to trigger and control the contact create consumer actions {@link ContactCreateEventHandler}
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class ContactCreateEventListener implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactCreateEventListener.class);
    private static final String REQUEST_CONSUMER_NAME = "tlc-crm-core-contact-create-consumer";
    private static final String THREAD_POOL_NAME = "contact-create-pool";
    private static final String TASK_SCHEDULER_NAME = "contact-create";

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    public ContactCreateEventListener() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the input of contact creation
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.task = new ContactCreateTask(new ContactCreationTaskController());
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, task);

            LOGGER.debug("Contact creation service started, Scheduler ID : {}", schedulerId);
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
            LOGGER.debug("Contact creation service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible to create contact
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class ContactCreateTask extends Task {

        private final BackgroundConsumer contactCreateConsumer;

        private ContactCreateTask(final TaskController controller) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(REQUEST_CONSUMER_NAME, false,
                    List.of(CoreQueueTopics.getContactCreateEventTopic(QueueModel.Type.KAFKA)), new ContactCreateEventHandler(),
                    new ConsumerConfig.PollConfig(10, 1000, 10));
            contactCreateConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            contactCreateConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            contactCreateConsumer.stop();
            return true;
        }
    }

    /**
     * <p>
     * A controller which controls the {@link ContactCreateTask}.
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class ContactCreationTaskController extends TaskController {

        private ContactCreationTaskController() {
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
     * An event handler that process the contact create requests and triggers the contact create action based on the request parameters
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class ContactCreateEventHandler implements StreamListener<byte[]>, SecureUserTaskProcessor {

        private static final Logger LOGGER = LoggerFactory.getLogger(ContactCreateEventHandler.class);
        private final ContactSyncService contactSyncService;
        private final ContactSyncManager contactSyncManager;
        private final ProviderService contactProviderService;
        private final ContactService contactService;

        private ContactCreateEventHandler() {
            contactSyncService = ContactSyncService.getInstance();
            contactProviderService = ProviderService.getContactProviderService();
            contactSyncManager = ContactSyncManager.getInstance();
            contactService = ContactService.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @param streamMessage Represents the {@link StreamMessage}
         */
        @Override
        public void listen(final StreamMessage<byte[]> streamMessage) {
            final JsonObject requestParameters = Json.object(streamMessage.data());
            final long orgId = requestParameters.getLong(ContactFields.ORG_ID);
            final long userId = requestParameters.getLong(ContactFields.USER_ID);

            secureProcess(orgId, userId, () -> {
                final com.tlc.notification.model.email.EmailAddress emailAddress = contactSyncService.getSyncedAddress(orgId, userId);

                if (Objects.nonNull(emailAddress)) {
                    final Collection<Contact> contacts = contactSyncService.convertJsonToModels(requestParameters.getJsonArray(ContactFields.CONTACTS));
                    final ContactProvider provider = contactProviderService.findProvider(new EmailAddress(emailAddress.getEmailId()));

                    for (final Contact contact : contacts) {
                        if (Objects.isNull(contact.getExternalPersonId())) {
                            contact.setOwnerId(userId);
                            contact.setExternalPersonId(contactService.addContact(contactSyncService.convertContactToPerson(contact, orgId), emailAddress));
                            final Long syncId = contactSyncManager.getSyncId(orgId, userId);

                            if (Objects.nonNull(syncId)) {
                                contactSyncManager.loadSyncedContactRowIntoContainer(contact, syncId, null);
                                LOGGER.debug("Contact created successfully in Contact provider : {}, Contact id : {} ", provider.getName(), contact.id());
                            }
                        }
                    }
                }
            });
        }
    }
}