package com.tlc.queue.kafka;

import com.tlc.commons.code.ErrorCode;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.config.consumer.ConsumerConfig;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.internal.resource.QueueErrorCodes;
import lombok.Getter;

import java.util.Collection;
import java.util.Objects;

/**
 * Holds the configuration related to the KAFKA consumer
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
public final class KafkaConsumerConfig extends KafkaModel implements ConsumerConfig {

    @Getter
    private final String groupId;
    @Getter
    private final String consumerName;
    @Getter
    private final boolean readFromLast;
    private final Collection<SubscriptionTopic> subscriptionTopics;
    private final StreamListener<?> dataProcessor;
    private final boolean isAutoCommitEnabled;
    private final PollConfig pollConfig;

    public KafkaConsumerConfig(final String groupId, final boolean readFromLast, final Collection<SubscriptionTopic> subscriptionTopics,
                               final StreamListener<?> dataProcessor, final PollConfig pollConfig) {
        this(groupId, readFromLast, subscriptionTopics, dataProcessor, pollConfig, true);
    }

    public KafkaConsumerConfig(final String groupId, final boolean readFromLast, final Collection<SubscriptionTopic> subscriptionTopics,
                               final StreamListener<?> dataProcessor, final PollConfig pollConfig, final boolean isAutoCommitEnabled) {
        this(groupId, groupId, readFromLast, subscriptionTopics, dataProcessor, pollConfig, isAutoCommitEnabled);
    }

    public KafkaConsumerConfig(final String groupId, final String consumerName, final boolean readFromLast, final Collection<SubscriptionTopic> subscriptionTopics,
                               final StreamListener<?> dataProcessor, final PollConfig pollConfig, final boolean isAutoCommitEnabled) {
        this.groupId = groupId;
        this.readFromLast = readFromLast;
        this.subscriptionTopics = subscriptionTopics;
        this.dataProcessor = dataProcessor;
        this.isAutoCommitEnabled = isAutoCommitEnabled;
        this.pollConfig = pollConfig;
        this.consumerName = consumerName;

        if (Objects.isNull(subscriptionTopics) || subscriptionTopics.isEmpty()) {
            throw ErrorCode.get(QueueErrorCodes.SUBSCRIPTION_TOPICS_SHOULD_NOT_BE_EMPTY);
        }
    }

    /**
     * Fetches the topics that have to be subscribed by the consumer
     *
     * @return Subscription topics
     */
    @Override
    public Collection<SubscriptionTopic> getSubscriptionTopics() {
        return subscriptionTopics;
    }

    /**
     * A listener that listens the records read by the consumer
     *
     * @return {@link StreamListener}
     */
    @Override
    public <T> StreamListener<T> getListener() {
        return (StreamListener<T>) this.dataProcessor;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link PollConfig}
     */
    @Override
    public PollConfig getPollConfig() {
        return pollConfig;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the auto commit is enabled; false otherwise
     */
    @Override
    public boolean isAutoCommitEnabled() {
        return isAutoCommitEnabled;
    }
}