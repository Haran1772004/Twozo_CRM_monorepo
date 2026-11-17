package com.tlc.social.server.facebook.internal.webhook.listener;

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

import com.tlc.social.common.model.Platform;
import com.tlc.social.common.queue.SocialQueueTopics;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.server.facebook.internal.webhook.model.FBDeliveryMessageEvent;
import com.tlc.social.server.facebook.internal.webhook.model.FBReadMessageEvent;
import com.tlc.social.server.facebook.internal.webhook.model.FBTextMessageEvent;
import com.tlc.social.server.internal.webhook.event.service.PageWebhookMessageEventService;
import com.tlc.social.server.facebook.internal.service.FBWebhookInboundService;
import com.tlc.social.server.internal.webhook.event.model.WebhookMessageEvent;
import com.tlc.social.server.facebook.manager.FBPageManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A listener which listens to the facebook webhook payload event
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class FBWebhookPayloadListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FBWebhookPayloadListener.class);

    private final FBWebhookInboundService fbWebhookInboundService;
    private final PageWebhookMessageEventService webhookMessageEventService;
    private final FBPageManager fbPageManager;
    private final ProducerStore producerStore;

    public FBWebhookPayloadListener() {
        fbWebhookInboundService = FBWebhookInboundService.getInstance();
        webhookMessageEventService = PageWebhookMessageEventService.getInstance();
        fbPageManager = FBPageManager.getInstance();
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
            final JsonObject data = Json.object(streamMessage.data());
            final String webhookObject = data.getString(FBFields.OBJECT);
            final JsonObject response = Json.object();

            Long firstEntryId = null;

            if (Objects.equals(FBFields.PAGE, webhookObject)) {
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
                        final WebhookMessageEvent webhookMessageEvent = webhookMessageEventService.get(messagingData, Platform.FACEBOOK);

                        switch (webhookMessageEvent.getMessageEventType()) {
                            case READ -> {
                                LOGGER.info("Facebook: Received webhook read receipt event");
                                fbWebhookInboundService.handleReadMessageEvent((FBReadMessageEvent) webhookMessageEvent);
                            }
                            case TEXT -> {
                                LOGGER.info("Facebook: Received webhook text event");
                                final FBTextMessageEvent textMessageEvent = (FBTextMessageEvent) webhookMessageEvent;

                                if (textMessageEvent.isMessageEcho() && fbPageManager.isMessageExists(textMessageEvent.getMessageId())) {
                                    break;
                                }
                                messagingDataResponse.put(fbWebhookInboundService.handleTextMessageEvent(textMessageEvent));
                            }
                            case DELIVERY -> {
                                LOGGER.info("Facebook: Received webhook message delivery event");
                                fbWebhookInboundService.handleDeliveryMessageEvent((FBDeliveryMessageEvent) webhookMessageEvent);
                            }
                        }
                    }

                    if (NumericalConstant.ZERO < messagingDataResponse.size()) {
                        entryResponse.put(messagingDataResponse);
                    }
                }
                LOGGER.info("Facebook: Webhook message events have been processed");

                if (NumericalConstant.ZERO < entryResponse.size()) {
                    final String requestId = Base64.getEncoder().encodeToString(String.format("FB:%d", firstEntryId).getBytes());

                    publish(requestId, response.put(FBFields.ENTRIES, entryResponse));
                }
            }
        } catch (Throwable throwable) {
            LOGGER.error("Facebook: Failed to handle webhook data. Reason:{}, Stack:{}", throwable.getMessage(), throwable);
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
        final SubscriptionTopic responseProcessTopic = SocialQueueTopics.getFBWebhookResponseProcessTopic(QueueModel.Type.KAFKA);
        final Optional<Producer<byte[]>> optionalProducer = producerStore.getProducer(responseProcessTopic);

        optionalProducer.ifPresentOrElse(producer -> {
            producer.send(new StreamMessage<>(requestId, responseProcessTopic, dataJson.getBytes()));

            LOGGER.info("Facebook: FB webhook response process request published. Request ID: {}", requestId);
        }, () -> LOGGER.error("Facebook: Kafka producer not found for the topic:{}", responseProcessTopic.getName()));
    }
}
