package com.tlc.cdc.core.publisher;

import com.tlc.commons.json.JsonObject;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.provider.QueueServiceProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Publishes aggregated CDC (Change Data Capture) events to a downstream messaging system, typically a Kafka topic.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class CdcAggregateEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdcAggregateEventPublisher.class);
    private static final AtomicReference<Producer<byte[]>> CDC_PRODUCER = new AtomicReference<>();

    private CdcAggregateEventPublisher() {
    }

    private static class Instance {
        private static final CdcAggregateEventPublisher INSTANCE = new CdcAggregateEventPublisher();
    }

    /**
     * Gets the {@link CdcAggregateEventPublisher} instance.
     *
     * @return {@link CdcAggregateEventPublisher}.
     */
    public static CdcAggregateEventPublisher getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Publishes the aggregated CDC event to the producer.
     *
     * @param transactionId  Refers the transaction ID.
     * @param aggregatedData Refers the aggregated {@code JsonObject}.
     */
    public void publish(final Long transactionId, final JsonObject aggregatedData) {
        final SubscriptionTopic aggregatedCdcEvent = CdcQueueTopics.aggregatedCdcTopic(QueueModel.Type.KAFKA);

        try {
            getCdcProducer().sendWithoutContext(new StreamMessage<>(transactionId.toString(), aggregatedCdcEvent, aggregatedData.getBytes()));
            LOGGER.debug("CDC: Aggregated data published for transaction ID: {}", transactionId);
        } catch (Exception exception) {
            LOGGER.error("CDC: Failed to publish the aggregated data for transaction ID: {}, Reason: {}", transactionId, exception.getMessage(), exception);
        }
    }

    /**
     * Closes the shared Kafka producer and releases its associated resources.
     * <p>
     * after all pending CDC events have been processed and published.</p>
     */
    public static void closeProducer() {
        final Producer<byte[]> producer = CDC_PRODUCER.getAndSet(null);

        if (Objects.nonNull(producer)) {
            producer.close();
            LOGGER.info("CDC: Kafka Producer closed successfully");
        }
    }

    /**
     * Gets the shared CDC Kafka producer, initializing it when required.
     *
     * <p>The producer is intended to be initialized once and reused for
     * subsequent CDC event publications.</p>
     *
     * @return the shared CDC Kafka {@link Producer}.
     */
    private static Producer<byte[]> getCdcProducer() {
        Producer<byte[]> producer = CDC_PRODUCER.get();

        if (Objects.isNull(producer)) {
            producer = initializeProducer();

            if (CDC_PRODUCER.compareAndSet(null, producer)) {
                LOGGER.info("CDC: Kafka Producer initialized");
            } else {
                producer.close();
                producer = CDC_PRODUCER.get();
            }
        }
        return producer;
    }

    /**
     * Initializes the Kafka producer used to publish aggregated CDC events.
     * <p>
     * for all subsequent CDC event publications.</p>
     *
     * @return the initialized Kafka {@link Producer}.
     */
    private static Producer<byte[]> initializeProducer() {
        return QueueServiceProvider.get()
                .createProducer(new KafkaProducerConfig(CdcQueueTopics.aggregatedCdcTopic(QueueModel.Type.KAFKA)));
    }
}
