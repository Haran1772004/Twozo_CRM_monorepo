package com.tlc.analytics.cache.internal;

import com.tlc.analytics.config.manager.table.LookupManager;
import com.tlc.analytics.config.model.table.TableRelation;
import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.analytics.cache.table.TableRelationCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementation of {@link TableRelationCache} using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class TableRelationCacheImpl implements TableRelationCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(TableRelationCacheImpl.class);
    private final Cache<Long, Collection<TableRelation>> cacheByParentTableId;
    private final Cache<Long, TableRelation> cacheById;
    private final LookupManager lookupManager;

    /**
     * <p>
     * Private constructor initializing the local caches.
     * </p>
     */
    private TableRelationCacheImpl() {
        this.cacheByParentTableId = CacheManager.getInstance().createCache();
        this.cacheById = CacheManager.getInstance().createCache();
        this.lookupManager = LookupManager.INSTANCE();
    }

    private static class Instance {
        private static final TableRelationCacheImpl INSTANCE = new TableRelationCacheImpl();
    }

    public static TableRelationCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        lookupManager.fetchAll().forEach(relation -> {
            cacheById.put(relation.id(), relation);

            final Long parentTableId = relation.getParentTableId();
            Collection<TableRelation> existing = cacheByParentTableId.get(parentTableId);

            if (existing == null) {
                existing = new ArrayList<>();
                cacheByParentTableId.put(parentTableId, existing);
            }

            existing.add(relation);
        });

        LOGGER.info("CRM Analytics : TableRelation loaded into cache successfully");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        cacheByParentTableId.clear();
        cacheById.clear();
        LOGGER.info("CRM Analytics : TableRelation cache cleared");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<TableRelation> get(final Long parentTableId) {
        return cacheByParentTableId.get(parentTableId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TableRelation getById(final Long relationId) {
        return cacheById.get(relationId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final Long parentTableId, final Collection<TableRelation> tableRelations) {
        cacheByParentTableId.put(parentTableId, tableRelations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putById(final Long relationId, final TableRelation tableRelation) {
        cacheById.put(relationId, tableRelation);
    }

}
