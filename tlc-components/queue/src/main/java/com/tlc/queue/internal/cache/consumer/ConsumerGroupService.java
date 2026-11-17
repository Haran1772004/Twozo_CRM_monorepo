package com.tlc.queue.internal.cache.consumer;

import com.tlc.queue.internal.model.ConsumerGroupInfo;

/**
 * Provides services to retrieve and access ConsumerInfo.
 *
 * @author sundar
 * @version 1.0
 */
public interface ConsumerGroupService {

    /**
     * Gets the instance of {@link ConsumerGroupService}
     *
     * @return {@link ConsumerGroupService}
     */
    static ConsumerGroupService getInstance() {
        return ConsumerGroupServiceImpl.getInstance();
    }

    /**
     * Loads the {@link ConsumerGroupInfo} associated with the given groupId to the cache
     *
     * @param groupId Represents the group ID of the consumer
     */
    void load(final String groupId);

    /**
     * Gets the {@link ConsumerGroupInfo} associated with the given group ID
     *
     * @param groupId Represents the group ID of the consumer
     * @return {@link ConsumerGroupInfo} associated with the given group ID
     */
    ConsumerGroupInfo get(final String groupId);

    /**
     * Gets the {@link ConsumerGroupInfo} associated with the given ID
     *
     * @param id Represents the ID of the consumer
     * @return {@link ConsumerGroupInfo} associated with the given ID
     */
    ConsumerGroupInfo get(final long id);

    /**
     * Clears the consumer cache
     */
    void clearCache();
}