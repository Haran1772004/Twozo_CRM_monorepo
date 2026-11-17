package com.tlc.queue.internal.cache.consumer;

import com.tlc.queue.internal.manager.consumer.ConsumerGroupManager;
import com.tlc.queue.internal.model.ConsumerGroupInfo;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Defines the services provided by the {@link ConsumerGroupService}
 *
 * @author sundar
 * @version 1.0
 */
class ConsumerGroupServiceImpl implements ConsumerGroupService {

    private final ConsumerGroupCache consumerGroupCache;
    private final ConsumerGroupManager consumerGroupManager;
    private final Lock lock;

    private ConsumerGroupServiceImpl() {
        this.consumerGroupCache = new ConsumerGroupCache();
        this.consumerGroupManager = ConsumerGroupManager.getInstance();
        this.lock = new ReentrantLock();
    }

    private static class Instance {
        private static final ConsumerGroupService CONSUMER_GROUP_SERVICE = new ConsumerGroupServiceImpl();
    }

    /**
     * Gets the instance of {@link ConsumerGroupService}
     *
     * @return {@link ConsumerGroupService}
     */
    static ConsumerGroupService getInstance() {
        return Instance.CONSUMER_GROUP_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param groupId Represents the group ID of the consumer
     */
    @Override
    public void load(final String groupId) {
        lock.lock();

        try {
            consumerGroupManager.createIfNotExist(groupId);
            consumerGroupManager.get(groupId).ifPresent(consumerGroupCache::load);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param consumerGroupId Represents the group ID of the consumer
     * @return {@link ConsumerGroupInfo} associated with the given group ID
     */
    @Override
    public ConsumerGroupInfo get(final String consumerGroupId) {
        return consumerGroupCache.get(consumerGroupId);
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the ID of the consumer
     * @return {@link ConsumerGroupInfo} associated with the given ID
     */
    @Override
    public ConsumerGroupInfo get(final long id) {
        return consumerGroupCache.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearCache() {
        consumerGroupCache.clear();
    }
}