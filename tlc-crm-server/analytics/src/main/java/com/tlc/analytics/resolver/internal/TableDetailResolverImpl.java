package com.tlc.analytics.resolver.internal;

import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.config.manager.table.TableManager;
import com.tlc.analytics.cache.table.TableDetailCache;
import com.tlc.analytics.resolver.table.TableDetailResolver;

import java.util.Collection;
import java.util.Objects;

/**
 * Implementation of {@link TableDetailResolver} that handles cache-miss resolution synchronously.
 * Checks cache first; on cache miss, fetches from DB via {@link TableManager} and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class TableDetailResolverImpl implements TableDetailResolver {

    private final TableDetailCache tableDetailCache;
    private final TableManager tableManager;

    /**
     * <p>
     * Private constructor initializing the cache and manager dependencies.
     * </p>
     */
    private TableDetailResolverImpl() {
        this.tableDetailCache = TableDetailCache.getInstance();
        this.tableManager = TableManager.INSTANCE();
    }

    private static class Instance {
        private static final TableDetailResolverImpl INSTANCE = new TableDetailResolverImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link TableDetailResolverImpl}.
     * </p>
     *
     * @return {@link TableDetailResolverImpl}
     */
    public static TableDetailResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<TableDetail> getBySourceId(final Integer moduleId) {
        final Collection<TableDetail> cached = tableDetailCache.get(moduleId);
        if (Objects.nonNull(cached) && !cached.isEmpty()) {
            return cached;
        }
        final Collection<TableDetail> data = tableManager.fetchByModuleId(moduleId);
        if (!data.isEmpty()) {
            tableDetailCache.put(moduleId, data);
        }
        return data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TableDetail getById(final Long tableId) {
        final TableDetail cached = tableDetailCache.getById(tableId);
        if (Objects.nonNull(cached)) {
            return cached;
        }
        final TableDetail data = tableManager.fetchByTableId(tableId);
        if (Objects.nonNull(data)) {
            tableDetailCache.putById(tableId, data);
        }
        return data;
    }
}
