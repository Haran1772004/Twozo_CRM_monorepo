package com.tlc.queue.internal.manager.offset;

import com.tlc.queue.internal.OffsetHolder;
import com.tlc.queue.internal.model.ProcessedOffset;

import java.util.Collection;

/**
 * Manager to handle the DB operations for processed offsets
 *
 * @author sundar
 * @version 1.0
 */
public interface ProcessedOffsetManager extends WritableProcessedOffsetManager {

    /**
     * Gets the {@link ProcessedOffsetManager}
     *
     * @return {@link ProcessedOffsetManager}
     */
    static ProcessedOffsetManager getInstance() {
        return ProcessedOffsetManagerImpl.getInstance();
    }

    /**
     * Gets the processed offsets associated with the given consumer group ID, topic and partition
     *
     * @param consumerGroupId Represents the consumer group ID
     * @param topic           Represents the topic name
     * @param partition       Represents the partition
     * @return {@link Collection} of processed offsets
     */
    Collection<ProcessedOffset> get(final String consumerGroupId, final String topic, final int partition);

    /**
     * Checks whether the given offset is exist or not
     *
     * @param consumerGroupId Represents the consumer group ID
     * @param offsetHolder    Represents the offset
     * @return true if the offset exists, false otherwise
     */
    boolean exists(final String consumerGroupId, final OffsetHolder offsetHolder);
}