package com.tlc.file.common.queue;

import com.tlc.commons.json.Json;

import com.tlc.file.common.fields.AttachmentAttributes;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.provider.QueueServiceProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Manages the attachment removal event to the messaging queue with required data.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AttachmentRemovalPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentRemovalPublisher.class);

    private AttachmentRemovalPublisher() {
    }

    private static final class Instance {
        private static final AttachmentRemovalPublisher INSTANCE_HOLDER = new AttachmentRemovalPublisher();
    }

    /**
     * Gets the instance of {@link AttachmentRemovalPublisher}.
     *
     * @return {@link AttachmentRemovalPublisher} instance.
     */
    public static AttachmentRemovalPublisher getInstance() {
        return Instance.INSTANCE_HOLDER;
    }

    /**
     * Publishes the attachment removal event to the messaging queue with required data.
     *
     * @param requestId     The unique identifier of the request associated with the attachments removal process.
     * @param attachmentIds A collection of IDs representing the attachments to be published.
     */
    public void publish(final String requestId, final Collection<Long> attachmentIds) {
        final SubscriptionTopic attachmentRemovalTopic = AttachmentQueueTopics.getAttachmentRemovalTopic(QueueModel.Type.KAFKA);
        final KafkaProducerConfig attachmentRemovalProducerConfig = new KafkaProducerConfig(attachmentRemovalTopic);

        try (final Producer<byte[]> attachmentRemovalProducer = QueueServiceProvider.get().createProducer(attachmentRemovalProducerConfig)) {
            attachmentRemovalProducer.send(new StreamMessage<>(requestId, attachmentRemovalTopic, Json.object().put(AttachmentAttributes.ATTACHMENTS, attachmentIds).getBytes()));
            LOGGER.info("File service: Attachment deletion request published. Ids: {}, Request ID: {}", attachmentIds, requestId);
        }
    }

}