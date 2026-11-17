package com.tlc.crm.core.features.internal.quota.listener;

import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.action.parser.DataParser;
import com.tlc.crm.core.action.parser.impl.NativeParser;
import com.tlc.crm.core.api.deal.manager.DealManager;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A post event listener which listens to the events related to the deal and sends the quota related events to the respective topic.
 *
 * @author kishore J
 * @version 1.0
 */
public class QuotaDealPostEventListener implements PostEventListener {

    private static final Set<String> DEAL_UPDATE_APIS = Set.of("updateDealState", "updatePipeline", "updateExpectedCloseDate", "updateDealStage", "revokeDealCommit", "commitDeal", "transferUserRecords");

    private final DataParser dataParser;
    private final ProducerStore producerStore;
    private final DealManager dealManager;

    private QuotaDealPostEventListener() {
        dataParser = NativeParser.getInstance();
        producerStore = ProducerStore.getInstance();
        dealManager = DealManager.getInstance();
    }

    private static class InstanceHolder {
        private static final QuotaDealPostEventListener INSTANCE = new QuotaDealPostEventListener();
    }

    /**
     * Gets the instance of QuotaPostEventListener.
     *
     * @return The instance of QuotaPostEventListener.
     */
    public static QuotaDealPostEventListener getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents {@link EventData}.
     */
    @Override
    public void processPostEvent(final EventData eventData) {

        if (Objects.equals(eventData.getAction(), Action.DELETE)) {
            return;
        }
        final Collection<? extends TlcModel> models = eventData.getValues();

        if (models.stream().anyMatch(Deal.class::isInstance)) {
            final SubscriptionTopic subscriptionTopic = CoreQueueTopics.getQuotaDealEventTopic(QueueModel.Type.KAFKA);

            producerStore.getProducer(subscriptionTopic).ifPresent(producer -> {
                final long orgId = eventData.getOrgId();
                final long userId = eventData.getUserId();
                final Map<Long, TlcModel> previousValues = eventData.getPreviousValues();
                final boolean previousValueIsPresent = Objects.nonNull(previousValues);
                final String apiType = eventData.getApiType();
                final Collection<? extends TlcModel> currentModels = Objects.nonNull(apiType) && DEAL_UPDATE_APIS.contains(apiType) ? dealManager.get(orgId,
                        models.stream().map(TlcModel::id).collect(Collectors.toSet())) : models;

                for (final TlcModel model : currentModels) {
                    final Long id = model.id();
                    final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s", orgId, userId).getBytes());
                    final Map<String, Object> payload = new HashMap<>();

                    payload.put(QuotaFields.MODEL, model);

                    if (previousValueIsPresent) {
                        final TlcModel existingModel = previousValues.get(id);

                        if (Objects.nonNull(existingModel)) {
                            payload.put(QuotaFields.EXISTING_MODEL, existingModel);
                        }
                    }
                    payload.put(ActionFields.ACTION, eventData.getAction());
                    producer.send(new StreamMessage<>(requestId, subscriptionTopic, dataParser.serialize(payload)));
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Event}
     */
    @Override
    public Event event() {
        return CoreEvent.QUOTA_DEAL_EVENT;
    }
}
