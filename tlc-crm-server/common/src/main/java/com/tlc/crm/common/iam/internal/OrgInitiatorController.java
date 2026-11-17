package com.tlc.crm.common.iam.internal;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.queue.CommonQueueTopics;
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
 * A service that is responsible to trigger and control the org initiator consumer actions {{@link OrgInitiatorProcessor}}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class OrgInitiatorController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrgInitiatorController.class);
    private static final String REQUEST_CONSUMER_NAME = "tlc-crm-common-org-initiator-request-consumer";
    private static final String THREAD_POOL_NAME = "org-initializer-pool";
    private static final String TASK_SCHEDULER_NAME = "org-initiator-processor";

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    public OrgInitiatorController() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Schedules an instance of OrgInitiatorTask with the specified name and logs the relevant information
     * </p>
     *
     * @param configurations Represents the input of the org initiator
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.task = new OrgInitiatorTask(new OrgInitiatorTaskController());
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, task);
            LOGGER.info("Org initiator service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Stops the Org Initiator consumer service by removing the task associated with the schedulerId.
     * </p>
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(task) && task.isRunning()) {
                task.stopTask();
            }
            LOGGER.info("Org initiator consumer service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible to do the org initialize action
     * </p>
     *
     * @author PandiarajkumarG
     * @version 1.0
     */
    private static class OrgInitiatorTask extends Task {

        private final BackgroundConsumer orgRequestConsumer;

        private OrgInitiatorTask(final TaskController controller) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(REQUEST_CONSUMER_NAME, false,
                    List.of(CommonQueueTopics.getOrgInitiatorTopic(QueueModel.Type.KAFKA)), new OrgInitiatorProcessor(),
                    new ConsumerConfig.PollConfig(10, 1000, 10));
            orgRequestConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            orgRequestConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            orgRequestConsumer.stop();
            return true;
        }
    }

    /**
     * <p>
     * A controller which controls the {@link OrgInitiatorTask}.
     * </p>
     *
     * @author PandiarajkumarG
     * @version 1.0
     */
    private static class OrgInitiatorTaskController extends TaskController {

        private OrgInitiatorTaskController() {
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
}
