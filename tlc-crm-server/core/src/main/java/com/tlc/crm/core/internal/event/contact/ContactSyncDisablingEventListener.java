package com.tlc.crm.core.internal.event.contact;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.internal.contact.sync.ContactSyncService;

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

/**
 * <p>
 * A service that is responsible to trigger and control contact sync stop action
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class ContactSyncDisablingEventListener implements Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContactSyncDisablingEventListener.class);

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    public ContactSyncDisablingEventListener() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the input of stopping contact sync
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.task = new ContactStopSyncEventTask(new ContactStopSyncTaskController());
            this.schedulerId = schedulerService.schedule("contact-stop-sync-event-pool", task);

            LOGGER.debug("Contact stop syncing service started, Scheduler ID : {}", schedulerId);
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
            LOGGER.debug("Contact stop sync service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible to stopping contact sync
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class ContactStopSyncEventTask extends Task {

        private static final String REQUEST_CONSUMER_NAME = "tlc-crm-core-contact-stop-sync-event-consumer";
        private final BackgroundConsumer contactSyncConsumer;

        private ContactStopSyncEventTask(final TaskController controller) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(REQUEST_CONSUMER_NAME, false,
                    List.of(CoreQueueTopics.getContactStopSyncEventTopic(QueueModel.Type.KAFKA)), new ContactStopSyncEventHandler(),
                    new ConsumerConfig.PollConfig(10, 1000, 10));
            contactSyncConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            contactSyncConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            contactSyncConsumer.stop();
            return true;
        }
    }

    /**
     * <p>
     * A controller which controls the {@link ContactStopSyncTaskController}.
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class ContactStopSyncTaskController extends TaskController {

        private ContactStopSyncTaskController() {
            super("contact-stop-sync-event");
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
     * An event handler that process the stopping contact sync requests and triggers the contact stop sync action based on the request parameters
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class ContactStopSyncEventHandler implements StreamListener<byte[]>, SecureUserTaskProcessor {

        private static final Logger LOGGER = LoggerFactory.getLogger(ContactStopSyncEventHandler.class);
        private final ContactSyncService contactSyncService;

        private ContactStopSyncEventHandler() {
            contactSyncService = ContactSyncService.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @param streamMessage Represents the {@link StreamMessage}
         */
        @Override
        public void listen(final StreamMessage<byte[]> streamMessage) {
            final JsonObject jsonObject = Json.object(streamMessage.data());
            final long orgId = jsonObject.getLong(ContactFields.ORG_ID);
            final long userId = jsonObject.getLong(ContactFields.USER_ID);

            secureProcess(orgId, userId, () -> {
                try {
                    contactSyncService.stopSync(orgId, userId);
                    LOGGER.debug("Contact sync stopped successfully for Org ID : {}, User ID : {}", orgId, userId);
                } catch (Exception e) {
                    LOGGER.debug("Contact sync stop action failed for Org ID : {}, User ID : {}", orgId, userId);
                }
            });
        }
    }
}