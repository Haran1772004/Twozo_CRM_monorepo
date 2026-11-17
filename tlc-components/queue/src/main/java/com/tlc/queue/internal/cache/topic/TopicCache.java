package com.tlc.queue.internal.cache.topic;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.queue.internal.cache.LocalCache;
import com.tlc.queue.internal.model.TopicInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Caches topic information in the local heap memory
 *
 * @author sundar
 * @version 1.0
 */
final class TopicCache implements LocalCache<String, TopicInfo> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopicCache.class);

    private final Cache<String, TopicInfo> topicsByName;
    private final Cache<Long, TopicInfo> topicsById;

    TopicCache() {
        this.topicsByName = CacheManager.getInstance().createCache();
        this.topicsById = CacheManager.getInstance().createCache();
    }

    /**
     * {@inheritDoc}
     *
     * @param topic Represents the key
     * @return
     */
    @Override
    public TopicInfo get(final String topic) {
        return topicsByName.get(topic);
    }

    /**
     * {@inheritDoc}
     *
     * @param value the value to be loaded
     */
    @Override
    public void load(final TopicInfo value) {

        if (Objects.nonNull(value)) {
            load(List.of(value));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param topicInfos Which contains the values to be stored
     */
    @Override
    public void load(final Collection<TopicInfo> topicInfos) {

        if (Objects.isNull(topicInfos) || topicInfos.isEmpty()) {
            return;
        }
        LOGGER.info("Queue topic load: Topics to be loaded into cache: {}", topicInfos);
        for (final TopicInfo topicInfo : topicInfos) {
            topicsByName.put(topicInfo.name(), topicInfo);
            topicsById.put(topicInfo.id(), topicInfo);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        topicsById.clear();
        topicsByName.clear();
    }

    /**
     * Checks whether the caches are empty or not
     *
     * @return true if the topics by ID and name are empty; false otherwise
     */
    boolean isEmpty() {
        final Map<Long, TopicInfo> allTopicsById = topicsById.getAll();
        final Map<String, TopicInfo> allTopicsByName = topicsByName.getAll();

        return (Objects.isNull(allTopicsById) || allTopicsById.isEmpty()) && (Objects.isNull(allTopicsByName) || allTopicsByName.isEmpty());
    }

    /**
     * Gets the {@link TopicInfo} associated with its ID
     *
     * @param id Represents the topic info ID
     * @return the {@link TopicInfo}
     */
    TopicInfo get(final long id) {
        return topicsById.get(id);
    }
}