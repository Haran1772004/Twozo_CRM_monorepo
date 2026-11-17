package com.tlc.crm.core.attachment.async;

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
 * A service that is responsible to trigger and control the attachment upload listener action.
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class UploadListenerController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadListenerController.class);

    private final SchedulerService schedulerService;

    private Task task;
    private Long schedulerId;

    public UploadListenerController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the input.
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.task = new UploadListenerTask(new UploadListenerTaskController("attachment-upload-request-handler"));
            this.schedulerId = schedulerService.schedule("attachment-upload-handler-pool", task);

            LOGGER.info("File service: Attachment upload action consumer service started, Scheduler ID : {}", schedulerId);
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
            LOGGER.info("File service: Attachment upload action consumer service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible to listen the upload action of the attachment.
     * </p>
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class UploadListenerTask extends Task {

        private static final String CONSUMER_GROUP = "tlc-crm-core-attachment-upload-request-consumer";
        private final BackgroundConsumer requestConsumer;

        private UploadListenerTask(final TaskController controller) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(CONSUMER_GROUP, false,
                    List.of(AttachmentQueueTopics.getAttachmentUploadTopic(QueueModel.Type.KAFKA)),
                    new UploadActionConsumer(), new ConsumerConfig.PollConfig(10, 10000, 10));

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
     * A controller which controls the {@link UploadListenerTask}.
     * </p>
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class UploadListenerTaskController extends TaskController {

        private UploadListenerTaskController(final String schedulerName) {
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
