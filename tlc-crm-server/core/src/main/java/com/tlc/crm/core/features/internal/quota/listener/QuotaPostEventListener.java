package com.tlc.crm.core.features.internal.quota.listener;

import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.core.action.parser.DataParser;
import com.tlc.crm.core.action.parser.impl.NativeParser;
import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
import com.tlc.crm.core.features.api.quota.model.QuotaHolder;
import com.tlc.crm.core.internal.event.CoreEvent;
import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.crm.notification.app.Event;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.ProducerStore;
import com.tlc.validator.TlcModel;

import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A post event listener which listens to the events related to the quota and performs the necessary operations after the event is processed.
 *
 * @author kishore J
 * @version 1.0
 */
public class QuotaPostEventListener implements PostEventListener {

    private final DataParser dataParser;
    private final ProducerStore producerStore;

    private QuotaPostEventListener() {
        dataParser = NativeParser.getInstance();
        producerStore = ProducerStore.getInstance();
    }

    private static class InstanceHolder {
        private static final QuotaPostEventListener INSTANCE = new QuotaPostEventListener();
    }

    /**
     * Gets the instance of QuotaPostEventListener.
     *
     * @return The instance of QuotaPostEventListener.
     */
    public static QuotaPostEventListener getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents {@link EventData}.
     */
    @Override
    public void processPostEvent(final EventData eventData) {

        final Collection<? extends TlcModel> models = eventData.getValues();

        if (models.stream().anyMatch(QuotaHolder.class::isInstance)) {
            final SubscriptionTopic subscriptionTopic = CoreQueueTopics.getQuotaEventTopic(QueueModel.Type.KAFKA);

            producerStore.getProducer(subscriptionTopic).ifPresent(producer -> {
                final long orgId = eventData.getOrgId();
                final long userId = eventData.getUserId();
                final Map<Long, TlcModel> previousValues = eventData.getPreviousValues();
                final boolean previousValueIsPresent = Objects.nonNull(previousValues);

                for (final TlcModel model : models) {
                    final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s", orgId, userId).getBytes());
                    final Map<String, Object> payload = new HashMap<>();

                    if (previousValueIsPresent) {
                        final TlcModel existingModel = previousValues.get(model.id());

                        if (Objects.nonNull(existingModel)) {
                            payload.put(QuotaFields.EXISTING_MODEL, existingModel);
                        }
                    }
                    payload.put(QuotaFields.MODEL, model);
                    payload.put(ActionFields.ACTION, eventData.getAction());
                    producer.send(new StreamMessage<>(requestId, subscriptionTopic, dataParser.serialize(payload)));
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The event which this listener listens to.
     */
    @Override
    public Event event() {
        return CoreEvent.QUOTA_EVENT;
    }
}
