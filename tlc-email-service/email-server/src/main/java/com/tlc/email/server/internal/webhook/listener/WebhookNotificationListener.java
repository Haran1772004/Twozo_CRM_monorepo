package com.tlc.email.server.internal.webhook.listener;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.email.server.fields.WebhookReceiverFields;
import com.tlc.email.server.service.WebhookNotificationHandler;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import com.tlc.sync.service.oauth.model.provider.EmailProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens incoming webhook events from a message stream and forwarding them for processing.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see StreamListener
 */
public class WebhookNotificationListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookNotificationListener.class);

    private final Executor taskExecutor;

    public WebhookNotificationListener(final Executor executor) {
        this.taskExecutor = executor;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param streamMessage stream message
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        taskExecutor.execute(() -> {
            try {
                final JsonObject payloadJson = Json.object(streamMessage.data());
                final EmailProvider emailProvider = EmailProvider.valueOf(payloadJson.getShort(WebhookReceiverFields.PROVIDER_ID));

                LOGGER.info("Webhook Receiver: Executing webhook notification handling in a background thread, Provider:{}", emailProvider.getName());
                final WebhookNotificationHandler webhookNotificationHandler = WebhookNotificationHandler.get(emailProvider);

                webhookNotificationHandler.handle(payloadJson);

                LOGGER.info("Webhook Receiver: Successfully handles the webhook notification with the received payload.");
            } catch (final Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, "Webhook Receiver: Failure occurred while attempting initialization, message: {}, trace: {}",
                        throwable.getMessage(), throwable);
            } finally {
                streamMessage.clean();
            }
        });
    }
}