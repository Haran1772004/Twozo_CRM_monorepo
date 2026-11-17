package com.tlc.queue.internal.cache.offset;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.queue.internal.OffsetHolder;
import com.tlc.queue.internal.manager.offset.ProcessedOffsetManager;
import com.tlc.queue.internal.model.ProcessedOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * A resolver that acts as a mediator for accessing {@link ProcessedOffset} from the cache and database
 *
 * @author sundar
 * @version 1.0
 */
final class ProcessedOffsetResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessedOffsetResolver.class);

    private final ProcessedOffsetCache offsetCache;
    private final ProcessedOffsetManager processedOffsetManager;
    private final LockStore<String> lockStore;

    ProcessedOffsetResolver() {
        this.offsetCache = new ProcessedOffsetCache();
        this.processedOffsetManager = ProcessedOffsetManager.getInstance();
        this.lockStore = new LockStore<>(5);
    }

    /**
     * Stores the offset holder into the cache and DB
     *
     * @param groupId      Represents the consumer group ID
     * @param offsetHolder The offset holder to be stored
     */
    void add(final String groupId, final OffsetHolder offsetHolder) {
        offsetCache.add(groupId, offsetHolder);
    }

    /**
     * Removes the offset holder from the cache and DB
     *
     * @param groupId      Represents the consumer group ID
     * @param offsetHolder The offset holder to be removed
     */
    void remove(final String groupId, final OffsetHolder offsetHolder) {
        offsetCache.remove(groupId, offsetHolder);
    }

    /**
     * Gets the collection of offset holders associated with the given consumer group ID, topic and partition
     *
     * @param groupId   Represents the consumer group ID
     * @param topic     Represents the topic name
     * @param partition Represents the partition
     * @return The offset holders associated with the given consumer group ID, topic and partition
     */
    Collection<OffsetHolder> getOffsets(final String groupId, final String topic, final int partition) {

        try {
            final ReentrantReadWriteLock lock = lockStore.getLock(groupId);
            final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
            final Collection<OffsetHolder> existingOffsetHolders;

            readLock.lock();

            try {
                existingOffsetHolders = offsetCache.get(groupId, topic, partition);
            } finally {
                readLock.unlock();
            }

            if (existingOffsetHolders.isEmpty()) {
                final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

                writeLock.lock();

                try {
                    final Collection<OffsetHolder> offsetHolders = fetchOffsetHolders(groupId, topic, partition);

                    offsetCache.add(groupId, offsetHolders);
                    return offsetHolders;
                } finally {
                    writeLock.unlock();
                }
            }
            return existingOffsetHolders;
        } catch (Throwable e) {
            LOGGER.error("Queue: Processed offset retrieval from the cache failed, Consumer group ID: {}, Topic: {}, Partition: {}, Message: {}", groupId, topic, partition, e.getMessage(), e);
            return fetchOffsetHolders(groupId, topic, partition);
        }
    }

    /**
     * Checks whether the given {@link OffsetHolder} associated with the given consumer group ID is available in the cache or not
     *
     * @param consumerGroupId Represents the consumer group ID
     * @param offsetHolder    Represents the offset holder
     * @return true if the {@link OffsetHolder} associated with the given consumer group ID is available in the cache, false otherwise
     */
    boolean exists(final String consumerGroupId, final OffsetHolder offsetHolder) {

        try {
            return getOffsets(consumerGroupId, offsetHolder.topic(), offsetHolder.partition()).contains(offsetHolder);
        } catch (Throwable e) {
            LOGGER.error("Queue: Processed offset contains check on the cache failed, Consumer group ID: {}, Offset: {}, Message: {}", consumerGroupId, offsetHolder, e.getMessage(), e);
            return processedOffsetManager.exists(consumerGroupId, offsetHolder);
        }
    }

    /**
     * Fetches the processed offset holders associated with the given consumer group ID, topic and partition
     *
     * @param groupId   Represents the consumer group ID
     * @param topic     Represents the topic name
     * @param partition Represents the partition
     * @return the collection of offset holders
     */
    private Collection<OffsetHolder> fetchOffsetHolders(final String groupId, final String topic, final int partition) {
        return processedOffsetManager.get(groupId, topic, partition).stream().map(ProcessedOffset::offsetHolder).collect(Collectors.toUnmodifiableSet());
    }
}