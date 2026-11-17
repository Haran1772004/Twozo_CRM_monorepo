package com.tlc.queue.internal.cache.topic;

import com.tlc.queue.internal.manager.topic.TopicManager;
import com.tlc.queue.internal.model.TopicInfo;

import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Defines the services offered by the {@link TopicService}
 *
 * @author sundar
 * @version 1.0
 */
class TopicServiceImpl implements TopicService {

    private final TopicCache topicCache;
    private final TopicManager topicManager;
    private final Lock lock;

    private TopicServiceImpl() {
        this.topicCache = new TopicCache();
        this.topicManager = TopicManager.getInstance();
        this.lock = new ReentrantLock();
    }

    private static class Instance {
        private static final TopicService TOPIC_SERVICE = new TopicServiceImpl();
    }

    /**
     * Gets the {@link TopicService}
     *
     * @return {@link TopicService}
     */
    static TopicService getInstance() {
        return Instance.TOPIC_SERVICE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load(final Collection<String> topicNames) {
        lock.lock();

        try {
            topicManager.createIfNotExist(topicNames);

            if (topicCache.isEmpty()) {
                topicCache.load(topicManager.getAll());
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param topicName Represents the topic name
     * @return {@link TopicInfo} associated with the given topic name
     */
    @Override
    public TopicInfo get(final String topicName) {
        return topicCache.get(topicName);
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the topic id
     * @return {@link TopicInfo} associated with the given topic ID
     */
    @Override
    public TopicInfo get(final long id) {
        return topicCache.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearCache() {
        topicCache.clear();
    }
}