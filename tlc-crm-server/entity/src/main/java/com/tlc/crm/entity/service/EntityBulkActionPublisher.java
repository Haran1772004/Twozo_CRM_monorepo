package com.tlc.crm.entity.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.producer.ProducerStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Service for handling asynchronous bulk update operations
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
public final class EntityBulkActionPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityBulkActionPublisher.class);

    private final ProducerStore producerStore;

    private EntityBulkActionPublisher() {
        producerStore = ProducerStore.getInstance();
    }

    private static final class InstanceHolder {
        private static final EntityBulkActionPublisher INSTANCE = new EntityBulkActionPublisher();
    }

    /**
     * <p>
     * Gets the {@link EntityBulkActionPublisher} instance
     * </p>
     *
     * @return The {@link EntityBulkActionPublisher}
     */
    public static EntityBulkActionPublisher getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Publishes data to a subscription topic for the given organization and user
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param userId             Represents an user id
     * @param messageJson        Represents the json data to be published [should contain data and type key in it]
     * @param modelIds           Which contains the collection of model ids //FIXME: Task 9406: Batched fetch Operation in Bulk Update
     * @param subscriptionTopic  Represents the {@link SubscriptionTopic} to which the data will be published
     */
    public void publish(final long orgId, final long userId, final JsonObject messageJson, final Collection<Long> modelIds,
                        final SubscriptionTopic subscriptionTopic) {
        LOGGER.info("Bulk Action: Bulk event started for {}", subscriptionTopic.getName());

        if (!modelIds.isEmpty()) {

            if (messageJson.containsKey("type") && messageJson.containsKey("data")) {
                final byte[] requestJsonBytes = messageJson.getBytes();
                final Optional<Producer<byte[]>> optionalProducer = producerStore.getProducer(subscriptionTopic);

                if (optionalProducer.isPresent()) {
                    final Producer<byte[]> producer = optionalProducer.get();

                    for (final Long id : modelIds) {
                        final String streamRequestId = Base64.getEncoder()
                                .encodeToString(String.format("%s:%s:%s", orgId, userId, id).getBytes());

                        producer.send(new StreamMessage<>(streamRequestId, subscriptionTopic, requestJsonBytes));
                        LOGGER.info("Bulk Action: Published the bulk event data for {}, Model ID: {}", subscriptionTopic.getName(), id);
                    }
                } else {
                    throw ErrorCode.get(EntityErrorCodes.PRODUCER_NOT_FOUND);
                }
            } else {
                throw ErrorCode.get(EntityErrorCodes.DATA_VALIDATION_FAILED);
            }
        }
    }
}
