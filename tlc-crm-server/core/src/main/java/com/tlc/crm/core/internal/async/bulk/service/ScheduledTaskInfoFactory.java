package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.internal.async.bulk.processor.AsyncActionProcessor;
import com.tlc.crm.core.internal.field.BulkActionFields;
import com.tlc.queue.QueueModel;
import com.tlc.queue.SubscriptionTopicHolder;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.consumer.BackgroundConsumer;
import com.tlc.queue.kafka.KafkaConsumerConfig;
import com.tlc.queue.service.provider.QueueServiceProvider;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * A factory class for creating and scheduling asynchronous tasks based on the provided {@link Configurations}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
final class ScheduledTaskInfoFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTaskInfoFactory.class);
    private static final SubscriptionTopicHolder SUBSCRIPTION_TOPIC_HOLDER = SubscriptionTopicHolder.getInstance();
    private static final String DEFAULT_MAX_SIZE = "10";
    private static final String DEFAULT_POLL_DURATION = "5000";

    private ScheduledTaskInfoFactory() {
    }

    /**
     * <p>
     * Schedules an async task based on the provided {@link Configurations} and {@link SchedulerService}.
     * </p>
     *
     * @param asyncBulkActionName Represents the async bulk action name
     * @param configurations      Represents the {@link Configurations}
     * @param schedulerService    Represents the {@link SchedulerService}
     * @return The collection of {@link ScheduledTaskInfo}
     */
    static Collection<ScheduledTaskInfo> schedule(final String asyncBulkActionName, final Configurations configurations, final SchedulerService schedulerService) {
        final Collection<ScheduledTaskInfo> scheduledTaskInfos = new ArrayList<>();
        final Collection<Map<String, String>> groupedConfigs = configurations.groupedConfigArray(asyncBulkActionName);

        for (final Map<String, String> eventConfigs : groupedConfigs) {
            final Action action = Action.valueOf(eventConfigs.get(BulkActionFields.EVENT).toUpperCase());
            final int maxRecord = Integer.parseInt(eventConfigs.getOrDefault(BulkActionFields.CONSUMER_MAX_RECORDS, DEFAULT_MAX_SIZE));
            final int pollDuration = Integer.parseInt(eventConfigs.getOrDefault(BulkActionFields.CONSUMER_POLL_DURATION, DEFAULT_POLL_DURATION));
            final int maxQueueSize = Integer.parseInt(eventConfigs.getOrDefault(BulkActionFields.CONSUMER_MAX_QUEUE_SIZE, DEFAULT_MAX_SIZE));
            final String consumerName = eventConfigs.get(BulkActionFields.CONSUMER_NAME);
            final String threadPool = eventConfigs.get(BulkActionFields.THREAD_POOL);
            final String taskController = eventConfigs.get(BulkActionFields.TASK_CONTROLLER);
            final SubscriptionTopic topic = SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(QueueModel.Type.KAFKA, eventConfigs.get(BulkActionFields.TOPIC).toUpperCase());
            final Optional<AsyncActionProcessor<?>> asyncActionProcessor = findAsyncActionProcessor(asyncBulkActionName, action);

            if (asyncActionProcessor.isPresent()) {
                final KafkaConsumerConfig kafkaConsumerConfig = new KafkaConsumerConfig(consumerName, false, List.of(topic), asyncActionProcessor.get(),
                        new ConsumerConfig.PollConfig(maxRecord, pollDuration, maxQueueSize));

                switch (asyncBulkActionName) {
                    case AsyncBulkActionName.CONTACT, AsyncBulkActionName.COMPANY, AsyncBulkActionName.DEAL,
                         AsyncBulkActionName.PRODUCT, AsyncBulkActionName.ACTIVITY,
                         AsyncBulkActionName.CALL_LOG, AsyncBulkActionName.FAVOURITE_CONTACT,
                         AsyncBulkActionName.MARKETING_LIST, AsyncBulkActionName.EMAIL -> {
                        switch (action) {
                            case CREATE, UPDATE, DELETE -> {
                                final AsyncBulkActionTask asyncBulkActionTask = new AsyncBulkActionTask(
                                        taskController, kafkaConsumerConfig);
                                final Long scheduledTaskId = schedulerService.schedule(threadPool, asyncBulkActionTask);

                                scheduledTaskInfos.add(new ScheduledTaskInfo(scheduledTaskId, asyncBulkActionTask));
                                LOGGER.info("Bulk Action: Started with Action Name: {}, Event Name: {}, Scheduler ID: {}", asyncBulkActionName, action, scheduledTaskId);
                            }
                        }
                    }
                }
            }
        }
        return scheduledTaskInfos;
    }

    /**
     * <p>
     * Finds and returns an appropriate {@link AsyncActionProcessor} based on the provided name and action
     * </p>
     *
     * @param name   Represents an async bulk action name
     * @param action Represents an {@link Action}
     * @return An {@link Optional} containing the appropriate {@link AsyncActionProcessor} if a match
     * is found, or an empty {@link Optional} if no match is found
     */
    private static Optional<AsyncActionProcessor<?>> findAsyncActionProcessor(final String name, final Action action) {
        return Optional.ofNullable(switch (name) {
            case AsyncBulkActionName.CONTACT -> switch (action) {
                case UPDATE -> new ContactAsyncUpdateProcessor();
                case DELETE -> new ContactAsyncDeleteProcessor();
                default -> null;
            };
            case AsyncBulkActionName.COMPANY -> switch (action) {
                case UPDATE -> new CompanyAsyncUpdateProcessor();
                case DELETE -> new CompanyAsyncDeleteProcessor();
                default -> null;
            };
            case AsyncBulkActionName.DEAL -> switch (action) {
                case UPDATE -> new DealAsyncUpdateProcessor();
                case DELETE -> new DealAsyncDeleteProcessor();
                default -> null;
            };
            case AsyncBulkActionName.PRODUCT -> switch (action) {
                case UPDATE -> new ProductAsyncUpdateProcessor();
                case DELETE -> new ProductAsyncDeleteProcessor();
                default -> null;
            };
            case AsyncBulkActionName.ACTIVITY -> switch (action) {
                case UPDATE -> new ActivityAsyncUpdateProcessor();
                case DELETE -> new ActivityAsyncDeleteProcessor();
                default -> null;
            };
            case AsyncBulkActionName.CALL_LOG -> switch (action) {
                case DELETE -> new CallLogAsyncDeleteProcessor();
                default -> null;
            };
            case AsyncBulkActionName.EMAIL -> switch (action) {
                case UPDATE -> new EntityThreadAsyncUpdateProcessor();
                case DELETE -> new EntityThreadAsyncDeleteProcessor();
                default -> null;
            };
            case AsyncBulkActionName.FAVOURITE_CONTACT -> switch (action) {
                case CREATE -> new FavouriteContactAsyncCreateProcessor();
                default -> null;
            };
            case AsyncBulkActionName.MARKETING_LIST -> switch (action) {
                case UPDATE -> new MarketingListAsyncUpdateProcessor();
                default -> null;
            };
            default -> null;
        });
    }

    /**
     * <p>
     * A task which is responsible for async bulk actions
     * </p>
     *
     * @author Kishore J
     * @version 1.0
     */
    private static class AsyncBulkActionTask extends Task {
        private final BackgroundConsumer backgroundConsumer;

        private AsyncBulkActionTask(final String controllerName, final KafkaConsumerConfig kafkaConsumerConfig) {
            super(new AsyncBulkActionTaskController(controllerName));

            this.backgroundConsumer = QueueServiceProvider.get().createBackgroundConsumer(kafkaConsumerConfig);
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController Represents the {@link TaskController}
         */
        @Override
        public void execute(final TaskController taskController) {
            backgroundConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            backgroundConsumer.stop();
            return true;
        }
    }

    /**
     * <p>
     * A controller which controls async bulk actions
     * </p>
     *
     * @author Kishore J
     * @version 1.0
     */
    private static class AsyncBulkActionTaskController extends TaskController {

        public AsyncBulkActionTaskController(final String scheduleName) {
            super(scheduleName);
        }

        /**
         * {@inheritDoc}
         *
         * @return The interval time
         */
        @Override
        public long getNextInterval() {
            return 0;
        }
    }

    /**
     * <p>
     * Holds constant string names for all supported async bulk action entities
     * </p>
     *
     * @author Kishore J
     * @version 1.0
     */
    private static class AsyncBulkActionName {

        private AsyncBulkActionName() {
        }

        private static final String CONTACT = "contact";
        private static final String COMPANY = "company";
        private static final String DEAL = "deal";
        private static final String PRODUCT = "product";
        private static final String ACTIVITY = "activity";
        private static final String CALL_LOG = "callLog";
        private static final String FAVOURITE_CONTACT = "favouriteContact";
        private static final String MARKETING_LIST = "marketingList";
        private static final String EMAIL = "email";
    }
}