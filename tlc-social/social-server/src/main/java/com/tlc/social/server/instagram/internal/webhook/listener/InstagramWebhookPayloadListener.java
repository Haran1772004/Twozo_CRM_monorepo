package com.tlc.social.server.instagram.internal.webhook.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.producer.ProducerStore;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.model.Platform;
import com.tlc.social.common.queue.SocialQueueTopics;
import com.tlc.social.server.instagram.internal.service.InstagramWebhookInboundService;
import com.tlc.social.server.instagram.internal.webhook.model.InstagramReadMessageEvent;
import com.tlc.social.server.instagram.internal.webhook.model.InstagramTextMessageEvent;
import com.tlc.social.server.instagram.manager.InstagramPageManager;
import com.tlc.social.server.internal.webhook.event.model.WebhookMessageEvent;
import com.tlc.social.server.internal.webhook.event.service.PageWebhookMessageEventService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Listens to the instagram's webhook payload events.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramWebhookPayloadListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstagramWebhookPayloadListener.class);

    private final InstagramWebhookInboundService webhookInboundService;
    private final PageWebhookMessageEventService webhookMessageEventService;
    private final InstagramPageManager instagramPageManager;
    private final ProducerStore producerStore;

    public InstagramWebhookPayloadListener() {
        webhookInboundService = InstagramWebhookInboundService.getInstance();
        webhookMessageEventService = PageWebhookMessageEventService.getInstance();
        instagramPageManager = InstagramPageManager.getInstance();
        producerStore = ProducerStore.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage Represents the stream message
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        try {
            Long firstEntryId = null;
            final JsonObject data = Json.object(streamMessage.data());
            final JsonObject response = Json.object();
            final JsonArray entries = data.getJsonArray(FBFields.ENTRY);
            final int entrySize = entries.size();
            final JsonArray entryResponse = Json.array();

            for (int index = NumericalConstant.ZERO; index < entrySize; index++) {
                final JsonObject entry = entries.getJsonObject(index);
                final JsonArray messagingDataArray = entry.getJsonArray(FBFields.MESSAGING);
                final int messagingDataSize = messagingDataArray.size();
                final JsonArray messagingDataResponse = Json.array();

                if (Objects.isNull(firstEntryId)) {
                    firstEntryId = entry.getLong(FBFields.ID);
                }
                for (int messagingIndex = NumericalConstant.ZERO; messagingIndex < messagingDataSize; messagingIndex++) {
                    final JsonObject messagingData = messagingDataArray.getJsonObject(messagingIndex);
                    final WebhookMessageEvent webhookMessageEvent = webhookMessageEventService.get(messagingData, Platform.INSTAGRAM);

                    switch (webhookMessageEvent.getMessageEventType()) {
                        case READ -> {
                            LOGGER.info("Instagram: Received webhook read receipt event");
                            webhookInboundService.handleReadMessageEvent((InstagramReadMessageEvent) webhookMessageEvent);
                        }
                        case TEXT -> {
                            LOGGER.info("Instagram: Received webhook text event");
                            final InstagramTextMessageEvent textMessageEvent = (InstagramTextMessageEvent) webhookMessageEvent;

                            if (textMessageEvent.isMessageEcho() && instagramPageManager.isMessageExists(textMessageEvent.getMessageId())) {
                                break;
                            }
                            messagingDataResponse.put(webhookInboundService.handleTextMessageEvent(textMessageEvent));

                        }
                        default -> {}
                    }
                }

                if (NumericalConstant.ZERO < messagingDataResponse.size()) {
                    entryResponse.put(messagingDataResponse);
                }
            }
            LOGGER.info("Instagram: Webhook message events have been processed");

            if (NumericalConstant.ZERO < entryResponse.size()) {
                final String requestId = Base64.getEncoder().encodeToString(String.format("FB:%d", firstEntryId).getBytes());

                publish(requestId, response.put(FBFields.ENTRIES, entryResponse));
            }
        } catch (Throwable throwable) {
            LOGGER.error("Instagram: Failed to handle webhook data. Reason:{}, Stack:{}", throwable.getMessage(), throwable);
        } finally {
            streamMessage.clean();
        }
    }

    /**
     * <p>
     * Publishes the webhook event to the messaging queue with required data.
     * </p>
     *
     * @param requestId Refers the unique identifier.
     * @param dataJson  Refers the json data to be published.
     */
    private void publish(final String requestId, final JsonObject dataJson) {
        final SubscriptionTopic responseProcessTopic = SocialQueueTopics.getInstagramWebhookResponseProcessTopic(
                QueueModel.Type.KAFKA);
        final Optional<Producer<byte[]>> optionalProducer = producerStore.getProducer(responseProcessTopic);

        optionalProducer.ifPresentOrElse(producer -> {
            producer.send(new StreamMessage<>(requestId, responseProcessTopic, dataJson.getBytes()));

            LOGGER.info("Instagram: FB webhook response process request published. Request ID: {}", requestId);
        }, () -> LOGGER.error("Instagram: Kafka producer not found for the topic:{}", responseProcessTopic.getName()));
    }
}
