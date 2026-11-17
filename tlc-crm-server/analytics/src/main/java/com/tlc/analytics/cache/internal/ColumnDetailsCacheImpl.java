package com.tlc.analytics.cache.internal;

import com.tlc.analytics.config.manager.table.ColumnManager;
import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.analytics.cache.column.ColumnDetailsCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementation of {@link ColumnDetailsCache} using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class ColumnDetailsCacheImpl implements ColumnDetailsCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColumnDetailsCacheImpl.class);
    private final Cache<Long, Collection<ColumnDetail>> cacheByTableId;
    private final Cache<Long, ColumnDetail> cacheById;
    private final ColumnManager columnManager;

    /**
     * <p>
     * Private constructor initializing the local caches.
     * </p>
     */
    private ColumnDetailsCacheImpl() {
        this.cacheByTableId = CacheManager.getInstance().createCache();
        this.cacheById = CacheManager.getInstance().createCache();
        this.columnManager = ColumnManager.INSTANCE();
    }

    private static class Instance {
        private static final ColumnDetailsCacheImpl INSTANCE = new ColumnDetailsCacheImpl();
    }

    /**
     * <p>
     * Retrieves the {@link ColumnDetailsCacheImpl} instance.
     * </p>
     *
     * @return {@link ColumnDetailsCacheImpl}
     */
    public static ColumnDetailsCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        columnManager.fetchAll().forEach(column -> {
            cacheById.put(column.id(), column);

            final Long tableId = column.getTableId();
            Collection<ColumnDetail> existing = cacheByTableId.get(tableId);

            if (existing == null) {
                existing = new ArrayList<>();
                cacheByTableId.put(tableId, existing);
            }

            existing.add(column);
        });

        LOGGER.info("CRM Analytics : ColumnDetail loaded into cache successfully");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        cacheByTableId.clear();
        cacheById.clear();
        LOGGER.info("CRM Analytics : ColumnDetail cache cleared");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ColumnDetail> get(final Long tableId) {
        return cacheByTableId.get(tableId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnDetail getById(final Long columnId) {
        return cacheById.get(columnId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final Long tableId, final Collection<ColumnDetail> columnDetails) {
        cacheByTableId.put(tableId, columnDetails);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putById(final Long columnId, final ColumnDetail columnDetail) {
        cacheById.put(columnId, columnDetail);
    }

}
