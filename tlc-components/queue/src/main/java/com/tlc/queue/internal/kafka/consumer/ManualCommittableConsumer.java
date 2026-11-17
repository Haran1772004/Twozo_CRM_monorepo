package com.tlc.queue.internal.kafka.consumer;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.internal.OffsetHolder;
import com.tlc.queue.internal.cache.offset.ProcessedOffsetService;
import com.tlc.queue.internal.resource.QueueErrorCodes;
import com.tlc.queue.kafka.KafkaConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Kafka consumer that processes messages with manual commit handling.
 *
 * @author sundar
 * @version 1.0
 * @see KafkaConsumer
 */
public class ManualCommittableConsumer extends KafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManualCommittableConsumer.class);
    private final BlockingQueue<OffsetHolder> offsetsToBeCommitted;
    private final Map<String, Map<Integer, Long>> nextOffsetsToBeCommitted;
    private final ProcessedOffsetService processedOffsetService;

    public ManualCommittableConsumer(final KafkaConsumerConfig kafkaConsumerConfig) {
        super(kafkaConsumerConfig);
        this.processedOffsetService = ProcessedOffsetService.getInstance();
        this.nextOffsetsToBeCommitted = new ConcurrentHashMap<>();
        this.offsetsToBeCommitted = new LinkedBlockingQueue<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void poll(long timeOutInMillis, final StreamListener<byte[]> listener) {

        if (this.isClosed.get()) {
            throw ErrorCode.get(QueueErrorCodes.STREAM_CONSUMER_CLOSED);
        }
        final ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(timeOutInMillis));

        if (isFirstPoll) {
            this.memberId = consumer.groupMetadata().memberId();
        }

        if (records != null) {
            final int recordsCount = records.count();

            if (recordsCount > 0) {
                LOGGER.debug("Queue {}: Manual commit offsets to be committed going to be updated: {}, Member ID: {}, Group ID: {}, Topic partitions size: {}", consumerName, offsetsToBeCommitted, memberId, groupId, topicPartitions.size());

                for (final TopicPartition topicPartition : records.partitions()) {
                    final Map<Integer, Long> partitionOffsetMap = nextOffsetsToBeCommitted.computeIfAbsent(topicPartition.topic(), nonExistingTopic -> new ConcurrentHashMap<>());
                    final int partition = topicPartition.partition();
                    final int size = records.records(topicPartition).size();
                    LOGGER.debug("Queue {}: {} records received from partition {}", consumerName, size, partition);
                    final long position = consumer.position(topicPartition) - size;
                    LOGGER.debug("Queue {}: Topic {} partition {} Offsets to be committed: {}, Member ID: {}, Group ID: {}, Current position of the consumer: {}", consumerName, topicPartition.topic(), partition, offsetsToBeCommitted, memberId, groupId, position);

                    partitionOffsetMap.put(partition, position);
                    updateOffsetsToBeCommitted(partitionOffsetMap, new OffsetHolder(topicPartition.topic(), partition, position), true);
                }
            }
            processRecords(records, listener);
        }
        commit();
    }

    /**
     * {@inheritDoc}
     *
     * @param clientConfig Which contains the client configuration
     */
    @Override
    void loadAdditionalClientConfig(final Map<String, Object> clientConfig) {
        clientConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    }

    /**
     * Processes the polled consumer records
     *
     * @param records  To be processed
     * @param listener To be trigger the process
     */
    private void processRecords(final ConsumerRecords<String, byte[]> records, final StreamListener<byte[]> listener) {

        records.iterator().forEachRemaining(consumedRecord -> {
            final long offset = consumedRecord.offset();
            final String topic = consumedRecord.topic();
            final OffsetHolder offsetHolder = new OffsetHolder(topic, consumedRecord.partition(), offset);
            final boolean recordCanBeProcessed;

            final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

            readLock.lock();

            try {
                /*
                 * We need to prevent processing the consumer from polling records that are still being processed in the background and remain uncommitted.
                 * Therefore, implementing this check is essential.
                 */
                recordCanBeProcessed = !(offsetsToBeCommitted.contains(offsetHolder) || processedOffsetService.isProcessed(groupId, offsetHolder));
            } finally {
                readLock.unlock();
            }

            if (recordCanBeProcessed) {
                processRecord(consumedRecord, offsetHolder, listener);
            }
        });
    }

    /**
     * Processes the {@link ConsumerRecord}
     *
     * @param consumedRecord the {@link ConsumerRecord} to be processed
     * @param offsetHolder   Which contains the offset information of the consumer record
     * @param listener       Represents the listener to be listened to process the record
     */
    private void processRecord(final ConsumerRecord<String, byte[]> consumedRecord, final OffsetHolder offsetHolder,
                               final StreamListener<byte[]> listener) {
        final byte[] data = consumedRecord.value();
        final long offset = offsetHolder.offset();
        final String topic = offsetHolder.topic();
        final JsonObject dataJson = Json.object(data);

        try {
            loadContextValues(dataJson);

            if (dataJson.containsKey(DATA_KEY)) {
                listener.listen(new ConsumerMessage(consumedRecord.key(), topicsByName.get(topic), convertStringToByteArray(dataJson.getString(DATA_KEY)), offsetHolder, listener));
            } else {
                listener.listen(new ConsumerMessage(consumedRecord.key(), topicsByName.get(topic), data, offsetHolder, listener));
            }

        } catch (Throwable exp) {
            LoggerUtil.logError(LOGGER, exp, "Record process: Record processing failed, Consumer: {}, Topic: {}, Offset: {}, Error message: {}", memberId, topic, offset, exp.getMessage());
        } finally {
            clearContextValues();
            LOGGER.info("Record process: Context values cleared, Consumer: {}, Topic: {}, Offset: {}", memberId, topic, offset);
        }
    }

    /**
     * Commits the offsets for records that have been fully processed when auto-commit is disabled,
     * and removes those offsets from the `offsetsToBeCommitted` collection
     */
    private void commit() {

        if (!offsetsToBeCommitted.isEmpty()) {
            final Map<TopicPartition, OffsetAndMetadata> offsetAndMetadataMap = new HashMap<>();
            final int offsetCount = offsetsToBeCommitted.size();
            /*
             * By committing the next offset (i.e., the offset of the next message to consume),
             * you ensure that Kafka knows the current message has already been processed,
             * and it can safely move on to the next one.
             *
             * If you commit the current offset, Kafka will think the message at that offset still needs to be processed,
             * and it will send the same message again, potentially causing duplicate processing.
             */

            for (int index = 0; index < offsetCount; index++) {

                try {
                    final OffsetHolder offsetHolder = offsetsToBeCommitted.take();
                    final long nextOffset = offsetHolder.offset() + 1;

                    offsetAndMetadataMap.put(new TopicPartition(offsetHolder.topic(), offsetHolder.partition()),
                            new OffsetAndMetadata(nextOffset));
                    processedOffsetService.remove(groupId, offsetHolder);
                } catch (InterruptedException e) {
                    LOGGER.error("Consumer offset commit: Failed to commit the offset, Member ID: {}, Error message: {}", memberId, e.getMessage(), e);
                }
            }

            try {
                consumer.commitSync(offsetAndMetadataMap);
            } catch (Throwable e) {
                LoggerUtil.logError(LOGGER, e, "Consumer offset commit: Failed to commit the offsets, Consumer: {}, Error message: {}", memberId, e.getMessage());
            }
        }
    }

    /**
     * Updates the committable offsets queue.
     *
     * @param partitionOffsetMap Contains the next position offset that can be committed.
     * @param offsetHolder       Represents the offset.
     * @param isInitialUpdate    A flag indicating whether the committable offsets queue is updated before or after processing.
     */
    private void updateOffsetsToBeCommitted(final Map<Integer, Long> partitionOffsetMap, final OffsetHolder offsetHolder,
                                            final boolean isInitialUpdate) {
        final int partition = offsetHolder.partition();
        final long offset = offsetHolder.offset();
        final SortedMap<Long, OffsetHolder> sortedOffsets = processedOffsetService.getSortedOffsets(groupId, offsetHolder.topic(), partition);

        LOGGER.debug("Queue {}: Non-committed processed offsets: {}, Member ID: {}, Group ID: {}, Offset: {}", consumerName, sortedOffsets, memberId, groupId, offset);

        if (isInitialUpdate) {

            if (!sortedOffsets.isEmpty()) {
                updateOffsetsToBeCommitted(partitionOffsetMap, sortedOffsets, offset, sortedOffsets.lastKey());
            }
        } else {
            final Long nextOffsetToBeCommitted = partitionOffsetMap.get(partition);
            final long from;
            final long to;

            if (nextOffsetToBeCommitted > offset) {
                from = offset;
                to = nextOffsetToBeCommitted;
            } else if (nextOffsetToBeCommitted == offset) {
                from = offset;
                to = sortedOffsets.isEmpty() ? nextOffsetToBeCommitted : sortedOffsets.lastKey();
            } else {
                from = nextOffsetToBeCommitted;
                to = sortedOffsets.isEmpty() ? offset : sortedOffsets.lastKey();
            }
            updateOffsetsToBeCommitted(partitionOffsetMap, sortedOffsets, from, to);
        }
    }

    /**
     * Updates the offsets to be committed using the specified offset range.
     *
     * @param partitionOffsetMap Contains the next position offset that can be committed.
     * @param sortedOffsets      Holds the processed offsets to be committed.
     * @param from               Represents the starting offset of the range.
     * @param to                 Represents the ending offset of the range.
     */
    private void updateOffsetsToBeCommitted(final Map<Integer, Long> partitionOffsetMap, final SortedMap<Long, OffsetHolder> sortedOffsets,
                                            final long from, final long to) {
        LOGGER.debug("Queue {}: Going to update the committable offset. From: {}, To: {}, Group ID: {}, Member ID: {}", consumerName, from, to, groupId, memberId);

        for (long offsetToBeCommitted = from; offsetToBeCommitted <= to; offsetToBeCommitted++) {

            if (sortedOffsets.containsKey(offsetToBeCommitted)) {
                final OffsetHolder committableOffsetHolder = sortedOffsets.get(offsetToBeCommitted);

                if (!offsetsToBeCommitted.contains(committableOffsetHolder)) {
                    offsetsToBeCommitted.add(committableOffsetHolder);
                    partitionOffsetMap.put(committableOffsetHolder.partition(), committableOffsetHolder.offset() + 1);
                }
            } else {
                break;
            }
        }
        LOGGER.debug("Queue {}: [After update] Offsets to be committed: {}, Consumer:{}, Group ID: {}", consumerName, offsetsToBeCommitted, memberId, groupId);
    }

    /**
     * {@link StreamMessage} consumed from the partition
     *
     * @author sundar
     * @version 1.0
     */
    private class ConsumerMessage extends StreamMessage<byte[]> {

        private final AtomicBoolean isCommitted;
        private final StreamListener<byte[]> listener;
        private final OffsetHolder offsetHolder;

        private ConsumerMessage(final String id, final SubscriptionTopic topic, final byte[] data,
                                final OffsetHolder offsetHolder, final StreamListener<byte[]> listener) {
            super(id, topic, data);
            this.isCommitted = new AtomicBoolean(false);
            this.listener = listener;
            this.offsetHolder = offsetHolder;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void clean() {
            LOGGER.debug("Record process: Record process completed, Going to commit the offset, Consumer:{}, Topic: {}, Offset: {}", memberId, offsetHolder.topic(), offsetHolder.offset());

            if (isCommitted.compareAndSet(false, true)) {
                LOGGER.debug("Queue {}: Offset {} is processed, going to update, Member ID: {}, Group ID: {}", consumerName, offsetHolder, memberId, groupId);
                processedOffsetService.add(groupId, offsetHolder);
                updateOffsetsToBeCommitted(offsetHolder);
                listener.afterListening();
            }
        }

        /**
         * Updates the specified {@link OffsetHolder} as processed
         *
         * @param offsetHolder Represents the processed offset
         */
        private void updateOffsetsToBeCommitted(final OffsetHolder offsetHolder) {
            final String topic = offsetHolder.topic();

            if (nextOffsetsToBeCommitted.containsKey(topic)) {
                final Map<Integer, Long> partitionOffsetMap = nextOffsetsToBeCommitted.get(topic);
                final int partition = offsetHolder.partition();

                if (partitionOffsetMap.containsKey(partition)) {
                    ManualCommittableConsumer.this.updateOffsetsToBeCommitted(partitionOffsetMap, offsetHolder, false);
                }
            }
        }
    }
}