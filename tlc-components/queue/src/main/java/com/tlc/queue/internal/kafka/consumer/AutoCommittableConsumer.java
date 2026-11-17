package com.tlc.queue.internal.kafka.consumer;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.internal.resource.QueueErrorCodes;
import com.tlc.queue.kafka.KafkaConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Kafka consumer that handles automatically committable messages.
 *
 * @author sundar
 * @version 1.0
 * @see KafkaConsumer
 */
public class AutoCommittableConsumer extends KafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoCommittableConsumer.class);

    public AutoCommittableConsumer(final KafkaConsumerConfig kafkaConsumerConfig) {
        super(kafkaConsumerConfig);
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

        if (Objects.nonNull(records)) {
            processRecords(records, listener);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param clientConfig Which contains the client configuration
     */
    @Override
    void loadAdditionalClientConfig(final Map<String, Object> clientConfig) {
        clientConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
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
            final byte[] data = consumedRecord.value();

            if (Objects.nonNull(data)) {
                final JsonObject dataJson = Json.object(data);

                try {
                    loadContextValues(dataJson);

                    if (dataJson.containsKey(DATA_KEY)) {
                        listener.listen(new StreamMessage<>(consumedRecord.key(), topicsByName.get(topic), convertStringToByteArray(dataJson.getString(DATA_KEY))));
                    } else {
                        listener.listen(new StreamMessage<>(consumedRecord.key(), topicsByName.get(topic), data));
                    }
                } catch (Throwable exp) {
                    LoggerUtil.logError(LOGGER, exp, "Record process: Record processing failed, Consumer: {}, Topic: {}, Offset: {}, Error message: {}", memberId, topic, offset, exp.getMessage());
                } finally {
                    clearContextValues();
                    LOGGER.debug("Record process: Context values cleared, Consumer: {}, Topic: {}, Offset: {}", memberId, topic, offset);
                }
                listener.afterListening();
            }
        });
    }
}