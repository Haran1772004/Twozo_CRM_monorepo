package com.tlc.queue.internal.manager.consumer;

import com.tlc.queue.internal.model.ConsumerGroupInfo;

import java.util.Collection;
import java.util.Optional;

/**
 * Manages database operations for the consumer
 *
 * @author sundar
 * @version 1.0
 */
public interface ConsumerGroupManager {

    /**
     * Gets the {@link ConsumerGroupManager}
     *
     * @return {@link ConsumerGroupManager}
     */
    static ConsumerGroupManager getInstance() {
        return ConsumerGroupManagerImpl.getInstance();
    }

    /**
     * Gets all consumer information
     *
     * @return the collection of {@link ConsumerGroupInfo}
     */
    Collection<ConsumerGroupInfo> getAll();

    /**
     * Creates the {@link ConsumerGroupInfo} if the given consumer group ID is not exist
     *
     * @param groupId Represents the consumer group ID
     */
    void createIfNotExist(final String groupId);

    /**
     * Gets the {@link ConsumerGroupInfo} associated with the given consumer group ID
     *
     * @param groupId Represents the consumer group ID
     * @return The {@link ConsumerGroupInfo} if exist
     */
    Optional<ConsumerGroupInfo> get(final String groupId);
}