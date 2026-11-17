package com.tlc.file.client.internal.async;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.file.common.queue.AttachmentQueueTopics;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.consumer.BackgroundConsumer;
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
 * A service that is responsible to trigger and control the attachment delete action.
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AttachmentDeletionController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentDeletionController.class);

    private final SchedulerService schedulerService;

    private Task task;
    private Long schedulerId;

    public AttachmentDeletionController() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the input.
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.task = new AttachmentDeletionTask(new AttachmentDeletionTaskController("attachment-deletion-request-handler"));
            this.schedulerId = schedulerService.schedule("attachment-deletion-handler-pool", task);

            LOGGER.info("File service: Attachment delete action consumer service started, Scheduler ID : {}", schedulerId);
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
            LOGGER.info("File service: Attachment delete action consumer service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible to delete the attachment.
     * </p>
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class AttachmentDeletionTask extends Task {

        private static final String CONSUMER_GROUP = "tlc-storage-client-attachment-deletion-request-consumer";
        private final BackgroundConsumer requestConsumer;

        private AttachmentDeletionTask(final TaskController controller) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(CONSUMER_GROUP, false,
                    List.of(AttachmentQueueTopics.getAttachmentRemovalTopic(QueueModel.Type.KAFKA)),
                    new AttachmentDeleteActionConsumer(), new ConsumerConfig.PollConfig(10, 10000, 10));

            requestConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            requestConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            requestConsumer.stop();
            return true;
        }
    }

    /**
     * <p>
     * A controller which controls the {@link AttachmentDeletionTaskController}.
     * </p>
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class AttachmentDeletionTaskController extends TaskController {

        private AttachmentDeletionTaskController(final String schedulerName) {
            super(schedulerName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getNextInterval() {
            return -1;
        }
    }
}
