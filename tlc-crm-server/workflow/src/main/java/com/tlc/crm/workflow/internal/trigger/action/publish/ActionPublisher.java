package com.tlc.crm.workflow.internal.trigger.action.publish;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.queue.QueueModel;

import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.provider.QueueServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Publisher responsible for dispatching the necessary data of
 * the {@link com.tlc.crm.workflow.api.model.WorkflowAction} over the stream network.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ActionPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionPublisher.class);

    private ActionPublisher() {
    }

    private static final class Instance {
        private static final ActionPublisher INSTANCE = new ActionPublisher();
    }

    /**
     * Gets the instance of {@link ActionPublisher}.
     *
     * @return {@link ActionPublisher} instance.
     */
    public static ActionPublisher getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Publishes the data to the topic over the stream network.
     *
     * @param actionRequestId Refers the published data identifier.
     */
    public void publish(final String actionRequestId, final JsonObject jsonObject) {
        final SubscriptionTopic actionInitiatorTopic = CoreQueueTopics.getActionInitiatorTopic(QueueModel.Type.KAFKA);
        final KafkaProducerConfig actionInitiatorProducerConfig = new KafkaProducerConfig(actionInitiatorTopic);

        try (final Producer<byte[]> actionInitiatorProducer = QueueServiceProvider.get().createProducer(actionInitiatorProducerConfig)) {
            actionInitiatorProducer.send(new StreamMessage<>(actionRequestId, actionInitiatorTopic, jsonObject.getBytes()));
        }
        LOGGER.info("Workflow Execution: Action request published. Request ID: {}", actionRequestId);
    }
}
