package com.tlc.queue.internal.cache.topic;

import com.tlc.queue.internal.model.TopicInfo;

import java.util.Collection;

/**
 * Provides services to access {@link TopicInfo}
 *
 * @author sundar
 * @version 1.0
 */
public interface TopicService {

    /**
     * Gets the {@link TopicService}
     *
     * @return {@link TopicService}
     */
    static TopicService getInstance() {
        return TopicServiceImpl.getInstance();
    }

    /**
     * Loads all the topics information from the DB to cache
     */
    void load(final Collection<String> topicNames);

    /**
     * Gets the {@link TopicInfo} associated with the given topic name
     *
     * @param topicName Represents the topic name
     * @return {@link TopicInfo} associated with the given topic name
     */
    TopicInfo get(final String topicName);

    /**
     * Gets the {@link TopicInfo} associated with the given topic ID
     *
     * @param id Represents the topic id
     * @return {@link TopicInfo} associated with the given topic ID
     */
    TopicInfo get(final long id);

    /**
     * Clears the topic info cache
     */
    void clearCache();
}