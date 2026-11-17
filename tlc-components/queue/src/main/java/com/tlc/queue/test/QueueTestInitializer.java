package com.tlc.queue.test;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.RandomIdGenerator;
import com.tlc.queue.QueueModel;
import com.tlc.queue.SubscriptionTopicHolder;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.internal.resource.QueueErrorCodes;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.provider.QueueServiceProvider;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Initializer to initialize the basic test cases for queue
 *
 * @author sundar
 * @version 1.0
 */
public class QueueTestInitializer implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueTestInitializer.class);
    private static final String MANUAL_TASK_PROCESSOR_NAME = "test-manual-processor";
    private static final String MANUAL_COMMIT_CONSUMER_SCHEDULER_NAME = "test-manual-task-pool";
    private static final String AUTO_COMMIT_CONSUMER_SCHEDULER_NAME = "test-auto-task-pool";
    private static final String AUTO_COMMIT_TOPIC_NAME = "TEST_AUTO_COMMIT";
    private static final String MANUAL_COMMIT_TOPIC_NAME = "TEST_MANUAL_COMMIT";
    private static final String MANUAL_TEST_DATA = "Manual test data";
    private static final String AUTO_TEST_DATA = "Auto test data";
    private static final String SLEEP_TIME_KEY = "sleepTime";
    private static final String MESSAGE_KEY = "message";
    private static final String ID_KEY = "id";

    private final SchedulerService schedulerService;
    private final SubscriptionTopicHolder subscriptionTopicHolder;
    private final RandomIdGenerator randomIdGenerator;

    private Long manualTaskId;
    private Task manualTask;
    private Long autoTaskId;
    private Task autoTask;
    private Executor manualExecutor;

    public QueueTestInitializer() {
        this.schedulerService = SchedulerServiceProvider.get();
        this.subscriptionTopicHolder = SubscriptionTopicHolder.getInstance();
        this.randomIdGenerator = RandomIdGenerator.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Which contains the queue configuration
     */
    @Override
    public void start(final Configurations configurations) {

        try {
            this.manualExecutor = ExecutorsHandler.createTaskExecutor(3, MANUAL_TASK_PROCESSOR_NAME, false);
            this.manualTask = new TestManualCommitTask("Manual consumer 1", new TestManualCommitTask.TestManualCommitTaskController("test-manual-task-processor"), manualExecutor);
            this.autoTask = new TestAutoCommitTask(new TestAutoCommitTask.TestAutoCommitTaskController("test-auto-task-processor"));
            this.manualTaskId = schedulerService.schedule(MANUAL_COMMIT_CONSUMER_SCHEDULER_NAME, manualTask);
            this.autoTaskId = schedulerService.schedule(AUTO_COMMIT_CONSUMER_SCHEDULER_NAME, autoTask);
        } catch (final ErrorCode errorCode) {

            if (errorCode.getCode() == 16777221) {
                LOGGER.warn("Queue test: Required scheduler is not found, Schedulers: {}", "\"test-auto-task-pool\", \"test-manual-task-pool\"");
            }
            throw errorCode;
        }
        testAutoCommit();
        testManualCommit();
        testManualCommitInShuffle();
    }

    /**
     * Tests the manual commit process.
     * <ol>
     *     <li>
     *         Manual test data 1 and 2 sleep for 10 seconds.
     *     </li>
     *     <li>
     *         Manual test data 3 sleeps for 5 seconds, causing its offset to be processed before 1 and 2. However, it is not allowed to commit.
     *     </li>
     *     <li>
     *         Manual test data 4 and 5 sleep for 15 seconds.
     *     </li>
     *     <li>
     *         Before 4 and 5 complete, 1 and 2 will also finish. At this point, polling occurs again, and offsets for 1, 2, and 3 are committed successfully. Subsequently, 4 and 5 will be committed in order.
     *     </li>
     * </ol>
     */
    private void testManualCommitInShuffle() {
        final SubscriptionTopic manualCommitTopic = getTopic(MANUAL_COMMIT_TOPIC_NAME);
        final KafkaProducerConfig manualCommitProducerConfig = new KafkaProducerConfig(manualCommitTopic);

        try (final Producer<byte[]> manualCommitProducer = QueueServiceProvider.get().createProducer(manualCommitProducerConfig)) {
            for (int i = 1; i <= 5; i++) {

                final long sleepTime = switch (i) {
                    case 1, 2 -> 10000;
                    case 3 -> 5000;
                    case 4, 5 -> 15000;
                    default -> 0;
                };

                final String uniqueId = randomIdGenerator.generateUniqueId();
                final JsonObject jsonObject = buildMessageJson(uniqueId, String.format("%s: %d", MANUAL_TEST_DATA, i))
                        .put(SLEEP_TIME_KEY, (i % 2 == 0 ? (sleepTime - 5000) : sleepTime) + (i * 300));

                manualCommitProducer.send(new StreamMessage<>(uniqueId, manualCommitTopic, jsonObject.getBytes()));
            }
        } catch (Throwable throwable) {
            LOGGER.error("Queue test: Manual commit data send failed, Message: {}", throwable.getMessage(), throwable);
        }
    }

    /**
     * Tests manual commit by gradually increasing the sleep time for messages 1 to 5, ensuring sequential processing,
     * which allows the consumer to commit from the lowest offset to highest.
     */
    private void testManualCommit() {
        final SubscriptionTopic manualCommitTopic = getTopic(MANUAL_COMMIT_TOPIC_NAME);
        final KafkaProducerConfig manualCommitProducerConfig = new KafkaProducerConfig(manualCommitTopic);

        try (final Producer<byte[]> manualCommitProducer = QueueServiceProvider.get().createProducer(manualCommitProducerConfig)) {
            for (int i = 1; i <= 5; i++) {
                LOGGER.info("Queue test: Going to send the manual commit test data: {}", i);
                final String uniqueId = randomIdGenerator.generateUniqueId();
                final JsonObject jsonObject = buildMessageJson(uniqueId, String.format("%s: %d", MANUAL_TEST_DATA, i))
                        .put(SLEEP_TIME_KEY, 10000 + i * 100);

                manualCommitProducer.send(new StreamMessage<>(uniqueId, manualCommitTopic, jsonObject.getBytes()));
            }
        } catch (Throwable throwable) {
            LOGGER.error("Queue test: Manual commit data send failed, Message: {}", throwable.getMessage(), throwable);
        }
    }

    /**
     * Tests the auto commit behaviour by sending and receiving test message
     */
    private void testAutoCommit() {
        final SubscriptionTopic autoCommitTopic = getTopic(AUTO_COMMIT_TOPIC_NAME);
        final KafkaProducerConfig autoCommitProducerConfig = new KafkaProducerConfig(autoCommitTopic);
        final String uniqueId = randomIdGenerator.generateUniqueId();
        final JsonObject jsonObject = buildMessageJson(uniqueId, AUTO_TEST_DATA);

        try (final Producer<byte[]> autoCommitProducer = QueueServiceProvider.get().createProducer(autoCommitProducerConfig)) {
            autoCommitProducer.send(new StreamMessage<>(uniqueId, autoCommitTopic, jsonObject.getBytes()));
        } catch (Throwable throwable) {
            LOGGER.error("Queue test: Auto commit data send failed, Message: {}", throwable.getMessage(), throwable);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

        if (Objects.nonNull(manualTask)) {
            manualTask.stopTask();
            schedulerService.removeTask(manualTaskId);
        }

        if (Objects.nonNull(autoTask)) {
            autoTask.stopTask();
            schedulerService.removeTask(autoTaskId);
        }

        if (Objects.nonNull(manualExecutor)) {
            manualExecutor.shutdown();
        }
    }

    /**
     * Gets the {@link SubscriptionTopic} of given topic name
     *
     * @param topicName Represents the topic name
     * @return {@link SubscriptionTopic} of given topic name
     */
    private SubscriptionTopic getTopic(final String topicName) {

        try {
            return subscriptionTopicHolder.getSubscriptionTopic(QueueModel.Type.KAFKA, topicName);
        } catch (ErrorCode errorCode) {

            if (Objects.equals(ErrorCode.get(QueueErrorCodes.TOPIC_NOT_FOUND), errorCode)) {
                LOGGER.warn("Queue test: Required topic is not configured, Topic: {}", topicName);
            }
            throw errorCode;
        }
    }

    /**
     * Builds the message JSON
     *
     * @param uniqueId Represents the unique ID
     * @param message  Represents the message
     * @return {@link JsonObject}
     */
    private JsonObject buildMessageJson(final String uniqueId, final String message) {
        return Json.object().put(MESSAGE_KEY, message).put(ID_KEY, uniqueId);
    }
}