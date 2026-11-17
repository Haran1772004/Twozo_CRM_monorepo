package com.tlc.social.server.whatsapp.internal.service;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.producer.ProducerStore;

import com.tlc.social.common.queue.SocialQueueTopics;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.model.WABusinessAccount;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Optional;

/**
 * <p>
 * Publishes WhatsApp template sync events to the messaging queue.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class WATemplateSyncPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(WATemplateSyncPublisher.class);

    private static final class InstanceHolder {
        private static final WATemplateSyncPublisher INSTANCE = new WATemplateSyncPublisher();
    }

    private final ProducerStore producerStore;

    private WATemplateSyncPublisher() {
        producerStore = ProducerStore.getInstance();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link WATemplateSyncPublisher}.
     * </p>
     *
     * @return the {@link WATemplateSyncPublisher} instance
     */
    public static WATemplateSyncPublisher getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Publishes the template fetch event to the messaging queue.
     * </p>
     *
     * @param waBusinessAccount Represents the whatsapp business account
     */
    public void publish(final WABusinessAccount waBusinessAccount) {
        final JsonObject dataJson = Json.object();
        final Long waBusinessAccountId = waBusinessAccount.id();
        final String requestId = Base64.getEncoder().encodeToString(String.format("WA Template Sync:%d", waBusinessAccountId).getBytes());
        final SubscriptionTopic responseProcessTopic = SocialQueueTopics.getWATemplateSyncTopic(QueueModel.Type.KAFKA);
        final Optional<Producer<byte[]>> optionalProducer = producerStore.getProducer(responseProcessTopic);

        dataJson.put(WAFields.BUSINESS_ACCOUNT_ID, waBusinessAccountId);
        dataJson.put(WAFields.EXTERNAL_WABA_ID, waBusinessAccount.getExternalWABAId());


        optionalProducer.ifPresentOrElse(producer -> {
            producer.send(new StreamMessage<>(requestId, responseProcessTopic, dataJson.getBytes()));

            LOGGER.info("Whatsapp: WhatsApp template fetch process request published. Request ID: {}", requestId);
        }, () -> LOGGER.error("Whatsapp: Kafka producer not found for the topic:{}", responseProcessTopic.getName()));
    }
}