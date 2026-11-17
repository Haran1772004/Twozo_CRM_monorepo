package com.tlc.analytics.cache.internal;

import com.tlc.analytics.config.manager.table.TableManager;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.analytics.cache.table.TableDetailCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * Implementation of {@link TableDetailCache} using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class TableDetailCacheImpl implements TableDetailCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(TableDetailCacheImpl.class);
    private final Cache<Integer, Collection<TableDetail>> cacheByModuleId;
    private final Cache<Long, TableDetail> cacheById;
    private final TableManager tableManager;

    private TableDetailCacheImpl() {
        this.cacheByModuleId = CacheManager.getInstance().createCache();
        this.cacheById = CacheManager.getInstance().createCache();
        this.tableManager = TableManager.INSTANCE();
    }

    private static class Instance {
        private static final TableDetailCacheImpl INSTANCE = new TableDetailCacheImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link TableDetailCacheImpl}.
     * </p>
     *
     * @return The {@link TableDetailCacheImpl} instance
     */
    public static TableDetailCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Map<Integer, Collection<TableDetail>> grouped = tableManager.fetchAllGroupedByModuleId();

        grouped.forEach((moduleId, tableDetails) -> {
            cacheByModuleId.put(moduleId, tableDetails);

            tableDetails.forEach(tableDetail -> cacheById.put(tableDetail.id(), tableDetail));
        });

        LOGGER.info("CRM Analytics : TableDetail loaded into cache successfully");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        cacheByModuleId.clear();
        cacheById.clear();
        LOGGER.info("CRM Analytics : TableDetail cache cleared");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<TableDetail> get(final Integer moduleId) {
        return cacheByModuleId.get(moduleId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TableDetail getById(final Long tableId) {
        return cacheById.get(tableId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final Integer moduleId, final Collection<TableDetail> tableDetails) {
        cacheByModuleId.put(moduleId, tableDetails);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putById(final Long tableId, final TableDetail tableDetail) {
        cacheById.put(tableId, tableDetail);
    }
}
