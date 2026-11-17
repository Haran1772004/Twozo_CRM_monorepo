package com.tlc.social.server.whatsapp.internal.webhook.listener;

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
import com.tlc.social.common.queue.SocialQueueTopics;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.server.internal.webhook.event.model.WebhookMessageEvent;
import com.tlc.social.server.whatsapp.internal.webhook.converter.WATemplateUpdateEventConverter;
import com.tlc.social.server.whatsapp.internal.webhook.converter.WAWebhookMessageEventConverter;
import com.tlc.social.server.whatsapp.internal.webhook.model.WAAccountUpdateEvent;
import com.tlc.social.server.whatsapp.internal.webhook.model.WADeliveryMessageEvent;
import com.tlc.social.server.whatsapp.internal.webhook.model.WAFailedMessageEvent;
import com.tlc.social.server.whatsapp.internal.webhook.model.WAReadMessageEvent;
import com.tlc.social.server.whatsapp.internal.webhook.model.WATemplateStatusUpdateEvent;
import com.tlc.social.server.whatsapp.internal.webhook.model.WATextMessageEvent;
import com.tlc.social.server.whatsapp.internal.service.WAWebhookInboundService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A listener which listens to the WhatsApp webhook payload event
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAWebhookPayloadListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WAWebhookPayloadListener.class);

    private final WAWebhookInboundService waWebhookInboundService;
    private final ProducerStore producerStore;
    private final WAWebhookMessageEventConverter waWebhookMessageEventConverter;
    private final WATemplateUpdateEventConverter waTemplateUpdateEventConverter;

    public WAWebhookPayloadListener() {
        waWebhookInboundService = WAWebhookInboundService.getInstance();
        producerStore = ProducerStore.getInstance();
        waWebhookMessageEventConverter = WAWebhookMessageEventConverter.getInstance();
        waTemplateUpdateEventConverter = WATemplateUpdateEventConverter.getInstance();
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
            final String webhookObject = data.getString(WAFields.OBJECT);
            final JsonObject response = Json.object();

            Long firstEntryId = null;

            if (Objects.equals(WAFields.WHATSAPP_BUSINESS_ACCOUNT, webhookObject)) {
                final JsonArray entries = data.getJsonArray(WAFields.ENTRY);
                final int entrySize = entries.size();
                final JsonArray entryResponse = Json.array();

                for (int index = 0; index < entrySize; index++) {
                    final JsonObject entry = entries.getJsonObject(index);
                    final JsonArray changesArray = entry.getJsonArray(WAFields.CHANGES);
                    final int changesSize = changesArray.size();
                    final JsonArray changesDataResponse = Json.array();
                    final long entryId = entry.getLong(WAFields.ID);

                    if (Objects.isNull(firstEntryId)) {
                        firstEntryId = entryId;
                    }

                    for (int changesIndex = 0; changesIndex < changesSize; changesIndex++) {
                        final JsonObject changesData = changesArray.getJsonObject(changesIndex);
                        final String field = changesData.getString(WAFields.FIELD);

                        switch (field) {
                            case WAFields.MESSAGES -> handeMessageEvent(changesData, changesDataResponse);
                            case WAFields.MESSAGE_TEMPLATE_STATUS_UPDATE -> {
                                LOGGER.debug("Whatsapp: Received webhook event for template status update. External WABA Id: {}", entryId);
                                handleTemplateStatusUpdateEvent(entryId, changesData);
                            }
                            case WAFields.MESSAGE_TEMPLATE_COMPONENT_UPDATE -> {
                                LOGGER.debug("Whatsapp: Received webhook event for template component update. External WABA Id: {}", entryId);
                                handleTemplateStatusUpdateEvent(entryId, changesData);
                            }
                            case WAFields.ACCOUNT_UPDATE -> {
                                LOGGER.debug("Whatsapp: Received webhook event for account update. External WABA Id: {}", entryId);
                                handleAccountUpdateEvent(entryId, changesData, changesDataResponse);
                            }
                        }
                    }

                    if (NumericalConstant.ZERO < changesDataResponse.size()) {
                        entryResponse.put(changesDataResponse);
                    }
                }
                LOGGER.debug("Whatsapp: Webhook message events have been processed. External WABA Id: {}", firstEntryId);

                if (NumericalConstant.ZERO < entryResponse.size()) {
                    final String requestId = Base64.getEncoder().encodeToString(String.format("WA:%d", firstEntryId).getBytes());

                    publish(requestId, response.put(WAFields.ENTRIES, entryResponse));
                }
            }

        } catch (Throwable throwable) {
            LOGGER.error("Whatsapp: Failed to handle webhook data. Reason:{}, Stack:{}", throwable.getMessage(), throwable);
        } finally {
            streamMessage.clean();
        }
    }

    /**
     * <p>
     * Handles the whatsapp message webhook event
     * </p>
     *
     * @param changesData         Represents the changes data in the webhook event
     * @param changesDataResponse Represents the response
     */
    private void handeMessageEvent(final JsonObject changesData, final JsonArray changesDataResponse) {
        final JsonObject valueObject = changesData.getJsonObject(WAFields.VALUE);
        final String messagingProduct = valueObject.getString(WAFields.MESSAGING_PRODUCT);

        if (Objects.equals(messagingProduct, WAFields.WHATSAPP)) {
            final WebhookMessageEvent waMessageEvent = waWebhookMessageEventConverter.convert(valueObject);

            if (Objects.nonNull(waMessageEvent)) {
                final Long senderId = waMessageEvent.getSenderId();
                final Long receiverId = waMessageEvent.getReceiverId();

                switch (waMessageEvent.getMessageEventType()) {
                    case TEXT -> {
                        LOGGER.debug("Whatsapp: Received webhook text message event. Sender Id: {}, Receiver Id: {}", senderId, receiverId);
                        final WATextMessageEvent textMessageEvent = (WATextMessageEvent) waMessageEvent;

                        changesDataResponse.put(waWebhookInboundService.handleTextMessageEvent(textMessageEvent));
                    }
                    case READ -> {
                        LOGGER.debug("Whatsapp: Received webhook read message event. Sender Id: {}, Receiver Id: {}", senderId, receiverId);
                        final WAReadMessageEvent readMessageEvent = (WAReadMessageEvent) waMessageEvent;

                        changesDataResponse.put(waWebhookInboundService.handleReadMessageEvent(readMessageEvent));
                    }
                    case DELIVERY -> {
                        LOGGER.debug("Whatsapp: Received webhook delivery message event. Sender Id: {}, Receiver Id: {}", senderId, receiverId);
                        final WADeliveryMessageEvent deliveryMessageEvent = (WADeliveryMessageEvent) waMessageEvent;

                        changesDataResponse.put(waWebhookInboundService.handleDeliveryMessageEvent(deliveryMessageEvent));
                    }
                    case FAILED -> {
                        LOGGER.debug("Whatsapp: Received webhook failed message event. Sender Id: {}, Receiver Id: {}", senderId, receiverId);
                        final WAFailedMessageEvent failedMessageEvent = (WAFailedMessageEvent) waMessageEvent;

                        changesDataResponse.put(waWebhookInboundService.handleFailedMessageEvent(failedMessageEvent));
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Handles the whatsapp template status update webhook event
     * </p>
     *
     * @param externalWABusinessAccountId Represents the external whatsapp business account id
     * @param changesData                 Represents the changes data in the webhook event
     */
    private void handleTemplateStatusUpdateEvent(final Long externalWABusinessAccountId, final JsonObject changesData) {
        final JsonObject valueObject = changesData.getJsonObject(WAFields.VALUE);
        final WATemplateStatusUpdateEvent waTemplateStatusUpdateEvent = waTemplateUpdateEventConverter.convert(valueObject);

        waTemplateStatusUpdateEvent.setExternalWABusinessAccountId(externalWABusinessAccountId);
        waWebhookInboundService.handleTemplateStatusUpdateEvent(waTemplateStatusUpdateEvent);
    }

    /**
     * <p>
     * Handles the whatsapp account update webhook event
     * </p>
     *
     * @param externalWABusinessAccountId Represents the external whatsapp business account id
     * @param changesData                 Represents the changes data in the webhook event
     * @param changesDataResponse         Represents the response
     */
    private void handleAccountUpdateEvent(final Long externalWABusinessAccountId, final JsonObject changesData, final JsonArray changesDataResponse) {
        final JsonObject valueObject = changesData.getJsonObject(WAFields.VALUE);
        final WAAccountUpdateEvent waAccountUpdateEvent = new WAAccountUpdateEvent();

        waAccountUpdateEvent.setUpdateEvent(WAAccountUpdateEvent.UpdateEvent.valueOf(valueObject.getString(WAFields.EVENT)));
        waAccountUpdateEvent.setDisplayPhoneNumber(valueObject.getString(WAFields.PHONE_NUMBER));
        waAccountUpdateEvent.setExternalWABusinessAccountId(externalWABusinessAccountId);

        changesDataResponse.put(waWebhookInboundService.handleAccountUpdateEvent(waAccountUpdateEvent));
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
        final SubscriptionTopic responseProcessTopic = SocialQueueTopics.getWAWebhookResponseProcessTopic(QueueModel.Type.KAFKA);
        final Optional<Producer<byte[]>> optionalProducer = producerStore.getProducer(responseProcessTopic);

        optionalProducer.ifPresentOrElse(producer -> {
            producer.send(new StreamMessage<>(requestId, responseProcessTopic, dataJson.getBytes()));

            LOGGER.info("Whatsapp: WhatsApp webhook response process request published. Request ID: {}", requestId);
        }, () -> LOGGER.error("Whatsapp: Kafka producer not found for the topic:{}", responseProcessTopic.getName()));
    }
}
