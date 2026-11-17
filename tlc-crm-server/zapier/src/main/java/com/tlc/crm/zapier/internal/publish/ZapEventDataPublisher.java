package com.tlc.crm.zapier.internal.publish;

import com.tlc.commons.json.JsonObject;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.producer.ProducerStore;

import com.tlc.crm.zapier.queue.ZapierQueueTopics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Publishes the zapier event data into the consumer.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ZapEventDataPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZapEventDataPublisher.class);

    private final ProducerStore producerStore;

    private ZapEventDataPublisher() {
        this.producerStore = ProducerStore.getInstance();
    }

    private static final class Instance {
        private static final ZapEventDataPublisher INSTANCE = new ZapEventDataPublisher();
    }

    /**
     * Gets the instance of {@link ZapEventDataPublisher}.
     *
     * @return {@link ZapEventDataPublisher} instance.
     */
    public static ZapEventDataPublisher getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Publishes the zapier event to the messaging queue with required data.
     *
     * @param orgId     Refers the organization id.
     * @param requestId Refers the unique identifier.
     * @param dataJson  Refers the {@code StreamData} json.
     */
    public void publish(final Long orgId, final String requestId, final JsonObject dataJson) {
        final SubscriptionTopic triggerInitiatorTopic = ZapierQueueTopics.getEventInitiatorTopic(QueueModel.Type.KAFKA);
        final Optional<Producer<byte[]>> optionalProducer = producerStore.getProducer(triggerInitiatorTopic);

        optionalProducer.ifPresent(producer -> {
            producer.send(new StreamMessage<>(requestId, triggerInitiatorTopic, dataJson.getBytes()));

            LOGGER.info("Zapier: Zap event trigger published for OrgId: {}, RequestId: {}", orgId, requestId);
        });
    }
}
