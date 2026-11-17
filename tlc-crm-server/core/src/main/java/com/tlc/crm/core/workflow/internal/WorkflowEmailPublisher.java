package com.tlc.crm.core.workflow.internal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.queue.CoreQueueTopics;

import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;

import com.tlc.crm.entity.fields.FieldKeys;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.producer.ProducerStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@code WorkflowEmailPublisher} is responsible for publishing email actions to the Kafka topic.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0.0
 */
public final class WorkflowEmailPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowEmailPublisher.class);

    private static final String INITIAL_MESSAGE = "initialMessage";
    private static final String WORKFLOW_CONFIGURATION = "workflowConfiguration";

    private final Base64.Encoder encoder;
    private final ProducerStore producerStore;

    private WorkflowEmailPublisher() {
        this.encoder = Base64.getEncoder();
        this.producerStore = ProducerStore.getInstance();
    }

    private static final class Instance {
        private static final WorkflowEmailPublisher INSTANCE = new WorkflowEmailPublisher();
    }

    /**
     * Gets the instance of {@link WorkflowEmailPublisher}.
     *
     * @return {@link WorkflowEmailPublisher} instance.
     */
    public static WorkflowEmailPublisher getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Publishes the email action to the Kafka topic.
     *
     * @param orgId        The organization id.
     * @param userId       The user id.
     * @param entityThread The entity thread.
     * @param stateJson    The state json.
     * @param isSuccess    The success flag.
     */
    public void publish(final Long orgId, final Long userId, final EntityThread entityThread, final JsonObject stateJson, final boolean isSuccess) {
        if (stateJson.containsKey(WORKFLOW_CONFIGURATION)) {
            final String requestId = encoder.encodeToString(String.format("%s:%s", orgId, userId).getBytes());
            final SubscriptionTopic topic = CoreQueueTopics.getEmailActionTopic(QueueModel.Type.KAFKA);
            final Optional<Producer<byte[]>> optionalProducer = producerStore.getProducer(topic);
            final JsonObject dataJson = Json.object();

            dataJson.put(EmailFields.STATE.getName(), stateJson);
            dataJson.put(FieldKeys.SUCCESS.getName(), isSuccess);

            if (isSuccess) {
                if (Objects.nonNull(entityThread)) {
                    final EntityEmail entityEmail = entityThread.getComposedEmail();
                    final EntityOutboundEmail outboundEmail = (EntityOutboundEmail) entityEmail;
                    final Collection<EmailTrackingDetail> emailTrackingDetails = outboundEmail.getEmailTrackingDetails();
                    final List<Long> trackerIds = emailTrackingDetails.stream()
                            .filter(emailTrackingDetail -> Objects.equals(EmailTrackingDetail.Type.CLICKED, emailTrackingDetail.getType())).map(EmailTrackingDetail::getId).toList();

                    dataJson.put(EmailFields.THREAD_ID.getName(), entityThread.getCrmId());
                    dataJson.put(EmailFields.CONVERSATION_ID.getName(), entityEmail.getConversationId());
                    dataJson.put(CoreEmailFields.ORG_ID.getName(), orgId);
                    dataJson.put(CoreEmailFields.USER_ID.getName(), userId);

                    if (!trackerIds.isEmpty()) {
                        dataJson.put(CoreEmailFields.TRACKING.getName(), trackerIds);
                    }
                }
            }
            optionalProducer.ifPresentOrElse(producer -> {
                producer.send(new StreamMessage<>(requestId, topic, dataJson.getBytes()));

                LOGGER.debug("Email: Email action request published. Request ID: {}", requestId);
            }, () -> LOGGER.error("Email: Kafka producer not found for the topic:{}", topic.getName()));
        }
    }

    public void publish(final Long conversationId, final Long threadId, final Boolean isClicked, final Boolean initialMessage) {
        final JsonObject dataJson = Json.object();

        dataJson.put(CoreEmailFields.IS_CLICKED.getName(), isClicked);
        dataJson.put(CoreEmailFields.INITIAL_MESSAGE.getName(), initialMessage);
        dataJson.put(EmailFields.THREAD_ID.getName(), threadId);
        dataJson.put(EmailFields.CONVERSATION_ID.getName(), conversationId);

        final SubscriptionTopic topic = CoreQueueTopics.getEmailActionTopic(QueueModel.Type.KAFKA);
        final Optional<Producer<byte[]>> optionalProducer = producerStore.getProducer(topic);

        optionalProducer.ifPresent(producer -> {
            final String requestId = String.valueOf(conversationId);

            producer.send(new StreamMessage<>(requestId, topic, dataJson.getBytes()));
            LOGGER.debug("Email: Email action request published. Request ID: {}", requestId);
        });
    }
}
