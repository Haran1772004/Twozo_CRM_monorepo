package com.tlc.crm.core.features.internal.dataimport.async.consumer;

import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.consumer.BackgroundConsumer;

import com.tlc.queue.kafka.KafkaConsumerConfig;
import com.tlc.queue.service.provider.QueueServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import com.tlc.crm.core.features.internal.dataimport.async.req.RevertRequestProcessor;

import java.util.List;

/**
 * A task which is responsible to do the revert action.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
class RevertTask extends Task {

    private static final String REQUEST_CONSUMER_NAME = "tlc-crm-core-data-revert-request-consumer";

    private final BackgroundConsumer dataRevertConsumer;

    RevertTask(final TaskController controller) {
        super(controller);
        final ConsumerConfig consumerConfig = new KafkaConsumerConfig(REQUEST_CONSUMER_NAME, false,
                List.of(CoreQueueTopics.getDataRevertTopic(QueueModel.Type.KAFKA)), new RevertRequestProcessor(),
                new ConsumerConfig.PollConfig(10, 1000, 10), false);
        this.dataRevertConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean stopTask() {
        dataRevertConsumer.stop();
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @param taskController Which controls the revert task.
     */
    @Override
    public void execute(final TaskController taskController) {
        dataRevertConsumer.run();
    }

    /**
     * A controller which controls the data revert task {@link RevertTask}.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    static class RevertTaskController extends TaskController {

        RevertTaskController(final String scheduleName) {
            super(scheduleName);
        }

        /**
         * {@inheritDoc}
         *
         * @return The value of next interval.
         */
        @Override
        public long getNextInterval() {
            return -1;
        }
    }
}