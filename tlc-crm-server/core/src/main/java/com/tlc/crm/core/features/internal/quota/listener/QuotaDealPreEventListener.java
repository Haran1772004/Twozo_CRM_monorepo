package com.tlc.crm.core.features.internal.quota.listener;

import com.tlc.crm.common.event.listener.PreEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.action.parser.DataParser;
import com.tlc.crm.core.action.parser.impl.NativeParser;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
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
 * A pre event listener which listens to the events related to the deal and performs the necessary operations before the event is processed.
 * Handles deal deletion by removing quota entries and decreasing respective quota data.
 *
 * @author kishore J
 * @version 1.0
 */
public class QuotaDealPreEventListener implements PreEventListener {

    private final DataParser dataParser;
    private final ProducerStore producerStore;

    private QuotaDealPreEventListener() {
        dataParser = NativeParser.getInstance();
        producerStore = ProducerStore.getInstance();
    }

    private static class InstanceHolder {
        private static final QuotaDealPreEventListener INSTANCE = new QuotaDealPreEventListener();
    }

    /**
     * Gets the instance of QuotaPostEventListener.
     *
     * @return The instance of QuotaPostEventListener.
     */
    public static QuotaDealPreEventListener getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents {@link EventData}.
     */
    @Override
    public void processPreEvent(final EventData eventData) {

        if (Objects.equals(eventData.getAction(), Action.DELETE)) {
            final Collection<? extends TlcModel> models = eventData.getValues();

            if (models.stream().anyMatch(Deal.class::isInstance)) {
                final SubscriptionTopic subscriptionTopic = CoreQueueTopics.getQuotaDealEventTopic(QueueModel.Type.KAFKA);

                producerStore.getProducer(subscriptionTopic).ifPresent(producer -> {
                    final long orgId = eventData.getOrgId();
                    final long userId = eventData.getUserId();

                    for (final TlcModel model : models) {
                        final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s", orgId, userId).getBytes());
                        final Map<String, Object> payload = new HashMap<>();

                        payload.put(QuotaFields.MODEL, model);

                        payload.put(ActionFields.ACTION, eventData.getAction());
                        producer.send(new StreamMessage<>(requestId, subscriptionTopic, dataParser.serialize(payload)));
                    }
                });
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Event}.
     */
    @Override
    public Event event() {
        return CoreEvent.QUOTA_DEAL_EVENT;
    }
}
