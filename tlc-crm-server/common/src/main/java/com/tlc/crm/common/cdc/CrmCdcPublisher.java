package com.tlc.crm.common.cdc;

import com.tlc.cdc.core.enrichment.EnrichedRecord;
import com.tlc.commons.json.JsonObject;

import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.kafka.KafkaTopic;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.provider.QueueServiceProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Publishes aggregated and enriched CDC events to downstream systems.
 *
 * <p>This class is responsible for taking processed CDC data and
 * sending it to the configured sink or messaging system.</p>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class CrmCdcPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrmCdcPublisher.class);

    private CrmCdcPublisher() {
    }

    private static class Instance {
        private static final CrmCdcPublisher CRM_CDC_PUBLISHER = new CrmCdcPublisher();
    }

    public static CrmCdcPublisher getInstance() {
        return Instance.CRM_CDC_PUBLISHER;
    }

    /**
     * Publishes the given collection of enriched records.
     *
     * @param enrichedRecords Collection of enriched records to be published.
     */
    public void publish(final Collection<EnrichedRecord> enrichedRecords) {
        for (final EnrichedRecord enrichedRecord : enrichedRecords) {
            final JsonObject jsonObject = enrichedRecord.enrichedPayload();
            final String topic = jsonObject.getString("topic");
            final SubscriptionTopic subscriptionTopic = new KafkaTopic(topic);
            final KafkaProducerConfig producerConfig = new KafkaProducerConfig(subscriptionTopic);

            try (final Producer<byte[]> producer = QueueServiceProvider.get().createProducer(producerConfig)) {
                producer.sendWithoutContext(new StreamMessage<>(String.valueOf(enrichedRecord.transactionId()), subscriptionTopic, jsonObject.remove("topic").getBytes()));
            } catch (Exception exception) {
                LOGGER.error("Analytics CDC: Failed to publish enriched record to the OLAP connector for Topic: {}, Transaction ID: {}, Reason: {}", topic, enrichedRecord.transactionId(), exception.getMessage(), exception);
            }
        }
    }
}
