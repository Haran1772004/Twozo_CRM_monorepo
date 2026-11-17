package com.tlc.cdc.core.consumer;

import com.tlc.cdc.core.manager.CdcConnectorManager;
import com.tlc.cdc.core.models.ConnectorConfig;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.consumer.BackgroundConsumer;
import com.tlc.queue.kafka.KafkaConsumerConfig;
import com.tlc.queue.kafka.KafkaTopic;
import com.tlc.queue.service.provider.QueueServiceProvider;

import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Controller responsible for managing the lifecycle of the CDC (Change Data Capture) consumer.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class CdcConsumerController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdcConsumerController.class);

    private final SchedulerService schedulerService;

    private Long schedulerId;
    private CdcTask cdcTask;

    public CdcConsumerController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.cdcTask = new CdcTask(new CdcTaskController("cdc-processor"));
            this.schedulerId = schedulerService.schedule("cdc-pool", cdcTask);

            LOGGER.info("CDC: CDC consumer service started, Scheduler ID: {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(cdcTask) && cdcTask.isRunning()) {
                cdcTask.stopTask();
            }
            LOGGER.info("CDC: CDC consumer service stopped, Scheduler ID: {}", schedulerId);
        }
    }

    /**
     * A task which is responsible to do the CDC actions.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class CdcTask extends Task {

        private static final String CONSUMER_GROUP = "cdc-request-consumer";

        private final BackgroundConsumer consumer;

        private CdcTask(final TaskController controller) {
            super(controller);

            final Collection<ConnectorConfig> connectorsGroup = CdcConnectorManager.getInstance().getAll(ConnectorConfig.Type.SOURCE);
            final String prefix = "crm.public";
            final Collection<SubscriptionTopic> topics = new HashSet<>();

            for (final ConnectorConfig connectorConfig : connectorsGroup) {
                for (final String topic : connectorConfig.tables()) {
                    topics.add(new KafkaTopic(String.join(".", prefix, topic.toLowerCase().trim())));
                }
            }

            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(CONSUMER_GROUP, false, topics, new CdcRequestProcessor(),
                    new ConsumerConfig.PollConfig(10, 1000, 10));
            this.consumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController Which controls the CDC task.
         */
        @Override
        public void execute(final TaskController taskController) {
            consumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            consumer.stop();
            return true;
        }
    }

    /**
     * A controller which controls CDC event.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class CdcTaskController extends TaskController {

        private CdcTaskController(final String scheduleName) {
            super(scheduleName);
        }

        /**
         * {@inheritDoc}
         *
         * @return the next interval time for the task is executed in milliseconds.
         */
        @Override
        public long getNextInterval() {
            return 0;
        }
    }
}
