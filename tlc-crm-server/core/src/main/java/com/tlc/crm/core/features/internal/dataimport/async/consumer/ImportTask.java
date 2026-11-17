package com.tlc.crm.core.features.internal.dataimport.async.consumer;

import com.tlc.commons.executor.Executor;
import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.consumer.BackgroundConsumer;

import com.tlc.queue.kafka.KafkaConsumerConfig;

import com.tlc.queue.service.provider.QueueServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import com.tlc.crm.core.features.internal.dataimport.async.req.ImportRequestProcessor;
import java.util.List;

/**
 * A task which is responsible to do the import action.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
class ImportTask extends Task {

    private static final String CONSUMER_GROUP = "tlc-crm-core-data-import-request-consumer";

    private final BackgroundConsumer dataImportRequestConsumer;

    ImportTask(final TaskController controller, final Executor dataImportExecutor) {
        super(controller);
        final ConsumerConfig consumerConfig = new KafkaConsumerConfig(CONSUMER_GROUP, false,
                List.of(CoreQueueTopics.getDataImporterTopic(QueueModel.Type.KAFKA)),
                new ImportRequestProcessor(dataImportExecutor), new ConsumerConfig.PollConfig(5, 10000, 5), false);
        this.dataImportRequestConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean stopTask() {
        dataImportRequestConsumer.stop();
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @param taskController Which controls the import task.
     */
    @Override
    public void execute(final TaskController taskController) {
        dataImportRequestConsumer.run();
    }

    /**
     * A controller which controls the data import task {@link ImportTask}.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    static class ImportTaskController extends TaskController {

        ImportTaskController(final String scheduleName) {
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