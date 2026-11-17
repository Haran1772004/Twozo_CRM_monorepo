package com.tlc.email.common.publish;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.service.Codec;
import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.email.common.codec.EmailEventRequestCodec;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.AbstractEmailEvent;
import com.tlc.email.common.model.EmailDeleteEvent;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.ThreadHolder;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.queue.EmailQueueTopics;
import com.tlc.email.common.request.EmailEventRequest;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>
 * Responsible for publishing {@link EmailEventRequest} events to Kafka.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailEventRequestPublisher extends AbstractEmailRequestPublisher<EmailEventRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailEventRequestPublisher.class);

    private final Codec<EmailEventRequest> emailWebhookRequestCodec;

    private EmailEventRequestPublisher() {
        this.emailWebhookRequestCodec = EmailEventRequestCodec.getInstance();
    }

    private static class Instance {
        private static final EmailEventRequestPublisher INSTANCE = new EmailEventRequestPublisher();
    }

    public static EmailEventRequestPublisher getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Publishes the given {@link EmailEventRequest}.
     * </p>
     *
     * @param emailEventRequest The webhook request to publish.
     */
    @Override
    public void publish(final EmailEventRequest emailEventRequest) {
        final AbstractEmailEvent createEvent = emailEventRequest.getCreateEvent();
        final AbstractEmailEvent updateEvent = emailEventRequest.getUpdateEvent();
        final EmailDeleteEvent deleteEvent = emailEventRequest.getDeleteEvent();

        final Collection<Thread> createdThreads = Optional.ofNullable(createEvent)
                .map(AbstractEmailEvent::getThreadHolder)
                .map(ThreadHolder::getThreads)
                .orElseGet(Collections::emptyList);

        final Collection<Thread> updatedThreads = Optional.ofNullable(updateEvent)
                .map(AbstractEmailEvent::getThreadHolder)
                .map(ThreadHolder::getThreads)
                .orElseGet(Collections::emptyList);

        final Collection<Thread> threadsToBeCreated = new HashSet<>(createdThreads);

        final Collection<Thread> allThreads = Stream.of(createdThreads, updatedThreads)
                .flatMap(Collection::stream)
                .toList();

        try {
            final SubscriptionTopic topic = EmailQueueTopics.getAutoSyncTopic(QueueModel.Type.KAFKA, EmailQueueTopics.EventAction.LOAD);
            final SyncAddressInfo syncAddressInfo = emailEventRequest.getReceiverConfiguration().getSyncEmailAddress();
            final String requestId = Base64.getEncoder().encodeToString(syncAddressInfo.getEmailAddress().getBytes());
            final Producer<byte[]> producer = emailKafkaProducer.getProducer(topic);

            emailEventRequest.setDeleteEvent(null);

            JavaUtil.splitCollection(allThreads, NumericalConstant.TEN).forEach(batchThreads -> {
                createdThreads.clear();
                updatedThreads.clear();

                batchThreads.forEach(thread -> {
                    if (threadsToBeCreated.contains(thread)) {
                        createdThreads.add(thread);
                    } else {
                        updatedThreads.add(thread);
                    }
                });

                producer.send(new StreamMessage<>(requestId, topic, emailWebhookRequestCodec.encode(emailEventRequest)));
            });

            Optional.ofNullable(deleteEvent)
                    .map(EmailDeleteEvent::ids)
                    .filter(ids -> !ids.isEmpty())
                    .ifPresent(ids -> {
                        createdThreads.clear();
                        updatedThreads.clear();

                        emailEventRequest.setDeleteEvent(deleteEvent);
                        producer.send(new StreamMessage<>(requestId, topic, emailWebhookRequestCodec.encode(emailEventRequest)));
                    });
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Email Webhook Action: Webhook request publish action failed: message: {}, trace: {}",
                    throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_WEBHOOK_REQUEST_PUBLISH_ACTION_FAILED, throwable);
        }
    }
}