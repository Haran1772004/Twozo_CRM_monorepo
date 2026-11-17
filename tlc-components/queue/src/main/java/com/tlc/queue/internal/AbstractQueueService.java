package com.tlc.queue.internal;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.queue.SubscriptionTopicHolder;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.consumer.BackgroundConsumer;
import com.tlc.queue.consumer.StreamConsumer;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.internal.cache.topic.TopicService;
import com.tlc.queue.service.QueueService;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * A queue service providing a general implementation partially required for various queue service implementations
 *
 * @author sundar
 * @version 1.0
 */
public abstract class AbstractQueueService implements QueueService, Service {

    private final Map<String, TopicConfig> topicConfigMap;
    private final TopicService topicService;

    protected AbstractQueueService() {
        this.topicConfigMap = new HashMap<>();
        this.topicService = TopicService.getInstance();
    }

    /**
     * Loads the topic configurations
     *
     * @param configuration Which contains the queue configurations
     */
    protected void loadTopicConfigurations(final Configurations configuration) {
        final Collection<String> topicConfValues = configuration.getValues("topics");
        final SubscriptionTopicHolderImpl subscriptionTopicHolder = (SubscriptionTopicHolderImpl) SubscriptionTopicHolder.getInstance();
        final Collection<String> topicNames = new HashSet<>();

        for (final String topicConfValue : topicConfValues) {
            final String[] topicConfig = topicConfValue.split(":");

            if (topicConfig.length == 2) {
                final String topicName = topicConfig[0].toUpperCase();
                final String topicPartition = topicConfig[1];

                topicNames.add(topicName);
                topicConfigMap.put(topicName, new TopicConfig(topicName, Integer.parseInt(topicPartition)));
                subscriptionTopicHolder.loadSubscriptionTopic(topicName, createSubscriptionTopic(topicName));
            }
        }
        topicService.load(topicNames);
    }

    /**
     * {@inheritDoc}
     *
     * @param consumerConfig Configuration to create a new background consumer
     * @return A {@link BackgroundConsumer}
     */
    @Override
    public BackgroundConsumer createBackgroundConsumer(final ConsumerConfig consumerConfig) {
        return new StreamConsumer<byte[]>(consumerConfig) {

            /**
             * {@inheritDoc}
             *
             * @param streamMessage Represents the {@link StreamMessage} to be processed
             */
            @Override
            public void consume(final StreamMessage<byte[]> streamMessage) {
                final StreamListener<byte[]> listener = consumerConfig.getListener();

                try {
                    listener.listen(streamMessage);
                } finally {
                    listener.afterListening();
                }
            }
        };
    }

    /**
     * Gets the configured topic names
     *
     * @return the topic names
     */
    protected Collection<TopicConfig> getTopicConfigs() {
        return topicConfigMap.values();
    }

    /**
     * Creates the {@link SubscriptionTopic} for the given topic name
     *
     * @param topicName Represents the name of the topic
     * @return the appropriate {@link SubscriptionTopic}
     */
    protected abstract SubscriptionTopic createSubscriptionTopic(final String topicName);
}