package com.tlc.queue.internal.cache.offset;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.queue.internal.OffsetHolder;
import com.tlc.queue.internal.cache.RemoteCache;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Stores the processed offsets for the consumer and its respective topics and partitions in the cache
 *
 * @author sundar
 * @version 1.0
 */
final class ProcessedOffsetCache implements RemoteCache<String, Set<OffsetHolder>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessedOffsetCache.class);

    private final Cache<String, Set<OffsetHolder>> processedOffsetHolders;

    ProcessedOffsetCache() {
        final CacheConfig<String, Set<OffsetHolder>> processedOffsetHoldersCacheConfig = new CacheConfig<>("PROCESSED_OFFSET_CACHE");

        this.processedOffsetHolders = CacheServiceProvider.get().createOrOpenCache(processedOffsetHoldersCacheConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param key Represents the key
     * @return the collection of {@link OffsetHolder}
     */
    public Set<OffsetHolder> get(final String key) {
        return processedOffsetHolders.get(key);
    }

    /**
     * Gets the collection of offset holders associated with the given, consumer group, topic and partition
     *
     * @param consumerGroupId Represents the consumer group ID
     * @param topic           Represents the topic name
     * @param partition       Represents the partition
     * @return the collection of offset holders associated with the given, consumer group
     */
    Collection<OffsetHolder> get(final String consumerGroupId, final String topic, final int partition) {
        final String key = buildKey(consumerGroupId, topic, partition);

        return processedOffsetHolders.containsKey(key) ? Collections.unmodifiableSet(processedOffsetHolders.get(key)) : Collections.emptyList();
    }

    /**
     * Adds the given {@link OffsetHolder} associated with the given consumer group ID to the cache
     *
     * @param consumerGroupId Represents the consumer group ID
     * @param offsetHolder    to be added to the cache
     */
    void add(final String consumerGroupId, final OffsetHolder offsetHolder) {
        final String key = buildKey(consumerGroupId, offsetHolder.topic(), offsetHolder.partition());
        final Set<OffsetHolder> offsetHolders = processedOffsetHolders.containsKey(key) ? processedOffsetHolders.get(key) : new HashSet<>();

        offsetHolders.add(offsetHolder);
        processedOffsetHolders.put(key, offsetHolders);
        LOGGER.debug("Queue: Processed Offset added to the cache, Offset: {}, Group ID: {}", offsetHolder, consumerGroupId);
    }

    /**
     * Adds the given {@link OffsetHolder} associated with the given consumer group ID to the cache
     *
     * @param groupId       Represents the consumer group ID
     * @param offsetHolders to be added to the cache
     */
    void add(final String groupId, final Collection<OffsetHolder> offsetHolders) {

        if (Objects.isNull(offsetHolders) || offsetHolders.isEmpty()) {
            return;
        }

        for (final OffsetHolder offsetHolder : offsetHolders) {
            add(groupId, offsetHolder);
        }
    }

    /**
     * Removes the specified {@link OffsetHolder} associated with the consumer group ID
     *
     * @param consumerGroupId Represents the consumer group ID
     * @param offsetHolder    Represents the offset holder
     */
    void remove(final String consumerGroupId, final OffsetHolder offsetHolder) {
        final String key = buildKey(consumerGroupId, offsetHolder.topic(), offsetHolder.partition());
        final Set<OffsetHolder> offsetHolders = get(key);

        if (Objects.nonNull(offsetHolders)) {
            offsetHolders.remove(offsetHolder);
            processedOffsetHolders.put(key, offsetHolders);
            LOGGER.debug("Queue: Processed Offset removed from the cache, Offset: {}, Group ID: {}", offsetHolder, consumerGroupId);
        }
    }

    /**
     * Builds the key using the given consumer group ID, topic name, partition
     *
     * @param consumerGroupId Represents the consumer group ID
     * @param topic           Represents the topic name
     * @param partition       Represents the partition
     * @return The key built using the given consumer group ID, topic name, partition
     */
    private String buildKey(final String consumerGroupId, @NonNull final String topic, final int partition) {
        return String.format("%s_%s_%d", consumerGroupId.toLowerCase(), topic.toLowerCase(), partition);
    }
}