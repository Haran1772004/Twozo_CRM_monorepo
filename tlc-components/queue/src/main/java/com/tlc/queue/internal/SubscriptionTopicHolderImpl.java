package com.tlc.queue.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.queue.QueueModel;
import com.tlc.queue.SubscriptionTopicHolder;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.internal.resource.QueueErrorCodes;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds the configured collection of {@link SubscriptionTopic}
 *
 * @author sundar
 * @version 1.0
 * @see SubscriptionTopicHolder
 */
public class SubscriptionTopicHolderImpl implements SubscriptionTopicHolder {

    private final Map<QueueModel.Type, Map<String, SubscriptionTopic>> subscriptionTopics;

    private static class Instance {
        private static final SubscriptionTopicHolder SUBSCRIPTION_TOPIC_HOLDER = new SubscriptionTopicHolderImpl();
    }

    private SubscriptionTopicHolderImpl() {
        this.subscriptionTopics = new EnumMap<>(QueueModel.Type.class);
    }

    /**
     * Gets the {@link SubscriptionTopicHolder} instance
     *
     * @return the {@link SubscriptionTopicHolder}
     */
    public static SubscriptionTopicHolder getInstance() {
        return Instance.SUBSCRIPTION_TOPIC_HOLDER;
    }

    /**
     * {@inheritDoc}
     *
     * @param type      Represents the queue model type
     * @param topicName Represents the topic name
     * @return the {@link SubscriptionTopic}
     */
    @Override
    public SubscriptionTopic getSubscriptionTopic(final QueueModel.Type type, final String topicName) {

        if (subscriptionTopics.containsKey(Objects.requireNonNull(type))) {
            final Map<String, SubscriptionTopic> typeWiseSubscriptionTopicMap = subscriptionTopics.get(type);
            final String topicNameInUpperCase = Objects.requireNonNull(topicName).toUpperCase();

            if (typeWiseSubscriptionTopicMap.containsKey(topicNameInUpperCase)) {
                return typeWiseSubscriptionTopicMap.get(topicName);
            }
        }
        throw ErrorCode.get(QueueErrorCodes.TOPIC_NOT_FOUND);
    }

    /**
     * Loads the {@link SubscriptionTopic}
     *
     * @param topicName         Represents the topic name
     * @param subscriptionTopic {@link SubscriptionTopic} to be loaded
     */
    void loadSubscriptionTopic(final String topicName, final SubscriptionTopic subscriptionTopic) {
        subscriptionTopics.computeIfAbsent(Objects.requireNonNull(subscriptionTopic).getType(), nonExistingType -> new HashMap<>()).putIfAbsent(Objects.requireNonNull(topicName), subscriptionTopic);
    }
}