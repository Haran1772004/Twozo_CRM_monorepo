package com.tlc.crm.core.internal.event.contact;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.core.api.contact.manager.ContactSyncManager;
import com.tlc.crm.core.api.contact.model.ContactSyncSettings;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.internal.contact.sync.ContactSyncService;
import com.tlc.crm.core.internal.status.CoreErrorCodes;

import com.tlc.crm.core.queue.CoreQueueTopics;
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A service that is responsible to trigger and control the contact push actions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class ContactPushEventListener implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactPushEventListener.class);
    private static final String REQUEST_CONSUMER_NAME = "tlc-crm-core-contact-push-consumer";
    private static final String THREAD_POOL_NAME = "contact-push-pool";
    private static final String TASK_SCHEDULER_NAME = "contact-push";

    private final SchedulerService schedulerService;
    private Task contactPushTask;
    private Long schedulerId;

    public ContactPushEventListener() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the configurations
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.contactPushTask = new ContactSyncTask(new ContactSyncTaskController());
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, contactPushTask);
            LOGGER.debug("Contact push consumer service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(contactPushTask) && contactPushTask.isRunning()) {
                contactPushTask.stopTask();
            }
            LOGGER.debug("Contact push consumer service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible for contact syncing activity
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class ContactSyncTask extends Task {

        private final BackgroundConsumer contactSyncConsumer;

        private ContactSyncTask(final TaskController controller) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(REQUEST_CONSUMER_NAME, false,
                    List.of(CoreQueueTopics.getContactPushEventTopic(QueueModel.Type.KAFKA)), new ContactPushHandler(),
                    new ConsumerConfig.PollConfig(10, 1000, 10));
            contactSyncConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * <p>
         * Executes the task which is scheduled
         * </p>
         *
         * @param taskController represents the controller of the task
         */
        @Override
        public void execute(final TaskController taskController) {
            contactSyncConsumer.run();
        }

        /**
         * {@inheritDoc}
         *
         * @return true
         */
        @Override
        public boolean stopTask() {
            contactSyncConsumer.stop();
            return true;
        }
    }

    /**
     * A controller which controls contact syncing task {@link ContactSyncTask}.
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    public static class ContactSyncTaskController extends TaskController {

        public ContactSyncTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        /**
         * {@inheritDoc}
         *
         * @return long of interval time
         */
        @Override
        public long getNextInterval() {
            return -1;
        }
    }

    /**
     * <p>
     * An event handler that process the contact push to the provider.
     * </p>
     *
     * @author Ajay.
     * @version 1.0.
     */
    private static class ContactPushHandler implements StreamListener<byte[]> {

        private final ContactSyncService contactSyncService;
        private final ContactSyncManager contactSyncManager;

        private ContactPushHandler() {
            contactSyncService = ContactSyncService.getInstance();
            contactSyncManager = ContactSyncManager.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @param streamMessage Represents the {@link StreamMessage}
         */
        @Override
        public void listen(final StreamMessage<byte[]> streamMessage) {
            try {
                final JsonObject jsonObject = Json.object(streamMessage.data());

                push(jsonObject.getLong(ContactFields.ORG_ID), jsonObject.getLong(ContactFields.USER_ID));
            } catch (final Exception exception) {
                throw ErrorCode.get(CoreErrorCodes.DESERIALIZATION_FAILED, exception);
            }
        }

        /**
         * <p>
         * Pushes contacts to the provider
         * </p>
         *
         * @param orgId  Represents an organization's id.
         * @param userId Represents a user's id.
         */
        private void push(final Long orgId, final Long userId) {
            final Optional<ContactSyncSettings> contactSyncSettings = contactSyncManager.getSettings(orgId, userId);

            if (contactSyncSettings.isPresent()) {
                final ContactSyncSettings settings = contactSyncSettings.get();

                try {
                    contactSyncService.pushContactsToProvider(settings);
                } catch (final Exception exception) {
                    LOGGER.debug("Contact push action failed. Sync ID {}", settings.id());
                }
            }
        }
    }
}