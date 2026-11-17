package com.tlc.queue.internal.manager.topic;

import com.tlc.queue.internal.model.TopicInfo;

import java.util.Collection;

/**
 * Manager to handle topic related the DB operations
 *
 * @author sundar
 * @version 1.0
 */
public interface TopicManager {

    /**
     * Gets the {@link TopicManager}
     *
     * @return the {@link TopicManager}
     */
    static TopicManager getInstance() {
        return TopicManagerImpl.getInstance();
    }

    /**
     * Stores the topic info if it not already exists
     *
     * @param topicNames Which contains the topic names to store
     */
    void createIfNotExist(final Collection<String> topicNames);

    /**
     * Gets all the topics info
     *
     * @return {@link Collection} of all topics
     */
    Collection<TopicInfo> getAll();
}