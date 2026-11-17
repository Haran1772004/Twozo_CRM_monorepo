package com.tlc.queue.internal.cache.offset;

import com.tlc.queue.internal.OffsetHolder;

import java.util.SortedMap;

/**
 * Provides services to access the {@link OffsetHolder}
 *
 * @author sundar
 * @version 1.0
 */
public interface ProcessedOffsetService {

    /**
     * Gets the instance of {@link ProcessedOffsetService}
     *
     * @return the instance of {@link ProcessedOffsetService}
     */
    static ProcessedOffsetService getInstance() {
        return ProcessedOffsetServiceImpl.getInstance();
    }

    /**
     * Checks whether the given {@link OffsetHolder} is processed or not
     *
     * @param groupId      Represents the consumer group ID
     * @param offsetHolder to be checked
     * @return true if the given {@link OffsetHolder} is processed; false otherwise
     */
    boolean isProcessed(final String groupId, final OffsetHolder offsetHolder);

    /**
     * Stores the given {@link OffsetHolder} to the processed offset storage
     *
     * @param groupId      Represents the consumer group ID
     * @param offsetHolder Represents the {@link OffsetHolder}
     */
    void add(final String groupId, final OffsetHolder offsetHolder);

    /**
     * Removes the given {@link OffsetHolder} from the processed offset storage
     *
     * @param groupId      Represents the consumer group ID
     * @param offsetHolder Represents the {@link OffsetHolder}
     */
    void remove(final String groupId, final OffsetHolder offsetHolder);

    /**
     * Retrieves offset holders grouped by offset for the specified consumer group ID, topic, and partition, sorted in ascending order
     *
     * @param groupId   Represents the consumer group ID
     * @param topic     Represents the topic name
     * @param partition Represents the partition
     * @return the sorted offset holders grouped by the offset
     */
    SortedMap<Long, OffsetHolder> getSortedOffsets(final String groupId, final String topic, final int partition);
}