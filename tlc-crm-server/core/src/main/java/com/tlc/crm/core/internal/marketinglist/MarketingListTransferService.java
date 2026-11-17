package com.tlc.crm.core.internal.marketinglist;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.core.api.marketinglist.manager.MarketingListContactManager;
import com.tlc.crm.core.field.marketinglist.MarketingListFields;

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
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * A service that is responsible to consume and process the marketing list transfer events
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class MarketingListTransferService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketingListTransferService.class);
    private static final String THREAD_POOL_NAME = "marketing-list-transfer-pool";
    private static final String TASK_SCHEDULER_NAME = "marketing-list-transfer-scheduler";
    private static final String REQUEST_CONSUMER_NAME = "tlc-crm-core-marketing-list-transfer-consumer";
    private static final String CONSUMER_MAX_RECORDS_KEY = "marketing-list.transfer.consumer.max.records";
    private static final String CONSUMER_POLL_DURATION_KEY = "marketing-list.transfer.consumer.poll.duration";
    private static final String CONSUMER_MAX_QUEUE_SIZE_KEY = "marketing-list.transfer.consumer.max.queue.size";

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    private MarketingListTransferService() {
        schedulerService = SchedulerServiceProvider.get();
    }

    private static final class InstanceHolder {
        private static final MarketingListTransferService MARKETING_LIST_TRANSFER_SERVICE = new MarketingListTransferService();
    }

    /**
     * <p>
     * Gets the instance of {@link MarketingListTransferService}
     * </p>
     *
     * @return the instance of {@link MarketingListTransferService}
     */
    public static MarketingListTransferService getInstance() {
        return InstanceHolder.MARKETING_LIST_TRANSFER_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the input configurations
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.task = new MarketingListTransferTask(configurations, new MarketingListTransferTaskController());
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, task);

            LOGGER.info("Marketing list: Transfer service started, Scheduler ID : {}", schedulerId);
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
            LOGGER.info("Marketing list: Transfer service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible to process marketing list transfer events
     * </p>
     *
     * @author Maharaja S
     * @version 1.0
     */
    private static class MarketingListTransferTask extends Task {

        private final BackgroundConsumer marketingListTransferConsumer;

        private MarketingListTransferTask(final Configurations configurations, final TaskController controller) {
            super(controller);
            final int maxRecords = configurations.optInt(CONSUMER_MAX_RECORDS_KEY, NumericalConstant.TEN);
            final int pollDuration = configurations.optInt(CONSUMER_POLL_DURATION_KEY, NumericalConstant.THOUSAND);
            final int maxQueueSize = configurations.optInt(CONSUMER_MAX_QUEUE_SIZE_KEY, NumericalConstant.TEN);

            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(REQUEST_CONSUMER_NAME, false,
                    List.of(CoreQueueTopics.getMarketingListContactTransferEventTopic(QueueModel.Type.KAFKA)),
                    new MarketingListTransferEventListener(), new ConsumerConfig.PollConfig(maxRecords, pollDuration, maxQueueSize), false);
            marketingListTransferConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            marketingListTransferConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            marketingListTransferConsumer.stop();
            return true;
        }
    }

    /**
     * <p>
     * An event handler that processes the marketing list transfer requests
     * </p>
     *
     * @author Maharaja S
     * @version 1.0
     */
    private static final class MarketingListTransferEventListener implements StreamListener<byte[]>, SecureUserTaskProcessor {

        private static final Logger LOGGER = LoggerFactory.getLogger(MarketingListTransferEventListener.class);

        private final MarketingListContactManager marketingListContactManager;

        private MarketingListTransferEventListener() {
            marketingListContactManager = MarketingListContactManager.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @param streamMessage Represents the {@link StreamMessage}
         */
        @Override
        public void listen(final StreamMessage<byte[]> streamMessage) {
            try {
                LOGGER.info("Marketing List: Contact transfer event received");
                final JsonObject requestParameters = Json.object(streamMessage.data());
                final long orgId = requestParameters.getLong(MarketingListFields.ORG_ID);
                final long userId = requestParameters.getLong(MarketingListFields.USER_ID);

                secureProcess(orgId, userId, () -> {
                    final Long sourceMarketingListId = requestParameters.getLong(MarketingListFields.SOURCE_MARKETING_LIST_ID);
                    final Long targetMarketingListId = requestParameters.getLong(MarketingListFields.TARGET_MARKETING_LIST_ID);

                    marketingListContactManager.transferContactsAndDelete(orgId, userId, sourceMarketingListId, targetMarketingListId);
                    LOGGER.info("Marketing List: Contact transfer event completed successfully. Source marketing list Id:{} Target marketing list Id:{}", sourceMarketingListId, targetMarketingListId);
                });
            } catch (Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, "Marketing List: Contact transfer event failed: Reason:{}", throwable.getMessage());
            } finally {
                streamMessage.clean();
            }
        }
    }

    /**
     * <p>
     * A controller which controls the {@link MarketingListTransferTask}
     * </p>
     *
     * @author Maharaja S
     * @version 1.0
     */
    private static final class MarketingListTransferTaskController extends TaskController {

        private MarketingListTransferTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getNextInterval() {
            return TimeUnit.SECONDS.toMillis(5);
        }
    }
}