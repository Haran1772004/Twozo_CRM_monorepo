package com.tlc.crm.core.internal.event.contact;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.internal.contact.sync.ContactSyncService;

import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.notification.model.email.EmailAddress;
import com.tlc.notification.service.contact.ContactService;

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
 * A listener that is responsible to trigger and control the contact deletion consumer actions {@link ContactDeleteEventListener}
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class ContactDeleteEventListener implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactDeleteEventListener.class);
    private static final String REQUEST_CONSUMER_NAME = "tlc-crm-core-contact-delete-consumer";
    private static final String THREAD_POOL_NAME = "contact-delete-pool";
    private static final String TASK_SCHEDULER_NAME = "contact-delete";

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    public ContactDeleteEventListener() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the input of contact deletion
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.task = new ContactDeleteTask(new ContactDeletionTaskController());
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, task);

            LOGGER.debug("Contact deletion service started, Scheduler ID : {}", schedulerId);
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
            LOGGER.debug("Contact deletion service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible to delete contact
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class ContactDeleteTask extends Task {

        private final BackgroundConsumer contactDeleteConsumer;

        private ContactDeleteTask(final TaskController controller) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(REQUEST_CONSUMER_NAME, false,
                    List.of(CoreQueueTopics.getContactDeleteEventTopic(QueueModel.Type.KAFKA)), new ContactDeleteEventHandler(),
                    new ConsumerConfig.PollConfig(10, 1000, 10));
            contactDeleteConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            contactDeleteConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            contactDeleteConsumer.stop();
            return true;
        }
    }

    /**
     * <p>
     * A controller which controls the {@link ContactDeletionTaskController}.
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class ContactDeletionTaskController extends TaskController {

        private ContactDeletionTaskController() {
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
     * An event handler that process the contact delete requests and triggers the contact delete action based on the request parameters
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class ContactDeleteEventHandler implements StreamListener<byte[]>, SecureUserTaskProcessor {

        private static final Logger LOGGER = LoggerFactory.getLogger(ContactDeleteEventHandler.class);
        private final ContactSyncService contactSyncService;
        private final ProviderService contactProviderService;
        private final ContactService contactService;

        private ContactDeleteEventHandler() {
            contactSyncService = ContactSyncService.getInstance();
            contactProviderService = ProviderService.getContactProviderService();
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
                final EmailAddress emailAddress = contactSyncService.getSyncedAddress(orgId, userId);

                if (Objects.nonNull(emailAddress)) {
                    final Collection<String> externalPersonIds = (Collection<String>) requestParameters.get(ContactFields.EXTERNAL_PERSON_IDS);

                    for (final String externalPersonId : externalPersonIds) {
                        contactService.deleteContact(externalPersonId, emailAddress);
                        LOGGER.debug("People deleted successfully, People id : {} ", externalPersonId);
                    }
                }
            });
        }
    }
}