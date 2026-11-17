package com.tlc.analytics.resolver.internal;

import com.tlc.analytics.config.model.table.ColumnRelation;
import com.tlc.analytics.config.manager.table.ColumnRelationManager;
import com.tlc.analytics.cache.column.ColumnRelationCache;
import com.tlc.analytics.resolver.column.ColumnRelationResolver;

import java.util.Collection;
import java.util.Objects;

/**
 * Implementation of {@link ColumnRelationResolver} that handles cache-miss resolution synchronously.
 * Checks cache first; on cache miss, fetches from DB via {@link ColumnRelationManager} and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class ColumnRelationResolverImpl implements ColumnRelationResolver {

    private final ColumnRelationCache cache;
    private final ColumnRelationManager columnRelationManager;

    /**
     * <p>
     * Private constructor initializing the cache and manager dependencies.
     * </p>
     */
    private ColumnRelationResolverImpl() {
        this.cache = ColumnRelationCache.getInstance();
        this.columnRelationManager = ColumnRelationManager.INSTANCE();
    }

    private static class Instance {
        private static final ColumnRelationResolverImpl INSTANCE = new ColumnRelationResolverImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link ColumnRelationResolverImpl}.
     * </p>
     *
     * @return {@link ColumnRelationResolverImpl}
     */
    public static ColumnRelationResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ColumnRelation> getBySourceId(final Long relationId) {
        final Collection<ColumnRelation> cached = cache.get(relationId);

        if (Objects.nonNull(cached) && !cached.isEmpty()) {
            return cached;
        }
        final Collection<ColumnRelation> data = columnRelationManager.fetchByRelId(relationId);

        if (!data.isEmpty()) {
            cache.put(relationId, data);
        }
        return data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnRelation getById(final Long columnRelationId) {
        final ColumnRelation cached = cache.getById(columnRelationId);

        if (Objects.nonNull(cached)) {
            return cached;
        }
        final ColumnRelation data = columnRelationManager.fetchByRelatedColumnId(columnRelationId);

        if (Objects.nonNull(data)) {
            cache.putById(columnRelationId, data);
        }
        return data;
    }
}
