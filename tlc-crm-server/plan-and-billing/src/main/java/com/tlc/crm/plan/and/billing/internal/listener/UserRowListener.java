package com.tlc.crm.plan.and.billing.internal.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.common.models.Action;

import com.tlc.crm.plan.and.billing.fields.PaymentFields;
import com.tlc.crm.plan.and.billing.queue.SubscriptionQueueTopics;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.QueueService;
import com.tlc.queue.service.provider.QueueServiceProvider;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowAddListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

/**
 * <p>
 * A Listener that listens create/delete actions of user entry.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class UserRowListener implements RowAddListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRowListener.class);

    private final QueueService queueService;
    private final ConfHolder confHolder;

    private final static class Instance {
        private static final UserRowListener USER_ROW_LISTENER = new UserRowListener();
    }

    public static UserRowListener getInstance() {
        return Instance.USER_ROW_LISTENER;
    }

    private UserRowListener() {
        queueService = QueueServiceProvider.get();
        confHolder = ConfHolder.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param userRows Refers the collection of user rows.
     */
    @Override
    public void addRows(final Collection<Row> userRows) {
        if (isPayerNeeded()) {
            final Optional<Long> optionalOrgId = userRows.stream().findFirst().map(Row::getOrgId);
            final Collection<Long> userIds = collectUserIds(userRows);

            optionalOrgId.ifPresent(orgId -> publish(orgId, userIds, Action.CREATE));
        }
    }

    /**
     * <p>
     * Collects the user ids from the user rows.
     * </p>
     *
     * @param userRows Represents the user rows.
     * @return Collection of user ids.
     */
    private Collection<Long> collectUserIds(final Collection<Row> userRows) {
        final Collection<Long> userIds = new HashSet<>();

        for (final Row userRow : userRows) {
            userIds.add(userRow.getPKValue());
        }
        return userIds;
    }

    /**
     * <p>
     * Publishes payer data to the provider.
     * </p>
     *
     * @param orgId   Represents the organisation ID.
     * @param userIds Represents the user ids.
     * @param action  Represents {@link Action}.
     */
    private void publish(final Long orgId, final Collection<Long> userIds, final Action action) {
        final String orgIdAsString = String.valueOf(orgId);
        final JsonObject jsonObject = Json.object();

        jsonObject.put(PaymentFields.ORG_ID, orgId);
        jsonObject.put(PaymentFields.USER_IDS, userIds);
        final byte[] bytes = jsonObject.getBytes();

        switch (action) {
            case CREATE -> {
                final SubscriptionTopic payerCreateEventTopic = SubscriptionQueueTopics.getPayerCreateEventTopic(QueueModel.Type.KAFKA);
                final KafkaProducerConfig payerCreateEventProducerConfig = new KafkaProducerConfig(payerCreateEventTopic);

                try (final Producer<byte[]> payerCreateEventProducer = queueService.createProducer(payerCreateEventProducerConfig)) {
                    payerCreateEventProducer.send(new StreamMessage<>(orgIdAsString, payerCreateEventTopic, bytes));
                    LOGGER.info("Payment Gateway: Published the payer creation data, Org ID: {}, User IDs: {}", orgId, userIds);
                }
            }
        }
    }

    /**
     * <p>
     * Checks whether the payer creation/deletion is needed or not.
     * </p>
     *
     * @return A flag that represents the payer creation/deletion is needed or not.
     */
    private boolean isPayerNeeded() {
        final Configurations paymentGatewayConfig = confHolder.getProperties(PaymentFields.PAYMENT_GATEWAY);

        return Boolean.parseBoolean(paymentGatewayConfig.getConfig(PaymentFields.NEED_PAYER_CREATION));
    }
}
