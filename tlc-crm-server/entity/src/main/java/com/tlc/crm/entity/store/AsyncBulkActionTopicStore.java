package com.tlc.crm.entity.store;

import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.models.Action;
import com.tlc.queue.QueueModel;
import com.tlc.queue.SubscriptionTopicHolder;
import com.tlc.queue.config.SubscriptionTopic;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Storing and managing topics related to asynchronous bulk actions
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public final class AsyncBulkActionTopicStore {

    private static final String ASYNC_BULK_ACTION_NAMES = "asyncBulkActionNames";
    private static final String TOPIC = "topic";
    private static final String EVENT = "event";

    private final SubscriptionTopicHolder subscriptionTopicHolder;
    private final Map<String, Map<Action, SubscriptionTopic>> subscriptionTopicMap;

    private AsyncBulkActionTopicStore() {
        subscriptionTopicMap = new HashMap<>();
        subscriptionTopicHolder = SubscriptionTopicHolder.getInstance();
    }

    private static class InstanceHolder {
        private static final AsyncBulkActionTopicStore INSTANCE = new AsyncBulkActionTopicStore();
    }

    /**
     * <p>
     * Gets the {@link AsyncBulkActionTopicStore} instance
     * </p>
     *
     * @return The {@link AsyncBulkActionTopicStore}
     */
    public static AsyncBulkActionTopicStore getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Adds the topics for the given {@link Configurations}
     * </p>
     *
     * @param configurations Represents the {@link Configurations}
     */
    public void add(final Configurations configurations) {
        final Collection<String> asyncBulkActionNames = configurations.getValues(ASYNC_BULK_ACTION_NAMES);

        for (final String asyncBulkActionName : asyncBulkActionNames) {
            final Collection<Map<String, String>> groupedConfigArray = configurations.groupedConfigArray(asyncBulkActionName);
            final Map<Action, SubscriptionTopic> actionTopics = subscriptionTopicMap
                    .computeIfAbsent(asyncBulkActionName, actionMap -> new EnumMap<>(Action.class));

            for (final Map<String, String> eventMap : groupedConfigArray) {
                actionTopics.put(Action.valueOf(eventMap.get(EVENT).toUpperCase()), subscriptionTopicHolder.getSubscriptionTopic(QueueModel.Type.KAFKA,
                        eventMap.get(TOPIC).toUpperCase()));
            }
        }
    }

    /**
     * <p>
     * Gets the {@link SubscriptionTopic} for the given action
     * </p>
     *
     * @param name   Represents the async bulk action name
     * @param action Represents an {@link Action}
     * @return The {@link SubscriptionTopic}
     */
    public Optional<SubscriptionTopic> getSubscriptionTopic(final String name, final Action action) {
        return Optional.ofNullable(subscriptionTopicMap.getOrDefault(name, Collections.emptyMap()).get(action));
    }

    /**
     * <p>
     * Clears the subscriptions topics
     * </p>
     */
    public void clear() {
        subscriptionTopicMap.clear();
    }
}
