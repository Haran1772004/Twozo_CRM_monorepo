package com.tlc.queue.internal.kafka;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.internal.AbstractQueueService;

import com.tlc.queue.internal.TopicConfig;
import com.tlc.queue.internal.cache.consumer.ConsumerGroupService;
import com.tlc.queue.internal.cache.topic.TopicService;
import com.tlc.queue.internal.kafka.consumer.AutoCommittableConsumer;
import com.tlc.queue.internal.kafka.consumer.ManualCommittableConsumer;
import com.tlc.queue.internal.manager.offset.AsyncProcessedOffsetManager;
import com.tlc.queue.internal.resource.QueueErrorCodes;
import com.tlc.queue.kafka.KafkaConsumerConfig;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.consumer.Consumer;
import com.tlc.queue.kafka.KafkaTopic;
import com.tlc.queue.producer.Producer;

import lombok.Getter;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * @author Abishek
 * @version 1.0
 */
public abstract class KafkaService extends AbstractQueueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaService.class);
    private static final String KAFKA_CONFIG = "kafka";

    @Getter
    protected final Map<String, Object> clientInput;
    private AdminClient adminClient;

    protected KafkaService() {
        this.clientInput = new HashMap<>();
    }

    /**
     * Starts the kafka service
     *
     * @param configurations Which contains the queue configurations
     */
    @Override
    public void start(final Configurations configurations) {
        final int bufferSize = Integer.parseInt(configurations.getConfig("consumerBufferBytes"));
        final int heartbeatInterval = Integer.parseInt(configurations.getConfig("heartbeatInterval"));
        final int sessionTimeout = Integer.parseInt(configurations.getConfig("sessionTimeout"));
        final int maxPollInterval = Integer.parseInt(configurations.getConfig("maxPollInterval"));

        try {
            clientInput.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, Class.forName(StringSerializer.class.getName()));
            clientInput.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, Class.forName(ByteArraySerializer.class.getName()));
            clientInput.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, Class.forName(StringDeserializer.class.getName()));
            clientInput.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, Class.forName(ByteArrayDeserializer.class.getName()));
        } catch (Exception e) {
            throw ErrorCode.get(ErrorCodeProvider.UNKNOWN, e);
        }
        clientInput.put(org.apache.kafka.clients.consumer.ConsumerConfig.RECEIVE_BUFFER_CONFIG, bufferSize);
        clientInput.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollInterval);
        clientInput.put(org.apache.kafka.clients.consumer.ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeout);
        clientInput.put(org.apache.kafka.clients.consumer.ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, heartbeatInterval);

        loadTopicConfigurations(configurations);
        preProcess(configurations.getNestedConfigs(KAFKA_CONFIG));
        LOGGER.info("Creating admin client");
        adminClient = AdminClient.create(clientInput);

        for (final TopicConfig topicConfig : getTopicConfigs()) {
            createTopicIfNotExist(topicConfig);
        }
    }

    /**
     * Stops the kafka service
     */
    @Override
    public void stop() {
        AsyncProcessedOffsetManager.getInstance().shutdownExecutor();
        TopicService.getInstance().clearCache();
        ConsumerGroupService.getInstance().clearCache();

        if (Objects.nonNull(adminClient)) {
            adminClient.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Producer<byte[]> createProducer(com.tlc.queue.config.ProducerConfig producerConfig) {
        LOGGER.info("Creating kafka producer for topic: {}", producerConfig.getTopic().getName());
        return new com.tlc.queue.internal.kafka.producer.KafkaProducer((KafkaProducerConfig) producerConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Consumer createConsumer(final ConsumerConfig consumerConfig) {
        final KafkaConsumerConfig kafkaConsumerConfig = (KafkaConsumerConfig) consumerConfig;

        LOGGER.info("Creating kafka consumer. Group ID: {}", kafkaConsumerConfig.getGroupId());

        return consumerConfig.isAutoCommitEnabled() ? new AutoCommittableConsumer(kafkaConsumerConfig)
                : new ManualCommittableConsumer(kafkaConsumerConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param topicConfig Represents the name of the topic
     * @return the kafka {@link SubscriptionTopic}
     */
    @Override
    protected SubscriptionTopic createSubscriptionTopic(final String topicConfig) {
        return new KafkaTopic(topicConfig);
    }

    /**
     * Performs preprocessing tasks during service startup
     *
     * @param configurations Which contains the kafka configuration
     */
    protected abstract void preProcess(final Configurations configurations);

    /**
     * Creates a topic if it doesn't already exist
     *
     * @param topicConfig Represents the name of the topic
     */
    @Override
    public void createTopicIfNotExist(final TopicConfig topicConfig) {

        try {
            LOGGER.info("Checking whether the topic - {} exists or not", topicConfig);
            final Set<String> existingTopics = adminClient.listTopics().names().get();
            final String topicName = topicConfig.name();

            if (!existingTopics.contains(topicName)) {
                LOGGER.info("Topic - {} doesn't exist. Creating a new one", topicConfig);

                final NewTopic newTopic = new NewTopic(topicName, topicConfig.partitions(), (short) 1);
                final KafkaFuture<Void> future = adminClient.createTopics(Collections.singleton(newTopic)).all();

                future.get();
                LOGGER.info("Topic {} created successfully.", topicConfig);
            } else {
                LOGGER.info("Topic {} already exists.", topicConfig);
            }
        } catch (InterruptedException | ExecutionException e) {
            LoggerUtil.logError(LOGGER, e, "Topic {} check or creation failed. Message: {}", topicConfig, e.getMessage());
            throw ErrorCode.get(QueueErrorCodes.TOPIC_VALIDATION_OR_CREATION_FAILED);
        }
    }
}