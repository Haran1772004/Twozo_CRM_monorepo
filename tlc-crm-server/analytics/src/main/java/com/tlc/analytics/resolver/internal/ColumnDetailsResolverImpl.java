package com.tlc.analytics.resolver.internal;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.manager.table.ColumnManager;
import com.tlc.analytics.cache.column.ColumnDetailsCache;
import com.tlc.analytics.resolver.column.ColumnDetailsResolver;

import java.util.Collection;
import java.util.Objects;

/**
 * Implementation of {@link ColumnDetailsResolver} that handles cache-miss resolution.
 * Checks cache first; on cache miss, fetches from DB via {@link ColumnManager} and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class ColumnDetailsResolverImpl implements ColumnDetailsResolver {
    private final ColumnDetailsCache columnDetailsCache;
    private final ColumnManager columnManager;

    /**
     * <p>
     * Private constructor initializing the cache and column manager dependencies.
     * </p>
     */
    private ColumnDetailsResolverImpl() {
        this.columnDetailsCache = ColumnDetailsCache.getInstance();
        this.columnManager = ColumnManager.INSTANCE();
    }

    private static class Instance {
        private static final ColumnDetailsResolverImpl INSTANCE = new ColumnDetailsResolverImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link ColumnDetailsResolverImpl}.
     * </p>
     *
     * @return {@link ColumnDetailsResolverImpl}
     */
    public static ColumnDetailsResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ColumnDetail> getBySourceId(final Long tableId) {
        final Collection<ColumnDetail> cached = columnDetailsCache.get(tableId);

        if (Objects.nonNull(cached) && !cached.isEmpty()) {
            return cached;
        }

        final Collection<ColumnDetail> columnDetails = columnManager.fetchByTableId(tableId);

        if (!columnDetails.isEmpty()) {
            columnDetailsCache.put(tableId, columnDetails);
        }

        return columnDetails;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnDetail getById(final Long columnId) {
        final ColumnDetail cached = columnDetailsCache.getById(columnId);

        if (Objects.nonNull(cached)) {
            return cached;
        }

        final ColumnDetail columnDetail = columnManager.fetchByColumnId(columnId);

        if (Objects.nonNull(columnDetail)) {
            columnDetailsCache.putById(columnId, columnDetail);
        }

        return columnDetail;
    }
}
