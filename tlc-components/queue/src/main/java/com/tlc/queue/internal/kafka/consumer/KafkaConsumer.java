package com.tlc.queue.internal.kafka.consumer;

import com.tlc.commons.executor.context.Context;
import com.tlc.commons.executor.context.ContextHolder;
import com.tlc.commons.executor.context.registry.ContextCodecRegistry;
import com.tlc.commons.executor.context.registry.ContextHolderRegistry;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.Consumer;
import com.tlc.queue.internal.ResourceManager;
import com.tlc.queue.internal.cache.consumer.ConsumerGroupService;
import com.tlc.queue.internal.kafka.KafkaService;
import com.tlc.queue.kafka.KafkaConsumerConfig;
import com.tlc.queue.service.provider.QueueServiceProvider;

import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * A kafka consumer that subscribes the topics produced by the Kafka producer.
 * This consumer is unsuitable for concurrent access
 *
 * @author author Nithin
 * @version 1.0
 */
abstract class KafkaConsumer implements Consumer {

    static final String DATA_KEY = "data";
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumer.class);
    private static final String COMMA_SUFFIXED_WITH_EMPTY_SPACE = ", ";
    private static final String CONTEXTS_KEY = "contexts";
    private static final String TYPE_KEY = "type";
    private static final String VALUE_KEY = "value";

    protected final org.apache.kafka.clients.consumer.KafkaConsumer<String, byte[]> consumer;
    protected final AtomicBoolean isClosed;
    protected final ReentrantReadWriteLock lock;
    protected final Map<String, SubscriptionTopic> topicsByName;
    protected final String groupId;
    protected final String consumerName;
    protected final Collection<TopicPartition> topicPartitions;
    protected boolean isFirstPoll;
    protected String memberId;

    private final ResourceManager resourceManager;

    KafkaConsumer(final KafkaConsumerConfig kafkaConsumerConfig) {
        final KafkaService kafkaService = (KafkaService) QueueServiceProvider.get();
        final Map<String, Object> clientConfig = new HashMap<>(kafkaService.getClientInput());
        final int maxRecords = kafkaConsumerConfig.getPollConfig().maxRecords();
        this.groupId = kafkaConsumerConfig.getGroupId();

        clientConfig.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, Objects.requireNonNull(groupId));
        clientConfig.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaConsumerConfig.isReadFromLast() ? "latest" : "earliest");
        loadAdditionalClientConfig(clientConfig);

        if (maxRecords > 0) {
            clientConfig.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxRecords);
        }
        this.consumerName = kafkaConsumerConfig.getConsumerName();
        this.topicPartitions = new HashSet<>();
        this.lock = new ReentrantReadWriteLock();
        this.isClosed = new AtomicBoolean(false);
        this.consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(clientConfig);
        this.isFirstPoll = true;
        this.topicsByName = kafkaConsumerConfig.getSubscriptionTopics().stream()
                .collect(Collectors.toMap(subscriptionTopic -> {
                    final String topicName = subscriptionTopic.getName();

                    for (final PartitionInfo partitionInfo : consumer.partitionsFor(topicName)) {
                        topicPartitions.add(new TopicPartition(topicName, partitionInfo.partition()));
                    }

                    return topicName;
                }, topic -> topic, (firstTopic, secondTopic) -> firstTopic));
        this.resourceManager = ResourceManager.getInstance();

        ConsumerGroupService.getInstance().load(groupId);
        resourceManager.add(consumer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void unsubscribe() {
        consumer.unsubscribe();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void close() {
        final Lock writeLock = lock.writeLock();

        try {
            if (!isClosed.get()) {
                writeLock.lock();

                if (!isClosed.get()) {
                    consumer.close();
                    resourceManager.remove(consumer);
                    isClosed.set(true);
                }

            }
        } finally {
            writeLock.unlock();
        }

    }

    /**
     * Wakes up the consumer
     */
    @Override
    public final void wakeup() {
        consumer.wakeup();
    }

    /**
     * Subscribes to the topics mentioned on the {@link KafkaConsumerConfig}
     */
    @Override
    public final void subscribe() {
        if (!topicsByName.isEmpty()) {
            final Set<String> topicNames = topicsByName.keySet();

            try {
                consumer.subscribe(topicNames);
            } catch (TimeoutException timeoutException) {
                LoggerUtil.logError(LOGGER, timeoutException, "Consumer {} subscription: Subscription of topics {} has been failed, Error message: {}", memberId, topicNames, timeoutException.getMessage());
            } catch (UnknownTopicOrPartitionException unknownTopicOrPartitionException) {
                LoggerUtil.logError(LOGGER, unknownTopicOrPartitionException, "Consumer {} subscription: Topics {} or partition is not exist, Error message: {}", memberId, topicNames, unknownTopicOrPartitionException.getMessage());
            } catch (Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, "Consumer subscription: Failed to subscribe to topics {}, Consumer: {}, Error message: {}", topicNames, memberId,throwable.getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void pause() {
        consumer.pause(topicPartitions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void resume() {
        consumer.resume(topicPartitions);
    }

    /**
     * Loads the additional client configuration needed for consumer initialization
     *
     * @param clientConfig Which contains the client configuration
     */
    abstract void loadAdditionalClientConfig(final Map<String, Object> clientConfig);

    /**
     * Converts a string representation of a byte array to an actual byte array
     *
     * @param arrayString the input string to convert (e.g., "[1, 2, 3]")
     * @return the resulting byte array
     */
    byte[] convertStringToByteArray(final String arrayString) {
        final String[] values = arrayString.substring(1, arrayString.length() - 1).split(COMMA_SUFFIXED_WITH_EMPTY_SPACE);
        final byte[] byteArray = new byte[values.length];

        for (int index = 0; index < values.length; index++) {
            byteArray[index] = Byte.parseByte(values[index]);
        }
        return byteArray;
    }

    void clearContextValues() {

        for (final ContextHolder<? extends Context> contextHolder : ContextHolderRegistry.getInstance().getContextHolders()) {
            contextHolder.clear();
        }
    }

    /**
     * Loads the context values transformed through the consumed message into the current thread
     *
     * @param dataJson Which contains the context values
     */
    void loadContextValues(final JsonObject dataJson) {

        if (dataJson.containsKey(CONTEXTS_KEY)) {
            final ContextCodecRegistry contextCodecRegistry = ContextCodecRegistry.getInstance();
            final ContextHolderRegistry contextHolderRegistry = ContextHolderRegistry.getInstance();
            final JsonArray contextArray = dataJson.getJsonArray(CONTEXTS_KEY);
            final int contextArraySize = contextArray.size();

            for (int index = 0; index < contextArraySize; index++) {
                loadContextValue(contextArray.getJsonObject(index), contextCodecRegistry, contextHolderRegistry);
            }
        }
    }

    /**
     * Loads the given context value into the current thread
     *
     * @param contextJson           Which contains the context value
     * @param contextCodecRegistry  Which contains the contexts' codec registries
     * @param contextHolderRegistry Which contains the contexts' holder registries
     */
    private void loadContextValue(final JsonObject contextJson, final ContextCodecRegistry contextCodecRegistry,
                                  final ContextHolderRegistry contextHolderRegistry) {

        if (contextJson.containsKey(VALUE_KEY)) {
            final String contextTypeName = contextJson.getString(TYPE_KEY);

            if (contextCodecRegistry.contains(contextTypeName)) {
                final byte[] contextBytes = convertStringToByteArray(contextJson.getString(VALUE_KEY));
                final Context context = Objects.requireNonNull(contextCodecRegistry.get(contextTypeName)).decode(contextBytes);

                if (Objects.nonNull(context) && contextHolderRegistry.contains(contextTypeName)) {
                    Objects.requireNonNull(contextHolderRegistry.get(contextTypeName)).setContext(context);
                }
            }
        }
    }
}