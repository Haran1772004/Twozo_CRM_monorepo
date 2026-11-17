package com.tlc.crm.core.queue;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.producer.ProducerStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Handles the publishing data of messaging event related to entity to downstream systems.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class EntityEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityEventPublisher.class);

    private final ProducerStore producerStore;

    private EntityEventPublisher() {
        this.producerStore = ProducerStore.getInstance();
    }

    private static class Instance {
        private static final EntityEventPublisher INSTANCE = new EntityEventPublisher();
    }

    /**
     * Gets the instance of {@link EntityEventPublisher}.
     */
    public static EntityEventPublisher getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Publishes an event indicating that a new entity has been created.
     *
     * @param orgId     The organization identifier under which the entity was created.
     * @param requestId A unique request or correlation ID used for tracing the event.
     * @param dataJson  The JSON payload representing the created entity’s data.
     * @param source    The {@code Source}.
     */
    public void publishEntityCreatedEvent(final long orgId, final String requestId, final JsonObject dataJson, final Source source) {
        final SubscriptionTopic topic = switch (source) {
            case COMPANY -> CoreQueueTopics.getCompanyCreatedTopic(QueueModel.Type.KAFKA);
            case CONTACT -> CoreQueueTopics.getContactCreatedTopic(QueueModel.Type.KAFKA);
            case DEAL -> CoreQueueTopics.getDealCreatedTopic(QueueModel.Type.KAFKA);
            default -> throw ErrorCode.get(CoreErrorCodes.INVALID_SOURCE);
        };  // TODO #2986 - Kafka event enhancement

        final Optional<Producer<byte[]>> optionalProducer = producerStore.getProducer(topic);

        optionalProducer.ifPresent(producer -> {
            producer.send(new StreamMessage<>(requestId, topic, dataJson.getBytes()));
            LOGGER.info("Data Publisher: Published the created event for OrgId: {}, RequestId: {}, Source: {}", orgId, requestId, source);
        });
    }
}
