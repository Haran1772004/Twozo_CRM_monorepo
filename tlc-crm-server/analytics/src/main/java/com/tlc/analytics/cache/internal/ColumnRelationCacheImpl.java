package com.tlc.analytics.cache.internal;

import com.tlc.analytics.config.manager.table.ColumnRelationManager;
import com.tlc.analytics.config.model.table.ColumnRelation;
import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.analytics.cache.column.ColumnRelationCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementation of {@link ColumnRelationCache} using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class ColumnRelationCacheImpl implements ColumnRelationCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColumnRelationCacheImpl.class);
    private final Cache<Long, Collection<ColumnRelation>> cacheByRelationId;
    private final Cache<Long, ColumnRelation> cacheById;
    private final ColumnRelationManager columnRelationManager;

    private ColumnRelationCacheImpl() {
        this.cacheByRelationId = CacheManager.getInstance().createCache();
        this.cacheById = CacheManager.getInstance().createCache();
        this.columnRelationManager = ColumnRelationManager.INSTANCE();
    }

    private static class Instance {
        private static final ColumnRelationCacheImpl INSTANCE = new ColumnRelationCacheImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link ColumnRelationCacheImpl}.
     * </p>
     *
     * @return The {@link ColumnRelationCacheImpl} instance
     */
    public static ColumnRelationCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        columnRelationManager.fetchAll().forEach(relatedColumn -> {
            cacheById.put(relatedColumn.id(), relatedColumn);

            final Long relationId = relatedColumn.getRelId();
            Collection<ColumnRelation> existing = cacheByRelationId.get(relationId);

            if (existing == null) {
                existing = new ArrayList<>();
                cacheByRelationId.put(relationId, existing);
            }

            existing.add(relatedColumn);
        });

        LOGGER.info("CRM Analytics : ColumnRelation loaded into cache successfully");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        cacheByRelationId.clear();
        cacheById.clear();
        LOGGER.info("CRM Analytics : ColumnRelation cache cleared");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ColumnRelation> get(final Long relationId) {
        return cacheByRelationId.get(relationId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnRelation getById(final Long columnRelationId) {
        return cacheById.get(columnRelationId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final Long relationId, final Collection<ColumnRelation> columnRelations) {
        cacheByRelationId.put(relationId, columnRelations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putById(final Long columnRelationId, final ColumnRelation columnRelation) {
        cacheById.put(columnRelationId, columnRelation);
    }

}
