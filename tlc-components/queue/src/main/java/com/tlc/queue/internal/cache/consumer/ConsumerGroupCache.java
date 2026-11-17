package com.tlc.queue.internal.cache.consumer;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.queue.internal.cache.LocalCache;
import com.tlc.queue.internal.model.ConsumerGroupInfo;

import java.util.Collection;
import java.util.Objects;

/**
 * A local cache that stores basic information about the consumer group
 *
 * @author sundar
 * @version 1.0
 * @see LocalCache
 */
final class ConsumerGroupCache implements LocalCache<String, ConsumerGroupInfo> {

    private final Cache<String, ConsumerGroupInfo> consumerGroupsByGroupId;
    private final Cache<Long, ConsumerGroupInfo> consumerGroupsById;

    ConsumerGroupCache() {
        this.consumerGroupsByGroupId = CacheManager.getInstance().createCache();
        this.consumerGroupsById = CacheManager.getInstance().createCache();
    }

    /**
     * {@inheritDoc}
     *
     * @param groupId Represents the key
     * @return the value mapped with the given key
     */
    @Override
    public ConsumerGroupInfo get(final String groupId) {
        return consumerGroupsByGroupId.get(groupId);
    }

    /**
     * {@inheritDoc}
     *
     * @param consumerGroupInfo the value to be loaded
     */
    @Override
    public void load(final ConsumerGroupInfo consumerGroupInfo) {

        if (Objects.isNull(consumerGroupInfo)) {
            return;
        }
        final String groupId = consumerGroupInfo.groupId();
        final Long consumerGroupPrimaryId = consumerGroupInfo.id();

        if (!consumerGroupsByGroupId.containsKey(groupId)) {
            consumerGroupsByGroupId.put(groupId, consumerGroupInfo);
        }

        if (!consumerGroupsById.containsKey(consumerGroupPrimaryId)) {
            consumerGroupsById.put(consumerGroupPrimaryId, consumerGroupInfo);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param values Which contains the values to be stored
     */
    @Override
    public void load(final Collection<ConsumerGroupInfo> values) {

        if (Objects.isNull(values) || values.isEmpty()) {
            return;
        }

        for (final ConsumerGroupInfo consumerGroupInfo : values) {
            load(consumerGroupInfo);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        consumerGroupsById.clear();
        consumerGroupsByGroupId.clear();
    }

    /**
     * Gets the {@link ConsumerGroupInfo} associated with the specified id
     *
     * @param id Represents the id of the consumer
     * @return The {@link ConsumerGroupInfo} associated with the specified id
     */
    ConsumerGroupInfo get(final long id) {
        return consumerGroupsById.get(id);
    }
}