package com.tlc.crm.zapier.internal.async;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.consumer.BackgroundConsumer;
import com.tlc.queue.kafka.KafkaConsumerConfig;
import com.tlc.queue.service.provider.QueueServiceProvider;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import com.tlc.crm.zapier.queue.ZapierQueueTopics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * A service that is responsible to trigger and control the zapier trigger consumer actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class ZapTriggerEventController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZapTriggerEventController.class);

    private final SchedulerService schedulerService;

    private Long schedulerId;
    private TriggerTask triggerTask;
    private Executor triggerExecutor;

    public ZapTriggerEventController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            final String eventHandlerPoolWithSize = configurations.getConfig("event.handler.pool");
            final String[] poolNameAndSize = eventHandlerPoolWithSize.split(":");
            this.triggerExecutor = ExecutorsHandler.createTaskExecutor(Integer.parseInt(poolNameAndSize[1]), poolNameAndSize[0], false);
            this.triggerTask = new TriggerTask(configurations.getNestedConfigs("event.consumer"), new TriggerTaskController("zap-event-trigger-processor"), triggerExecutor);
            this.schedulerId = schedulerService.schedule("zap-event-trigger-pool", triggerTask);

            LOGGER.info("Zapier: Zapier trigger event consumer service started, Scheduler ID: {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(triggerTask) && triggerTask.isRunning()) {
                triggerTask.stopTask();
            }
            LOGGER.info("Zapier: Zapier trigger event consumer service stopped, Scheduler ID: {}", schedulerId);
        }

        if (Objects.nonNull(triggerExecutor)) {
            triggerExecutor.shutdown();
        }
    }

    /**
     * A task which is responsible to do the zapier trigger actions.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class TriggerTask extends Task {

        private static final String CONSUMER_GROUP = "tlc-crm-zapier-trigger-request-consumer";

        private final BackgroundConsumer eventConsumer;

        private TriggerTask(final Configurations configurations, final TaskController controller, final Executor triggerExecutor) {
            super(controller);
            final int maxRecord = configurations.optInt("max.records", NumericalConstant.TEN);
            final int poolDuration = configurations.optInt("poll.duration", NumericalConstant.FIVE_THOUSAND);
            final int maxQueueSize = configurations.optInt("max.queue.size", NumericalConstant.TEN);

            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(CONSUMER_GROUP, false,
                    List.of(ZapierQueueTopics.getEventInitiatorTopic(QueueModel.Type.KAFKA)), new ZapTriggerEventProcessor(triggerExecutor),
                    new ConsumerConfig.PollConfig(maxRecord, poolDuration, maxQueueSize), false);
            this.eventConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController Which controls the trigger task.
         */
        @Override
        public void execute(final TaskController taskController) {
            eventConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            eventConsumer.stop();
            return true;
        }
    }

    /**
     * A controller which controls trigger task of zapier.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class TriggerTaskController extends TaskController {

        private TriggerTaskController(final String scheduleName) {
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
