package com.tlc.queue.internal.manager.offset;

import com.tlc.queue.internal.model.ProcessedOffset;

/**
 * Manager to handle the processed offset related DB write operations
 *
 * @author sundar
 * @version 1.0
 */
public interface WritableProcessedOffsetManager {

    /**
     * Stores the processed offset info into the DB
     *
     * @param offsetHolder To be stored
     */
    void create(final ProcessedOffset offsetHolder);

    /**
     * Removes the processed offset from the DB
     *
     * @param offsetHolder To be removed
     */
    void remove(final ProcessedOffset offsetHolder);
}